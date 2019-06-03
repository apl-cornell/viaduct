package edu.cornell.cs.apl.viaduct;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author st0le
 * https://stackoverflow.com/a/3078491
 */
public class PowersetIterator<E> implements Iterator<Set<E>>, Iterable<Set<E>> {
  private E[] arr = null;
  private BitSet bset = null;

  @SuppressWarnings("unchecked")
  public PowersetIterator(Set<E> set) {
    arr = (E[])set.toArray();
    bset = new BitSet(arr.length + 1);
  }

  @Override
  public boolean hasNext() {
    return !bset.get(arr.length);
  }

  @Override
  public Set<E> next() {
    Set<E> returnSet = new TreeSet<E>();
    for (int i = 0; i < arr.length; i++) {
      if (bset.get(i)) {
        returnSet.add(arr[i]);
      }
    }

    // increment bset
    for (int i = 0; i < bset.size(); i++) {
      if (!bset.get(i)) {
        bset.set(i);
        break;

      } else {
        bset.clear(i);
      }
    }

    return returnSet;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException("Not Supported!");
  }

  @Override
  public Iterator<Set<E>> iterator() {
    return this;
  }
}
