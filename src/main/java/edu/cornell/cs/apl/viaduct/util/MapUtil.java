package edu.cornell.cs.apl.viaduct.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class MapUtil {
  /** Create a map given a list of keys and a separate list of values. */
  public static <K, V> Map<K, V> zip(Iterable<K> keys, Iterable<V> values) {
    final Iterator<K> keyIterator = keys.iterator();
    final Iterator<V> valueIterator = values.iterator();
    final Map<K, V> result = new HashMap<>();

    while (keyIterator.hasNext() && valueIterator.hasNext()) {
      result.put(keyIterator.next(), valueIterator.next());
    }

    return result;
  }
}
