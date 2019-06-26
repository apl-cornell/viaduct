package edu.cornell.cs.apl.viaduct.imp.ast;

public final class IntegerType implements ImpType {
  private static final IntegerType INSTANCE = new IntegerType();

  private IntegerType() {}

  public static IntegerType create() {
    return INSTANCE;
  }

  @Override
  public String toString() {
    return "int";
  }
}
