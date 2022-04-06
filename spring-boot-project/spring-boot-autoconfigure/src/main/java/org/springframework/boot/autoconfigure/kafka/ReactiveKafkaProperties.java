/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.kafka;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.RetriableCommitFailedException;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.util.CollectionUtils;
import reactor.kafka.receiver.KafkaReceiver;

import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Configuration properties for Project Reactor for Apache Kafka.
 * <p>
 * Users should refer to Reactor Kafka documentation for complete descriptions of these
 * properties.
 *
 * @author Almog Tavor
 * @since 2.7.0
 */
@ConfigurationProperties(prefix = "spring.reactor.kafka")
public class ReactiveKafkaProperties {
    /**
     * Additional properties, common to producers and consumers, used to configure the
     * client.
     */
    private final Map<String, String> properties = new HashMap<>();

    private final Receiver receiver = new Receiver();

    private final Sender sender = new Sender();

    public Map<String, String> getProperties() {
        return this.properties;
    }

    public Receiver getReceiver() {
        return this.receiver;
    }

    public Sender getSender() {
        return this.sender;
    }


    private Map<String, Object> buildCommonProperties() {
        Map<String, Object> commonProperties = new HashMap<>();
        if (!CollectionUtils.isEmpty(this.properties)) {
            commonProperties.putAll(this.properties);
        }
        return commonProperties;
    }

    /**
     * Create an initial map of consumer properties from the state of this instance.
     * <p>
     * This allows you to add additional properties, if necessary, and override the
     * default kafkaReceiver bean.
     *
     * @return the consumer properties initialized with the customizations defined on this
     * instance
     */
    public Map<String, Object> buildReceiverProperties() {
        Map<String, Object> receiverProperties = buildCommonProperties();
        receiverProperties.putAll(this.receiver.buildProperties());
        return receiverProperties;
    }

    /**
     * Create an initial map of producer properties from the state of this instance.
     * This allows you to add additional properties, if necessary, and override the
     * default kafkaSender bean.
     *
     * @return the producer properties initialized with the customizations defined on this
     * instance
     */
    public Map<String, Object> buildSenderProperties() {
        Map<String, Object> senderProperties = buildCommonProperties();
        senderProperties.putAll(this.sender.buildProperties());
        return senderProperties;
    }


    public static class Receiver {
        /**
         * Sets the timeout for each {@link KafkaConsumer#poll(Duration)} operation. Since
         * the underlying Kafka consumer is not thread-safe, long poll intervals may delay
         * commits and other operations invoked using {@link KafkaReceiver#doOnConsumer(java.util.function.Function)}.
         * Very short timeouts may reduce batching and increase load on the broker.
         */
        private Duration pollTimeout;

        /**
         * Sets timeout for graceful shutdown of {@link KafkaConsumer}.
         */
        private Duration closeTimeout;

        /**
         * Sets subscription using group management to the specified collection of topics.
         */
        private Collection<String> subscribeTopics;

        /**
         * Sets subscription using group management to the specified pattern.
         */
        private Pattern subscribePattern;

        /**
         * Configures commit interval for automatic commits. At least one commit operation is
         * attempted within this interval if records are consumed and acknowledged.
         */
        private Duration commitInterval;


        /**
         * Configures commit batch size for automatic commits. At least one commit operation is
         * attempted  when the number of acknowledged uncommitted offsets reaches this batch size.
         */
        private int commitBatchSize;

        /**
         * Configures commit ahead size per partition for at-most-once delivery. Before dispatching
         * each record, an offset ahead by this size may be committed. The maximum number
         * of records that may be lost if the application fails is <code>commitAheadSize + 1</code>.
         */
        private int atmostOnceCommitAheadSize;

        /**
         * Configures the maximum number of consecutive non-fatal {@link RetriableCommitFailedException}
         * commit failures that are tolerated. For manual commits, failure in commit after the configured
         * number of attempts fails the commit operation. For auto commits, the received Flux is terminated
         * if the commit does not succeed after these attempts.
         */
        private int maxCommitAttempts;

        /**
         * Set to greater than 0 to enable out of order commit sequencing. If the number of
         * deferred commits exceeds this value, the consumer is paused until the deferred
         * commits are reduced.
         */
        private int maxDeferredCommits;

        public Duration getPollTimeout() {
            return pollTimeout;
        }

        public void setPollTimeout(Duration pollTimeout) {
            this.pollTimeout = pollTimeout;
        }

        public Duration getCloseTimeout() {
            return closeTimeout;
        }

        public void setCloseTimeout(Duration closeTimeout) {
            this.closeTimeout = closeTimeout;
        }

        public Collection<String> getSubscribeTopics() {
            return subscribeTopics;
        }

        public void setSubscribeTopics(Collection<String> subscribeTopics) {
            this.subscribeTopics = subscribeTopics;
        }

        public Pattern getSubscribePattern() {
            return subscribePattern;
        }

        public void setSubscribePattern(Pattern subscribePattern) {
            this.subscribePattern = subscribePattern;
        }

        public Duration getCommitInterval() {
            return commitInterval;
        }

        public void setCommitInterval(Duration commitInterval) {
            this.commitInterval = commitInterval;
        }

        public int getCommitBatchSize() {
            return commitBatchSize;
        }

        public void setCommitBatchSize(int commitBatchSize) {
            this.commitBatchSize = commitBatchSize;
        }

        public int getAtmostOnceCommitAheadSize() {
            return atmostOnceCommitAheadSize;
        }

        public void setAtmostOnceCommitAheadSize(int atmostOnceCommitAheadSize) {
            this.atmostOnceCommitAheadSize = atmostOnceCommitAheadSize;
        }

        public int getMaxCommitAttempts() {
            return maxCommitAttempts;
        }

        public void setMaxCommitAttempts(int maxCommitAttempts) {
            this.maxCommitAttempts = maxCommitAttempts;
        }

        public int getMaxDeferredCommits() {
            return maxDeferredCommits;
        }

        public void setMaxDeferredCommits(int maxDeferredCommits) {
            this.maxDeferredCommits = maxDeferredCommits;
        }

        public Map<String, Object> buildProperties() {
            Properties receiverProperties = new Properties();
            PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
            map.from(this::getPollTimeout).asInt(Duration::toMillis).to(receiverProperties.in("pollTimeout"));
            map.from(this::getCloseTimeout).asInt(Duration::toMillis).to(receiverProperties.in("closeTimeout"));
            map.from(this::getCommitInterval).asInt(Duration::toMillis).to(receiverProperties.in("commitInterval"));
            map.from(this::getCommitBatchSize).to(receiverProperties.in("commitBatchSize"));
            map.from(this::getAtmostOnceCommitAheadSize).to(receiverProperties.in("atmostOnceCommitAheadSize"));
            map.from(this::getMaxCommitAttempts).to(receiverProperties.in("maxCommitAttempts"));
            map.from(this::getMaxDeferredCommits).to(receiverProperties.in("maxDeferredCommits"));
            map.from(this::getSubscribeTopics).to(receiverProperties.in("subscribeTopics"));
            map.from(this::getSubscribePattern).to(receiverProperties.in("subscribePattern"));
            return receiverProperties;
        }
    }

    public static class Sender {

        private final Map<String, String> properties = new HashMap<>();

        /**
         * Configures the maximum number of in-flight records that are fetched
         * from the outbound record publisher while acknowledgements are pending.
         * This limit must be configured along with {@link ProducerConfig#BUFFER_MEMORY_CONFIG}
         * to control memory usage and to avoid blocking the reactive pipeline.
         */
        private int maxInFlight;


        /**
         * Configures error handling behaviour for {@link reactor.kafka.sender.KafkaSender#send(org.reactivestreams.Publisher)}.
         * If set to true, send fails when an error is encountered and only records
         * that are already in transit may be delivered after the first error. If set to false,
         * an attempt is made to send each record to Kafka, even if one or more records cannot
         * be delivered after the configured number of retries due to a non-fatal exception.
         * This flag should be set along with {@link ProducerConfig#RETRIES_CONFIG} and
         * {@link ProducerConfig#ACKS_CONFIG} to configure the required quality-of-service.
         * By default, stopOnError is true.
         */
        private boolean stopOnError;

        /**
         * Configures the timeout for graceful shutdown of this sender.
         */
        private Duration closeTimeout;

        public int getMaxInFlight() {
            return maxInFlight;
        }

        public void setMaxInFlight(int maxInFlight) {
            this.maxInFlight = maxInFlight;
        }

        public boolean isStopOnError() {
            return stopOnError;
        }

        public void setStopOnError(boolean stopOnError) {
            this.stopOnError = stopOnError;
        }

        public Duration getCloseTimeout() {
            return closeTimeout;
        }

        public void setCloseTimeout(Duration closeTimeout) {
            this.closeTimeout = closeTimeout;
        }

        public Map<String, String> getProperties() {
            return this.properties;
        }

        public Map<String, Object> buildProperties() {
            Properties senderProperties = new Properties();
            PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
            map.from(this::getCloseTimeout).asInt(Duration::toMillis).to(senderProperties.in("closeTimeout"));
            map.from(this::isStopOnError).to(senderProperties.in("stopOnError"));
            map.from(this::getMaxInFlight).to(senderProperties.in("maxInFlight"));
            return senderProperties;
        }
    }

    @SuppressWarnings("serial")
    public static class Properties extends HashMap<String, Object> {

        public <V> java.util.function.Consumer<V> in(String key) {
            return (value) -> put(key, value);
        }

        public Properties with(KafkaProperties.Ssl ssl, KafkaProperties.Security security, Map<String, String> properties) {
            putAll(ssl.buildProperties());
            putAll(security.buildProperties());
            putAll(properties);
            return this;
        }

    }
}
