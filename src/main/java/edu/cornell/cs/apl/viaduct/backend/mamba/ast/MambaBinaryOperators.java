package edu.cornell.cs.apl.viaduct.backend.mamba.ast;

/** binary operators in MAMBA. */
public class MambaBinaryOperators {
  public static final class Or implements MambaBinaryOperator {
    private static final Or INSTANCE = new Or();

    private Or() {}

    public static Or create() {
      return INSTANCE;
    }

    @Override
    public String toString() {
      return "||";
    }
  }

  public static final class And implements MambaBinaryOperator {
    private static final And INSTANCE = new And();

    private And() {}

    public static And create() {
      return INSTANCE;
    }

    @Override
    public String toString() {
      return "&&";
    }
  }

  public static final class EqualTo implements MambaBinaryOperator {
    private static final EqualTo INSTANCE = new EqualTo();

    private EqualTo() {}

    public static EqualTo create() {
      return INSTANCE;
    }

    @Override
    public String toString() {
      return "==";
    }
  }

  public static final class LessThan implements MambaBinaryOperator {
    private static final LessThan INSTANCE = new LessThan();

    private LessThan() {}

    public static LessThan create() {
      return INSTANCE;
    }

    @Override
    public String toString() {
      return "<";
    }
  }

  public static final class LessThanOrEqualTo implements MambaBinaryOperator {
    private static final LessThanOrEqualTo INSTANCE = new LessThanOrEqualTo();

    private LessThanOrEqualTo() {}

    public static LessThanOrEqualTo create() {
      return INSTANCE;
    }

    @Override
    public String toString() {
      return "<=";
    }
  }

  public static final class Plus implements MambaBinaryOperator {
    private static final Plus INSTANCE = new Plus();

    private Plus() {}

    public static Plus create() {
      return INSTANCE;
    }

    @Override
    public String toString() {
      return "+";
    }
  }

  public static final class Minus implements MambaBinaryOperator {
    private static final Minus INSTANCE = new Minus();

    private Minus() {}

    public static Minus create() {
      return INSTANCE;
    }

    @Override
    public String toString() {
      return "-";
    }
  }

  public static final class Times implements MambaBinaryOperator {
    private static final Times INSTANCE = new Times();

    private Times() {}

    public static Times create() {
      return INSTANCE;
    }

    @Override
    public String toString() {
      return "*";
    }
  }
}
