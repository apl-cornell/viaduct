/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 4.0.2
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package edu.cornell.cs.apl.viaduct.libsnarkwrapper;

public class ByteBuf {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected ByteBuf(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(ByteBuf obj) {
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
        libsnarkwrapperJNI.delete_ByteBuf(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void setContents(String value) {
    libsnarkwrapperJNI.ByteBuf_contents_set(swigCPtr, this, value);
  }

  public String getContents() {
    return libsnarkwrapperJNI.ByteBuf_contents_get(swigCPtr, this);
  }

  public byte[] get_data() {
    return libsnarkwrapperJNI.ByteBuf_get_data(swigCPtr, this);
  }

  public ByteBuf() {
    this(libsnarkwrapperJNI.new_ByteBuf(), true);
  }
}
