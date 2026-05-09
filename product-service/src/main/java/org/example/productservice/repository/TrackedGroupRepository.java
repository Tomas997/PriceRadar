package org.example.productservice.repository;

import org.example.productservice.model.TrackedGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrackedGroupRepository extends JpaRepository<TrackedGroup, Long> {
    List<TrackedGroup> findByUserEmail(String userEmail);
}
