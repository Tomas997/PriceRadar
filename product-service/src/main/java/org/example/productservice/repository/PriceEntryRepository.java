package org.example.productservice.repository;

import org.example.productservice.model.PriceEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PriceEntryRepository extends JpaRepository<PriceEntry, Long> {
    List<PriceEntry> findByProductIdOrderByParsedAtDesc(Long productId);
    void deleteByProductId(Long productId);
}