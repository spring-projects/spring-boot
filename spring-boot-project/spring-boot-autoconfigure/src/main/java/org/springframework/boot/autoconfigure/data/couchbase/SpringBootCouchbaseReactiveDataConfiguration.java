/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.data.couchbase;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.AbstractReactiveCouchbaseDataConfiguration;
import org.springframework.data.couchbase.config.BeanNames;
import org.springframework.data.couchbase.config.CouchbaseConfigurer;
import org.springframework.data.couchbase.core.RxJavaCouchbaseTemplate;
import org.springframework.data.couchbase.core.query.Consistency;
import org.springframework.data.couchbase.repository.config.ReactiveRepositoryOperationsMapping;

/**
 * Configure Spring Data's reactive couchbase support.
 *
 * @author Alex Derkach
 */
@Configuration
@ConditionalOnMissingBean(AbstractReactiveCouchbaseDataConfiguration.class)
@ConditionalOnBean(CouchbaseConfigurer.class)
class SpringBootCouchbaseReactiveDataConfiguration
		extends AbstractReactiveCouchbaseDataConfiguration {

	private final CouchbaseDataProperties properties;

	private final CouchbaseConfigurer couchbaseConfigurer;

	SpringBootCouchbaseReactiveDataConfiguration(CouchbaseDataProperties properties,
			CouchbaseConfigurer couchbaseConfigurer) {
		this.properties = properties;
		this.couchbaseConfigurer = couchbaseConfigurer;
	}

	@Override
	protected CouchbaseConfigurer couchbaseConfigurer() {
		return this.couchbaseConfigurer;
	}

	@Override
	protected Consistency getDefaultConsistency() {
		return this.properties.getConsistency();
	}

	@Override
	@ConditionalOnMissingBean(name = BeanNames.RXJAVA1_COUCHBASE_TEMPLATE)
	@Bean(name = BeanNames.RXJAVA1_COUCHBASE_TEMPLATE)
	public RxJavaCouchbaseTemplate reactiveCouchbaseTemplate() throws Exception {
		return super.reactiveCouchbaseTemplate();
	}

	@Override
	@ConditionalOnMissingBean(name = BeanNames.REACTIVE_COUCHBASE_OPERATIONS_MAPPING)
	@Bean(name = BeanNames.REACTIVE_COUCHBASE_OPERATIONS_MAPPING)
	public ReactiveRepositoryOperationsMapping reactiveRepositoryOperationsMapping(
			RxJavaCouchbaseTemplate reactiveCouchbaseTemplate) throws Exception {
		return super.reactiveRepositoryOperationsMapping(reactiveCouchbaseTemplate);
	}

}
