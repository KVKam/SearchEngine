package searchengine;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.dto.search.SearchData;
import searchengine.model.LemmaTable;
import searchengine.model.PageTable;
import searchengine.model.SiteTable;
import searchengine.services.IndexTableService;
import searchengine.services.LemmaTableService;
import searchengine.services.PageTableService;
import searchengine.services.SiteTableService;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SearchText {
    private final SiteTableService siteTableService;
    private final PageTableService pageTableService;
    private final LemmaTableService lemmaTableService;
    private final IndexTableService indexTableService;
    private Lemmatizer lemmatizer;

    public SearchText(SiteTableService siteTableService,
                      PageTableService pageTableService,
                      LemmaTableService lemmaTableService,
                      IndexTableService indexTableService) {
        this.siteTableService = siteTableService;
        this.pageTableService = pageTableService;
        this.lemmaTableService = lemmaTableService;
        this.indexTableService = indexTableService;
    }

    public List<SearchData> searching(String query, String siteUrl, int offset, int limit) {
        lemmatizer = new Lemmatizer();
        List<String> lemmatizerList = lemmatizer.normFormLemmatizer(query);
        Map<Integer, List<String>> listIdPageAndLemma = new HashMap<>();
        if (siteUrl.equals("")) {
            List<String> listUrl = siteTableService.getAllUrl();
            for (String url : listUrl) {
                listIdPageAndLemma.putAll(analysisQuery(lemmatizerList, url));
            }
        } else {
            listIdPageAndLemma.putAll(analysisQuery(lemmatizerList, siteUrl));
        }
        if (listIdPageAndLemma.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Integer, Float> index = findRelativeRelevance(listIdPageAndLemma);
        List<SearchData> list = new ArrayList<>();
        int size = offset + limit < index.size() ? limit
                : index.size() - offset;
        int marker = 0;
        for (Integer pageId : index.keySet()) {
            if (marker < offset) {
                break;
            }
            list.add(getSearchData(pageId, lemmatizerList, index.get(pageId)));
            ++marker;
            if (marker == size) {
                break;
            }
        }
        return list;
    }

    private Map<Integer, List<String>> analysisQuery(List<String> lemmatizerList, String siteUrl) {
        SiteTable site = siteTableService.findUrl(siteUrl);
        long size = pageTableService.count(site);
        List<LemmaTable> lemmaTableList = new ArrayList<>();
        for (String partText : lemmatizerList) {
            LemmaTable lemmaTable = lemmaTableService.findLemmaOnSite(partText, site.getId());
            if (lemmaTable == null) {
                return new HashMap<>();
            }
            if (lemmaTable.getFrequency() < (size * 0.7)) {
                lemmaTableList.add(lemmaTable);
            }
        }
        lemmaTableList.sort(Comparator.comparing(LemmaTable::getFrequency));
        List<Integer> pageId = new ArrayList<>();
        for (int i = 0; i < lemmaTableList.size(); i++) {
            List<Integer> list = indexTableService.findIdPageByLemmas(lemmaTableList.get(i).getId());
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
        Map<Integer, List<String>> idPage = new HashMap<>();
        List<String> lemmaList = new ArrayList<>();
        lemmaTableList.forEach(o -> lemmaList.add(o.getLemma()));
        pageId.forEach(t -> idPage.put(t, lemmaList));
        return idPage;
    }

    private Map<Integer, Float> findRelativeRelevance(Map<Integer, List<String>> listIdPageAndLemma) {
        Map<Integer, Float> index = new HashMap<>();
        for (Integer idPage : listIdPageAndLemma.keySet()) {
            for (String lemma : listIdPageAndLemma.get(idPage)) {
                Float rank = indexTableService.findRank(lemma, idPage);
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
        PageTable pageTable = pageTableService.findById(pageId);
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

    private StringBuilder getSnippet(Document document, List<String> searchText) {
        StringBuilder body = new StringBuilder(getTitle(document)).append(" ");
        Elements elementsBody = document.select("body");
        elementsBody.forEach(element -> body.append(element.text()));
        System.out.println(searchText);
        List<String> listText = lemmatizer.findWord(body, searchText);
        System.out.println(listText);
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
