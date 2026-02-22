package com.example.bill_manager.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PurchaseCategory {
  GROCERY("grocery"),
  ELECTRONICS("electronics"),
  RESTAURANT("restaurant"),
  PHARMACY("pharmacy"),
  CLOTHING("clothing"),
  HOME_AND_GARDEN("home_and_garden"),
  TRANSPORT("transport"),
  ENTERTAINMENT("entertainment"),
  SERVICES("services"),
  OTHER("other");

  private final String displayName;

  PurchaseCategory(final String displayName) {
    this.displayName = displayName;
  }

  @JsonValue
  public String getDisplayName() {
    return displayName;
  }

  @JsonCreator
  public static PurchaseCategory fromString(final String value) {
    if (value == null) {
      return OTHER;
    }
    for (final PurchaseCategory category : values()) {
      if (category.displayName.equalsIgnoreCase(value)) {
        return category;
      }
    }
    return OTHER;
  }
}
