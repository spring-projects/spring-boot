/*
 * Copyright 2012-2020 the original author or authors.
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

import java.util.List;

import com.couchbase.client.core.env.KeyValueServiceConfig;
import com.couchbase.client.core.env.QueryServiceConfig;
import com.couchbase.client.core.env.ViewServiceConfig;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.cluster.ClusterInfo;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;

import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties.Endpoints;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;

/**
 * Support class to configure Couchbase based on {@link CouchbaseProperties}.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 2.1.0
 */
@Configuration
public class CouchbaseConfiguration {

	private final CouchbaseProperties properties;

	public CouchbaseConfiguration(CouchbaseProperties properties) {
		this.properties = properties;
	}

	@Bean
	@Primary
	public DefaultCouchbaseEnvironment couchbaseEnvironment() {
		return initializeEnvironmentBuilder(this.properties).build();
	}

	@Bean
	@Primary
	public Cluster couchbaseCluster() {
		CouchbaseCluster couchbaseCluster = CouchbaseCluster.create(couchbaseEnvironment(), determineBootstrapHosts());
		if (isRoleBasedAccessControlEnabled()) {
			return couchbaseCluster.authenticate(this.properties.getUsername(), this.properties.getPassword());
		}
		return couchbaseCluster;
	}

	/**
	 * Determine the Couchbase nodes to bootstrap from.
	 * @return the Couchbase nodes to bootstrap from
	 */
	protected List<String> determineBootstrapHosts() {
		return this.properties.getBootstrapHosts();
	}

	@Bean
	@Primary
	@DependsOn("couchbaseClient")
	public ClusterInfo couchbaseClusterInfo() {
		if (isRoleBasedAccessControlEnabled()) {
			return couchbaseCluster().clusterManager().info();
		}
		return couchbaseCluster()
				.clusterManager(this.properties.getBucket().getName(), this.properties.getBucket().getPassword())
				.info();
	}

	@Bean
	@Primary
	public Bucket couchbaseClient() {
		if (isRoleBasedAccessControlEnabled()) {
			return couchbaseCluster().openBucket(this.properties.getBucket().getName());
		}
		return couchbaseCluster().openBucket(this.properties.getBucket().getName(),
				this.properties.getBucket().getPassword());
	}

	private boolean isRoleBasedAccessControlEnabled() {
		return this.properties.getUsername() != null && this.properties.getPassword() != null;
	}

	/**
	 * Initialize an environment builder based on the specified settings.
	 * @param properties the couchbase properties to use
	 * @return the {@link DefaultCouchbaseEnvironment} builder.
	 */
	protected DefaultCouchbaseEnvironment.Builder initializeEnvironmentBuilder(CouchbaseProperties properties) {
		CouchbaseProperties.Endpoints endpoints = properties.getEnv().getEndpoints();
		CouchbaseProperties.Timeouts timeouts = properties.getEnv().getTimeouts();
		CouchbaseProperties.Bootstrap bootstrap = properties.getEnv().getBootstrap();
		DefaultCouchbaseEnvironment.Builder builder = DefaultCouchbaseEnvironment.builder();
		if (bootstrap.getHttpDirectPort() != null) {
			builder.bootstrapHttpDirectPort(bootstrap.getHttpDirectPort());
		}
		if (bootstrap.getHttpSslPort() != null) {
			builder.bootstrapHttpSslPort(bootstrap.getHttpSslPort());
		}
		if (timeouts.getConnect() != null) {
			builder = builder.connectTimeout(timeouts.getConnect().toMillis());
		}
		builder = builder.keyValueServiceConfig(KeyValueServiceConfig.create(endpoints.getKeyValue()));
		if (timeouts.getKeyValue() != null) {
			builder = builder.kvTimeout(timeouts.getKeyValue().toMillis());
		}
		if (timeouts.getQuery() != null) {
			builder = builder.queryTimeout(timeouts.getQuery().toMillis());
			builder = builder.queryServiceConfig(getQueryServiceConfig(endpoints));
			builder = builder.viewServiceConfig(getViewServiceConfig(endpoints));
		}
		if (timeouts.getSocketConnect() != null) {
			builder = builder.socketConnectTimeout((int) timeouts.getSocketConnect().toMillis());
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

	private QueryServiceConfig getQueryServiceConfig(Endpoints endpoints) {
		return QueryServiceConfig.create(endpoints.getQueryservice().getMinEndpoints(),
				endpoints.getQueryservice().getMaxEndpoints());
	}

	private ViewServiceConfig getViewServiceConfig(Endpoints endpoints) {
		return ViewServiceConfig.create(endpoints.getViewservice().getMinEndpoints(),
				endpoints.getViewservice().getMaxEndpoints());
	}

}
