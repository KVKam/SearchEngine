package searchengine.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.index.IndexData;
import searchengine.parsing.IndexingThread;
import searchengine.config.SiteConfig;
import searchengine.config.SitesList;
import searchengine.dto.error.ErrorResponse;
import searchengine.dto.index.IndexResponse;
import searchengine.model.SiteTable;
import searchengine.model.StatusType;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class IndexingServiceImpl implements IndexingService {

    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private final IndexRepository indexRepository;
    private final SitesList sitesList;
    private final Logger logger = LoggerFactory.getLogger(IndexingServiceImpl.class);
    private ThreadPoolExecutor executor;
    private List<IndexingThread> threads = new ArrayList<>();

    public IndexingServiceImpl(SiteRepository siteRepository,
                               PageRepository pageRepository,
                               LemmaRepository lemmaRepository,
                               IndexRepository indexRepository,
                               SitesList sitesList) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.sitesList = sitesList;
    }

    @Override
    public Object startIndexing() {
        List<IndexData> configSiteListTable = getConfigSitesList();
        if (siteRepository.isStatus(StatusType.INDEXING)) {
            return new ErrorResponse(false, "Индексация уже запущена");
        }
        checkAndCreatingStatusExecutor(configSiteListTable.size());
        logger.info("Создание потока");
        for (IndexData indexData : configSiteListTable) {
            indexData.setOneIndexing(false);
            IndexingThread indexingThread = new IndexingThread(
                    siteRepository,
                    pageRepository,
                    lemmaRepository,
                    indexRepository,
                    indexData);
            executor.execute(indexingThread);
            threads.add(indexingThread);
        }
        return new IndexResponse(true);
    }

    @Override
    public Object indexPade(String url) {
        if (url.equals("")) {
            return new ErrorResponse(false, "Строка пуста");
        }
        if (siteRepository.isStatus(StatusType.INDEXING)) {
            return new ErrorResponse(false, "Запущена индексация, дождитесь ее окончания для добавления/изменения");
        }
        List<IndexData> configSiteListTable = getConfigSitesList();
        url = normalizeUrl(url);
        IndexData indexData = findSiteByUrlInConfigList(configSiteListTable, url);
        if (indexData == null) {
            return new ErrorResponse(false, "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }
        checkAndCreatingStatusExecutor(configSiteListTable.size());
        indexData.setOneIndexing(true);
        IndexingThread indexingThread = new IndexingThread(
                siteRepository,
                pageRepository,
                lemmaRepository,
                indexRepository,
                indexData);
        executor.execute(indexingThread);
        threads.add(indexingThread);
        return new IndexResponse(true);
    }

    @Override
    public Object stopIndexing() {
        logger.info("Попытка остановки потоков");
        if (!siteRepository.isStatus(StatusType.INDEXING)) {
            return new ErrorResponse(false, "Индексация не запущена");
        }
        boolean liveThread;
        if (executor != null) {
            try {
                threads.forEach(IndexingThread::interruptTread);
                executor.shutdown();
                liveThread = executor.awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                logger.error("Ошибка закрытия потоков: " + e);
                return new ErrorResponse(false, "Ошибка закрытия потоков");
            }
        } else {
            liveThread = true;
        }
        List<SiteTable> siteTableList = (List<SiteTable>) siteRepository.findAll();
        for (SiteTable siteTable : siteTableList) {
            logger.info("Запись статуса FAILED в БД");
            synchronized (siteRepository) {
                siteRepository.updateStatusAndStatusTimeAndLastErrorById(
                        StatusType.FAILED,
                        LocalDateTime.now(),
                        "Индексация остановлена пользователем",
                        siteTable.getId()
                );
            }
        }
        return liveThread ? new IndexResponse(true) : new ErrorResponse(false, "Ошибка закрытия потоков");
    }

    private List<IndexData> getConfigSitesList() {
        List<IndexData> configSiteListTable = new ArrayList<>();
        for (SiteConfig siteConfigStatic : sitesList.getSitesConfig()) {
            int parseTimeout = siteConfigStatic.getTimeout() == 0 ? 600 : siteConfigStatic.getTimeout();
            String siteUrl = normalizeUrl(siteConfigStatic.getUrl());
            SiteTable siteTable = new SiteTable();
            siteTable.setName(siteConfigStatic.getName());
            siteTable.setUrl(siteUrl);
            siteTable.setLastError(null);
            siteTable.setStatus(StatusType.INDEXING);
            siteTable.setStatusTime(LocalDateTime.now());
            IndexData indexData = new IndexData(siteTable, parseTimeout, siteUrl);
            configSiteListTable.add(indexData);
        }
        return configSiteListTable;
    }


    private String normalizeUrl(String siteUrl) {
        if (siteUrl.length() - siteUrl.lastIndexOf("/") != 1) {
            siteUrl += "/";
        }
        return siteUrl;
    }

    private IndexData findSiteByUrlInConfigList(List<IndexData> configSiteListTable, String url) {
        for (IndexData indexData : configSiteListTable) {
            SiteTable siteTable = indexData.getSiteTable();
            String siteUrl = siteTable.getUrl();
            if (!url.contains("www.") || !siteUrl.contains("www.")) {
                siteUrl = siteUrl.replace("www.", "");
                url = url.replace("www.", "");
            }
            siteUrl = siteUrl.replace(siteUrl.substring(siteUrl.indexOf('/', 16) + 1), "");
            if (url.startsWith(siteUrl)) {
                indexData.setUrl(url);
                return indexData;
            }
        }
        return null;
    }

    private void checkAndCreatingStatusExecutor(int sizePool) {
        if (executor == null || executor.isTerminated()) {
            executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(sizePool);
        }
    }
}
