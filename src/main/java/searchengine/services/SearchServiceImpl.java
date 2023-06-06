package searchengine.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.searching.SearchQuery;
import searchengine.dto.error.ErrorResponse;
import searchengine.dto.search.SearchData;
import searchengine.dto.search.SearchResponse;
import searchengine.model.StatusType;

import java.util.List;

@Service
public class SearchServiceImpl implements SearchService {
    @Autowired
    private final SiteRepository siteRepository;
    @Autowired
    private final PageRepository pageRepository;
    @Autowired
    private final LemmaRepository lemmaRepository;
    @Autowired
    private final IndexRepository indexRepository;
    private final Logger logger = LoggerFactory.getLogger(SearchServiceImpl.class);

    public SearchServiceImpl(SiteRepository siteRepository,
                             PageRepository pageRepository,
                             LemmaRepository lemmaRepository,
                             IndexRepository indexRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    @Override
    public Object search(String query, String siteUrl, int offset, int limit) {
        if (query.equals("")) {
            logger.info("Задан пустой поисковый запрос");
            return new ErrorResponse(false, "Задан пустой поисковый запрос");
        } else if (!siteUrl.equals("")) {
            StatusType statusTypeByUrl = siteRepository.findByUrl(siteUrl).getStatus();
            if (!statusTypeByUrl.equals(StatusType.INDEXED)) {
                logger.info("Сайт не проиндексирован");
                return new ErrorResponse(false, "Проведите процидуру индексации сайта " + siteUrl + " перед поиском.");
            }
        } else {
            if (siteRepository.isStatus(StatusType.INDEXING)) {
                return new ErrorResponse(false, "Дождитесь окончания процедуры индексации сайта(ов)");
            }
            if (siteRepository.isStatus(StatusType.FAILED)) {
                logger.info("Есть не проиндексированные сайты");
                return new ErrorResponse(false, "Проведите процедуру индексации всех сайтов для поиска по ним.");
            }
        }
        SearchQuery searchQuery = new SearchQuery(siteRepository, pageRepository,
                lemmaRepository, indexRepository);
        List<SearchData> list = searchQuery.getSortedSearchList(query, siteUrl, offset, limit);
        if (list.isEmpty()) {
            logger.info("Страница с указанной фразой не найдена.");
            return new ErrorResponse(false, "Страница с указанной фразой не найдена.");
        }
        logger.info("Станица успешно найдена");
        return new SearchResponse(true, list.size(), list);
    }
}
