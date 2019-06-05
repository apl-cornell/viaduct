package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import edu.cornell.cs.apl.viaduct.imp.ast.ProgramNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
import edu.cornell.cs.apl.viaduct.imp.visitors.ProgramVisitor;
import io.vavr.Tuple2;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

class InterpretProgramVisitor implements ProgramVisitor<Map<ProcessName, Store>> {
  InterpretProgramVisitor() {}

  /** Create a map given a list of keys and a separate list of values. */
  private static <K, V> Map<K, V> zipMap(Iterable<K> keys, Iterable<V> values) {
    final Iterator<K> keyIterator = keys.iterator();
    final Iterator<V> valueIterator = values.iterator();
    final Map<K, V> result = new HashMap<>();

    while (keyIterator.hasNext() && valueIterator.hasNext()) {
      result.put(keyIterator.next(), valueIterator.next());
    }

    return result;
  }

  /** Run all processes in the given configuration concurrently, and return their local stores. */
  Map<ProcessName, Store> run(ProgramNode program) {
    return this.visit(program);
  }

  @Override
  public Map<ProcessName, Store> visit(ProgramNode program) {
    final Channel<ImpValue> channel = new Channel<>(program.processes());

    final ExecutorService pool = Executors.newCachedThreadPool();

    final List<ProcessExecutor> processExecutors = new ArrayList<>();
    for (Tuple2<ProcessName, StmtNode> process : program) {
      final ProcessName processName = process._1();
      final StmtNode code = process._2();
      processExecutors.add(new ProcessExecutor(processName, code, channel));
    }

    final List<Store> results = new ArrayList<>();
    try {
      List<Future<Store>> futureResults = pool.invokeAll(processExecutors);
      for (Future<Store> futureResult : futureResults) {
        results.add(futureResult.get());
      }
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }

    pool.shutdown();

    if (!channel.isEmpty()) {
      throw new Error("Some sent messages were never received.");
    }

    return zipMap(program.processes(), results);
  }

  /** Execute a single process. Should be run on its own thread. */
  private static final class ProcessExecutor implements Callable<Store> {
    private final ProcessName process;
    private final Channel<ImpValue> channel;
    private final StmtNode code;

    ProcessExecutor(ProcessName process, StmtNode code, Channel<ImpValue> channel) {
      this.process = process;
      this.code = code;
      this.channel = channel;
    }

    @Override
    public Store call() {
      final InterpretProcessVisitor interpreter = new InterpretProcessVisitor(process, channel);
      return interpreter.run(code);
    }
  }
}
