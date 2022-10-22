/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.2
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package io.github.aplcornell.viaduct.libsnarkwrapper;

public class BoolVector extends java.util.AbstractList<Boolean> implements java.util.RandomAccess {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected BoolVector(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(BoolVector obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  @SuppressWarnings("deprecation")
  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        libsnarkwrapperJNI.delete_BoolVector(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public BoolVector(boolean[] initialElements) {
    this();
    reserve(initialElements.length);

    for (boolean element : initialElements) {
      add(element);
    }
  }

  public BoolVector(Iterable<Boolean> initialElements) {
    this();
    for (boolean element : initialElements) {
      add(element);
    }
  }

  public Boolean get(int index) {
    return doGet(index);
  }

  public Boolean set(int index, Boolean e) {
    return doSet(index, e);
  }

  public boolean add(Boolean e) {
    modCount++;
    doAdd(e);
    return true;
  }

  public void add(int index, Boolean e) {
    modCount++;
    doAdd(index, e);
  }

  public Boolean remove(int index) {
    modCount++;
    return doRemove(index);
  }

  protected void removeRange(int fromIndex, int toIndex) {
    modCount++;
    doRemoveRange(fromIndex, toIndex);
  }

  public int size() {
    return doSize();
  }

  public BoolVector() {
    this(libsnarkwrapperJNI.new_BoolVector__SWIG_0(), true);
  }

  public BoolVector(BoolVector other) {
    this(libsnarkwrapperJNI.new_BoolVector__SWIG_1(BoolVector.getCPtr(other), other), true);
  }

  public long capacity() {
    return libsnarkwrapperJNI.BoolVector_capacity(swigCPtr, this);
  }

  public void reserve(long n) {
    libsnarkwrapperJNI.BoolVector_reserve(swigCPtr, this, n);
  }

  public boolean isEmpty() {
    return libsnarkwrapperJNI.BoolVector_isEmpty(swigCPtr, this);
  }

  public void clear() {
    libsnarkwrapperJNI.BoolVector_clear(swigCPtr, this);
  }

  public BoolVector(int count, boolean value) {
    this(libsnarkwrapperJNI.new_BoolVector__SWIG_2(count, value), true);
  }

  private int doSize() {
    return libsnarkwrapperJNI.BoolVector_doSize(swigCPtr, this);
  }

  private void doAdd(boolean x) {
    libsnarkwrapperJNI.BoolVector_doAdd__SWIG_0(swigCPtr, this, x);
  }

  private void doAdd(int index, boolean x) {
    libsnarkwrapperJNI.BoolVector_doAdd__SWIG_1(swigCPtr, this, index, x);
  }

  private boolean doRemove(int index) {
    return libsnarkwrapperJNI.BoolVector_doRemove(swigCPtr, this, index);
  }

  private boolean doGet(int index) {
    return libsnarkwrapperJNI.BoolVector_doGet(swigCPtr, this, index);
  }

  private boolean doSet(int index, boolean val) {
    return libsnarkwrapperJNI.BoolVector_doSet(swigCPtr, this, index, val);
  }

  private void doRemoveRange(int fromIndex, int toIndex) {
    libsnarkwrapperJNI.BoolVector_doRemoveRange(swigCPtr, this, fromIndex, toIndex);
  }
}
