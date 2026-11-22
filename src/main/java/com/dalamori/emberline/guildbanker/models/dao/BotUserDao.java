package com.dalamori.emberline.guildbanker.models.dao;

import com.dalamori.emberline.guildbanker.models.BotUser;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface BotUserDao extends CrudRepository<BotUser, Long> {
    Optional<BotUser> findBySnowflake(String snowflake);
    boolean existsBySnowflake(String snowflake);
    Long deleteBySnowflake(String snowflake);
}
