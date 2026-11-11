/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.elasticsearch.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.elasticsearch.repository.support.ElasticsearchRepositoryFactoryBean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Elasticsearch
 * Repositories.
 *
 * @author Artur Konczak
 * @author Mohsin Husen
 * @since 4.0.0
 * @see EnableElasticsearchRepositories
 */
@AutoConfiguration
@ConditionalOnClass(ElasticsearchRepository.class)
@ConditionalOnBooleanProperty(name = "spring.data.elasticsearch.repositories.enabled", matchIfMissing = true)
@ConditionalOnMissingBean(ElasticsearchRepositoryFactoryBean.class)
@Import(DataElasticsearchRepositoriesRegistrar.class)
public final class DataElasticsearchRepositoriesAutoConfiguration {

}
