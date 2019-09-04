package edu.cornell.cs.apl.viaduct.imp.interpreter;

/** A mutable cell or an array allocated in the program store. */
abstract class AllocatedObject extends Reference {
  /**
   * Returns an immutable value that represents the current contents of this object. This value will
   * not change even if the data stored in this object is modified.
   *
   * <p>Useful for comparing contents of store objects at different times or across different
   * stores.
   */
  abstract Object getImmutableValue();
}
