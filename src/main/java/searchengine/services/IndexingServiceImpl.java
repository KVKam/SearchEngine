package searchengine.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.IndexingThread;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.error.ErrorResponse;
import searchengine.dto.index.IndexResponse;
import searchengine.model.SiteTable;
import searchengine.model.StatusType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class IndexingServiceImpl implements IndexingService {

    private final SiteTableService siteTableService;
    private final PageTableService pageTableService;
    private final LemmaTableService lemmaTableService;
    private final IndexTableService indexTableService;
    private final SitesList sitesList;
    private final Logger logger = LoggerFactory.getLogger(IndexingServiceImpl.class);
    private ThreadPoolExecutor executorService;
    private List<IndexingThread> threads = new ArrayList<>();

    public IndexingServiceImpl(SiteTableService siteTableService,
                               PageTableService pageTableService,
                               LemmaTableService lemmaTableService,
                               IndexTableService indexTableService,
                               SitesList sitesList) {
        this.siteTableService = siteTableService;
        this.pageTableService = pageTableService;
        this.lemmaTableService = lemmaTableService;
        this.indexTableService = indexTableService;
        this.sitesList = sitesList;
    }

    @Override
    public Object startIndexing() {
        List<SiteTable> siteTableList = startIndexingList();
        if (siteTableService.presenceStatusType(StatusType.INDEXING)) {
            return new ErrorResponse(false, "Индексация уже запущена");
        }
        if (executorService == null || executorService.isTerminated()) {
            executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(siteTableList.size());
        }
        logger.info("Создание потока");
        for (SiteTable siteTable : siteTableList) {
            IndexingThread indexingThread = new IndexingThread(siteTable,
                    siteTableService,
                    pageTableService,
                    lemmaTableService,
                    indexTableService,
                    siteTable.getUrl(), false);
            executorService.execute(indexingThread);
            threads.add(indexingThread);
        }
        return new IndexResponse(true);
    }

    @Override
    public Object indexPade(String url) {
        url = url.trim();
        if (url.equals("")) {
            return new ErrorResponse(false, "Строка пуста");
        }
        if (siteTableService.presenceStatusType(StatusType.INDEXING)) {
            return new ErrorResponse(false, "Запущена полная индексация, дождитесь ее окончания для добавления/изменения");
        }
        List<SiteTable> siteTableList = startIndexingList();
        url = normalizeUrl(url);
        SiteTable siteTable = findSiteUrl(siteTableList, url);
        if (siteTable == null) {
            return new ErrorResponse(false, "Данная страница находится за пределами сайтов, указанных в конфигурационном файле");
        }
        if (executorService == null || executorService.isTerminated()) {
            executorService = (ThreadPoolExecutor) Executors.newFixedThreadPool(siteTableList.size());
        }
        IndexingThread indexingThread = new IndexingThread(siteTable,
                siteTableService,
                pageTableService,
                lemmaTableService,
                indexTableService,
                url, true);
        executorService.execute(indexingThread);
        threads.add(indexingThread);
        return new IndexResponse(true);
    }

    @Override
    public Object stopIndexing() {
        logger.info("Попытка остановки потоков");
        if (!siteTableService.presenceStatusType(StatusType.INDEXING)) {
            return new ErrorResponse(false, "Индексация не запущена");
        }
        boolean liveThread;
        if (executorService != null) {
            try {
                threads.forEach(IndexingThread::interruptTread);
                executorService.shutdown();
                liveThread = executorService.awaitTermination(5, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                logger.error("Ошибка закрытия потоков: " + e);
                return new ErrorResponse(false, "Ошибка закрытия потоков");
            }
        } else {
            liveThread = true;
        }
        List<SiteTable> siteTableList = siteTableService.getAll();
        for (SiteTable siteTable : siteTableList) {
            logger.info("Запись статуса FAILED в БД");
            siteTable.setStatus(StatusType.FAILED);
            siteTable.setStatusTime(LocalDateTime.now());
            siteTable.setId(siteTable.getId());
            siteTableService.update(siteTable);
        }
        return liveThread ? new IndexResponse(true) : new ErrorResponse(false, "Ошибка закрытия потоков");
    }

    private SiteTable findSiteUrl(List<SiteTable> siteTables, String url) {
        for (SiteTable site : siteTables) {
            String siteUrl = site.getUrl();
            if (!url.contains("www.")) {
                siteUrl = siteUrl.replace("www.", "");
            }
            siteUrl = siteUrl.replace(siteUrl.substring(siteUrl.indexOf('/', 16) + 1), "");
            if (url.startsWith(siteUrl)) {
                return site;
            }
        }
        return null;
    }

    private List<SiteTable> startIndexingList() {
        List<SiteTable> siteList = new ArrayList<>();
        List<Site> siteStatics = sitesList.getSites();
        for (Site siteStatic : siteStatics) {
            String siteUrl = normalizeUrl(siteStatic.getUrl());
            SiteTable siteTable = new SiteTable();
            siteTable.setName(siteStatic.getName());
            siteTable.setUrl(siteUrl);
            siteTable.setLastError(null);
            siteTable.setStatus(StatusType.INDEXING);
            siteTable.setStatusTime(LocalDateTime.now());
            siteList.add(siteTable);
        }
        return siteList;
    }

    private String normalizeUrl(String siteUrl) {
        if (siteUrl.length() - siteUrl.lastIndexOf("/") != 1) {
            siteUrl += "/";
        }
        return siteUrl;
    }
}
