package edu.cornell.cs.apl.viaduct.imp.interpreter;

import edu.cornell.cs.apl.viaduct.imp.ast.Host;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import io.vavr.collection.SortedSet;
import io.vavr.collection.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A channel that connects any number of hosts models private binary channels between them. Messages
 * between different hosts never mix, and they are only visible to their intended recipient.
 * Essentially maintains two unidirectional queues between each pair of hosts.
 *
 * <p>This is thread safe.
 *
 * @param <M> type of messages
 */
public class Channel<M> {
  /** Set of hosts this channel connects. */
  private final SortedSet<Host> hosts;

  /** Map from (sender, receiver) to the queue that accumulates the messages between the two. */
  private final Map<Tuple2<Host, Host>, BlockingQueue<M>> queues;

  /**
   * Create an empty channel given the hosts to be connected.
   *
   * @param hosts hosts this channel connects.
   */
  public Channel(Iterable<? extends Host> hosts) {
    this.hosts = TreeSet.ofAll(hosts);

    Map<Tuple2<Host, Host>, BlockingQueue<M>> queuesBuilder = HashMap.empty();
    for (Host sender : hosts) {
      for (Host receiver : hosts) {
        queuesBuilder = queuesBuilder.put(Tuple.of(sender, receiver), new LinkedBlockingQueue<>());
      }
    }
    this.queues = queuesBuilder;
  }

  /** Returns {@code true} if all queues in the channel are empty. */
  public synchronized boolean isEmpty() {
    for (Tuple2<?, BlockingQueue<M>> entry : this.queues) {
      if (!entry._2.isEmpty()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Send a message from a host to another.
   *
   * <p>Enqueues a new message to the corresponding queue.
   */
  public void send(Host sender, Host receiver, M message)
      throws UnknownHostException, InterruptedException {
    assertHost(sender);
    assertHost(receiver);
    queues.get(Tuple.of(sender, receiver)).get().put(message);
  }

  /**
   * Receive a message sent from a host to another. Blocks until a message becomes available.
   *
   * <p>Pops the next message from the corresponding queue.
   */
  public M receive(Host sender, Host receiver) throws UnknownHostException, InterruptedException {
    assertHost(sender);
    assertHost(receiver);
    return queues.get(Tuple.of(sender, receiver)).get().take();
  }

  /** Assert that the specified host is connected to this channel. */
  private void assertHost(Host host) throws UnknownHostException {
    if (!this.hosts.contains(host)) {
      throw new UnknownHostException(host);
    }
  }
}
