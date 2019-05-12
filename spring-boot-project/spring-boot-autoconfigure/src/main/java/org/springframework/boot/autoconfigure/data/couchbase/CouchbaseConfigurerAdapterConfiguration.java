/*
 * Copyright 2012-2019 the original author or authors.
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

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.CouchbaseConfigurer;

/**
 * Adapt the core Couchbase configuration to an expected {@link CouchbaseConfigurer} if
 * necessary.
 *
 * @author Stephane Nicoll
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(CouchbaseConfigurer.class)
@ConditionalOnBean(CouchbaseConfiguration.class)
class CouchbaseConfigurerAdapterConfiguration {

	private final CouchbaseConfiguration configuration;

	CouchbaseConfigurerAdapterConfiguration(CouchbaseConfiguration configuration) {
		this.configuration = configuration;
	}

	@Bean
	@ConditionalOnMissingBean
	public CouchbaseConfigurer springBootCouchbaseConfigurer() throws Exception {
		return new SpringBootCouchbaseConfigurer(
				this.configuration.couchbaseEnvironment(),
				this.configuration.couchbaseCluster(),
				this.configuration.couchbaseClusterInfo(),
				this.configuration.couchbaseClient());
	}

}
