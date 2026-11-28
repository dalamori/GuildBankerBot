package com.dalamori.emberline.guildbanker.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "bot_users",
        uniqueConstraints = {@UniqueConstraint(name = "uniq_snowflake", columnNames = {"snowflake"})}
)
public class BotUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, name = "snowflake")
    private Long snowflake;

    @Column(nullable = false, name = "last_nick")
    private String lastKnownNickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name="auth_level")
    private BotUserAuthLevel authLevel;
}