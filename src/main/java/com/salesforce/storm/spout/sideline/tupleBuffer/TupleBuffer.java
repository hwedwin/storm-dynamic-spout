package com.salesforce.storm.spout.sideline.tupleBuffer;

import com.salesforce.storm.spout.sideline.KafkaMessage;

/**
 * This interface defines an abstraction around essentially a concurrent queue.
 * Abstracting this instead of using a simple queue object allows us to do things like
 * implement a "fairness" algorithm on the poll() method for pulling off of the queue instead of a simple FIFO.
 */
public interface TupleBuffer {
    /**
     * Let the Implementation know that we're adding a new VirtualSpoutId.
     * @param virtualSpoutId - Identifier of new Virtual Spout.
     */
    void addVirtualSpoutId(final String virtualSpoutId);

    /**
     * Let the Implementation know that we're removing/cleaning up from closing a VirtualSpout.
     * @param virtualSpoutId - Identifier of Virtual Spout to be cleaned up.
     */
    void removeVirtualSpoutId(final String virtualSpoutId);

    /**
     * Put a new message onto the queue.  This method is blocking if the queue buffer is full.
     * @param virtualSpoutId - ConsumerId this message is from.
     * @param kafkaMessage - KafkaMessage to be added to the queue.
     * @throws InterruptedException - thrown if a thread is interrupted while blocked adding to the queue.
     */
    void put(final String virtualSpoutId, final KafkaMessage kafkaMessage) throws InterruptedException;

    /**
     * @return - returns the next KafkaMessage to be processed out of the queue.
     */
    KafkaMessage poll();
}
