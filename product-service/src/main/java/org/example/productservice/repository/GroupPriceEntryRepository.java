package org.example.productservice.repository;

import org.example.productservice.model.GroupPriceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupPriceEntryRepository extends JpaRepository<GroupPriceEntry, Long> {
    List<GroupPriceEntry> findByTrackedItemIdInOrderByRecordedAtAsc(List<Long> trackedItemIds);

    Optional<GroupPriceEntry> findTopByTrackedItemIdOrderByRecordedAtDesc(Long trackedItemId);

    boolean existsByTrackedItemIdInAndDemoTrue(List<Long> trackedItemIds);

    void deleteByTrackedItemIdInAndDemoTrue(List<Long> trackedItemIds);
}