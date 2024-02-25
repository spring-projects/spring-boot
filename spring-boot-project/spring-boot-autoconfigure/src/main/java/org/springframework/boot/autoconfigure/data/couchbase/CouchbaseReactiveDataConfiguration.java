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

package org.springframework.boot.autoconfigure.data.couchbase;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.CouchbaseClientFactory;
import org.springframework.data.couchbase.config.BeanNames;
import org.springframework.data.couchbase.core.ReactiveCouchbaseTemplate;
import org.springframework.data.couchbase.core.convert.MappingCouchbaseConverter;
import org.springframework.data.couchbase.repository.config.ReactiveRepositoryOperationsMapping;

/**
 * Configuration for Spring Data's couchbase reactive support.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnSingleCandidate(CouchbaseClientFactory.class)
class CouchbaseReactiveDataConfiguration {

	/**
     * Creates a new instance of ReactiveCouchbaseTemplate using the provided CouchbaseClientFactory and MappingCouchbaseConverter.
     * 
     * @param couchbaseClientFactory The CouchbaseClientFactory used to create the underlying Couchbase client.
     * @param mappingCouchbaseConverter The MappingCouchbaseConverter used for object mapping.
     * @return A new instance of ReactiveCouchbaseTemplate.
     */
    @Bean(name = BeanNames.REACTIVE_COUCHBASE_TEMPLATE)
	@ConditionalOnMissingBean(name = BeanNames.REACTIVE_COUCHBASE_TEMPLATE)
	ReactiveCouchbaseTemplate reactiveCouchbaseTemplate(CouchbaseClientFactory couchbaseClientFactory,
			MappingCouchbaseConverter mappingCouchbaseConverter) {
		return new ReactiveCouchbaseTemplate(couchbaseClientFactory, mappingCouchbaseConverter);
	}

	/**
     * Creates a new instance of ReactiveRepositoryOperationsMapping bean if it is missing in the application context.
     * This bean is responsible for mapping reactive repository operations to the corresponding Couchbase template.
     * 
     * @param reactiveCouchbaseTemplate The reactive Couchbase template used for repository operations.
     * @return The ReactiveRepositoryOperationsMapping bean.
     */
    @Bean(name = BeanNames.REACTIVE_COUCHBASE_OPERATIONS_MAPPING)
	@ConditionalOnMissingBean(name = BeanNames.REACTIVE_COUCHBASE_OPERATIONS_MAPPING)
	ReactiveRepositoryOperationsMapping reactiveCouchbaseRepositoryOperationsMapping(
			ReactiveCouchbaseTemplate reactiveCouchbaseTemplate) {
		return new ReactiveRepositoryOperationsMapping(reactiveCouchbaseTemplate);
	}

}
