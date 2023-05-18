package searchengine.services;

import searchengine.model.SiteTable;
import searchengine.model.StatusType;

import java.util.List;

public interface SiteTableService {

    boolean presenceStatusType(StatusType statusType);

    void update(SiteTable siteTable);

    void save(SiteTable siteTable);

    SiteTable findUrl(String url);

    List<String> getAllUrl();

    List<SiteTable> getAll();

    long count();

    void delete(int id);

}
