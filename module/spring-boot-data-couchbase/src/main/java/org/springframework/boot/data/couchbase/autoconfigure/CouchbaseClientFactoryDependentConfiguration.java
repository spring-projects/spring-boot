/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.couchbase.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.CouchbaseClientFactory;
import org.springframework.data.couchbase.config.BeanNames;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.data.couchbase.core.convert.MappingCouchbaseConverter;
import org.springframework.data.couchbase.repository.config.RepositoryOperationsMapping;

/**
 * Configuration for Couchbase-related beans that depend on a
 * {@link CouchbaseClientFactory}.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnSingleCandidate(CouchbaseClientFactory.class)
class CouchbaseClientFactoryDependentConfiguration {

	@Bean(name = BeanNames.COUCHBASE_TEMPLATE)
	@ConditionalOnMissingBean(name = BeanNames.COUCHBASE_TEMPLATE)
	CouchbaseTemplate couchbaseTemplate(CouchbaseClientFactory couchbaseClientFactory,
			MappingCouchbaseConverter mappingCouchbaseConverter) {
		return new CouchbaseTemplate(couchbaseClientFactory, mappingCouchbaseConverter);
	}

	@Bean(name = BeanNames.COUCHBASE_OPERATIONS_MAPPING)
	@ConditionalOnMissingBean(name = BeanNames.COUCHBASE_OPERATIONS_MAPPING)
	RepositoryOperationsMapping couchbaseRepositoryOperationsMapping(CouchbaseTemplate couchbaseTemplate) {
		return new RepositoryOperationsMapping(couchbaseTemplate);
	}

}
