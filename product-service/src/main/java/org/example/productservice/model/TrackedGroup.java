package org.example.productservice.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tracked_groups")
@Data
@NoArgsConstructor
public class TrackedGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userEmail;
    private String telegramChatId;
    private Long lastMinPrice;
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private boolean telegramBlocked = false;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<TrackedItem> items = new ArrayList<>();
}
