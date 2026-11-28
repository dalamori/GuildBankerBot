package com.dalamori.emberline.guildbanker.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "loot_autocomplete")
public class LootAutocomplete {
  @Id
  @Column(nullable = false, name = "name")
  private String name;
}