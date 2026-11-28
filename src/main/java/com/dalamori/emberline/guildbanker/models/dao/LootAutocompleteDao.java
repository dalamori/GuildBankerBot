package com.dalamori.emberline.guildbanker.models.dao;

import com.dalamori.emberline.guildbanker.models.LootAutocomplete;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface LootAutocompleteDao extends CrudRepository<LootAutocomplete, String> {
    List<LootAutocomplete> findFirst25ByNameStartingWith(String prefix);
}
