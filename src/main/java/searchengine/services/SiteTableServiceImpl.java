package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.SiteTable;
import searchengine.model.StatusType;
import searchengine.repositories.SiteTableRepository;

import java.util.List;

@Service
public class SiteTableServiceImpl implements SiteTableService {

    private final SiteTableRepository siteTableRepository;

    @Autowired
    public SiteTableServiceImpl(SiteTableRepository siteTableRepository) {
        this.siteTableRepository = siteTableRepository;
    }

    @Override
    public synchronized void update(SiteTable siteTable) {
        siteTableRepository.updateStatusAndStatusTimeAndLastErrorById(
                siteTable.getStatus(),
                siteTable.getStatusTime(),
                siteTable.getLastError(),
                siteTable.getId()
        );
    }

    @Override
    public synchronized void save(SiteTable siteTable) {
        siteTableRepository.save(siteTable);
    }

    @Override
    public SiteTable findUrl(String url) {
        return siteTableRepository.findByUrl(url);
    }


    @Override
    public List<String> getAllUrl() {
        return siteTableRepository.findAllUrl();
    }

    @Override
    public List<SiteTable> getAll() {
        return (List<SiteTable>) siteTableRepository.findAll();
    }

    @Override
    public long count() {
        return siteTableRepository.count();
    }

    @Override
    public synchronized void delete(int id) {
        siteTableRepository.deleteById(id);
    }

    @Override
    public boolean presenceStatusType(StatusType statusType) {
        List<SiteTable> status = siteTableRepository.findByStatus(statusType);
        return !status.isEmpty();
    }
}
