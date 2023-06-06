package searchengine.parsing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.UncheckedIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.dto.index.IndexData;
import searchengine.lemmatisator.Lemmatizer;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class IndexingThread extends Thread {

    private final SiteTable siteTable;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final String absolutePath;
    private final boolean oneIndexing;
    private ForkJoinPool forkJoinPool;
    private SiteMap siteMap;
    protected final int parseTimeout;
    private final Logger logger = LoggerFactory.getLogger(IndexingThread.class);

    public IndexingThread(SiteRepository siteRepository,
                          PageRepository pageRepository,
                          LemmaRepository lemmaRepository,
                          IndexRepository indexRepository,
                          IndexData indexData) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.siteTable = indexData.getSiteTable();
        this.absolutePath = indexData.getUrl();
        this.oneIndexing = indexData.isOneIndexing();
        this.parseTimeout = indexData.getParseTimeout();
    }

    @Override
    public void run() {
        creatingOrOverwritingTables();
        if (this.isInterrupted()) {
            return;
        }
        if (oneIndexing) {
            runIndexing(absolutePath);
        } else {
            runIndexingAll();
        }
    }

    private void creatingOrOverwritingTables() {
        SiteTable siteTableBD = siteRepository.findByUrl(siteTable.getUrl());
        if (siteTableBD == null) {
            logger.info("Создание новой записи в БД");
            saveSite(siteTable);
            return;
        }
        siteTable.setId(siteTableBD.getId());
        siteTable.setStatus(StatusType.INDEXING);
        siteTable.setStatusTime(LocalDateTime.now());
        updateSiteTable(siteTable);
        if (oneIndexing) {
            String relativePath = convertPath(absolutePath);
            PageTable onePage = pageRepository.findByPathAndSiteTable_Id(relativePath, siteTable.getId());
            if (onePage == null) {
                return;
            }
            List<String> deleteLemmaList = indexRepository.findByPageTable_Id(onePage.getId());
            for (String lemma : deleteLemmaList) {
                deleteLemma(lemma, siteTable);
            }
            deletePage(onePage.getId());
        } else {
            logger.info("Удаление данных из БД");
            deleteSite(siteTable.getId());
            saveSite(siteTable);
        }
    }

    public void interruptTread() {
        logger.info("STOP потока");
        if (forkJoinPool != null) {
            forkJoinPool.shutdown();
        }
        super.interrupt();
        if (siteMap != null) {
            siteMap.cleanAllIn();
        }
    }

    public void runIndexingAll() {
        logger.info("Запуск индексации сайта");
        siteMap = new SiteMap(absolutePath, parseTimeout);
        forkJoinPool = new ForkJoinPool(6);
        StringBuilder listUrl = forkJoinPool.invoke(siteMap);
        for (String url : listUrl.toString().split("\n")) {
            if (this.isInterrupted()) {
                break;
            }
            if (url.startsWith(absolutePath.replace("www.", ""))) {
                runIndexing(url);
            }
        }
        if (!this.isInterrupted()) {
            logger.info("Запись об удачной индексации в БД");
            siteTable.setStatus(StatusType.INDEXED);
            siteTable.setStatusTime(LocalDateTime.now());
            siteTable.setId(siteRepository.findByUrl(absolutePath).getId());
            updateSiteTable(siteTable);
        }
    }

    public void runIndexing(String absolutePath) {
        try {
            Connection.Response response = connectPath(absolutePath, parseTimeout).execute();
            int code = response.statusCode();
            if (code != 200) {
                return;
            }
            SiteTable siteTableUrl = siteRepository.findByUrl(siteTable.getUrl());
            siteTable.setId(siteTableUrl.getId());
            String relativePath = convertPath(absolutePath);
            if (pageRepository.findByPathAndSiteTable_Id(relativePath, siteTable.getId()) != null) {
                return;
            }
            String content = response.body();
            String title = response.parse().title();
            String body = response.parse().body().text();
            PageTable pageTable = new PageTable();
            pageTable.setCode(code);
            pageTable.setPath(relativePath);
            pageTable.setContent(content);
            pageTable.setSiteTable(siteTable);
            savePage(pageTable);
            Lemmatizer lemmatizer = new Lemmatizer();
            HashMap<String, Float> lemmtizerBody = lemmatizer.addRank(title, body);
            saveLemmasAndIndexes(lemmtizerBody, relativePath);
            if (oneIndexing) {
                logger.info("Запись в БД об удачной индексации");
                siteTable.setStatus(StatusType.INDEXED);
            }
        } catch (IOException | InterruptedException | UncheckedIOException ex) {
            logger.error("Ошибка индексации страницы" + ex + " " + absolutePath);
            siteTable.setLastError("Ошибка индексации страницы: " + ex + " " + absolutePath);
            siteTable.setStatus(StatusType.FAILED);
            interruptTread();
        } finally {
            siteTable.setStatusTime(LocalDateTime.now());
            updateSiteTable(siteTable);
        }
    }

    private void saveLemmasAndIndexes(HashMap<String, Float> lemmtizerBody, String url) {
        PageTable pageTableUrl = pageRepository.findByPathAndSiteTable_Id(url, siteTable.getId());
        List<IndexTable> indexTableList = new ArrayList<>();
        for (String lemma : lemmtizerBody.keySet()) {
            LemmaTable lemmaTable = new LemmaTable();
            lemmaTable.setSiteTable(siteTable);
            lemmaTable.setLemma(lemma);
            LemmaTable table = lemmaRepository.findByLemmaAndSiteTable_Id(lemma, siteTable.getId());
            if (table != null) {
                int frequency = table.getFrequency() + 1;
                lemmaTable.setFrequency(frequency);
                lemmaTable.setId(table.getId());
                saveLemma(lemmaTable);
            } else {
                lemmaTable.setFrequency(1);
                saveLemma(lemmaTable);
                table = lemmaRepository.findByLemmaAndSiteTable_Id(lemma, siteTable.getId());
            }
            IndexTable indexTable = new IndexTable();
            indexTable.setRankLemma(lemmtizerBody.get(lemma));
            indexTable.setPageTable(pageTableUrl);
            indexTable.setLemmaTable(table);
            indexTableList.add(indexTable);
        }
        saveAllIndex(indexTableList);
    }

    private String convertPath(String absolutePath) {
        String relativePath =
                absolutePath.contains("www.") ?
                        absolutePath.replace("www.", "")
                        : absolutePath;
        relativePath = relativePath.replace(siteTable.getUrl().replace("www.", ""), "/");

        relativePath = relativePath.length() != 1 ?
                relativePath.substring(0, relativePath.length() - 1) :
                relativePath;
        return relativePath;
    }

    private synchronized void deleteLemma(String lemma, SiteTable siteTable) {
        LemmaTable lemmaTable = lemmaRepository.findByLemmaAndSiteTable_Id(lemma, siteTable.getId());
        if (lemmaTable.getFrequency() > 1) {
            lemmaTable.setFrequency(lemmaTable.getFrequency() - 1);
            lemmaRepository.updateFrequencyByLemmaAndSiteTable(
                    lemmaTable.getFrequency(), lemma, siteTable);
        } else {
            lemmaRepository.deleteById(lemmaTable.getId());
        }
    }

    protected static Connection connectPath(String path, int parseTimeout) throws InterruptedException {
        Thread.sleep(parseTimeout);
        return Jsoup.connect(path)
                .userAgent("MollySearchBot")
                .referrer("http://www.google.com")
                .timeout(5000000)
                .maxBodySize(0)
                .followRedirects(true)
                .ignoreHttpErrors(true)
                .ignoreContentType(true);
    }

    private synchronized void deleteSite(int id) {
        siteRepository.deleteById(siteTable.getId());
    }

    private synchronized void deletePage(int id) {
        pageRepository.deleteById(id);
    }

    private synchronized void saveSite(SiteTable siteTable) {
        siteRepository.save(siteTable);
    }

    private synchronized void savePage(PageTable pageTable) {
        pageRepository.save(pageTable);
    }

    private synchronized void saveLemma(LemmaTable lemmaTable) {
        lemmaRepository.save(lemmaTable);
    }

    private synchronized void saveAllIndex(List<IndexTable> indexTableList) {
        indexRepository.saveAll(indexTableList);
    }

    private synchronized void updateSiteTable(SiteTable siteTable) {
        siteRepository.updateStatusAndStatusTimeAndLastErrorById(
                siteTable.getStatus(),
                siteTable.getStatusTime(),
                siteTable.getLastError(),
                siteTable.getId()
        );
    }
}
