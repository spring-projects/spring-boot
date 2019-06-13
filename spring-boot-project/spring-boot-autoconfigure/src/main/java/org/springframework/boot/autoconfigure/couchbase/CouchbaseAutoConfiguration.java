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

package org.springframework.boot.autoconfigure.couchbase;

import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseBucket;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Couchbase.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Yulin Qin
 * @since 1.4.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ CouchbaseBucket.class, Cluster.class })
@Conditional(CouchbaseAutoConfiguration.CouchbaseCondition.class)
@EnableConfigurationProperties(CouchbaseProperties.class)
public class CouchbaseAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(value = CouchbaseConfiguration.class,
			type = "org.springframework.data.couchbase.config.CouchbaseConfigurer")
	@Import(CouchbaseConfiguration.class)
	static class DefaultCouchbaseConfiguration {

	}

	/**
	 * Determine if Couchbase should be configured. This happens if either the
	 * user-configuration defines a {@code CouchbaseConfigurer} or if at least the
	 * "bootstrapHosts" property is specified.
	 * <p>
	 * The reason why we check for the presence of {@code CouchbaseConfigurer} is that it
	 * might use {@link CouchbaseProperties} for its internal customization.
	 */
	static class CouchbaseCondition extends AnyNestedCondition {

		CouchbaseCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@Conditional(OnBootstrapHostsCondition.class)
		static class BootstrapHostsProperty {

		}

		@ConditionalOnBean(type = "org.springframework.data.couchbase.config.CouchbaseConfigurer")
		static class CouchbaseConfigurerAvailable {

		}

	}

}
