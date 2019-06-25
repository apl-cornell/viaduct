package edu.cornell.cs.apl.viaduct.imp.ast;

public final class IntType implements ImpType {
  private static final IntType INSTANCE = new IntType();

  private IntType() {}

  public static IntType instance() {
    return INSTANCE;
  }

  @Override
  public String toString() {
    return "int";
  }
}
