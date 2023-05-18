package searchengine.services;

import searchengine.model.IndexTable;

import java.util.List;

public interface IndexTableService {

    void save(IndexTable indexTable);

    void saveAll(List<IndexTable> list);

    List<String> findByPages(int id);

    Float findRank(String lemma, Integer page);

    List<Integer> findIdPageByLemmas(int idLemma);
}
