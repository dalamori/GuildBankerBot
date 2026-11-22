package com.dalamori.emberline.guildbanker.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "bot_user",
        uniqueConstraints = {@UniqueConstraint(name = "uniq_snowflake", columnNames = {"snowflake"})}
)
public class BotUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "snowflake")
    private String snowflake;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name="auth_level")
    private BotUserAuthLevel authLevel;
}