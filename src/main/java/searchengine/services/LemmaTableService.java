package searchengine.services;

import searchengine.model.LemmaTable;
import searchengine.model.SiteTable;

public interface LemmaTableService {

    void save(LemmaTable lemmaTable);

    void update(String lemma, int frequency, SiteTable siteTable);

    long count(SiteTable siteTable);

    LemmaTable findLemmaOnSite(String lemma, int idSite);

    LemmaTable findLemmasById(int idLemma);

    void delete(String lemma, SiteTable siteTable);
}
