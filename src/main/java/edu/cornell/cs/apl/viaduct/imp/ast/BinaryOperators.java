package edu.cornell.cs.apl.viaduct.imp.ast;

/** Enumerates binary operators in the language. */
public class BinaryOperators {
  public static final class Or extends BinaryOperator {
    private static final Or INSTANCE = new Or();

    private Or() {}

    /** Create an instance of this class. */
    public static Or create() {
      return INSTANCE;
    }

    @Override
    public ImpValue evaluate(ImpValue left, ImpValue right) {
      boolean leftV = ((BooleanValue) left).getValue();
      boolean rightV = ((BooleanValue) right).getValue();
      return new BooleanValue(leftV || rightV);
    }

    @Override
    public String toString() {
      return "||";
    }
  }

  public static final class And extends BinaryOperator {
    private static final And INSTANCE = new And();

    private And() {}

    /** Create an instance of this class. */
    public static And create() {
      return INSTANCE;
    }

    @Override
    public ImpValue evaluate(ImpValue left, ImpValue right) {
      boolean leftV = ((BooleanValue) left).getValue();
      boolean rightV = ((BooleanValue) right).getValue();
      return new BooleanValue(leftV && rightV);
    }

    @Override
    public String toString() {
      return "&&";
    }
  }

  public static final class EqualTo extends BinaryOperator {
    private static final EqualTo INSTANCE = new EqualTo();

    private EqualTo() {}

    /** Create an instance of this class. */
    public static EqualTo create() {
      return INSTANCE;
    }

    @Override
    public ImpValue evaluate(ImpValue left, ImpValue right) {
      return new BooleanValue(left.equals(right));
    }

    @Override
    public String toString() {
      return "==";
    }
  }

  public static final class LessThan extends BinaryOperator {
    private static final LessThan INSTANCE = new LessThan();

    private LessThan() {}

    /** Create an instance of this class. */
    public static LessThan create() {
      return INSTANCE;
    }

    @Override
    public ImpValue evaluate(ImpValue left, ImpValue right) {
      int leftV = ((IntegerValue) left).getValue();
      int rightV = ((IntegerValue) right).getValue();
      return new BooleanValue(leftV < rightV);
    }

    @Override
    public String toString() {
      return "<";
    }
  }

  public static final class LessThanOrEqualTo extends BinaryOperator {
    private static final LessThanOrEqualTo INSTANCE = new LessThanOrEqualTo();

    private LessThanOrEqualTo() {}

    /** Create an instance of this class. */
    public static LessThanOrEqualTo create() {
      return INSTANCE;
    }

    @Override
    public ImpValue evaluate(ImpValue left, ImpValue right) {
      int leftV = ((IntegerValue) left).getValue();
      int rightV = ((IntegerValue) right).getValue();
      return new BooleanValue(leftV <= rightV);
    }

    @Override
    public String toString() {
      return "<=";
    }
  }

  public static final class Plus extends BinaryOperator {
    private static final Plus INSTANCE = new Plus();

    private Plus() {}

    /** Create an instance of this class. */
    public static Plus create() {
      return INSTANCE;
    }

    @Override
    public ImpValue evaluate(ImpValue left, ImpValue right) {
      int leftV = ((IntegerValue) left).getValue();
      int rightV = ((IntegerValue) right).getValue();
      return new IntegerValue(leftV + rightV);
    }

    @Override
    public String toString() {
      return "+";
    }
  }
}
