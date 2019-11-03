package edu.cornell.cs.apl.viaduct.util;

import io.vavr.collection.List;


/** Used for maintaining ordering information about PDG nodes. */
public class AbstractLineNumber implements Comparable<AbstractLineNumber> {
  static class LineNumberComponent {
    String marker;
    int seqNum;

    LineNumberComponent(String m, int seq) {
      this.marker = m;
      this.seqNum = seq;
    }

    String getMarker() {
      return this.marker;
    }

    int getSequenceNum() {
      return this.seqNum;
    }
  }

  List<LineNumberComponent> componentList;

  private AbstractLineNumber() {
    this.componentList = List.empty();
  }

  public AbstractLineNumber(String main) {
    this.componentList = List.of(new LineNumberComponent(main, 1));
  }

  /** returns a clone but with the last component's sequence number incremented. */
  public AbstractLineNumber increment() {
    AbstractLineNumber newLn = new AbstractLineNumber();

    int n = this.componentList.size();
    LineNumberComponent last = this.componentList.get(n - 1);
    newLn.componentList = this.componentList.append(new LineNumberComponent(last.getMarker(), last.getSequenceNum() + 1));

    return newLn;
  }

  /** returns a clone but with a new component appended. */
  public AbstractLineNumber addBranch(String branch) {
    AbstractLineNumber newLn = new AbstractLineNumber();
    newLn.componentList = this.componentList.append(new LineNumberComponent(branch, 1));
    return newLn;
  }

  /**
   * Compares ordering between abstract line numbers.
   *
   * @return 1 (greater than), 0 (unordered), or -1 (less than)
   */
  public int compareTo(AbstractLineNumber other) {
    int sizeThis = this.componentList.size();
    int sizeOther = other.componentList.size();
    int size = sizeThis > sizeOther ? sizeOther : sizeThis;

    for (int i = 0; i < size; i++) {
      LineNumberComponent thisComp = this.componentList.get(i);
      LineNumberComponent otherComp = other.componentList.get(i);

      // if markers are equal, then we can compare components.
      // otherwise if markers are not equal, line numbers are incomparable
      if (thisComp.getMarker().equals(otherComp.getMarker())) {
        int thisSeq = thisComp.getSequenceNum();
        int otherSeq = otherComp.getSequenceNum();

        if (thisSeq > otherSeq) {
          return 1;
        } else if (thisSeq < otherSeq) {
          return -1;
        } else {
          continue;
        }

      } else {
        return 0;
      }
    }

    // we can only reach this if all components are equal,
    // thus at this point we know line numbers are equal
    return 0;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof AbstractLineNumber) {
      return this.compareTo((AbstractLineNumber) o) == 0;

    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return this.componentList.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("[");
    for (LineNumberComponent comp : this.componentList) {
      buf.append("(" + comp.getMarker() + ":" + comp.getSequenceNum() + ")");
    }
    buf.append("]");

    return buf.toString();
  }
}
