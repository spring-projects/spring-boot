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
import java.util.Locale;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.util.unit.DataSize;

/**
 * Spring Kafka configuration properties for Kafka consumer client.
 * <p>
 * Users should refer to Kafka documentation for complete descriptions of these properties.
 *
 * @author Chris Bono
 */
public class Consumer extends KafkaPropertiesBaseWithBootstrapServers {

	/**
	 * Frequency with which the consumer offsets are auto-committed to Kafka if
	 * 'enable.auto.commit' is set to true.
	 */
	private Duration autoCommitInterval;

	/**
	 * What to do when there is no initial offset in Kafka or if the current offset no
	 * longer exists on the server.
	 */
	private String autoOffsetReset;

	/**
	 * Whether the consumer's offset is periodically committed in the background.
	 */
	private Boolean enableAutoCommit;

	/**
	 * Maximum amount of time the server blocks before answering the fetch request if
	 * there isn't sufficient data to immediately satisfy the requirement given by
	 * "fetch-min-size".
	 */
	private Duration fetchMaxWait;

	/**
	 * Minimum amount of data the server should return for a fetch request.
	 */
	private DataSize fetchMinSize;

	/**
	 * Unique string that identifies the consumer group to which this consumer belongs.
	 */
	private String groupId;

	/**
	 * Expected time between heartbeats to the consumer coordinator.
	 */
	private Duration heartbeatInterval;

	/**
	 * Isolation level for reading messages that have been written transactionally.
	 */
	private IsolationLevel isolationLevel = IsolationLevel.READ_UNCOMMITTED;

	/**
	 * Deserializer class for keys.
	 */
	private Class<?> keyDeserializer = StringDeserializer.class;

	/**
	 * Deserializer class for values.
	 */
	private Class<?> valueDeserializer = StringDeserializer.class;

	/**
	 * Maximum number of records returned in a single call to poll().
	 */
	private Integer maxPollRecords;

	public Duration getAutoCommitInterval() {
		return this.autoCommitInterval;
	}

	public void setAutoCommitInterval(Duration autoCommitInterval) {
		this.autoCommitInterval = autoCommitInterval;
	}

	public String getAutoOffsetReset() {
		return this.autoOffsetReset;
	}

	public void setAutoOffsetReset(String autoOffsetReset) {
		this.autoOffsetReset = autoOffsetReset;
	}

	public Boolean getEnableAutoCommit() {
		return this.enableAutoCommit;
	}

	public void setEnableAutoCommit(Boolean enableAutoCommit) {
		this.enableAutoCommit = enableAutoCommit;
	}

	public Duration getFetchMaxWait() {
		return this.fetchMaxWait;
	}

	public void setFetchMaxWait(Duration fetchMaxWait) {
		this.fetchMaxWait = fetchMaxWait;
	}

	public DataSize getFetchMinSize() {
		return this.fetchMinSize;
	}

	public void setFetchMinSize(DataSize fetchMinSize) {
		this.fetchMinSize = fetchMinSize;
	}

	public String getGroupId() {
		return this.groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public Duration getHeartbeatInterval() {
		return this.heartbeatInterval;
	}

	public void setHeartbeatInterval(Duration heartbeatInterval) {
		this.heartbeatInterval = heartbeatInterval;
	}

	public IsolationLevel getIsolationLevel() {
		return this.isolationLevel;
	}

	public void setIsolationLevel(IsolationLevel isolationLevel) {
		this.isolationLevel = isolationLevel;
	}

	public Class<?> getKeyDeserializer() {
		return this.keyDeserializer;
	}

	public void setKeyDeserializer(Class<?> keyDeserializer) {
		this.keyDeserializer = keyDeserializer;
	}

	public Class<?> getValueDeserializer() {
		return this.valueDeserializer;
	}

	public void setValueDeserializer(Class<?> valueDeserializer) {
		this.valueDeserializer = valueDeserializer;
	}

	public Integer getMaxPollRecords() {
		return this.maxPollRecords;
	}

	public void setMaxPollRecords(Integer maxPollRecords) {
		this.maxPollRecords = maxPollRecords;
	}

	public Map<String, Object> buildProperties() {

		// spring.kafka.consumer.<common-props>
		Map<String, Object> properties = super.buildProperties();

		// spring.kafka.consumer.<specific-props>
		PropertiesMap props = new PropertiesMap();
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(this::getAutoCommitInterval).asInt(Duration::toMillis)
				.to(props.in(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG));
		map.from(this::getAutoOffsetReset).to(props.in(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
		map.from(this::getEnableAutoCommit).to(props.in(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG));
		map.from(this::getFetchMaxWait).asInt(Duration::toMillis).to(props.in(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG));
		map.from(this::getFetchMinSize).asInt(DataSize::toBytes).to(props.in(ConsumerConfig.FETCH_MIN_BYTES_CONFIG));
		map.from(this::getGroupId).to(props.in(ConsumerConfig.GROUP_ID_CONFIG));
		map.from(this::getHeartbeatInterval).asInt(Duration::toMillis)
				.to(props.in(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG));
		map.from(() -> getIsolationLevel().name().toLowerCase(Locale.ROOT))
				.to(props.in(ConsumerConfig.ISOLATION_LEVEL_CONFIG));
		map.from(this::getKeyDeserializer).to(props.in(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
		map.from(this::getValueDeserializer).to(props.in(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
		map.from(this::getMaxPollRecords).to(props.in(ConsumerConfig.MAX_POLL_RECORDS_CONFIG));
		properties.putAll(props);

		return properties;
	}

	public enum IsolationLevel {

		/**
		 * Read everything including aborted transactions.
		 */
		READ_UNCOMMITTED((byte) 0),

		/**
		 * Read records from committed transactions, in addition to records not part of
		 * transactions.
		 */
		READ_COMMITTED((byte) 1);

		private final byte id;

		IsolationLevel(byte id) {
			this.id = id;
		}

		public byte id() {
			return this.id;
		}

	}

}
