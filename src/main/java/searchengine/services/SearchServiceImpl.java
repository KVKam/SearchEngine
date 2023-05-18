package searchengine.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import searchengine.SearchText;
import searchengine.dto.error.ErrorResponse;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.StatusType;

import java.util.List;

@Service
public class SearchServiceImpl implements SearchService {
    private final SiteTableService siteTableService;
    private final PageTableService pageTableService;
    private final LemmaTableService lemmaTableService;
    private final IndexTableService indexTableService;
    private final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

    public SearchServiceImpl(SiteTableService siteTableService,
                             PageTableService pageTableService,
                             LemmaTableService lemmaTableService,
                             IndexTableService indexTableService) {
        this.siteTableService = siteTableService;
        this.pageTableService = pageTableService;
        this.lemmaTableService = lemmaTableService;
        this.indexTableService = indexTableService;
    }

    @Override
    public Object search(String query, String site, int offset, int limit) {
        if (query.equals("")) {
            logger.info("Задан пустой поисковый запрос");
            return new ErrorResponse(false, "Задан пустой поисковый запрос");
        } else if (!site.equals("")) {
            if (!siteTableService.findUrl(site).getStatus().equals(StatusType.INDEXED)) {
                logger.info("Сайт не проиндексирован");
                return new ErrorResponse(false, "Проведите процидуру индексации сайта " + site + " перед поиском.");
            }
        } else {
            if (siteTableService.presenceStatusType(StatusType.INDEXING)) {
                return new ErrorResponse(false, "Дождитесь окончания процедуры индексации сайта(ов)");
            }
            if (siteTableService.presenceStatusType(StatusType.FAILED)) {
                logger.info("Есть не проиндексированные сайты");
                return new ErrorResponse(false, "Проведите процедуру индексации всех сайтов для поиска по ним.");
            }
        }
        SearchText searchText = new SearchText(siteTableService, pageTableService, lemmaTableService, indexTableService);
        List<SearchData> list = searchText.searching(query, site, offset, limit);
        if (list.isEmpty()) {
            logger.info("Страница с указанной фразой не найдена.");
            return new ErrorResponse(false, "Страница с указанной фразой не найдена.");
        }
        logger.info("Станица успешно найдена");
        return new SearchResponse(true, list.size(), list);
    }
}
