package edu.cornell.cs.apl.viaduct.imp.transformers;

import edu.cornell.cs.apl.viaduct.errors.ElaborationException;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ArrayIndexingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssertNode;
import edu.cornell.cs.apl.viaduct.imp.ast.AssignNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BinaryExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.BreakNode;
import edu.cornell.cs.apl.viaduct.imp.ast.DowngradeNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ForNode;
import edu.cornell.cs.apl.viaduct.imp.ast.HostDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.IfNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LetBindingNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LiteralNode;
import edu.cornell.cs.apl.viaduct.imp.ast.LoopNode;
import edu.cornell.cs.apl.viaduct.imp.ast.NotNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReadNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReceiveNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ReferenceNode;
import edu.cornell.cs.apl.viaduct.imp.ast.SendNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.ast.TopLevelDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.Variable;
import edu.cornell.cs.apl.viaduct.imp.ast.VariableDeclarationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.WhileNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.AbstractExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.AbstractProgramVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.AbstractReferenceVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ReferenceVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.StmtVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.TopLevelDeclarationVisitor;
import edu.cornell.cs.apl.viaduct.util.FreshNameGenerator;
import java.util.LinkedList;
import java.util.List;

/**
 * Convert a program into A-normal form. In A-normal form, every operation is performed on literal
 * constants or variables.
 */
public class AnfConverter {
  private static final String TMP_NAME = "tmp";

  public static ProgramNode run(ProgramNode program) {
    return program.accept(new AnfProgramVisitor());
  }

  // TODO: we should never need this.
  public static StatementNode run(StatementNode program) {
    return program.accept(new AnfStmtVisitor());
  }

  /** Create a read node from a reference while maintaining source location. */
  private static ExpressionNode read(ReferenceNode reference) {
    return ReadNode.builder().setReference(reference).setSourceLocation(reference).build();
  }

  /** Create a block node from a single statement. */
  private static BlockNode single(StatementNode statement) {
    return BlockNode.builder().add(statement).build();
  }

  private static final class AnfProgramVisitor
      extends AbstractProgramVisitor<AnfProgramVisitor, TopLevelDeclarationNode, ProgramNode> {
    private final AnfDeclarationVisitor declarationVisitor = new AnfDeclarationVisitor();

    @Override
    protected TopLevelDeclarationVisitor<TopLevelDeclarationNode> getDeclarationVisitor() {
      return declarationVisitor;
    }

    @Override
    protected AnfProgramVisitor enter(ProgramNode node) {
      return this;
    }

    @Override
    protected ProgramNode leave(
        ProgramNode node,
        AnfProgramVisitor visitor,
        Iterable<TopLevelDeclarationNode> declarations) {
      return node.toBuilder().setDeclarations(declarations).build();
    }
  }

  private static final class AnfDeclarationVisitor
      implements TopLevelDeclarationVisitor<TopLevelDeclarationNode> {

    @Override
    public TopLevelDeclarationNode visit(ProcessDeclarationNode node) {
      return node.toBuilder().setBody(node.getBody().accept(new AnfStmtVisitor())).build();
    }

    @Override
    public TopLevelDeclarationNode visit(HostDeclarationNode node) {
      return node;
    }
  }

  private static final class AnfStmtVisitor implements StmtVisitor<BlockNode> {
    private final FreshNameGenerator nameGenerator = new FreshNameGenerator();

    @Override
    public BlockNode visit(VariableDeclarationNode node) {
      return single(node);
    }

    @Override
    public BlockNode visit(ArrayDeclarationNode node) {
      final AtomicExprVisitor atomicConverter = new AtomicExprVisitor();
      final ExpressionNode length = atomicConverter.run(node.getLength());
      return BlockNode.builder()
          .addAll(atomicConverter.getBindings())
          .add(node.toBuilder().setLength(length).build())
          .build();
    }

    @Override
    public BlockNode visit(LetBindingNode node) {
      final AnfExprVisitor anfConverter = new AnfExprVisitor();
      final ExpressionNode rhs = anfConverter.run(node.getRhs());
      return BlockNode.builder()
          .addAll(anfConverter.getBindings())
          .add(node.toBuilder().setRhs(rhs).build())
          .build();
    }

    @Override
    public BlockNode visit(AssignNode node) {
      final AnfExprVisitor anfConverter = new AnfExprVisitor();
      final ReferenceNode lhs = anfConverter.run(node.getLhs());
      final ExpressionNode rhs = anfConverter.run(node.getRhs());
      return BlockNode.builder()
          .addAll(anfConverter.getBindings())
          .add(node.toBuilder().setLhs(lhs).setRhs(rhs).build())
          .build();
    }

    @Override
    public BlockNode visit(SendNode node) {
      final AtomicExprVisitor atomicConverter = new AtomicExprVisitor();
      final ExpressionNode sentExpression = atomicConverter.run(node.getSentExpression());
      return BlockNode.builder()
          .addAll(atomicConverter.getBindings())
          .add(node.toBuilder().setSentExpression(sentExpression).build())
          .build();
    }

    @Override
    public BlockNode visit(ReceiveNode node) {
      return single(node);
    }

    @Override
    public BlockNode visit(IfNode node) {
      final AtomicExprVisitor atomicConverter = new AtomicExprVisitor();
      final ExpressionNode guard = atomicConverter.run(node.getGuard());
      return BlockNode.builder()
          .addAll(atomicConverter.getBindings())
          .add(
              node.toBuilder()
                  .setGuard(guard)
                  .setThenBranch(node.getThenBranch().accept(this))
                  .setElseBranch(node.getElseBranch().accept(this))
                  .build())
          .build();
    }

    @Override
    public BlockNode visit(WhileNode whileNode) {
      throw new ElaborationException();
    }

    @Override
    public BlockNode visit(ForNode forNode) {
      throw new ElaborationException();
    }

    @Override
    public BlockNode visit(LoopNode node) {
      final BlockNode body = node.getBody().accept(this);
      return single(node.toBuilder().setBody(body).build());
    }

    @Override
    public BlockNode visit(BreakNode node) {
      return single(node);
    }

    @Override
    public BlockNode visit(BlockNode node) {
      // Flatten nested blocks (all of them are generated by this class)
      final List<StatementNode> body = new LinkedList<>();
      for (StatementNode statement : node) {
        body.addAll(statement.accept(this).getStatements());
      }
      return node.toBuilder().setStatements(body).build();
    }

    @Override
    public BlockNode visit(AssertNode node) {
      final AtomicExprVisitor atomicConverter = new AtomicExprVisitor();
      final ExpressionNode expression = atomicConverter.run(node.getExpression());
      return BlockNode.builder()
          .addAll(atomicConverter.getBindings())
          .add(node.toBuilder().setExpression(expression).build())
          .build();
    }

    /**
     * Produce an expression in A-normal form. An expression is in A-normal form if all children
     * nodes are atomic.
     */
    private final class AnfExprVisitor
        implements ReferenceVisitor<ReferenceNode>, ExprVisitor<ExpressionNode> {
      private final AtomicExprVisitor atomicConverter = new AtomicExprVisitor();

      ReferenceNode run(ReferenceNode reference) {
        return reference.accept(this);
      }

      ExpressionNode run(ExpressionNode expression) {
        return expression.accept(this);
      }

      /**
       * Returns all binding statements that need to be executed before the expression returned by
       * this node becomes valid.
       */
      List<LetBindingNode> getBindings() {
        return atomicConverter.getBindings();
      }

      @Override
      public ReferenceNode visit(Variable node) {
        return node;
      }

      @Override
      public ReferenceNode visit(ArrayIndexingNode node) {
        final ExpressionNode index = atomicConverter.run(node.getIndex());
        return node.toBuilder().setIndex(index).build();
      }

      @Override
      public ExpressionNode visit(LiteralNode node) {
        return node;
      }

      @Override
      public ExpressionNode visit(ReadNode node) {
        final ReferenceNode reference = node.getReference().accept(this);
        return node.toBuilder().setReference(reference).build();
      }

      @Override
      public ExpressionNode visit(NotNode node) {
        final ExpressionNode expression = atomicConverter.run(node.getExpression());
        return node.toBuilder().setExpression(expression).build();
      }

      @Override
      public ExpressionNode visit(BinaryExpressionNode node) {
        final ExpressionNode lhs = atomicConverter.run(node.getLhs());
        final ExpressionNode rhs = atomicConverter.run(node.getRhs());
        return node.toBuilder().setLhs(lhs).setRhs(rhs).build();
      }

      @Override
      public ExpressionNode visit(DowngradeNode node) {
        final ExpressionNode expression = atomicConverter.run(node.getExpression());
        return node.toBuilder().setExpression(expression).build();
      }
    }

    /** Produce an atomic expression. */
    private final class AtomicExprVisitor
        extends AbstractExprVisitor<AtomicExprVisitor, Variable, ExpressionNode> {
      private final AtomicReferenceVisitor referenceVisitor = new AtomicReferenceVisitor();
      private final List<LetBindingNode> bindings = new LinkedList<>();

      ExpressionNode run(ExpressionNode expression) {
        return expression.accept(this);
      }

      /**
       * Returns all binding statements that need to be executed before the expression returned by
       * this node becomes valid.
       */
      List<LetBindingNode> getBindings() {
        return bindings;
      }

      /**
       * Bind an expression to a fresh variable, add the binding to the binding list, and return the
       * new variable.
       */
      private Variable addBinding(ExpressionNode expression) {
        final Variable tmpVar =
            Variable.builder()
                .setName(nameGenerator.getFreshName(TMP_NAME))
                .setSourceLocation(expression)
                .build();
        final LetBindingNode binding =
            LetBindingNode.builder()
                .setVariable(tmpVar)
                .setRhs(expression)
                .setSourceLocation(expression)
                .build();
        bindings.add(binding);
        return tmpVar;
      }

      @Override
      protected ReferenceVisitor<Variable> getReferenceVisitor() {
        return referenceVisitor;
      }

      @Override
      protected AtomicExprVisitor enter(ExpressionNode node) {
        return this;
      }

      @Override
      protected ExpressionNode leave(LiteralNode node, AtomicExprVisitor visitor) {
        return node;
      }

      @Override
      protected ExpressionNode leave(ReadNode node, AtomicExprVisitor visitor, Variable reference) {
        return node.toBuilder().setReference(reference).build();
      }

      @Override
      protected ExpressionNode leave(
          NotNode node, AtomicExprVisitor visitor, ExpressionNode expression) {
        return read(addBinding(node.toBuilder().setExpression(expression).build()));
      }

      @Override
      protected ExpressionNode leave(
          BinaryExpressionNode node,
          AtomicExprVisitor visitor,
          ExpressionNode lhs,
          ExpressionNode rhs) {
        return read(addBinding(node.toBuilder().setLhs(lhs).setRhs(rhs).build()));
      }

      @Override
      protected ExpressionNode leave(
          DowngradeNode node, AtomicExprVisitor visitor, ExpressionNode expression) {
        return read(addBinding(node.toBuilder().setExpression(expression).build()));
      }

      private final class AtomicReferenceVisitor
          extends AbstractReferenceVisitor<AtomicReferenceVisitor, Variable, ExpressionNode> {

        @Override
        protected ExprVisitor<ExpressionNode> getExpressionVisitor() {
          return AtomicExprVisitor.this;
        }

        @Override
        protected AtomicReferenceVisitor enter(ReferenceNode node) {
          return this;
        }

        @Override
        protected Variable leave(Variable node, AtomicReferenceVisitor visitor) {
          return node;
        }

        @Override
        protected Variable leave(
            ArrayIndexingNode node,
            AtomicReferenceVisitor visitor,
            Variable array,
            ExpressionNode index) {
          return addBinding(read(node.toBuilder().setArray(array).setIndex(index).build()));
        }
      }
    }
  }
}
