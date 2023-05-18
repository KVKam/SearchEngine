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

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    @Autowired
    private final SiteTableService siteTableService;
    @Autowired
    private final PageTableService pageTableService;
    @Autowired
    private final LemmaTableService lemmaTableService;
    private static final Logger logger = LoggerFactory.getLogger(StatisticsServiceImpl.class);
    @Override
    public StatisticsResponse getStatistics() {
        List<String> siteTableUrl = siteTableService.getAllUrl();
        TotalStatistics total = new TotalStatistics();
        total.setSites(Math.toIntExact(siteTableService.count()));
        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        for (String site : siteTableUrl) {
            SiteTable tableSite = siteTableService.findUrl(site);
            long pages = pageTableService.count(tableSite);
            long lemmas = lemmaTableService.count(tableSite);
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(tableSite.getName());
            item.setUrl(tableSite.getUrl());
            item.setStatus(tableSite.getStatus().toString());
            item.setError(tableSite.getLastError());
            item.setStatusTime(tableSite.getStatusTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            item.setPages(pages);
            item.setLemmas(lemmas);
            detailed.add(item);
            total.setIndexing(tableSite.getStatus().equals(StatusType.INDEXED));
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
        }
        StatisticsData data = new StatisticsData(total, detailed);
        logger.info("Вывод статистики");
        return new StatisticsResponse(true, data);
    }
}
