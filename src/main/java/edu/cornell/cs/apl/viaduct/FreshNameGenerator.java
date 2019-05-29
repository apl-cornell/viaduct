package edu.cornell.cs.apl.viaduct;

import java.util.HashMap;
import java.util.Map;

/** generate fresh names from base names. */
public class FreshNameGenerator {
  Map<String, Integer> freshNameMap;

  public FreshNameGenerator() {
    this.freshNameMap = new HashMap<>();
  }

  /** get fresh name from base name.
   * base name must NOT have an underscore! */
  public String getFreshName(String base) {
    Integer i = this.freshNameMap.get(base);
    if (i != null) {
      this.freshNameMap.put(base, i + 1);
      return String.format("%s_%d", base, i);

    } else {
      this.freshNameMap.put(base, 2);
      return base;
    }
  }
}
