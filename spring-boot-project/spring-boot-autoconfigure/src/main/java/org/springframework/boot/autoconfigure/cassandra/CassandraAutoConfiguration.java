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

package org.springframework.boot.autoconfigure.cassandra;

import java.time.Duration;
import java.util.List;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.SocketOptions;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Cassandra.
 *
 * @author Julien Dubois
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@Configuration
@ConditionalOnClass({ Cluster.class })
@EnableConfigurationProperties(CassandraProperties.class)
public class CassandraAutoConfiguration {

	private final CassandraProperties properties;

	private final List<ClusterBuilderCustomizer> builderCustomizers;

	public CassandraAutoConfiguration(CassandraProperties properties,
			ObjectProvider<List<ClusterBuilderCustomizer>> builderCustomizers) {
		this.properties = properties;
		this.builderCustomizers = builderCustomizers.getIfAvailable();
	}

	@Bean
	@ConditionalOnMissingBean
	public Cluster cassandraCluster() {
		PropertyMapper map = PropertyMapper.get();
		CassandraProperties cassandraProperties = this.properties;
		Cluster.Builder builder = Cluster.builder()
				.withClusterName(cassandraProperties.getClusterName())
				.withPort(cassandraProperties.getPort());
		map.from(cassandraProperties::getUsername).whenNonNull().to((username) -> builder
				.withCredentials(username, cassandraProperties.getPassword()));
		map.from(cassandraProperties::getCompression).whenNonNull().to(builder::withCompression);
		map.from(cassandraProperties::getLoadBalancingPolicy).whenNonNull()
				.as(BeanUtils::instantiateClass).to(builder::withLoadBalancingPolicy);
		map.from(this::getQueryOptions).to(builder::withQueryOptions);
		map.from(cassandraProperties::getReconnectionPolicy).whenNonNull()
				.as(BeanUtils::instantiateClass).to(builder::withReconnectionPolicy);
		map.from(cassandraProperties::getRetryPolicy).whenNonNull().as(BeanUtils::instantiateClass)
				.to(builder::withRetryPolicy);
		map.from(this::getSocketOptions).to(builder::withSocketOptions);
		map.from(cassandraProperties::isSsl).whenTrue().toCall(builder::withSSL);
		map.from(this::getPoolingOptions).to(builder::withPoolingOptions);
		map.from(cassandraProperties::getContactPoints)
				.as((list) -> list.toArray(new String[list.size()]))
				.to(builder::addContactPoints);
		customize(builder);
		return builder.build();
	}

	private void customize(Cluster.Builder builder) {
		if (this.builderCustomizers != null) {
			for (ClusterBuilderCustomizer customizer : this.builderCustomizers) {
				customizer.customize(builder);
			}
		}
	}

	private QueryOptions getQueryOptions() {
		PropertyMapper map = PropertyMapper.get();
		QueryOptions options = new QueryOptions();
		CassandraProperties cassandraProperties = this.properties;
		map.from(cassandraProperties::getConsistencyLevel).whenNonNull()
				.to(options::setConsistencyLevel);
		map.from(cassandraProperties::getSerialConsistencyLevel).whenNonNull()
				.to(options::setSerialConsistencyLevel);
		map.from(cassandraProperties::getFetchSize).to(options::setFetchSize);
		return options;
	}

	private SocketOptions getSocketOptions() {
		PropertyMapper map = PropertyMapper.get();
		SocketOptions options = new SocketOptions();
		map.from(this.properties::getConnectTimeout).whenNonNull()
				.asInt(Duration::toMillis).to(options::setConnectTimeoutMillis);
		map.from(this.properties::getReadTimeout).whenNonNull().asInt(Duration::toMillis)
				.to(options::setReadTimeoutMillis);
		return options;
	}

	private PoolingOptions getPoolingOptions() {
		PropertyMapper map = PropertyMapper.get();
		CassandraProperties.Pool propertiesPool = this.properties.getPool();
		PoolingOptions options = new PoolingOptions();
		map.from(propertiesPool::getIdleTimeout).whenNonNull().asInt(Duration::getSeconds)
				.to(options::setIdleTimeoutSeconds);
		map.from(propertiesPool::getPoolTimeout).whenNonNull().asInt(Duration::toMillis)
				.to(options::setPoolTimeoutMillis);
		map.from(propertiesPool::getHeartbeatInterval).whenNonNull()
				.asInt(Duration::getSeconds).to(options::setHeartbeatIntervalSeconds);
		map.from(propertiesPool::getMaxQueueSize).to(options::setMaxQueueSize);
		return options;
	}

}
