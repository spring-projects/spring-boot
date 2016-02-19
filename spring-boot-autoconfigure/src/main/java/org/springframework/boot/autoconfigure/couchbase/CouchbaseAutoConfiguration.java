/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.couchbase;

import java.util.List;

import javax.validation.Validator;

import com.couchbase.client.java.CouchbaseBucket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.AnyNestedCondition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.data.couchbase.core.mapping.event.ValidatingCouchbaseEventListener;
import org.springframework.data.couchbase.core.query.Consistency;
import org.springframework.data.couchbase.repository.support.IndexManager;

/**
 * {@link EnableAutoConfiguration Auto-Configuration} for Couchbase.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 1.4.0
 */
@Configuration
@ConditionalOnClass({ CouchbaseBucket.class, AbstractCouchbaseConfiguration.class })
@Conditional(CouchbaseAutoConfiguration.CouchbaseCondition.class)
@EnableConfigurationProperties(CouchbaseProperties.class)
public class CouchbaseAutoConfiguration {

	@Bean
	@ConditionalOnBean(Validator.class)
	public ValidatingCouchbaseEventListener validationEventListener(Validator validator) {
		return new ValidatingCouchbaseEventListener(validator);
	}

	@Configuration
	@ConditionalOnMissingBean(AbstractCouchbaseConfiguration.class)
	public static class CouchbaseConfiguration extends AbstractCouchbaseConfiguration {

		@Autowired
		private CouchbaseProperties properties;

		@Override
		protected List<String> getBootstrapHosts() {
			return this.properties.getBootstrapHosts();
		}

		@Override
		protected String getBucketName() {
			return this.properties.getBucket().getName();
		}

		@Override
		protected String getBucketPassword() {
			return this.properties.getBucket().getPassword();
		}

		@Override
		protected Consistency getDefaultConsistency() {
			return this.properties.getConsistency();
		}

		@Override
		@ConditionalOnMissingBean(name = "couchbaseTemplate")
		@Bean(name = "couchbaseTemplate")
		public CouchbaseTemplate couchbaseTemplate() throws Exception {
			return super.couchbaseTemplate();
		}

		@Override
		@ConditionalOnMissingBean(name = "couchbaseIndexManager")
		@Bean(name = "couchbaseIndexManager")
		public IndexManager indexManager() {
			if (this.properties.isAutoIndex()) {
				return new IndexManager(true, true, true);
			}
			else {
				return new IndexManager(false, false, false);
			}
		}

	}

	/**
	 * Determine if Couchbase should be configured. This happens if either the
	 * user-configuration defines a couchbase configuration or if at least the bucket name
	 * is specified.
	 */
	static class CouchbaseCondition extends AnyNestedCondition {

		CouchbaseCondition() {
			super(ConfigurationPhase.REGISTER_BEAN);
		}

		@ConditionalOnProperty(prefix = "spring.data.couchbase.bucket", name = "name")
		static class BucketNameProperty {
		}

		@ConditionalOnBean(AbstractCouchbaseConfiguration.class)
		static class CouchbaseConfiguration {
		}

	}

}
