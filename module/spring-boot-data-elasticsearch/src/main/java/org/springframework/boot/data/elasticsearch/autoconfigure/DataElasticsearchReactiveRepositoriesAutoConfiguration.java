/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.elasticsearch.autoconfigure;

import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;
import org.springframework.data.elasticsearch.repository.support.ReactiveElasticsearchRepositoryFactoryBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Elasticsearch
 * Reactive Repositories.
 *
 * @author Brian Clozel
 * @since 4.0.0
 * @see EnableReactiveElasticsearchRepositories
 */
@AutoConfiguration
@ConditionalOnClass({ ReactiveElasticsearchClient.class, ReactiveElasticsearchRepository.class, Mono.class })
@ConditionalOnBooleanProperty(name = "spring.data.elasticsearch.repositories.enabled", matchIfMissing = true)
@ConditionalOnMissingBean(ReactiveElasticsearchRepositoryFactoryBean.class)
@Import(DataElasticsearchReactiveRepositoriesRegistrar.class)
public final class DataElasticsearchReactiveRepositoriesAutoConfiguration {

}
