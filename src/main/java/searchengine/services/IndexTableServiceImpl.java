package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.IndexTable;
import searchengine.repositories.IndexTableRepository;

import java.util.List;

@Service
public class IndexTableServiceImpl implements IndexTableService {

    private final IndexTableRepository indexTableRepository;

    @Autowired
    public IndexTableServiceImpl(IndexTableRepository indexTableRepository) {
        this.indexTableRepository = indexTableRepository;
    }

    @Override
    public synchronized void save(IndexTable indexTable) {
        indexTableRepository.save(indexTable);
    }

    @Override
    public synchronized void saveAll(List<IndexTable> list) {
        indexTableRepository.saveAll(list);
    }

    @Override
    public List<String> findByPages(int idPage) {
        return indexTableRepository.findByPageTable_Id(idPage);
    }

    @Override
    public Float findRank(String lemma, Integer page) {
        return indexTableRepository.findByLemmaTable_LemmaAndPageTable_Id(lemma, page)
                .describeConstable().orElse(0.0F);
    }

    @Override
    public List<Integer> findIdPageByLemmas(int idLemma) {
        return indexTableRepository.findByLemmaTable_IdPage(idLemma);
    }
}
