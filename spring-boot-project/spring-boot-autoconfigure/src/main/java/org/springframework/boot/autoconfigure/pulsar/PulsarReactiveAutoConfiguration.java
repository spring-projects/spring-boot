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

package org.springframework.boot.autoconfigure.pulsar;

import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.reactive.client.adapter.AdaptedReactivePulsarClientFactory;
import org.apache.pulsar.reactive.client.adapter.ProducerCacheProvider;
import org.apache.pulsar.reactive.client.api.ReactiveMessageSenderCache;
import org.apache.pulsar.reactive.client.api.ReactivePulsarClient;
import org.apache.pulsar.reactive.client.producercache.CaffeineShadedProducerCacheProvider;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.pulsar.core.SchemaResolver;
import org.springframework.pulsar.core.TopicResolver;
import org.springframework.pulsar.reactive.core.DefaultReactivePulsarConsumerFactory;
import org.springframework.pulsar.reactive.core.DefaultReactivePulsarReaderFactory;
import org.springframework.pulsar.reactive.core.DefaultReactivePulsarSenderFactory;
import org.springframework.pulsar.reactive.core.ReactivePulsarConsumerFactory;
import org.springframework.pulsar.reactive.core.ReactivePulsarReaderFactory;
import org.springframework.pulsar.reactive.core.ReactivePulsarSenderFactory;
import org.springframework.pulsar.reactive.core.ReactivePulsarTemplate;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring for Apache Pulsar
 * Reactive.
 *
 * @author Chris Bono
 * @author Christophe Bornet
 * @since 3.2.0
 */
@AutoConfiguration(after = PulsarAutoConfiguration.class)
@ConditionalOnClass({ ReactivePulsarTemplate.class, ReactivePulsarClient.class, Flux.class })
@EnableConfigurationProperties(PulsarReactiveProperties.class)
@Import({ PulsarReactiveAnnotationDrivenConfiguration.class })
public class PulsarReactiveAutoConfiguration {

	private final PulsarReactiveProperties properties;

	PulsarReactiveAutoConfiguration(PulsarReactiveProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	ReactivePulsarClient pulsarReactivePulsarClient(PulsarClient pulsarClient) {
		return AdaptedReactivePulsarClientFactory.create(pulsarClient);
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnClass(CaffeineShadedProducerCacheProvider.class)
	@ConditionalOnProperty(name = "spring.pulsar.reactive.sender.cache.enabled", havingValue = "true",
			matchIfMissing = true)
	ProducerCacheProvider pulsarProducerCacheProvider() {
		PulsarReactiveProperties.Cache cache = this.properties.getSender().getCache();
		return new CaffeineShadedProducerCacheProvider(cache.getExpireAfterAccess(), cache.getExpireAfterWrite(),
				cache.getMaximumSize(), cache.getInitialCapacity());
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(name = "spring.pulsar.reactive.sender.cache.enabled", havingValue = "true",
			matchIfMissing = true)
	ReactiveMessageSenderCache pulsarReactiveMessageSenderCache(
			ObjectProvider<ProducerCacheProvider> producerCacheProvider) {
		return producerCacheProvider.stream()
			.findFirst()
			.map(AdaptedReactivePulsarClientFactory::createCache)
			.orElseGet(AdaptedReactivePulsarClientFactory::createCache);
	}

	@Bean
	@ConditionalOnMissingBean
	ReactivePulsarSenderFactory<?> reactivePulsarSenderFactory(ReactivePulsarClient pulsarReactivePulsarClient,
			ObjectProvider<ReactiveMessageSenderCache> cache, TopicResolver topicResolver) {
		return new DefaultReactivePulsarSenderFactory<>(pulsarReactivePulsarClient,
				this.properties.buildReactiveMessageSenderSpec(), cache.getIfAvailable(), topicResolver);
	}

	@Bean
	@ConditionalOnMissingBean
	ReactivePulsarConsumerFactory<?> reactivePulsarConsumerFactory(ReactivePulsarClient pulsarReactivePulsarClient) {
		return new DefaultReactivePulsarConsumerFactory<>(pulsarReactivePulsarClient,
				this.properties.buildReactiveMessageConsumerSpec());
	}

	@Bean
	@ConditionalOnMissingBean
	ReactivePulsarReaderFactory<?> reactivePulsarReaderFactory(ReactivePulsarClient pulsarReactivePulsarClient) {
		return new DefaultReactivePulsarReaderFactory<>(pulsarReactivePulsarClient,
				this.properties.buildReactiveMessageReaderSpec());
	}

	@Bean
	@ConditionalOnMissingBean
	ReactivePulsarTemplate<?> pulsarReactiveTemplate(ReactivePulsarSenderFactory<?> reactivePulsarSenderFactory,
			SchemaResolver schemaResolver, TopicResolver topicResolver) {
		return new ReactivePulsarTemplate<>(reactivePulsarSenderFactory, schemaResolver, topicResolver);
	}

}
