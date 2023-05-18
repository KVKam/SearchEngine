package searchengine.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaTable;
import searchengine.model.SiteTable;

@Repository
public interface LemmaTableRepository extends CrudRepository<LemmaTable, Integer> {
    @Transactional
    @Modifying
    @Query("update LemmaTable l set l.frequency = ?1 where l.lemma = ?2 and l.siteTable = ?3")
    void updateFrequencyByLemmaAndSiteTable(int frequency, String lemma, SiteTable siteTable);



    long countBySiteTable(SiteTable siteTable);


    LemmaTable findByLemmaAndSiteTable_Id(String lemma, int id);

}
