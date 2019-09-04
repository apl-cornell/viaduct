package edu.cornell.cs.apl.viaduct.imp.ast;

/** Objects that name things. */
public interface Name {
  /** The given name. */
  String getName();

  /** Class of things this objects names. */
  String getNameCategory();
}
