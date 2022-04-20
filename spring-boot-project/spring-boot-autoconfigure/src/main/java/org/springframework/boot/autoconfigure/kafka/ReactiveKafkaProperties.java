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

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.RetriableCommitFailedException;
import org.apache.kafka.clients.producer.ProducerConfig;
import reactor.kafka.receiver.KafkaReceiver;

import org.springframework.boot.context.properties.ConfigurationProperties;

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

	public static class Receiver {

		/**
		 * Sets the timeout for each KafkaConsumer's poll operation duration.
		 */
		private Duration pollTimeout;

		/**
		 * Sets timeout for graceful shutdown of the KafkaConsumer.
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
		 * Configures commit interval for automatic commits. At least one commit operation
		 * is attempted within this interval if records are consumed and acknowledged.
		 */
		private Duration commitInterval;

		/**
		 * Configures commit batch size for automatic commits. At least one commit
		 * operation is attempted when the number of acknowledged uncommitted offsets
		 * reaches this batch size.
		 */
		private int commitBatchSize;

		/**
		 * Configures commit ahead size per partition for at-most-once delivery. Before
		 * dispatching each record, an offset ahead by this size may be committed.
		 */
		private int atmostOnceCommitAheadSize;

		/**
		 * Configures the maximum number of consecutive non-fatal RetriableCommitFailedException
		 * commit failures that are tolerated.
		 */
		private int maxCommitAttempts;

		/**
		 * The limit for the number of deferred commits to pause the consumer until the
		 * deferred commits are reduced.
		 */
		private int maxDeferredCommits;

		public Duration getPollTimeout() {
			return this.pollTimeout;
		}

		public void setPollTimeout(Duration pollTimeout) {
			this.pollTimeout = pollTimeout;
		}

		public Duration getCloseTimeout() {
			return this.closeTimeout;
		}

		public void setCloseTimeout(Duration closeTimeout) {
			this.closeTimeout = closeTimeout;
		}

		public Collection<String> getSubscribeTopics() {
			return this.subscribeTopics;
		}

		public void setSubscribeTopics(Collection<String> subscribeTopics) {
			this.subscribeTopics = subscribeTopics;
		}

		public Pattern getSubscribePattern() {
			return this.subscribePattern;
		}

		public void setSubscribePattern(Pattern subscribePattern) {
			this.subscribePattern = subscribePattern;
		}

		public Duration getCommitInterval() {
			return this.commitInterval;
		}

		public void setCommitInterval(Duration commitInterval) {
			this.commitInterval = commitInterval;
		}

		public int getCommitBatchSize() {
			return this.commitBatchSize;
		}

		public void setCommitBatchSize(int commitBatchSize) {
			this.commitBatchSize = commitBatchSize;
		}

		public int getAtmostOnceCommitAheadSize() {
			return this.atmostOnceCommitAheadSize;
		}

		public void setAtmostOnceCommitAheadSize(int atmostOnceCommitAheadSize) {
			this.atmostOnceCommitAheadSize = atmostOnceCommitAheadSize;
		}

		public int getMaxCommitAttempts() {
			return this.maxCommitAttempts;
		}

		public void setMaxCommitAttempts(int maxCommitAttempts) {
			this.maxCommitAttempts = maxCommitAttempts;
		}

		public int getMaxDeferredCommits() {
			return this.maxDeferredCommits;
		}

		public void setMaxDeferredCommits(int maxDeferredCommits) {
			this.maxDeferredCommits = maxDeferredCommits;
		}

	}

	public static class Sender {

		private final Map<String, String> properties = new HashMap<>();

		/**
		 * Configures the maximum number of in-flight records that are fetched from the
		 * outbound record publisher while acknowledgements are pending.
		 */
		private int maxInFlight;

		/**
		 * Configures error handling behaviour for the KafkaSender's send function.
		 */
		private boolean stopOnError;

		/**
		 * Configures the timeout for graceful shutdown of this sender.
		 */
		private Duration closeTimeout;

		public int getMaxInFlight() {
			return this.maxInFlight;
		}

		public void setMaxInFlight(int maxInFlight) {
			this.maxInFlight = maxInFlight;
		}

		public boolean isStopOnError() {
			return this.stopOnError;
		}

		public void setStopOnError(boolean stopOnError) {
			this.stopOnError = stopOnError;
		}

		public Duration getCloseTimeout() {
			return this.closeTimeout;
		}

		public void setCloseTimeout(Duration closeTimeout) {
			this.closeTimeout = closeTimeout;
		}

		public Map<String, String> getProperties() {
			return this.properties;
		}

	}

}
