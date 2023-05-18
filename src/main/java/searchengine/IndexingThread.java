package searchengine;

import org.jsoup.Connection;
import org.jsoup.UncheckedIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import searchengine.model.*;
import searchengine.services.IndexTableService;
import searchengine.services.LemmaTableService;
import searchengine.services.PageTableService;
import searchengine.services.SiteTableService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ForkJoinPool;


public class IndexingThread extends Thread {
    private final SiteTable siteTable;
    private final SiteTableService siteTableService;
    private final PageTableService pageTableService;
    private final LemmaTableService lemmaTableService;
    private final IndexTableService indexTableService;
    private final String siteUrl;
    private final boolean oneIndexing;
    private ForkJoinPool forkJoinPool;
    private SiteMap siteMap;
    private final Logger logger = LoggerFactory.getLogger(IndexingThread.class);

    public IndexingThread(SiteTable siteTable,
                          SiteTableService siteTableService,
                          PageTableService pageTableService,
                          LemmaTableService lemmaTableService,
                          IndexTableService indexTableService,
                          String siteUrl, boolean oneIndexing) {
        this.siteTable = siteTable;
        this.siteTableService = siteTableService;
        this.pageTableService = pageTableService;
        this.lemmaTableService = lemmaTableService;
        this.indexTableService = indexTableService;
        this.siteUrl = siteUrl;
        this.oneIndexing = oneIndexing;
    }

    @Override
    public void run() {
        tableValidation();
        if (this.isInterrupted()) {
            return;
        }
        if (oneIndexing) {
            runIndexing(siteUrl);
        } else {
            runIndexingAll();
        }
    }

    private void tableValidation() {
        SiteTable siteBD = siteTableService.findUrl(siteTable.getUrl());
        if (siteBD == null) {
            logger.info("Создание новой записи в БД");
            siteTableService.save(siteTable);
            return;
        }
        siteTable.setId(siteBD.getId());
        siteTable.setStatus(StatusType.INDEXING);
        siteTable.setStatusTime(LocalDateTime.now());
        siteTableService.update(siteTable);
        if (oneIndexing) {
            String url = siteUrl.contains("www.") ? siteUrl.replace("www.", "") : siteUrl;
            url = url.replace(siteTable.getUrl().replace("www.", ""), "/");
            if (url.length() - url.lastIndexOf("/") == 1) {
                url = url.replace(url, url.substring(0, url.length() - 1));
            }
            PageTable onePage = pageTableService.findPathAndSiteId(url, siteTable.getId());
            if (onePage == null) {
                return;
            }
            List<String> deleteLemmaList = indexTableService.findByPages(onePage.getId());
            for (String lemma : deleteLemmaList) {
                lemmaTableService.delete(lemma, siteTable);
            }
            pageTableService.delete(onePage.getId());
        } else {
            logger.info("Удаление данных из БД");
            try {
                siteTableService.delete(siteTable.getId());
            } catch (OutOfMemoryError ex){
                logger.error(String.valueOf(ex));
                siteTable.setStatus(StatusType.FAILED);
                interruptTread();
            } finally {
                siteTableService.save(siteTable);
            }
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
        siteMap = new SiteMap(siteUrl);
        forkJoinPool = new ForkJoinPool(6);
        StringBuilder listUrl = forkJoinPool.invoke(siteMap);
        for (String url : listUrl.toString().split("\n")) {
            if (this.isInterrupted()) {
                break;
            }
            if (url.startsWith(siteUrl.replace("www.", ""))) {
                runIndexing(url);
            }
        }
        if (!this.isInterrupted()) {
            logger.info("Запись об удачной индексации в БД");
            siteTable.setStatus(StatusType.INDEXED);
            siteTable.setStatusTime(LocalDateTime.now());
            siteTable.setId(siteTableService.findUrl(siteUrl).getId());
            siteTableService.update(siteTable);
        }
    }

    public void runIndexing(String url) {
        try {
            Connection.Response response = SiteMap.connectPath(url).execute();
            int code = response.statusCode();
            if (code != 200) {
                return;
            }
            url = url.contains("www.") ? url.replace(siteTable.getUrl(), "/") : url.replace(siteTable.getUrl().replace("www.", ""), "/");
            url = url.length() != 1 ? url.substring(0, url.length() - 1) : url;
            SiteTable siteTableUrl = siteTableService.findUrl(siteTable.getUrl());
            siteTable.setId(siteTableUrl.getId());
            if (pageTableService.findPathAndSiteId(url, siteTable.getId()) != null) {
                return;
            }
            String content = response.body();
            String title = response.parse().title();
            String body = response.parse().body().text();
            PageTable pageTable = new PageTable();
            pageTable.setCode(code);
            pageTable.setPath(url);
            pageTable.setContent(content);
            pageTable.setSiteTable(siteTable);
            pageTableService.save(pageTable);
            Lemmatizer lemmatizer = new Lemmatizer();
            HashMap<String, Float> lemmtizerBody = lemmatizer.addRank(title, body);
            saveLemma(lemmtizerBody, url);
            if (oneIndexing) {
                logger.info("Запись в БД об удачной индексации");
                siteTable.setStatus(StatusType.INDEXED);
            }
        } catch (IOException | InterruptedException | UncheckedIOException ex) {
            logger.error("Ошибка индексации страницы" + ex + " " + url);
            siteTable.setLastError("Ошибка индексации страницы: " + ex + " " + url);
            siteTable.setStatus(StatusType.FAILED);
            interruptTread();
        } finally {
            siteTable.setStatusTime(LocalDateTime.now());
            siteTableService.update(siteTable);
        }
    }

    private void saveLemma(HashMap<String, Float> lemmtizerBody, String url) {
        PageTable pageTableUrl = pageTableService.findPathAndSiteId(url, siteTable.getId());
        List<IndexTable> indexTableList = new ArrayList<>();
        for (String lemma : lemmtizerBody.keySet()) {
            LemmaTable lemmaTable = new LemmaTable();
            lemmaTable.setSiteTable(siteTable);
            lemmaTable.setLemma(lemma);
            LemmaTable table = lemmaTableService.findLemmaOnSite(lemma, siteTable.getId());
            if (table != null) {
                int frequency = table.getFrequency() + 1;
                lemmaTable.setFrequency(frequency);
                lemmaTable.setId(table.getId());
                lemmaTableService.save(lemmaTable);
            } else {
                lemmaTable.setFrequency(1);
                lemmaTableService.save(lemmaTable);
                table = lemmaTableService.findLemmaOnSite(lemma, siteTable.getId());
            }
            IndexTable indexTable = new IndexTable();
            indexTable.setRankLemma(lemmtizerBody.get(lemma));
            indexTable.setPageTable(pageTableUrl);
            indexTable.setLemmaTable(table);
            indexTableList.add(indexTable);
        }
        indexTableService.saveAll(indexTableList);
    }
}
