package edu.cornell.cs.apl.viaduct;

import java.util.ArrayList;

/** Used for maintaining ordering information about PDG nodes. */
public class AbstractLineNumber {
  ArrayList<Integer> numberList;

  public AbstractLineNumber() {
    this.numberList = new ArrayList<Integer>();
  }

  /**
   * Compares ordering between abstract line numbers.
   *
   * @param lineno - abstract line number to compare with
   * @return 1 (greater than), 0 (unordered), or -1 (less than)
   */
  public int lessThan(AbstractLineNumber lineno) {
    return lineno.numberList.get(0);
  }
}
