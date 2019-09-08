package edu.cornell.cs.apl.viaduct.imp.ast;

import edu.cornell.cs.apl.viaduct.imp.ast.types.BinaryOperatorType;
import edu.cornell.cs.apl.viaduct.imp.ast.types.BooleanType;
import edu.cornell.cs.apl.viaduct.imp.ast.types.ImpBaseType;
import edu.cornell.cs.apl.viaduct.imp.ast.types.IntegerType;
import edu.cornell.cs.apl.viaduct.imp.ast.values.BooleanValue;
import edu.cornell.cs.apl.viaduct.imp.ast.values.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.values.IntegerValue;

/** Enumerates binary operators in the language. */
public class BinaryOperators {
  private abstract static class BinaryLogicalOperator extends BinaryOperator {
    @Override
    public BinaryOperatorType getType() {
      final ImpBaseType bool = BooleanType.create();
      return BinaryOperatorType.create(bool, bool, bool);
    }
  }

  public static final class Or extends BinaryLogicalOperator {
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
      return BooleanValue.create(leftV || rightV);
    }

    @Override
    public String toString() {
      return "||";
    }
  }

  public static final class And extends BinaryLogicalOperator {
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
      return BooleanValue.create(leftV && rightV);
    }

    @Override
    public String toString() {
      return "&&";
    }
  }

  private abstract static class ArithComparisonOperator extends BinaryOperator {
    @Override
    public BinaryOperatorType getType() {
      final ImpBaseType bool = BooleanType.create();
      final ImpBaseType integer = IntegerType.create();
      return BinaryOperatorType.create(bool, integer, integer);
    }
  }

  // TODO: this should also work with booleans
  public static final class EqualTo extends ArithComparisonOperator {
    private static final EqualTo INSTANCE = new EqualTo();

    private EqualTo() {}

    /** Create an instance of this class. */
    public static EqualTo create() {
      return INSTANCE;
    }

    @Override
    public ImpValue evaluate(ImpValue left, ImpValue right) {
      return BooleanValue.create(left.equals(right));
    }

    @Override
    public String toString() {
      return "==";
    }
  }

  public static final class LessThan extends ArithComparisonOperator {
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
      return BooleanValue.create(leftV < rightV);
    }

    @Override
    public String toString() {
      return "<";
    }
  }

  public static final class LessThanOrEqualTo extends ArithComparisonOperator {
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
      return BooleanValue.create(leftV <= rightV);
    }

    @Override
    public String toString() {
      return "<=";
    }
  }

  private abstract static class ArithmeticOperator extends BinaryOperator {
    @Override
    public BinaryOperatorType getType() {
      final ImpBaseType integer = IntegerType.create();
      return BinaryOperatorType.create(integer, integer, integer);
    }
  }

  public static final class Plus extends ArithmeticOperator {
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
      return IntegerValue.create(leftV + rightV);
    }

    @Override
    public String toString() {
      return "+";
    }
  }

  public static final class Minus extends ArithmeticOperator {
    private static final Minus INSTANCE = new Minus();

    private Minus() {}

    /** Create an instance of this class. */
    public static Minus create() {
      return INSTANCE;
    }

    @Override
    public ImpValue evaluate(ImpValue left, ImpValue right) {
      int leftV = ((IntegerValue) left).getValue();
      int rightV = ((IntegerValue) right).getValue();
      return IntegerValue.create(leftV - rightV);
    }

    @Override
    public String toString() {
      return "-";
    }
  }

  public static final class Times extends ArithmeticOperator {
    private static final Times INSTANCE = new Times();

    private Times() {}

    /** Create an instance of this class. */
    public static Times create() {
      return INSTANCE;
    }

    @Override
    public ImpValue evaluate(ImpValue left, ImpValue right) {
      int leftV = ((IntegerValue) left).getValue();
      int rightV = ((IntegerValue) right).getValue();
      return IntegerValue.create(leftV * rightV);
    }

    @Override
    public String toString() {
      return "*";
    }
  }
}
