/*
 * Copyright 2012-2023 the original author or authors.
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
import java.util.function.Function;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties.Listener;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AfterRollbackProcessor;
import org.springframework.kafka.listener.BatchInterceptor;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.support.converter.BatchMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.transaction.KafkaAwareTransactionManager;

/**
 * Configure {@link ConcurrentKafkaListenerContainerFactory} with sensible defaults.
 *
 * @author Gary Russell
 * @author Eddú Meléndez
 * @author Thomas Kåsene
 * @author Moritz Halbritter
 * @since 1.5.0
 */
public class ConcurrentKafkaListenerContainerFactoryConfigurer {

	private KafkaProperties properties;

	private BatchMessageConverter batchMessageConverter;

	private RecordMessageConverter recordMessageConverter;

	private RecordFilterStrategy<Object, Object> recordFilterStrategy;

	private KafkaTemplate<Object, Object> replyTemplate;

	private KafkaAwareTransactionManager<Object, Object> transactionManager;

	private ConsumerAwareRebalanceListener rebalanceListener;

	private CommonErrorHandler commonErrorHandler;

	private AfterRollbackProcessor<Object, Object> afterRollbackProcessor;

	private RecordInterceptor<Object, Object> recordInterceptor;

	private BatchInterceptor<Object, Object> batchInterceptor;

	private Function<MessageListenerContainer, String> threadNameSupplier;

	private SimpleAsyncTaskExecutor listenerTaskExecutor;

	/**
	 * Set the {@link KafkaProperties} to use.
	 * @param properties the properties
	 */
	void setKafkaProperties(KafkaProperties properties) {
		this.properties = properties;
	}

	/**
	 * Set the {@link BatchMessageConverter} to use.
	 * @param batchMessageConverter the message converter
	 */
	void setBatchMessageConverter(BatchMessageConverter batchMessageConverter) {
		this.batchMessageConverter = batchMessageConverter;
	}

	/**
	 * Set the {@link RecordMessageConverter} to use.
	 * @param recordMessageConverter the message converter
	 */
	void setRecordMessageConverter(RecordMessageConverter recordMessageConverter) {
		this.recordMessageConverter = recordMessageConverter;
	}

	/**
	 * Set the {@link RecordFilterStrategy} to use to filter incoming records.
	 * @param recordFilterStrategy the record filter strategy
	 */
	void setRecordFilterStrategy(RecordFilterStrategy<Object, Object> recordFilterStrategy) {
		this.recordFilterStrategy = recordFilterStrategy;
	}

	/**
	 * Set the {@link KafkaTemplate} to use to send replies.
	 * @param replyTemplate the reply template
	 */
	void setReplyTemplate(KafkaTemplate<Object, Object> replyTemplate) {
		this.replyTemplate = replyTemplate;
	}

	/**
	 * Set the {@link KafkaAwareTransactionManager} to use.
	 * @param transactionManager the transaction manager
	 */
	void setTransactionManager(KafkaAwareTransactionManager<Object, Object> transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * Set the {@link ConsumerAwareRebalanceListener} to use.
	 * @param rebalanceListener the rebalance listener.
	 * @since 2.2
	 */
	void setRebalanceListener(ConsumerAwareRebalanceListener rebalanceListener) {
		this.rebalanceListener = rebalanceListener;
	}

	/**
	 * Set the {@link CommonErrorHandler} to use.
	 * @param commonErrorHandler the error handler.
	 * @since 2.6.0
	 */
	public void setCommonErrorHandler(CommonErrorHandler commonErrorHandler) {
		this.commonErrorHandler = commonErrorHandler;
	}

	/**
	 * Set the {@link AfterRollbackProcessor} to use.
	 * @param afterRollbackProcessor the after rollback processor
	 */
	void setAfterRollbackProcessor(AfterRollbackProcessor<Object, Object> afterRollbackProcessor) {
		this.afterRollbackProcessor = afterRollbackProcessor;
	}

	/**
	 * Set the {@link RecordInterceptor} to use.
	 * @param recordInterceptor the record interceptor.
	 */
	void setRecordInterceptor(RecordInterceptor<Object, Object> recordInterceptor) {
		this.recordInterceptor = recordInterceptor;
	}

	/**
	 * Set the {@link BatchInterceptor} to use.
	 * @param batchInterceptor the batch interceptor.
	 */
	void setBatchInterceptor(BatchInterceptor<Object, Object> batchInterceptor) {
		this.batchInterceptor = batchInterceptor;
	}

	/**
	 * Set the thread name supplier to use.
	 * @param threadNameSupplier the thread name supplier to use
	 */
	void setThreadNameSupplier(Function<MessageListenerContainer, String> threadNameSupplier) {
		this.threadNameSupplier = threadNameSupplier;
	}

	/**
	 * Set the executor for threads that poll the consumer.
	 * @param listenerTaskExecutor task executor
	 */
	void setListenerTaskExecutor(SimpleAsyncTaskExecutor listenerTaskExecutor) {
		this.listenerTaskExecutor = listenerTaskExecutor;
	}

	/**
	 * Configure the specified Kafka listener container factory. The factory can be
	 * further tuned and default settings can be overridden.
	 * @param listenerFactory the {@link ConcurrentKafkaListenerContainerFactory} instance
	 * to configure
	 * @param consumerFactory the {@link ConsumerFactory} to use
	 */
	public void configure(ConcurrentKafkaListenerContainerFactory<Object, Object> listenerFactory,
			ConsumerFactory<Object, Object> consumerFactory) {
		listenerFactory.setConsumerFactory(consumerFactory);
		configureListenerFactory(listenerFactory);
		configureContainer(listenerFactory.getContainerProperties());
	}

	private void configureListenerFactory(ConcurrentKafkaListenerContainerFactory<Object, Object> factory) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		Listener properties = this.properties.getListener();
		map.from(properties::getConcurrency).to(factory::setConcurrency);
		map.from(properties::isAutoStartup).to(factory::setAutoStartup);
		map.from(this.batchMessageConverter).to(factory::setBatchMessageConverter);
		map.from(this.recordMessageConverter).to(factory::setRecordMessageConverter);
		map.from(this.recordFilterStrategy).to(factory::setRecordFilterStrategy);
		map.from(this.replyTemplate).to(factory::setReplyTemplate);
		if (properties.getType().equals(Listener.Type.BATCH)) {
			factory.setBatchListener(true);
		}
		map.from(this.commonErrorHandler).to(factory::setCommonErrorHandler);
		map.from(this.afterRollbackProcessor).to(factory::setAfterRollbackProcessor);
		map.from(this.recordInterceptor).to(factory::setRecordInterceptor);
		map.from(this.batchInterceptor).to(factory::setBatchInterceptor);
		map.from(this.threadNameSupplier).to(factory::setThreadNameSupplier);
		map.from(properties::getChangeConsumerThreadName).to(factory::setChangeConsumerThreadName);
	}

	private void configureContainer(ContainerProperties container) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		Listener properties = this.properties.getListener();
		map.from(properties::getAckMode).to(container::setAckMode);
		map.from(properties::getAsyncAcks).to(container::setAsyncAcks);
		map.from(properties::getClientId).to(container::setClientId);
		map.from(properties::getAckCount).to(container::setAckCount);
		map.from(properties::getAckTime).as(Duration::toMillis).to(container::setAckTime);
		map.from(properties::getPollTimeout).as(Duration::toMillis).to(container::setPollTimeout);
		map.from(properties::getNoPollThreshold).to(container::setNoPollThreshold);
		map.from(properties.getIdleBetweenPolls()).as(Duration::toMillis).to(container::setIdleBetweenPolls);
		map.from(properties::getIdleEventInterval).as(Duration::toMillis).to(container::setIdleEventInterval);
		map.from(properties::getIdlePartitionEventInterval)
			.as(Duration::toMillis)
			.to(container::setIdlePartitionEventInterval);
		map.from(properties::getMonitorInterval)
			.as(Duration::getSeconds)
			.as(Number::intValue)
			.to(container::setMonitorInterval);
		map.from(properties::getLogContainerConfig).to(container::setLogContainerConfig);
		map.from(properties::isMissingTopicsFatal).to(container::setMissingTopicsFatal);
		map.from(properties::isImmediateStop).to(container::setStopImmediate);
		map.from(this.transactionManager).to(container::setTransactionManager);
		map.from(this.rebalanceListener).to(container::setConsumerRebalanceListener);
		map.from(this.listenerTaskExecutor).to(container::setListenerTaskExecutor);
	}

}
