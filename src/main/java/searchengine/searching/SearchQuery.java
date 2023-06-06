package searchengine.searching;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.dto.search.SearchData;
import searchengine.lemmatisator.Lemmatizer;
import searchengine.model.LemmaTable;
import searchengine.model.PageTable;
import searchengine.model.SiteTable;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SearchQuery {
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private Lemmatizer lemmatizer;

    public SearchQuery(SiteRepository siteRepository,
                       PageRepository pageRepository,
                       LemmaRepository lemmaRepository,
                       IndexRepository indexRepository) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
    }

    public List<SearchData> getSortedSearchList(String query, String siteUrl, int offset, int limit) {
        lemmatizer = new Lemmatizer();
        List<String> lemmatizerList = lemmatizer.getQueryLemmatizer(query);
        Map<Integer, List<String>> lemmaMapByPage = getListsLemmasByPageId(lemmatizerList, siteUrl);
        if (lemmaMapByPage.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Integer, Float> index = findRelativeRelevance(lemmaMapByPage);
        List<SearchData> sortedList = new ArrayList<>();
        int size = offset + limit < index.size() ? limit : index.size() - offset;
        int marker = 0;
        for (Integer pageId : index.keySet()) {
            if (marker < offset) {
                break;
            }
            sortedList.add(getSearchData(pageId, lemmatizerList, index.get(pageId)));
            ++marker;
            if (marker == size) {
                break;
            }
        }
        return sortedList;
    }

    private Map<Integer, List<String>> getListsLemmasByPageId(List<String> lemmatizerList, String siteUrl) {
        Map<Integer, List<String>> lemmaMapByPage = new HashMap<>();
        if (siteUrl.equals("")) {
            List<String> listUrl = siteRepository.findAllUrl();
            for (String url : listUrl) {
                lemmaMapByPage.putAll(getLemmaMapByUrl(lemmatizerList, url));
            }
        } else {
            lemmaMapByPage.putAll(getLemmaMapByUrl(lemmatizerList, siteUrl));
        }
        return lemmaMapByPage;
    }

    private Map<Integer, List<String>> getLemmaMapByUrl(List<String> lemmatizerList, String siteUrl) {
        SiteTable foundSite = siteRepository.findByUrl(siteUrl);
        long size = pageRepository.countBySiteTable(foundSite);
        List<LemmaTable> foundRowsLemmaTable = new ArrayList<>();
        for (String lemmatizerWord : lemmatizerList) {
            LemmaTable lemmaTable = lemmaRepository.findByLemmaAndSiteTable_Id(lemmatizerWord, foundSite.getId());
            if (lemmaTable == null) {
                return new HashMap<>();
            }
            if (lemmaTable.getFrequency() < (size * 0.7)) {
                foundRowsLemmaTable.add(lemmaTable);
            }
        }
        foundRowsLemmaTable.sort(Comparator.comparing(LemmaTable::getFrequency));
        List<String> lemmaList = new ArrayList<>();
        foundRowsLemmaTable.forEach(o -> lemmaList.add(o.getLemma()));
        List<Integer> pageIdList = getDuplicatePageIdByLemmas(foundRowsLemmaTable);
        Map<Integer, List<String>> lemmaMapByPage = new HashMap<>();
        pageIdList.forEach(t -> lemmaMapByPage.put(t, lemmaList));
        return lemmaMapByPage;
    }

    private List<Integer> getDuplicatePageIdByLemmas(List<LemmaTable> rowsLemmaTable) {
        List<Integer> pageId = new ArrayList<>();
        for (int i = 0; i < rowsLemmaTable.size(); i++) {
            List<Integer> list = indexRepository.findByLemmaTable_IdPage(rowsLemmaTable.get(i).getId());
            if (i == 0) {
                pageId.addAll(list);
                continue;
            }
            List<Integer> deleteList = new ArrayList<>();
            for (Integer id : pageId) {
                if (!list.contains(id)) {
                    deleteList.add(id);
                }
            }
            pageId.removeAll(deleteList);
        }
        return pageId;
    }


    private Map<Integer, Float> findRelativeRelevance(Map<Integer, List<String>> listIdPageAndLemma) {
        Map<Integer, Float> index = new HashMap<>();
        for (Integer idPage : listIdPageAndLemma.keySet()) {
            for (String lemma : listIdPageAndLemma.get(idPage)) {
                Float rank = indexRepository.findByLemmaTable_LemmaAndPageTable_Id(lemma, idPage)
                        .describeConstable().orElse(0.0F);
                if (rank == 0.0F) {
                    continue;
                }
                index.merge(idPage, rank, Float::sum);
            }
        }
        Float max = Collections.max(index.values());
        index.entrySet().forEach(integerFloatEntry ->
                integerFloatEntry.setValue(integerFloatEntry.getValue() / max));
        index = index.entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .collect(Collectors
                        .toMap(Map.Entry::getKey,
                                Map.Entry::getValue,
                                (e1, e2) -> e1,
                                LinkedHashMap::new));
        return index;
    }

    private SearchData getSearchData(Integer pageId, List<String> textFound, Float relevance) {
        PageTable pageTable = pageRepository.findById(pageId).orElse(null);
        String urlSite = pageTable.getSiteTable().getUrl();
        String page = pageTable.getPath();
        Document document = Jsoup.parse(pageTable.getContent());
        SearchData searchData = new SearchData();
        searchData.setSite(urlSite.substring(0, urlSite.length() - 1));
        searchData.setSiteName(pageTable.getSiteTable().getName());
        searchData.setUrl(page + '/');
        searchData.setTitle(String.valueOf(getTitle(document)));
        searchData.setSnippet(String.valueOf(getSnippet(document, textFound)));
        searchData.setRelevance(relevance);
        return searchData;
    }

    private StringBuilder getTitle(Document document) {
        Elements elementsTitle = document.select("title");
        StringBuilder title = new StringBuilder();
        elementsTitle.forEach(element -> title.append(element.text()));
        return title;
    }

    private StringBuilder getSnippet(Document document, List<String> searchQuery) {
        StringBuilder body = new StringBuilder(getTitle(document)).append(" ");
        Elements elementsBody = document.select("body");
        elementsBody.forEach(element -> body.append(element.text()));
        List<String> listText = lemmatizer.searchQueryInText(body, searchQuery);
        String textPart = listText.get(0);
        Pattern pattern = Pattern.compile(Pattern.quote(textPart));
        Matcher matcher = pattern.matcher(Lemmatizer.letterSubstitution(body.toString()));
        String partBody;
        if (matcher.find()) {
            partBody = body.substring(matcher.start());
        } else {
            return new StringBuilder();
        }
        StringBuilder snippet = new StringBuilder();
        for (String word : partBody.split(" ")) {
            if (word.contains("-") || word.contains(")") || word.contains("(")) {
                for (String partWord : word.split("[-)(]")) {
                    if (listText.contains(normText(partWord))) {
                        word = word.replaceAll(Pattern.quote(word), "<b>" + word + "</b>");
                    }
                }
            } else if (listText.contains(normText(word))) {
                word = word.replaceAll(Pattern.quote(word), "<b>" + word + "</b>");
            }
            snippet.append(word);
            if (snippet.length() > 200) {
                break;
            }
            snippet.append(" ");
        }
        return snippet.append("...");
    }

    private String normText(String text) {
        if (text != null && text.length() > 0) {
            text = text.toLowerCase();
            text = text.replaceAll("\\p{Punct}", "");
            text = Lemmatizer.letterSubstitution(text);
            text = text.replaceAll("[«»]", "");
        }
        return text;
    }
}
