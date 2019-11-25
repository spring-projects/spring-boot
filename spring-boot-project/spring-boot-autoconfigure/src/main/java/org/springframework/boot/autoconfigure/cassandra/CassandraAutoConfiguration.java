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

package org.springframework.boot.autoconfigure.cassandra;

import java.time.Duration;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.SocketOptions;

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
 * @author Steffen F. Qvistgaard
 * @since 1.3.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ Cluster.class })
@EnableConfigurationProperties(CassandraProperties.class)
public class CassandraAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public Cluster cassandraCluster(CassandraProperties properties,
			ObjectProvider<ClusterBuilderCustomizer> builderCustomizers,
			ObjectProvider<ClusterFactory> clusterFactory) {
		PropertyMapper map = PropertyMapper.get();
		Cluster.Builder builder = Cluster.builder().withClusterName(properties.getClusterName())
				.withPort(properties.getPort());
		map.from(properties::getUsername).whenNonNull()
				.to((username) -> builder.withCredentials(username, properties.getPassword()));
		map.from(properties::getCompression).whenNonNull().to(builder::withCompression);
		QueryOptions queryOptions = getQueryOptions(properties);
		map.from(queryOptions).to(builder::withQueryOptions);
		SocketOptions socketOptions = getSocketOptions(properties);
		map.from(socketOptions).to(builder::withSocketOptions);
		map.from(properties::isSsl).whenTrue().toCall(builder::withSSL);
		PoolingOptions poolingOptions = getPoolingOptions(properties);
		map.from(poolingOptions).to(builder::withPoolingOptions);
		map.from(properties::getContactPoints).as(StringUtils::toStringArray).to(builder::addContactPoints);
		map.from(properties::isJmxEnabled).whenFalse().toCall(builder::withoutJMXReporting);
		builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return clusterFactory.getIfAvailable(() -> Cluster::buildFrom).create(builder);
	}

	private QueryOptions getQueryOptions(CassandraProperties properties) {
		PropertyMapper map = PropertyMapper.get();
		QueryOptions options = new QueryOptions();
		map.from(properties::getConsistencyLevel).whenNonNull().to(options::setConsistencyLevel);
		map.from(properties::getSerialConsistencyLevel).whenNonNull().to(options::setSerialConsistencyLevel);
		map.from(properties::getFetchSize).to(options::setFetchSize);
		return options;
	}

	private SocketOptions getSocketOptions(CassandraProperties properties) {
		PropertyMapper map = PropertyMapper.get();
		SocketOptions options = new SocketOptions();
		map.from(properties::getConnectTimeout).whenNonNull().asInt(Duration::toMillis)
				.to(options::setConnectTimeoutMillis);
		map.from(properties::getReadTimeout).whenNonNull().asInt(Duration::toMillis).to(options::setReadTimeoutMillis);
		return options;
	}

	private PoolingOptions getPoolingOptions(CassandraProperties properties) {
		PropertyMapper map = PropertyMapper.get();
		CassandraProperties.Pool poolProperties = properties.getPool();
		PoolingOptions options = new PoolingOptions();
		map.from(poolProperties::getIdleTimeout).whenNonNull().asInt(Duration::getSeconds)
				.to(options::setIdleTimeoutSeconds);
		map.from(poolProperties::getPoolTimeout).whenNonNull().asInt(Duration::toMillis)
				.to(options::setPoolTimeoutMillis);
		map.from(poolProperties::getHeartbeatInterval).whenNonNull().asInt(Duration::getSeconds)
				.to(options::setHeartbeatIntervalSeconds);
		map.from(poolProperties::getMaxQueueSize).to(options::setMaxQueueSize);
		return options;
	}

}
