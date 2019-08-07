package edu.cornell.cs.apl.viaduct.util;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * A queue implementation that keeps only one copy of each element. Trying to insert an element that
 * is already in the queue will ignore the new copy, keeping the old. Specifically, the element is
 * not moved to the end of the queue, it maintains its original position.
 */
public class UniqueQueue<E> extends AbstractQueue<E> implements Queue<E> {
  private final Queue<E> queue = new LinkedList<>();
  private final Set<E> elements = new HashSet<>();

  public UniqueQueue() {
    super();
  }

  public UniqueQueue(Collection<? extends E> collection) {
    this();
    addAll(collection);
  }

  @Override
  public int size() {
    return queue.size();
  }

  @Override
  public void clear() {
    queue.clear();
    elements.clear();
  }

  @Override
  public boolean offer(E e) {
    Objects.requireNonNull(e);
    if (elements.add(e)) {
      return queue.add(e);
    }
    return true;
  }

  @Override
  public E peek() {
    return queue.peek();
  }

  @Override
  public E poll() {
    E e = queue.poll();
    if (e != null) {
      elements.remove(e);
    }
    return e;
  }

  @Override
  public @Nonnull Iterator<E> iterator() {
    return new QueueIterator();
  }

  /**
   * Iterates over the internal queue (ignoring the set), but disables {@link Iterator#remove()}.
   */
  private class QueueIterator implements Iterator<E> {
    private final Iterator<E> queueIterator = queue.iterator();

    @Override
    public boolean hasNext() {
      return queueIterator.hasNext();
    }

    @Override
    public E next() {
      return queueIterator.next();
    }
  }
}
