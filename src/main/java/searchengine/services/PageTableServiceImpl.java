package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.PageTable;
import searchengine.model.SiteTable;
import searchengine.repositories.PageTableRepository;

@Service
public class PageTableServiceImpl implements PageTableService {

    private final PageTableRepository pageTableRepository;

    @Autowired
    public PageTableServiceImpl(PageTableRepository pageTableRepository) {
        this.pageTableRepository = pageTableRepository;
    }

    @Override
    public synchronized void save(PageTable pageTable) {
        pageTableRepository.save(pageTable);
    }

    @Override
    public synchronized void delete(int id) {
        pageTableRepository.deleteById(id);
    }

    @Override
    public PageTable findById(int id) {
        return pageTableRepository.findById(id).orElse(null);
    }

    @Override
    public long count(SiteTable siteTable) {
        if(siteTable == null){
            return pageTableRepository.count();
        }
        return pageTableRepository.countBySiteTable(siteTable);
    }

    @Override
    public PageTable findPathAndSiteId(String path, int siteId) {
        return pageTableRepository.findByPathAndSiteTable_Id(path, siteId);
    }
}
