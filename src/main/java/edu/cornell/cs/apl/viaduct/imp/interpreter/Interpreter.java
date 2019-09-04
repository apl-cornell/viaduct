package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.errors.CompilationError;
import edu.cornell.cs.apl.viaduct.imp.ast.BlockNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ExpressionNode;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StatementNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.ElaborationVisitor;
import edu.cornell.cs.apl.viaduct.imp.visitors.ExprVisitor;
import edu.cornell.cs.apl.viaduct.util.MapUtil;
import io.vavr.Tuple2;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.annotation.Nullable;

/** Interpreter for Imp programs. */
public final class Interpreter {
  /** Evaluate an expression in the empty context and return its value. */
  public static ImpValue run(ExpressionNode expression) {
    final ExprVisitor<ImpValue> visitor = new InterpretStmtVisitor().getExpressionVisitor();
    return expression.accept(visitor);
  }

  /** Execute a statement in the empty context and return the resulting store. */
  public static Store run(StatementNode statement) {
    return run(statement, null, null);
  }

  /** Similar to {@link #run(StatementNode)}, but executes the statement as the given process. */
  private static Store run(
      StatementNode statement, @Nullable ProcessName process, @Nullable Channel<ImpValue> channel) {
    final InterpretStmtVisitor visitor =
        (process == null || channel == null)
            ? new InterpretStmtVisitor()
            : new InterpretStmtVisitor(process, channel);

    statement = new ElaborationVisitor().run(statement);

    if (statement instanceof BlockNode) {
      // Blocks create new scope. If we don't do this, we will get an empty store.
      for (StatementNode stmt : (BlockNode) statement) {
        stmt.accept(visitor);
      }
    } else {
      statement.accept(visitor);
    }

    return visitor.getStore();
  }

  /** Execute the code on all processes in the configuration, and return their local stores. */
  public static Map<ProcessName, Store> run(ProgramNode program) {
    final Channel<ImpValue> channel = new Channel<>(program.processes());

    final ExecutorService pool = Executors.newCachedThreadPool();

    // Add each process in the program to a task list
    final List<ProcessExecutor> processExecutors = new ArrayList<>();
    for (Tuple2<ProcessName, StatementNode> process : program) {
      processExecutors.add(new ProcessExecutor(process._1(), process._2(), channel));
    }

    final List<Store> results = new ArrayList<>();
    try {
      final List<Future<Store>> futureResults = pool.invokeAll(processExecutors);
      for (Future<Store> futureResult : futureResults) {
        results.add(futureResult.get());
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      pool.shutdownNow();
      if (e.getCause() instanceof CompilationError) {
        throw (CompilationError) e.getCause();
      } else {
        throw new RuntimeException(e);
      }
    } finally {
      pool.shutdown();
    }

    if (!channel.isEmpty()) {
      // TODO: turn into CompilationError
      throw new Error("Some sent messages were never received.");
    }

    return MapUtil.zip(program.processes(), results);
  }

  /** Execute a single process. Should be run on its own thread. */
  private static final class ProcessExecutor implements Callable<Store> {
    private final ProcessName process;
    private final Channel<ImpValue> channel;
    private final StatementNode code;

    ProcessExecutor(ProcessName process, StatementNode code, Channel<ImpValue> channel) {
      this.process = process;
      this.code = code;
      this.channel = channel;
    }

    @Override
    public Store call() {
      return run(code, process, channel);
    }
  }
}
