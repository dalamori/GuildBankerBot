package com.dalamori.emberline.guildbanker.models.dao;

import com.dalamori.emberline.guildbanker.models.Loot;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface LootDao extends CrudRepository<Loot, Long> {
    List<Loot> findByNameStartingWith(String prefix);
    List<Loot> findByHeldBy(String heldBy);
    Optional<Loot> findByNameAndHeldBy(String name, String heldBy);
}
