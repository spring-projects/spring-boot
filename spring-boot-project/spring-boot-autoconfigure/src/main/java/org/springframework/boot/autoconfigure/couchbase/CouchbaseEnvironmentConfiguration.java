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

import com.couchbase.client.core.env.KeyValueServiceConfig;
import com.couchbase.client.core.env.QueryServiceConfig;
import com.couchbase.client.core.env.ViewServiceConfig;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Support class to configure Couchbase based on {@link CouchbaseConfiguration}.
 *
 * @author Artsiom Yudovin
 * @since 2.1.0
 */
@Configuration
public class CouchbaseEnvironmentConfiguration {

	private final CouchbaseProperties properties;

	public CouchbaseEnvironmentConfiguration(CouchbaseProperties properties) {
		this.properties = properties;
	}

	@Bean
	@Primary
	public DefaultCouchbaseEnvironment couchbaseEnvironment() {
		return initializeEnvironmentBuilder(this.properties).build();
	}

	/**
	 * Initialize an environment builder based on the specified settings.
	 * @param properties the couchbase properties to use
	 * @return the {@link DefaultCouchbaseEnvironment} builder.
	 */
	protected DefaultCouchbaseEnvironment.Builder initializeEnvironmentBuilder(
			CouchbaseProperties properties) {
		CouchbaseProperties.Endpoints endpoints = properties.getEnv().getEndpoints();
		CouchbaseProperties.Timeouts timeouts = properties.getEnv().getTimeouts();
		DefaultCouchbaseEnvironment.Builder builder = DefaultCouchbaseEnvironment
				.builder();
		if (timeouts.getConnect() != null) {
			builder = builder.connectTimeout(timeouts.getConnect().toMillis());
		}
		builder = builder.keyValueServiceConfig(
				KeyValueServiceConfig.create(endpoints.getKeyValue()));
		if (timeouts.getKeyValue() != null) {
			builder = builder.kvTimeout(timeouts.getKeyValue().toMillis());
		}
		if (timeouts.getQuery() != null) {
			builder = builder.queryTimeout(timeouts.getQuery().toMillis());
			builder = builder.queryServiceConfig(getQueryServiceConfig(endpoints));
			builder = builder.viewServiceConfig(getViewServiceConfig(endpoints));
		}
		if (timeouts.getSocketConnect() != null) {
			builder = builder
					.socketConnectTimeout((int) timeouts.getSocketConnect().toMillis());
		}
		if (timeouts.getView() != null) {
			builder = builder.viewTimeout(timeouts.getView().toMillis());
		}
		CouchbaseProperties.Ssl ssl = properties.getEnv().getSsl();
		if (ssl.getEnabled()) {
			builder = builder.sslEnabled(true);
			if (ssl.getKeyStore() != null) {
				builder = builder.sslKeystoreFile(ssl.getKeyStore());
			}
			if (ssl.getKeyStorePassword() != null) {
				builder = builder.sslKeystorePassword(ssl.getKeyStorePassword());
			}
		}
		return builder;
	}

	private QueryServiceConfig getQueryServiceConfig(
			CouchbaseProperties.Endpoints endpoints) {
		return QueryServiceConfig.create(endpoints.getQueryservice().getMinEndpoints(),
				endpoints.getQueryservice().getMaxEndpoints());
	}

	private ViewServiceConfig getViewServiceConfig(
			CouchbaseProperties.Endpoints endpoints) {
		return ViewServiceConfig.create(endpoints.getViewservice().getMinEndpoints(),
				endpoints.getViewservice().getMaxEndpoints());
	}

}
