/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.2
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package edu.cornell.cs.apl.viaduct.libsnarkwrapper;

public class VarArray {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected VarArray(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(VarArray obj) {
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
        libsnarkwrapperJNI.delete_VarArray(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setValues(SWIGTYPE_p_libsnark__pb_variable_arrayT_field128_t value) {
    libsnarkwrapperJNI.VarArray_values_set(
        swigCPtr, this, SWIGTYPE_p_libsnark__pb_variable_arrayT_field128_t.getCPtr(value));
  }

  public SWIGTYPE_p_libsnark__pb_variable_arrayT_field128_t getValues() {
    return new SWIGTYPE_p_libsnark__pb_variable_arrayT_field128_t(
        libsnarkwrapperJNI.VarArray_values_get(swigCPtr, this), true);
  }

  public VarArray() {
    this(libsnarkwrapperJNI.new_VarArray(), true);
  }
}
