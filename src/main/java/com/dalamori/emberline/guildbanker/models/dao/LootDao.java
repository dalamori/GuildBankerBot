package com.dalamori.emberline.guildbanker.models.dao;

import com.dalamori.emberline.guildbanker.models.Loot;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface LootDao extends CrudRepository<Loot, Long> {
    List<Loot> findByNameStartingWith(String prefix);
    List<Loot> findByHeldBy(Long heldBy);
    Optional<Loot> findByNameAndHeldBy(String name, Long heldBy);

    List<Loot> findByNameStartingWithAndQuantityLessThan(String prefix, Float maxQuantity);
    List<Loot> findByNameStartingWithAndQuantityGreaterThan(String prefix, Float minQuantity);
    List<Loot> findByQuantityLessThan(Float maxQuantity);
    List<Loot> findByQuantityGreaterThan(Float MinQuantity);

    @Transactional
    void deleteByHeldBy(Long heldBy);
}
