package searchengine.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.IndexTable;

import java.util.List;

@Repository
public interface IndexRepository extends CrudRepository<IndexTable, Integer> {
    @Query("select i.lemmaTable.lemma from IndexTable i where i.pageTable.id = ?1")
    List<String> findByPageTable_Id(int id);
    @Query("select i.rankLemma from IndexTable i where i.lemmaTable.lemma = ?1 and i.pageTable.id = ?2")
    Float findByLemmaTable_LemmaAndPageTable_Id(String lemma, int id);
    @Query("select i.pageTable.id from IndexTable i where i.lemmaTable.id = ?1")
    List<Integer> findByLemmaTable_IdPage(int id);


}
