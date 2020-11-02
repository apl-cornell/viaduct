/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.2
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package edu.cornell.cs.apl.viaduct.libsnarkwrapper;

public class Constraint {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected Constraint(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(Constraint obj) {
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
        libsnarkwrapperJNI.delete_Constraint(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setLhs(LinComb value) {
    libsnarkwrapperJNI.Constraint_lhs_set(swigCPtr, this, LinComb.getCPtr(value), value);
  }

  public LinComb getLhs() {
    long cPtr = libsnarkwrapperJNI.Constraint_lhs_get(swigCPtr, this);
    return (cPtr == 0) ? null : new LinComb(cPtr, false);
  }

  public void setRhs(LinComb value) {
    libsnarkwrapperJNI.Constraint_rhs_set(swigCPtr, this, LinComb.getCPtr(value), value);
  }

  public LinComb getRhs() {
    long cPtr = libsnarkwrapperJNI.Constraint_rhs_get(swigCPtr, this);
    return (cPtr == 0) ? null : new LinComb(cPtr, false);
  }

  public void setEq(LinComb value) {
    libsnarkwrapperJNI.Constraint_eq_set(swigCPtr, this, LinComb.getCPtr(value), value);
  }

  public LinComb getEq() {
    long cPtr = libsnarkwrapperJNI.Constraint_eq_get(swigCPtr, this);
    return (cPtr == 0) ? null : new LinComb(cPtr, false);
  }

  public Constraint() {
    this(libsnarkwrapperJNI.new_Constraint(), true);
  }
}
