package com.dalamori.emberline.guildbanker.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "loot_logs")
public class LootLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, name="id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(nullable = false, name="loot_id", referencedColumnName = "id")
    private Loot loot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name="type")
    private LootLogType type;

    @Column(nullable = false, name = "who_said")
    private String whoSaid;

    @Column(nullable = false, name="reason")
    private String reason;
}