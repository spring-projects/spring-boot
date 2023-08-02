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

import java.util.function.Function;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnThreading;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.ContainerCustomizer;
import org.springframework.kafka.config.KafkaListenerConfigUtils;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.AfterRollbackProcessor;
import org.springframework.kafka.listener.BatchInterceptor;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.support.converter.BatchMessageConverter;
import org.springframework.kafka.support.converter.BatchMessagingMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.transaction.KafkaAwareTransactionManager;

/**
 * Configuration for Kafka annotation-driven support.
 *
 * @author Gary Russell
 * @author Eddú Meléndez
 * @author Thomas Kåsene
 * @author Moritz Halbritter
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EnableKafka.class)
class KafkaAnnotationDrivenConfiguration {

	private final KafkaProperties properties;

	private final RecordMessageConverter recordMessageConverter;

	private final RecordFilterStrategy<Object, Object> recordFilterStrategy;

	private final BatchMessageConverter batchMessageConverter;

	private final KafkaTemplate<Object, Object> kafkaTemplate;

	private final KafkaAwareTransactionManager<Object, Object> transactionManager;

	private final ConsumerAwareRebalanceListener rebalanceListener;

	private final CommonErrorHandler commonErrorHandler;

	private final AfterRollbackProcessor<Object, Object> afterRollbackProcessor;

	private final RecordInterceptor<Object, Object> recordInterceptor;

	private final BatchInterceptor<Object, Object> batchInterceptor;

	private final Function<MessageListenerContainer, String> threadNameSupplier;

	KafkaAnnotationDrivenConfiguration(KafkaProperties properties,
			ObjectProvider<RecordMessageConverter> recordMessageConverter,
			ObjectProvider<RecordFilterStrategy<Object, Object>> recordFilterStrategy,
			ObjectProvider<BatchMessageConverter> batchMessageConverter,
			ObjectProvider<KafkaTemplate<Object, Object>> kafkaTemplate,
			ObjectProvider<KafkaAwareTransactionManager<Object, Object>> kafkaTransactionManager,
			ObjectProvider<ConsumerAwareRebalanceListener> rebalanceListener,
			ObjectProvider<CommonErrorHandler> commonErrorHandler,
			ObjectProvider<AfterRollbackProcessor<Object, Object>> afterRollbackProcessor,
			ObjectProvider<RecordInterceptor<Object, Object>> recordInterceptor,
			ObjectProvider<BatchInterceptor<Object, Object>> batchInterceptor,
			ObjectProvider<Function<MessageListenerContainer, String>> threadNameSupplier) {
		this.properties = properties;
		this.recordMessageConverter = recordMessageConverter.getIfUnique();
		this.recordFilterStrategy = recordFilterStrategy.getIfUnique();
		this.batchMessageConverter = batchMessageConverter
			.getIfUnique(() -> new BatchMessagingMessageConverter(this.recordMessageConverter));
		this.kafkaTemplate = kafkaTemplate.getIfUnique();
		this.transactionManager = kafkaTransactionManager.getIfUnique();
		this.rebalanceListener = rebalanceListener.getIfUnique();
		this.commonErrorHandler = commonErrorHandler.getIfUnique();
		this.afterRollbackProcessor = afterRollbackProcessor.getIfUnique();
		this.recordInterceptor = recordInterceptor.getIfUnique();
		this.batchInterceptor = batchInterceptor.getIfUnique();
		this.threadNameSupplier = threadNameSupplier.getIfUnique();
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnThreading(Threading.PLATFORM)
	ConcurrentKafkaListenerContainerFactoryConfigurer kafkaListenerContainerFactoryConfigurer() {
		return configurer();
	}

	@Bean(name = "kafkaListenerContainerFactoryConfigurer")
	@ConditionalOnMissingBean
	@ConditionalOnThreading(Threading.VIRTUAL)
	ConcurrentKafkaListenerContainerFactoryConfigurer kafkaListenerContainerFactoryConfigurerVirtualThreads() {
		ConcurrentKafkaListenerContainerFactoryConfigurer configurer = configurer();
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("kafka-");
		executor.setVirtualThreads(true);
		configurer.setListenerTaskExecutor(executor);
		return configurer;
	}

	private ConcurrentKafkaListenerContainerFactoryConfigurer configurer() {
		ConcurrentKafkaListenerContainerFactoryConfigurer configurer = new ConcurrentKafkaListenerContainerFactoryConfigurer();
		configurer.setKafkaProperties(this.properties);
		configurer.setBatchMessageConverter(this.batchMessageConverter);
		configurer.setRecordMessageConverter(this.recordMessageConverter);
		configurer.setRecordFilterStrategy(this.recordFilterStrategy);
		configurer.setReplyTemplate(this.kafkaTemplate);
		configurer.setTransactionManager(this.transactionManager);
		configurer.setRebalanceListener(this.rebalanceListener);
		configurer.setCommonErrorHandler(this.commonErrorHandler);
		configurer.setAfterRollbackProcessor(this.afterRollbackProcessor);
		configurer.setRecordInterceptor(this.recordInterceptor);
		configurer.setBatchInterceptor(this.batchInterceptor);
		configurer.setThreadNameSupplier(this.threadNameSupplier);
		return configurer;
	}

	@Bean
	@ConditionalOnMissingBean(name = "kafkaListenerContainerFactory")
	ConcurrentKafkaListenerContainerFactory<?, ?> kafkaListenerContainerFactory(
			ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
			ObjectProvider<ConsumerFactory<Object, Object>> kafkaConsumerFactory,
			ObjectProvider<ContainerCustomizer<Object, Object, ConcurrentMessageListenerContainer<Object, Object>>> kafkaContainerCustomizer) {
		ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
		configurer.configure(factory, kafkaConsumerFactory
			.getIfAvailable(() -> new DefaultKafkaConsumerFactory<>(this.properties.buildConsumerProperties())));
		kafkaContainerCustomizer.ifAvailable(factory::setContainerCustomizer);
		return factory;
	}

	@Configuration(proxyBeanMethods = false)
	@EnableKafka
	@ConditionalOnMissingBean(name = KafkaListenerConfigUtils.KAFKA_LISTENER_ANNOTATION_PROCESSOR_BEAN_NAME)
	static class EnableKafkaConfiguration {

	}

}
