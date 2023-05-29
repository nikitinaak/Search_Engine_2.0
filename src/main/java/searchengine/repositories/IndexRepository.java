package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexEntity;
import searchengine.model.PageEntity;

import java.util.List;
import java.util.Optional;

@Transactional
public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    @Modifying
    @Query(value = "ALTER TABLE `indexest` AUTO_INCREMENT = 0", nativeQuery = true)
    void resetId();

    @Modifying
    void deleteIndexEntityByPage(PageEntity page);

    List<IndexEntity> findAllIndexEntityByPage(PageEntity page);

    @Query(value = "SELECT * FROM `indexest` WHERE `page_id` = :pageId AND `lemma_id` = :lemmaId",
            nativeQuery = true)
    Optional<IndexEntity> findByPageIdAndLemmaId(int pageId, int lemmaId);

    @Query(value = "SELECT `page_id` FROM `indexest` WHERE `lemma_id` = :lemmaId", nativeQuery = true)
    List<Integer> findAllPageIdByLemmaId(int lemmaId);
}
