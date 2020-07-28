package edu.cornell.cs.apl.viaduct.util;

import java.util.HashMap;
import java.util.Map;

/** Generates distinct names. Never generates the same name twice. */
public class FreshNameGenerator {
  private final Map<String, Integer> freshNameMap = new HashMap<>();

  /** Returns a new name derived from base. */
  public String getFreshName(String base) {
    final Integer i = this.freshNameMap.get(base);
    if (i != null) {
      this.freshNameMap.put(base, i + 1);
      return String.format("%s_%d", base, i);

    } else {
      this.freshNameMap.put(base, 1);
      return base;
    }
  }
}
