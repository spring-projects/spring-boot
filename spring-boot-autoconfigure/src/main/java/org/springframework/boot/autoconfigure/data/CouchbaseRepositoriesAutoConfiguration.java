/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.autoconfigure.data;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.couchbase.core.CouchbaseTemplate;
import org.springframework.data.couchbase.repository.CouchbaseRepository;
import org.springframework.data.couchbase.repository.config.EnableCouchbaseRepositories;

import com.couchbase.client.CouchbaseClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's Couchbase
 * Repositories.
 *
 * @author Michael Nitschinger
 * @see EnableCouchbaseRepositories
 */
@Configuration
@ConditionalOnClass({ CouchbaseClient.class, CouchbaseRepository.class })
public class CouchbaseRepositoriesAutoConfiguration {

	@Import(CouchbaseRepositoriesAutoConfigureRegistrar.class)
	@Configuration
	@EnableConfigurationProperties(CouchbaseProperties.class)
	protected static class CouchbaseRepositoriesConfiguration {

		@Autowired
		private CouchbaseProperties config;

		@PreDestroy
		public void close() throws URISyntaxException, IOException {
			couchbaseClient().shutdown();
		}

		@Bean
		@ConditionalOnMissingBean(CouchbaseClient.class)
		CouchbaseClient couchbaseClient() throws URISyntaxException, IOException {
			return this.config.couchbaseClient();
		}

		@Bean
		@ConditionalOnMissingBean(CouchbaseTemplate.class)
		CouchbaseTemplate couchbaseTemplate(CouchbaseClient couchbaseClient) {
			return new CouchbaseTemplate(couchbaseClient);
		}
	}

	@ConfigurationProperties(prefix = "spring.data.couchbase")
	public static class CouchbaseProperties {

		private String host = "127.0.0.1";
		private String bucket = "default";
		private String password = "";

		public CouchbaseClient couchbaseClient() throws URISyntaxException, IOException {
			return new CouchbaseClient(Arrays.asList(new URI("http://" + getHost()
					+ ":8091/pools")), getBucket(), getPassword());
		}

		public String getHost() {
			return this.host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public String getBucket() {
			return this.bucket;
		}

		public void setBucket(String bucket) {
			this.bucket = bucket;
		}

		public String getPassword() {
			return this.password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
	}
}
