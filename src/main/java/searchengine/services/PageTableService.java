package searchengine.services;

import searchengine.model.PageTable;
import searchengine.model.SiteTable;

public interface PageTableService {
    void save(PageTable pageTable);

    void delete(int id);

    PageTable findById(int id);

    long count(SiteTable siteTable);

    PageTable findPathAndSiteId(String path, int siteId);
}
