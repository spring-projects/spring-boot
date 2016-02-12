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

import java.util.Arrays;
import java.util.List;

import javax.validation.Validator;

import com.couchbase.client.java.CouchbaseBucket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.couchbase.config.AbstractCouchbaseConfiguration;
import org.springframework.data.couchbase.config.CouchbaseBucketFactoryBean;
import org.springframework.data.couchbase.core.mapping.event.ValidatingCouchbaseEventListener;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-Configuration} for Couchbase.
 *
 * @author Eddú Meléndez
 * @since 1.4.0
 */
@Configuration
@ConditionalOnClass({ CouchbaseBucket.class, CouchbaseBucketFactoryBean.class })
@EnableConfigurationProperties(CouchbaseProperties.class)
public class CouchbaseAutoConfiguration {

	@Bean
	@ConditionalOnBean(Validator.class)
	public ValidatingCouchbaseEventListener validationEventListener(Validator validator) {
		return new ValidatingCouchbaseEventListener(validator);
	}

	@Configuration
	static class CouchbaseConfiguration extends AbstractCouchbaseConfiguration {

		@Autowired
		private CouchbaseProperties properties;

		@Override
		protected List<String> getBootstrapHosts() {
			return Arrays.asList(this.properties.getHosts());
		}

		@Override
		protected String getBucketName() {
			return this.properties.getBucketName();
		}

		@Override
		protected String getBucketPassword() {
			return this.properties.getBucketPassword();
		}

	}

}
