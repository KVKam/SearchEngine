package searchengine.repositories;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteTable;
import searchengine.model.StatusType;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SiteRepository extends CrudRepository<SiteTable, Integer> {

    @Query(value = "select new java.lang.Boolean(count(*) > 0) from SiteTable s where s.status = ?1")
    boolean isStatus(StatusType status);
    @Transactional
    @Modifying
    @Query("update SiteTable s set s.status = ?1, s.statusTime = ?2, s.lastError = ?3 where s.id = ?4")
    void updateStatusAndStatusTimeAndLastErrorById(StatusType status, LocalDateTime statusTime, String lastError, int id);

    @Query( "SELECT s.url FROM SiteTable s" )
    List<String> findAllUrl();

    SiteTable findByUrl(String url);
}
