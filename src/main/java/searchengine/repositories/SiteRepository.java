package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.SiteEntity;

import java.util.Optional;

@Transactional
public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {

    @Modifying
    @Query(value = "ALTER TABLE `sites` AUTO_INCREMENT = 0", nativeQuery = true)
    void resetId();

    Optional<SiteEntity> findSiteEntityByUrl(String url);
}
