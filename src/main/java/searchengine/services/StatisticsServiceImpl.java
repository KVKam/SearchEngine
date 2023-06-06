package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.SiteTable;
import searchengine.model.StatusType;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    private static final Logger logger = LoggerFactory.getLogger(StatisticsServiceImpl.class);

    @Override
    public StatisticsResponse getStatistics() {
        List<String> siteTableUrl = siteRepository.findAllUrl();
        TotalStatistics total = new TotalStatistics();
        total.setSites(Math.toIntExact(siteRepository.count()));
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for (String site : siteTableUrl) {
            SiteTable tableSiteTable = siteRepository.findByUrl(site);
            long pages = pageRepository.countBySiteTable(tableSiteTable);
            long lemmas = lemmaRepository.countBySiteTable(tableSiteTable);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(tableSiteTable.getName());
            item.setUrl(tableSiteTable.getUrl());
            item.setStatus(tableSiteTable.getStatus().toString());
            item.setError(tableSiteTable.getLastError());
            item.setStatusTime(tableSiteTable.getStatusTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            item.setPages(pages);
            item.setLemmas(lemmas);
            detailed.add(item);
            total.setIndexing(tableSiteTable.getStatus().equals(StatusType.INDEXED));
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
        }
        StatisticsData data = new StatisticsData(total, detailed);
        logger.info("Вывод статистики");
        return new StatisticsResponse(true, data);
    }
}
