package com.dalamori.emberline.guildbanker.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(
        name = "loot_items",
        uniqueConstraints = {@UniqueConstraint(name = "uniq_name_heldby", columnNames = {"name", "held_by"})}
)
public class Loot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, name = "id")
    private Long id;

    @Column(nullable = false, name = "name")
    private String name;

    @Column(nullable = false, name = "quantity")
    private float quantity;

    @Column(nullable = false, name = "when_created")
    private Instant whenCreated;

    @Column(nullable = false, name = "last_seen")
    private Instant lastSeen;

    @Column(nullable = false, name = "held_by")
    private String heldBy;

    @OneToMany(mappedBy = "loot", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<LootLog> lootLogs = new ArrayList<>();
}