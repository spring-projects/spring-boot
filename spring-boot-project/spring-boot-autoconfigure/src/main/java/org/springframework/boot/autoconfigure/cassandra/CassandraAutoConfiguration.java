/*
 * Copyright 2012-2018 the original author or authors.
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
import org.springframework.util.StringUtils;

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

	private final ObjectProvider<ClusterBuilderCustomizer> builderCustomizers;

	public CassandraAutoConfiguration(CassandraProperties properties,
			ObjectProvider<ClusterBuilderCustomizer> builderCustomizers) {
		this.properties = properties;
		this.builderCustomizers = builderCustomizers;
	}

	@Bean
	@ConditionalOnMissingBean
	@SuppressWarnings("deprecation")
	public Cluster cassandraCluster() {
		PropertyMapper map = PropertyMapper.get();
		CassandraProperties properties = this.properties;
		Cluster.Builder builder = Cluster.builder()
				.withClusterName(properties.getClusterName())
				.withPort(properties.getPort());
		map.from(properties::getUsername).whenNonNull().to((username) -> builder
				.withCredentials(username, properties.getPassword()));
		map.from(properties::getCompression).whenNonNull().to(builder::withCompression);
		map.from(properties::getLoadBalancingPolicy).whenNonNull()
				.as(BeanUtils::instantiateClass).to(builder::withLoadBalancingPolicy);
		map.from(this::getQueryOptions).to(builder::withQueryOptions);
		map.from(properties::getReconnectionPolicy).whenNonNull()
				.as(BeanUtils::instantiateClass).to(builder::withReconnectionPolicy);
		map.from(properties::getRetryPolicy).whenNonNull().as(BeanUtils::instantiateClass)
				.to(builder::withRetryPolicy);
		map.from(this::getSocketOptions).to(builder::withSocketOptions);
		map.from(properties::isSsl).whenTrue().toCall(builder::withSSL);
		map.from(this::getPoolingOptions).to(builder::withPoolingOptions);
		map.from(properties::getContactPoints).as(StringUtils::toStringArray)
				.to(builder::addContactPoints);
		map.from(properties::isJmxEnabled).whenFalse()
				.toCall(builder::withoutJMXReporting);
		customize(builder);
		return builder.build();
	}

	private void customize(Cluster.Builder builder) {
		this.builderCustomizers.orderedStream()
				.forEach((customizer) -> customizer.customize(builder));
	}

	private QueryOptions getQueryOptions() {
		PropertyMapper map = PropertyMapper.get();
		QueryOptions options = new QueryOptions();
		CassandraProperties properties = this.properties;
		map.from(properties::getConsistencyLevel).whenNonNull()
				.to(options::setConsistencyLevel);
		map.from(properties::getSerialConsistencyLevel).whenNonNull()
				.to(options::setSerialConsistencyLevel);
		map.from(properties::getFetchSize).to(options::setFetchSize);
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
		CassandraProperties.Pool properties = this.properties.getPool();
		PoolingOptions options = new PoolingOptions();
		map.from(properties::getIdleTimeout).whenNonNull().asInt(Duration::getSeconds)
				.to(options::setIdleTimeoutSeconds);
		map.from(properties::getPoolTimeout).whenNonNull().asInt(Duration::toMillis)
				.to(options::setPoolTimeoutMillis);
		map.from(properties::getHeartbeatInterval).whenNonNull()
				.asInt(Duration::getSeconds).to(options::setHeartbeatIntervalSeconds);
		map.from(properties::getMaxQueueSize).to(options::setMaxQueueSize);
		return options;
	}

}
