package edu.cornell.cs.apl.viaduct.imp.ast;

public final class BoolType implements ImpType {
  private static final BoolType INSTANCE = new BoolType();

  private BoolType() {}

  public static BoolType instance() {
    return INSTANCE;
  }

  @Override
  public String toString() {
    return "bool";
  }
}
