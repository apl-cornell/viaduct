package edu.cornell.cs.apl.viaduct;

import java.util.ArrayList;

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

  ArrayList<LineNumberComponent> componentList;

  public AbstractLineNumber() {
    this.componentList = new ArrayList<LineNumberComponent>();
  }

  public void addComponent(String m, int seq) {
    this.componentList.add(new LineNumberComponent(m, seq));
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
      return this.compareTo((AbstractLineNumber)o) == 0;

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
    buf.append("<");
    for (LineNumberComponent comp : this.componentList) {
      buf.append("(" + comp.getMarker() + ":" + comp.getSequenceNum() + ")");
    }
    buf.append(">");

    return buf.toString();
  }
}
