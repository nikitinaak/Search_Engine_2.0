package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.LemmaEntity;
import searchengine.model.SiteEntity;

import java.util.Optional;

@Transactional
public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {

    @Modifying
    @Query(value = "ALTER TABLE `lemmas` AUTO_INCREMENT = 0", nativeQuery = true)
    void resetId();

    @Query(value = "SELECT * FROM `lemmas` WHERE `lemma` = :lemma AND `site_id` = :siteId LIMIT 1",
            nativeQuery = true)
    Optional<LemmaEntity> findFirstByLemmaAndSiteId(String lemma, int siteId);

    int countAllLemmaEntityBySite(SiteEntity site);

    @Query(value = "SELECT * FROM `lemmas` WHERE `lemma` = :lemma AND `site_id` = :siteId", nativeQuery = true)
    Optional<LemmaEntity> findLemmaByLemmaAndSiteId(String lemma, int siteId);

    @Modifying
    void deleteAllLemmaEntityBySite(SiteEntity site);
}
