package com.salesforce.storm.spout.sideline.kafka;

import com.google.common.collect.Maps;
import com.salesforce.storm.spout.sideline.Tools;
import org.apache.kafka.common.TopicPartition;

import java.util.Map;
import java.util.Set;

/**
 * This represents the State of a Consumer.
 */
public class ConsumerState {
    private final Map<TopicPartition, Long> state;

    private ConsumerState(Map<TopicPartition, Long> state) {
        this.state = Tools.immutableCopy(state);
    }

    /**
     * Return the current offset for the given TopicPartition.
     * @param topicPartition - The TopicPartition to get the offset for.
     * @return - The current offset, or null if none is available.
     */
    public Long getOffsetForTopicAndPartition(TopicPartition topicPartition) {
        return getState().get(topicPartition);
    }

    /**
     * Set the current offset for a specified TopicPartition.
     *
     * @param topicPartition - the TopicPartition to set the offset for.
     * @param offset - the offset
     */
    public void setOffset(TopicPartition topicPartition, Long offset) {
        if (offset == null) {
            getState().remove(topicPartition);
        } else {
            getState().put(topicPartition, offset);
        }
    }

    /**
     * @return - returns internal hashmap representation.
     */
    public Map<TopicPartition, Long> getState() {
        return state;
    }

    /**
     * @return - returns all of the TopicPartitions represented by the state.
     */
    public Set<TopicPartition> getTopicPartitions() {
        return getState().keySet();
    }

    /**
     * @return - return true if this contains no information.
     */
    public boolean isEmpty() {
        return state == null || state.isEmpty();
    }

    public static ConsumerStateBuilder builder() {
        return new ConsumerStateBuilder();
    }

    @Override
    public String toString() {
        return "ConsumerState{"
                + "state=" + state
                + '}';
    }

    /**
     * WIP builder pattern.
     */
    public static final class ConsumerStateBuilder {
        private Map<TopicPartition, Long> state = Maps.newHashMap();

        public ConsumerStateBuilder() {
        }

        public ConsumerStateBuilder withPartition(TopicPartition topicPartition, long offset) {
            state.put(topicPartition, offset);
            return this;
        }

        public ConsumerState build() {
            return new ConsumerState(state);
        }
    }
}
