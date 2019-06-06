package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.collection.SortedSet;
import io.vavr.collection.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A channel that connects any number of processes with private binary channels between them.
 * Messages between different processes never mix, and they are only visible to their intended
 * recipient.
 *
 * <p>Essentially maintains two unidirectional queues between each pair of processes.
 *
 * <p>This is thread safe.
 *
 * @param <M> type of messages
 */
class Channel<M> {
  /** Set of hosts this channel connects. */
  private final SortedSet<ProcessName> processes;

  /** Map from (sender, receiver) to the queue that accumulates the messages between the two. */
  private final Map<Tuple2<ProcessName, ProcessName>, BlockingQueue<M>> queues;

  /**
   * Create an empty channel given the processes to connect.
   *
   * @param processes processes this channel connects.
   */
  Channel(Iterable<? extends ProcessName> processes) {
    this.processes = TreeSet.ofAll(processes);

    Map<Tuple2<ProcessName, ProcessName>, BlockingQueue<M>> queuesBuilder = HashMap.empty();
    for (ProcessName sender : processes) {
      for (ProcessName receiver : processes) {
        queuesBuilder = queuesBuilder.put(Tuple.of(sender, receiver), new LinkedBlockingQueue<>());
      }
    }
    this.queues = queuesBuilder;
  }

  /** Returns {@code true} if all queues in the channel are empty. */
  synchronized boolean isEmpty() {
    for (Tuple2<?, BlockingQueue<M>> entry : this.queues) {
      if (!entry._2.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Send a message from a process to another.
   *
   * <p>Enqueues a new message to the corresponding queue.
   */
  void send(ProcessName sender, ProcessName receiver, M message)
      throws UnknownProcessException, InterruptedException {
    assertProcess(sender);
    assertProcess(receiver);
    queues.get(Tuple.of(sender, receiver)).get().put(message);
  }

  /**
   * Receive a message sent from a process to another. Blocks until a message becomes available.
   *
   * <p>Pops the next message from the corresponding queue.
   */
  M receive(ProcessName sender, ProcessName receiver)
      throws UnknownProcessException, InterruptedException {
    assertProcess(sender);
    assertProcess(receiver);
    return queues.get(Tuple.of(sender, receiver)).get().take();
  }

  /** Assert that the specified process is connected to this channel. */
  private void assertProcess(ProcessName processName) throws UnknownProcessException {
    if (!this.processes.contains(processName)) {
      throw new UnknownProcessException(processName);
    }
  }
}
