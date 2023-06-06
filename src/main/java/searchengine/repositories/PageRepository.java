package searchengine.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.PageTable;
import searchengine.model.SiteTable;

@Repository
public interface PageRepository extends CrudRepository<PageTable, Integer> {
    long countBySiteTable(SiteTable siteTable);

    PageTable findByPathAndSiteTable_Id(String path, int id);


}
