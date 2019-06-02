package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import edu.cornell.cs.apl.viaduct.imp.ast.ImpValue;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessConfigurationNode;
import edu.cornell.cs.apl.viaduct.imp.ast.StmtNode;
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

class InterpretProcessConfiguration {
  /** ProcessConfigurationNode to interpret. */
  private final ProcessConfigurationNode processConfigurationNode;

  /** Channels that hosts use to communicate. */
  private final Channel<ImpValue> channel;

  InterpretProcessConfiguration(ProcessConfigurationNode processConfigurationNode) {
    this.processConfigurationNode = processConfigurationNode;
    this.channel = new Channel<>(processConfigurationNode.hosts());
  }

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

  /** Execute the code on all processes in the configuration, and return their local stores. */
  Map<Host, Store> run() {
    final ExecutorService pool = Executors.newCachedThreadPool();

    final List<ProcessExecutor> processExecutors = new ArrayList<>();
    for (Tuple2<Host, StmtNode> process : processConfigurationNode) {
      processExecutors.add(new ProcessExecutor(process._1, process._2));
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
    // TODO: error if channel is not empty.
    return zipMap(processConfigurationNode.hosts(), results);
  }

  /** Execute a single process. Should be run on its own thread. */
  private class ProcessExecutor implements Callable<Store> {
    private final Host host;
    private final StmtNode code;

    ProcessExecutor(Host host, StmtNode code) {
      this.host = host;
      this.code = code;
    }

    @Override
    public Store call() {
      final InterpretProcessVisitor interpreter = new InterpretProcessVisitor(host, channel);
      return interpreter.run(code);
    }
  }
}
