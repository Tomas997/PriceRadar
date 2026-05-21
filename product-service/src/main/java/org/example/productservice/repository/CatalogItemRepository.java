package org.example.productservice.repository;

import org.example.productservice.model.CatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogItemRepository extends JpaRepository<CatalogItem, Long> {
    Optional<CatalogItem> findByUrl(String url);

    @Query("SELECT DISTINCT ti.catalogItem FROM TrackedItem ti WHERE ti.group.telegramBlocked = false")
    List<CatalogItem> findActiveItems();
}