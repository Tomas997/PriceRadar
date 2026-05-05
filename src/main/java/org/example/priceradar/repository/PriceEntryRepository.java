package org.example.priceradar.repository;

import org.example.priceradar.model.PriceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PriceEntryRepository extends JpaRepository<PriceEntry, Long> {
    List<PriceEntry> findByProductIdOrderByParsedAtDesc(Long productId);
}