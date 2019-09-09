package edu.cornell.cs.apl.viaduct.imp.interpreter;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import edu.cornell.cs.apl.viaduct.errors.UndefinedNameError;
import edu.cornell.cs.apl.viaduct.imp.ast.ProcessName;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A channel that connects any number of processes with private binary channels between them.
 * Messages between different processes never mix, and they are only visible to their intended
 * recipients.
 *
 * <p>Essentially maintains two unidirectional queues between each pair of processes.
 *
 * <p>This is thread safe.
 *
 * @param <M> type of messages
 */
final class Channel<M> {
  /** Set of hosts this channel connects. */
  private final ImmutableSet<ProcessName> processes;

  /** Map from (sender, receiver) to the queue that accumulates the messages between the two. */
  private final ImmutableMap<Tuple2<ProcessName, ProcessName>, BlockingQueue<M>> queues;

  /**
   * Create an empty channel given the processes to connect.
   *
   * @param processes processes this channel connects.
   */
  Channel(Iterable<? extends ProcessName> processes) {
    this.processes = ImmutableSet.copyOf(processes);

    final ImmutableMap.Builder<Tuple2<ProcessName, ProcessName>, BlockingQueue<M>> queuesBuilder =
        ImmutableMap.builder();
    for (ProcessName sender : processes) {
      for (ProcessName receiver : processes) {
        queuesBuilder.put(Tuple.of(sender, receiver), new LinkedBlockingQueue<>());
      }
    }
    this.queues = queuesBuilder.build();
  }

  /** Returns {@code true} if all queues in the channel are empty. */
  synchronized boolean isEmpty() {
    for (BlockingQueue<M> queue : this.queues.values()) {
      if (!queue.isEmpty()) {
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
  void send(ProcessName sender, ProcessName receiver, M message) throws UndefinedNameError {
    assertProcess(sender);
    assertProcess(receiver);
    try {
      queues.get(Tuple.of(sender, receiver)).put(message);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Receive a message sent from a process to another. Blocks until a message becomes available.
   *
   * <p>Pops the next message from the corresponding queue.
   */
  M receive(ProcessName sender, ProcessName receiver) throws UndefinedNameError {
    assertProcess(sender);
    assertProcess(receiver);
    try {
      return queues.get(Tuple.of(sender, receiver)).take();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /** Assert that the specified process is connected to this channel. */
  private void assertProcess(ProcessName processName) throws UndefinedNameError {
    if (!this.processes.contains(processName)) {
      throw new UndefinedNameError(processName);
    }
  }
}
