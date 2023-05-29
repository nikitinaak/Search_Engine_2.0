package searchengine.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.SearchData;

@Repository
public interface SearchDataRepository extends PagingAndSortingRepository<SearchData, Integer> {

    Page<SearchData> findAll(Pageable pageable);

    @Query(value = "SELECT COUNT(*) FROM `search_data`", nativeQuery = true)
    int countAllSearchData();
}
