package org.example.productservice.repository;

import org.example.productservice.model.GroupPriceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupPriceEntryRepository extends JpaRepository<GroupPriceEntry, Long> {
    List<GroupPriceEntry> findByCatalogItemIdInOrderByRecordedAtAsc(List<Long> catalogItemIds);

    Optional<GroupPriceEntry> findTopByCatalogItemIdOrderByRecordedAtDesc(Long catalogItemId);

    boolean existsByCatalogItemId(Long catalogItemId);

    boolean existsByCatalogItemIdInAndDemoTrue(List<Long> catalogItemIds);

    void deleteByCatalogItemIdInAndDemoTrue(List<Long> catalogItemIds);
}