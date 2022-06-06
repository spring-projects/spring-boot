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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.security.jaas.KafkaJaasLoginModuleInitializer;
import org.springframework.util.unit.DataSize;

/**
 * Configuration properties for Spring for Apache Kafka.
 * <p>
 * Users should refer to Kafka documentation for complete descriptions of these
 * properties.
 *
 * @author Gary Russell
 * @author Stephane Nicoll
 * @author Artem Bilan
 * @author Nakul Mishra
 * @author Tomaz Fernandes
 * @author Chris Bono
 * @since 1.5.0
 */
@ConfigurationProperties(prefix = "spring.kafka")
public class KafkaProperties extends KafkaPropertiesBaseWithBootstrapServersAndProducerConsumer {

	private final Admin admin = new Admin();

	private final Streams streams = new Streams();

	private final Listener listener = new Listener();

	private final Jaas jaas = new Jaas();

	private final Template template = new Template();

	private final Retry retry = new Retry();

	public KafkaProperties() {
		super(new ArrayList<>(Collections.singletonList("localhost:9092")));
	}

	public Listener getListener() {
		return this.listener;
	}

	public Admin getAdmin() {
		return this.admin;
	}

	public Streams getStreams() {
		return this.streams;
	}

	public Jaas getJaas() {
		return this.jaas;
	}

	public Template getTemplate() {
		return this.template;
	}

	public Retry getRetry() {
		return this.retry;
	}

	/**
	 * Create an initial map of admin properties from the state of this instance.
	 * <p>
	 * This allows you to add additional properties, if necessary, and override the
	 * default kafkaAdmin bean.
	 * @return the admin properties initialized with the customizations defined on this
	 * instance
	 */
	public Map<String, Object> buildAdminProperties() {
		// spring.kafka.<common-props>
		Map<String, Object> properties = buildProperties();

		// spring.kafka.admin.<common-props>
		// spring.kafka.admin.<specific-props>
		properties.putAll(this.admin.buildProperties());
		return properties;
	}

	/**
	 * Create an initial map of streams properties from the state of this instance.
	 * <p>
	 * This allows you to add additional properties, if necessary.
	 * @return the streams properties initialized with the customizations defined on this
	 * instance
	 */
	public Map<String, Object> buildStreamsProperties() {
		// spring.kafka.<common-props>
		Map<String, Object> properties = buildProperties();

		// spring.kafka.streams.<common-props>
		// spring.kafka.streams.<specific-props>
		properties.putAll(this.streams.buildProperties());
		return properties;
	}

	/**
	 * Spring Kafka configuration properties for Kafka admin client.
	 */
	public static class Admin extends KafkaPropertiesBase {

		/**
		 * Whether to fail fast if the broker is not available on startup.
		 */
		private boolean failFast;

		public boolean isFailFast() {
			return this.failFast;
		}

		public void setFailFast(boolean failFast) {
			this.failFast = failFast;
		}

		public Map<String, Object> buildProperties() {
			// spring.kafka.admin.<common-props>
			return super.buildProperties();
		}

	}

	/**
	 * Spring Kafka configuration properties for Kafka streams.
	 */
	public static class Streams extends KafkaPropertiesBaseWithBootstrapServers {

		private final Cleanup cleanup = new Cleanup();

		/**
		 * Kafka streams 'application.id' property; default spring.application.name.
		 */
		private String applicationId;

		/**
		 * Whether to auto-start the streams factory bean.
		 */
		private boolean autoStartup = true;

		/**
		 * Maximum memory size to be used for buffering across all threads.
		 */
		private DataSize cacheMaxSizeBuffering;

		/**
		 * The replication factor for change log topics and repartition topics created by
		 * the stream processing application.
		 */
		private Integer replicationFactor;

		/**
		 * Directory location for the state store.
		 */
		private String stateDir;

		public Cleanup getCleanup() {
			return this.cleanup;
		}

		public String getApplicationId() {
			return this.applicationId;
		}

		public void setApplicationId(String applicationId) {
			this.applicationId = applicationId;
		}

		public boolean isAutoStartup() {
			return this.autoStartup;
		}

		public void setAutoStartup(boolean autoStartup) {
			this.autoStartup = autoStartup;
		}

		public DataSize getCacheMaxSizeBuffering() {
			return this.cacheMaxSizeBuffering;
		}

		public void setCacheMaxSizeBuffering(DataSize cacheMaxSizeBuffering) {
			this.cacheMaxSizeBuffering = cacheMaxSizeBuffering;
		}

		public Integer getReplicationFactor() {
			return this.replicationFactor;
		}

		public void setReplicationFactor(Integer replicationFactor) {
			this.replicationFactor = replicationFactor;
		}

		public String getStateDir() {
			return this.stateDir;
		}

		public void setStateDir(String stateDir) {
			this.stateDir = stateDir;
		}

		public Map<String, Object> buildProperties() {
			// spring.kafka.streams.<common-props>
			Map<String, Object> properties = super.buildProperties();

			// spring.kafka.streams.<specific-props>
			PropertiesMap props = new PropertiesMap();
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
			map.from(this::getApplicationId).to(props.in("application.id"));
			map.from(this::getCacheMaxSizeBuffering).asInt(DataSize::toBytes).to(props.in("cache.max.bytes.buffering"));
			map.from(this::getReplicationFactor).to(props.in("replication.factor"));
			map.from(this::getStateDir).to(props.in("state.dir"));
			properties.putAll(props);
			return properties;
		}

	}

	public static class Template {

		/**
		 * Default topic to which messages are sent.
		 */
		private String defaultTopic;

		/**
		 * Transaction id prefix, override the transaction id prefix in the producer
		 * factory.
		 */
		private String transactionIdPrefix;

		public String getDefaultTopic() {
			return this.defaultTopic;
		}

		public void setDefaultTopic(String defaultTopic) {
			this.defaultTopic = defaultTopic;
		}

		public String getTransactionIdPrefix() {
			return this.transactionIdPrefix;
		}

		public void setTransactionIdPrefix(String transactionIdPrefix) {
			this.transactionIdPrefix = transactionIdPrefix;
		}

	}

	public static class Listener {

		public enum Type {

			/**
			 * Invokes the endpoint with one ConsumerRecord at a time.
			 */
			SINGLE,

			/**
			 * Invokes the endpoint with a batch of ConsumerRecords.
			 */
			BATCH

		}

		/**
		 * Listener type.
		 */
		private Type type = Type.SINGLE;

		/**
		 * Listener AckMode. See the spring-kafka documentation.
		 */
		private AckMode ackMode;

		/**
		 * Prefix for the listener's consumer 'client.id' property.
		 */
		private String clientId;

		/**
		 * Number of threads to run in the listener containers.
		 */
		private Integer concurrency;

		/**
		 * Timeout to use when polling the consumer.
		 */
		private Duration pollTimeout;

		/**
		 * Multiplier applied to "pollTimeout" to determine if a consumer is
		 * non-responsive.
		 */
		private Float noPollThreshold;

		/**
		 * Number of records between offset commits when ackMode is "COUNT" or
		 * "COUNT_TIME".
		 */
		private Integer ackCount;

		/**
		 * Time between offset commits when ackMode is "TIME" or "COUNT_TIME".
		 */
		private Duration ackTime;

		/**
		 * Sleep interval between Consumer.poll(Duration) calls.
		 */
		private Duration idleBetweenPolls = Duration.ZERO;

		/**
		 * Time between publishing idle consumer events (no data received).
		 */
		private Duration idleEventInterval;

		/**
		 * Time between publishing idle partition consumer events (no data received for
		 * partition).
		 */
		private Duration idlePartitionEventInterval;

		/**
		 * Time between checks for non-responsive consumers. If a duration suffix is not
		 * specified, seconds will be used.
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration monitorInterval;

		/**
		 * Whether to log the container configuration during initialization (INFO level).
		 */
		private Boolean logContainerConfig;

		/**
		 * Whether to suppress the entire record from being written to the log when
		 * retries are being attempted.
		 */
		private boolean onlyLogRecordMetadata = true;

		/**
		 * Whether the container should fail to start if at least one of the configured
		 * topics are not present on the broker.
		 */
		private boolean missingTopicsFatal = false;

		/**
		 * Whether the container stops after the current record is processed or after all
		 * the records from the previous poll are processed.
		 */
		private boolean immediateStop = false;

		public Type getType() {
			return this.type;
		}

		public void setType(Type type) {
			this.type = type;
		}

		public AckMode getAckMode() {
			return this.ackMode;
		}

		public void setAckMode(AckMode ackMode) {
			this.ackMode = ackMode;
		}

		public String getClientId() {
			return this.clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public Integer getConcurrency() {
			return this.concurrency;
		}

		public void setConcurrency(Integer concurrency) {
			this.concurrency = concurrency;
		}

		public Duration getPollTimeout() {
			return this.pollTimeout;
		}

		public void setPollTimeout(Duration pollTimeout) {
			this.pollTimeout = pollTimeout;
		}

		public Float getNoPollThreshold() {
			return this.noPollThreshold;
		}

		public void setNoPollThreshold(Float noPollThreshold) {
			this.noPollThreshold = noPollThreshold;
		}

		public Integer getAckCount() {
			return this.ackCount;
		}

		public void setAckCount(Integer ackCount) {
			this.ackCount = ackCount;
		}

		public Duration getAckTime() {
			return this.ackTime;
		}

		public void setAckTime(Duration ackTime) {
			this.ackTime = ackTime;
		}

		public Duration getIdleBetweenPolls() {
			return this.idleBetweenPolls;
		}

		public void setIdleBetweenPolls(Duration idleBetweenPolls) {
			this.idleBetweenPolls = idleBetweenPolls;
		}

		public Duration getIdleEventInterval() {
			return this.idleEventInterval;
		}

		public void setIdleEventInterval(Duration idleEventInterval) {
			this.idleEventInterval = idleEventInterval;
		}

		public Duration getIdlePartitionEventInterval() {
			return this.idlePartitionEventInterval;
		}

		public void setIdlePartitionEventInterval(Duration idlePartitionEventInterval) {
			this.idlePartitionEventInterval = idlePartitionEventInterval;
		}

		public Duration getMonitorInterval() {
			return this.monitorInterval;
		}

		public void setMonitorInterval(Duration monitorInterval) {
			this.monitorInterval = monitorInterval;
		}

		public Boolean getLogContainerConfig() {
			return this.logContainerConfig;
		}

		public void setLogContainerConfig(Boolean logContainerConfig) {
			this.logContainerConfig = logContainerConfig;
		}

		@Deprecated
		@DeprecatedConfigurationProperty(reason = "Use KafkaUtils#setConsumerRecordFormatter instead.")
		public boolean isOnlyLogRecordMetadata() {
			return this.onlyLogRecordMetadata;
		}

		@Deprecated
		public void setOnlyLogRecordMetadata(boolean onlyLogRecordMetadata) {
			this.onlyLogRecordMetadata = onlyLogRecordMetadata;
		}

		public boolean isMissingTopicsFatal() {
			return this.missingTopicsFatal;
		}

		public void setMissingTopicsFatal(boolean missingTopicsFatal) {
			this.missingTopicsFatal = missingTopicsFatal;
		}

		public boolean isImmediateStop() {
			return this.immediateStop;
		}

		public void setImmediateStop(boolean immediateStop) {
			this.immediateStop = immediateStop;
		}

	}

	public static class Jaas {

		/**
		 * Whether to enable JAAS configuration.
		 */
		private boolean enabled;

		/**
		 * Login module.
		 */
		private String loginModule = "com.sun.security.auth.module.Krb5LoginModule";

		/**
		 * Control flag for login configuration.
		 */
		private KafkaJaasLoginModuleInitializer.ControlFlag controlFlag = KafkaJaasLoginModuleInitializer.ControlFlag.REQUIRED;

		/**
		 * Additional JAAS options.
		 */
		private final Map<String, String> options = new HashMap<>();

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getLoginModule() {
			return this.loginModule;
		}

		public void setLoginModule(String loginModule) {
			this.loginModule = loginModule;
		}

		public KafkaJaasLoginModuleInitializer.ControlFlag getControlFlag() {
			return this.controlFlag;
		}

		public void setControlFlag(KafkaJaasLoginModuleInitializer.ControlFlag controlFlag) {
			this.controlFlag = controlFlag;
		}

		public Map<String, String> getOptions() {
			return this.options;
		}

		public void setOptions(Map<String, String> options) {
			if (options != null) {
				this.options.putAll(options);
			}
		}

	}

	public static class Retry {

		private final Topic topic = new Topic();

		public Topic getTopic() {
			return this.topic;
		}

		/**
		 * Properties for non-blocking, topic-based retries.
		 */
		public static class Topic {

			/**
			 * Whether to enable topic-based non-blocking retries.
			 */
			private boolean enabled;

			/**
			 * Total number of processing attempts made before sending the message to the
			 * DLT.
			 */
			private int attempts = 3;

			/**
			 * Canonical backoff period. Used as an initial value in the exponential case,
			 * and as a minimum value in the uniform case.
			 */
			private Duration delay = Duration.ofSeconds(1);

			/**
			 * Multiplier to use for generating the next backoff delay.
			 */
			private double multiplier = 0.0;

			/**
			 * Maximum wait between retries. If less than the delay then the default of 30
			 * seconds is applied.
			 */
			private Duration maxDelay = Duration.ZERO;

			/**
			 * Whether to have the backoff delays.
			 */
			private boolean randomBackOff = false;

			public boolean isEnabled() {
				return this.enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			public int getAttempts() {
				return this.attempts;
			}

			public void setAttempts(int attempts) {
				this.attempts = attempts;
			}

			public Duration getDelay() {
				return this.delay;
			}

			public void setDelay(Duration delay) {
				this.delay = delay;
			}

			public double getMultiplier() {
				return this.multiplier;
			}

			public void setMultiplier(double multiplier) {
				this.multiplier = multiplier;
			}

			public Duration getMaxDelay() {
				return this.maxDelay;
			}

			public void setMaxDelay(Duration maxDelay) {
				this.maxDelay = maxDelay;
			}

			public boolean isRandomBackOff() {
				return this.randomBackOff;
			}

			public void setRandomBackOff(boolean randomBackOff) {
				this.randomBackOff = randomBackOff;
			}

		}

	}

	public static class Cleanup {

		/**
		 * Cleanup the application’s local state directory on startup.
		 */
		private boolean onStartup = false;

		/**
		 * Cleanup the application’s local state directory on shutdown.
		 */
		private boolean onShutdown = false;

		public boolean isOnStartup() {
			return this.onStartup;
		}

		public void setOnStartup(boolean onStartup) {
			this.onStartup = onStartup;
		}

		public boolean isOnShutdown() {
			return this.onShutdown;
		}

		public void setOnShutdown(boolean onShutdown) {
			this.onShutdown = onShutdown;
		}

	}

}
