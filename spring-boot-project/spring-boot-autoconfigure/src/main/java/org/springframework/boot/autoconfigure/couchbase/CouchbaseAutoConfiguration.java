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

import com.couchbase.client.core.env.KeyValueServiceConfig;
import com.couchbase.client.core.env.QueryServiceConfig;
import com.couchbase.client.core.env.ViewServiceConfig;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseBucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.cluster.ClusterInfo;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment.Builder;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties.Endpoints;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

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
@Conditional(OnBootstrapHostsCondition.class)
@EnableConfigurationProperties(CouchbaseProperties.class)
public class CouchbaseAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(CouchbaseEnvironment.class)
	public DefaultCouchbaseEnvironment couchbaseEnvironment(CouchbaseProperties properties,
			ObjectProvider<CouchbaseEnvironmentBuilderCustomizer> customizers) {
		Builder builder = initializeEnvironmentBuilder(properties);
		customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	@Bean
	@ConditionalOnMissingBean(Cluster.class)
	public CouchbaseCluster couchbaseCluster(CouchbaseProperties properties,
			CouchbaseEnvironment couchbaseEnvironment) {
		CouchbaseCluster couchbaseCluster = CouchbaseCluster.create(couchbaseEnvironment,
				properties.getBootstrapHosts());
		if (isRoleBasedAccessControlEnabled(properties)) {
			return couchbaseCluster.authenticate(properties.getUsername(), properties.getPassword());
		}
		return couchbaseCluster;
	}

	@Bean
	@ConditionalOnMissingBean
	@DependsOn("couchbaseClient")
	public ClusterInfo couchbaseClusterInfo(CouchbaseProperties properties, Cluster couchbaseCluster) {
		if (isRoleBasedAccessControlEnabled(properties)) {
			return couchbaseCluster.clusterManager().info();
		}
		return couchbaseCluster.clusterManager(properties.getBucket().getName(), properties.getBucket().getPassword())
				.info();
	}

	@Bean
	@ConditionalOnMissingBean
	public Bucket couchbaseClient(CouchbaseProperties properties, Cluster couchbaseCluster) {
		if (isRoleBasedAccessControlEnabled(properties)) {
			return couchbaseCluster.openBucket(properties.getBucket().getName());
		}
		return couchbaseCluster.openBucket(properties.getBucket().getName(), properties.getBucket().getPassword());
	}

	private boolean isRoleBasedAccessControlEnabled(CouchbaseProperties properties) {
		return properties.getUsername() != null && properties.getPassword() != null;
	}

	private DefaultCouchbaseEnvironment.Builder initializeEnvironmentBuilder(CouchbaseProperties properties) {
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
