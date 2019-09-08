package edu.cornell.cs.apl.viaduct.util;

import com.ibm.icu.text.BreakIterator;

/** Helper functions for dealing with Unicode strings. */
public final class UnicodeUtil {
  private static final UnicodeUtil INSTANCE = new UnicodeUtil();

  // Apparently these are expensive to create, so we reuse.
  private final BreakIterator characterIterator = BreakIterator.getCharacterInstance();

  private UnicodeUtil() {}

  /** Count the number of Grapheme Clusters (i.e. user-perceived characters) in the given string. */
  public static int countGraphemeClusters(String string) {
    synchronized (INSTANCE) {
      INSTANCE.characterIterator.setText(string);
      INSTANCE.characterIterator.first();

      int count = 0;
      while (INSTANCE.characterIterator.next() != BreakIterator.DONE) {
        count += 1;
      }
      return count;
    }
  }
}
