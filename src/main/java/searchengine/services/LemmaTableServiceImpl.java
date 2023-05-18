package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.LemmaTable;
import searchengine.model.SiteTable;
import searchengine.repositories.LemmaTableRepository;

@Service
public class LemmaTableServiceImpl implements LemmaTableService {

    private final LemmaTableRepository lemmaTableRepository;

    @Autowired
    public LemmaTableServiceImpl(LemmaTableRepository lemmaTableRepository) {
        this.lemmaTableRepository = lemmaTableRepository;
    }

    @Override
    public synchronized void save(LemmaTable lemmaTable) {
        lemmaTableRepository.save(lemmaTable);
    }

    @Override
    public synchronized void update(String lemma, int frequency, SiteTable siteTable) {
        lemmaTableRepository.updateFrequencyByLemmaAndSiteTable(frequency, lemma, siteTable);
    }

    @Override
    public long count(SiteTable siteTable) {
        if (siteTable == null) {
            return lemmaTableRepository.count();
        }
        return lemmaTableRepository.countBySiteTable(siteTable);
    }

    @Override
    public LemmaTable findLemmaOnSite(String lemma, int idSite) {
        return lemmaTableRepository.findByLemmaAndSiteTable_Id(lemma, idSite);
    }

    @Override
    public LemmaTable findLemmasById(int idLemma) {
        return lemmaTableRepository.findById(idLemma).orElseThrow();
    }

    @Override
    public synchronized void delete(String lemma, SiteTable siteTable) {
        LemmaTable lemmaTable = lemmaTableRepository.findByLemmaAndSiteTable_Id(lemma, siteTable.getId());
        if (lemmaTable.getFrequency() > 1) {
            lemmaTable.setFrequency(lemmaTable.getFrequency() - 1);
            lemmaTableRepository.updateFrequencyByLemmaAndSiteTable(
                    lemmaTable.getFrequency(), lemma, siteTable);
        } else {
            lemmaTableRepository.deleteById(lemmaTable.getId());
        }
    }
}
