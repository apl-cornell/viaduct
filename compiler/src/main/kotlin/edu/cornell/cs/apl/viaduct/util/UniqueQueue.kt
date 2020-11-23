package edu.cornell.cs.apl.viaduct.util

import java.util.AbstractQueue
import java.util.LinkedList
import java.util.Queue

/**
 * A queue implementation that keeps only one copy of each element.
 *
 *
 * Trying to insert an element that is already in the queue will ignore the new copy, keeping the
 * old. Additionally, the element will keep its original position in the queue, and is *not*
 * moved to the end of the queue.
 */
class UniqueQueue<E>() : AbstractQueue<E>() {
    private val queue: Queue<E> = LinkedList()
    private val elements: MutableSet<E> = mutableSetOf()

    constructor(collection: Collection<E>) : this() {
        addAll(collection)
    }

    override val size: Int
        get() = queue.size

    override fun clear() {
        queue.clear()
        elements.clear()
    }

    override fun offer(e: E): Boolean {
        if (elements.add(e)) {
            return queue.add(e)
        }
        return true
    }

    override fun peek(): E? {
        return queue.peek()
    }

    override fun poll(): E? {
        val e = queue.poll()
        if (e != null) {
            elements.remove(e)
        }
        return e
    }

    override fun iterator(): MutableIterator<E> {
        return QueueIterator()
    }

    /**
     * Iterates over the internal queue (ignoring the set), but disables [MutableIterator.remove].
     */
    private inner class QueueIterator : MutableIterator<E> {
        private val queueIterator: Iterator<E> = queue.iterator()

        override fun hasNext(): Boolean {
            return queueIterator.hasNext()
        }

        override fun next(): E {
            return queueIterator.next()
        }

        override fun remove() {
            throw UnsupportedOperationException("remove")
        }
    }
}
