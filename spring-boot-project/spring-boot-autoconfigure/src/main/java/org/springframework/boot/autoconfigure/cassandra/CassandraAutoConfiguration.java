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

package org.springframework.boot.autoconfigure.cassandra;

import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.config.DefaultDriverOption;
import com.datastax.oss.driver.api.core.config.DriverConfigLoader;
import com.datastax.oss.driver.api.core.config.DriverOption;
import com.datastax.oss.driver.api.core.config.ProgrammaticDriverConfigLoaderBuilder;
import com.datastax.oss.driver.internal.core.config.typesafe.DefaultDriverConfigLoader;
import com.datastax.oss.driver.internal.core.config.typesafe.DefaultProgrammaticDriverConfigLoaderBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties.Connection;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties.Request;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties.Throttler;
import org.springframework.boot.autoconfigure.cassandra.CassandraProperties.ThrottlerType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

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
@ConditionalOnClass({ CqlSession.class })
@EnableConfigurationProperties(CassandraProperties.class)
public class CassandraAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Lazy
	public CqlSession cassandraSession(CqlSessionBuilder cqlSessionBuilder) {
		return cqlSessionBuilder.build();
	}

	@Bean
	@ConditionalOnMissingBean
	@Scope("prototype")
	public CqlSessionBuilder cassandraSessionBuilder(CassandraProperties properties,
			DriverConfigLoader driverConfigLoader, ObjectProvider<CqlSessionBuilderCustomizer> builderCustomizers) {
		CqlSessionBuilder builder = CqlSession.builder().withConfigLoader(driverConfigLoader);
		configureAuthentication(properties, builder);
		configureSsl(properties, builder);
		builder.withKeyspace(properties.getKeyspaceName());
		builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder;
	}

	private void configureAuthentication(CassandraProperties properties, CqlSessionBuilder builder) {
		if (properties.getUsername() != null) {
			builder.withAuthCredentials(properties.getUsername(), properties.getPassword());
		}
	}

	private void configureSsl(CassandraProperties properties, CqlSessionBuilder builder) {
		if (properties.isSsl()) {
			try {
				builder.withSslContext(SSLContext.getDefault());
			}
			catch (NoSuchAlgorithmException ex) {
				throw new IllegalStateException("Could not setup SSL default context for Cassandra", ex);
			}
		}
	}

	@Bean
	@ConditionalOnMissingBean
	public DriverConfigLoader cassandraDriverConfigLoader(CassandraProperties properties,
			ObjectProvider<DriverConfigLoaderBuilderCustomizer> builderCustomizers) {
		ProgrammaticDriverConfigLoaderBuilder builder = new DefaultProgrammaticDriverConfigLoaderBuilder(
				() -> cassandraConfiguration(properties), DefaultDriverConfigLoader.DEFAULT_ROOT_PATH);
		builderCustomizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	private Config cassandraConfiguration(CassandraProperties properties) {
		CassandraDriverOptions options = new CassandraDriverOptions();
		PropertyMapper map = PropertyMapper.get();
		map.from(properties.getSessionName()).whenHasText()
				.to((sessionName) -> options.add(DefaultDriverOption.SESSION_NAME, sessionName));
		map.from(properties::getUsername).whenNonNull()
				.to((username) -> options.add(DefaultDriverOption.AUTH_PROVIDER_USER_NAME, username)
						.add(DefaultDriverOption.AUTH_PROVIDER_PASSWORD, properties.getPassword()));
		map.from(properties::getCompression).whenNonNull()
				.to((compression) -> options.add(DefaultDriverOption.PROTOCOL_COMPRESSION, compression));
		mapConnectionOptions(properties, options);
		mapPoolingOptions(properties, options);
		mapRequestOptions(properties, options);
		map.from(mapContactPoints(properties))
				.to((contactPoints) -> options.add(DefaultDriverOption.CONTACT_POINTS, contactPoints));
		map.from(properties.getLocalDatacenter()).to(
				(localDatacenter) -> options.add(DefaultDriverOption.LOAD_BALANCING_LOCAL_DATACENTER, localDatacenter));
		ConfigFactory.invalidateCaches();
		return ConfigFactory.defaultOverrides().withFallback(options.build())
				.withFallback(ConfigFactory.defaultReference()).resolve();
	}

	private void mapConnectionOptions(CassandraProperties properties, CassandraDriverOptions options) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		Connection connectionProperties = properties.getConnection();
		map.from(connectionProperties::getConnectTimeout).asInt(Duration::toMillis)
				.to((connectTimeout) -> options.add(DefaultDriverOption.CONNECTION_CONNECT_TIMEOUT, connectTimeout));
		map.from(connectionProperties::getInitQueryTimeout).asInt(Duration::toMillis).to(
				(initQueryTimeout) -> options.add(DefaultDriverOption.CONNECTION_INIT_QUERY_TIMEOUT, initQueryTimeout));
	}

	private void mapPoolingOptions(CassandraProperties properties, CassandraDriverOptions options) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		CassandraProperties.Pool poolProperties = properties.getPool();
		map.from(poolProperties::getIdleTimeout).asInt(Duration::toMillis)
				.to((idleTimeout) -> options.add(DefaultDriverOption.HEARTBEAT_TIMEOUT, idleTimeout));
		map.from(poolProperties::getHeartbeatInterval).asInt(Duration::toMillis)
				.to((heartBeatInterval) -> options.add(DefaultDriverOption.HEARTBEAT_INTERVAL, heartBeatInterval));
	}

	private void mapRequestOptions(CassandraProperties properties, CassandraDriverOptions options) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		Request requestProperties = properties.getRequest();
		map.from(requestProperties::getTimeout).asInt(Duration::toMillis)
				.to(((timeout) -> options.add(DefaultDriverOption.REQUEST_TIMEOUT, timeout)));
		map.from(requestProperties::getConsistency)
				.to(((consistency) -> options.add(DefaultDriverOption.REQUEST_CONSISTENCY, consistency)));
		map.from(requestProperties::getSerialConsistency).to(
				(serialConsistency) -> options.add(DefaultDriverOption.REQUEST_SERIAL_CONSISTENCY, serialConsistency));
		map.from(requestProperties::getPageSize)
				.to((pageSize) -> options.add(DefaultDriverOption.REQUEST_PAGE_SIZE, pageSize));
		Throttler throttlerProperties = requestProperties.getThrottler();
		map.from(throttlerProperties::getType).as(ThrottlerType::type)
				.to((type) -> options.add(DefaultDriverOption.REQUEST_THROTTLER_CLASS, type));
		map.from(throttlerProperties::getMaxQueueSize)
				.to((maxQueueSize) -> options.add(DefaultDriverOption.REQUEST_THROTTLER_MAX_QUEUE_SIZE, maxQueueSize));
		map.from(throttlerProperties::getMaxConcurrentRequests).to((maxConcurrentRequests) -> options
				.add(DefaultDriverOption.REQUEST_THROTTLER_MAX_CONCURRENT_REQUESTS, maxConcurrentRequests));
		map.from(throttlerProperties::getMaxRequestsPerSecond).to((maxRequestsPerSecond) -> options
				.add(DefaultDriverOption.REQUEST_THROTTLER_MAX_REQUESTS_PER_SECOND, maxRequestsPerSecond));
		map.from(throttlerProperties::getDrainInterval).asInt(Duration::toMillis).to(
				(drainInterval) -> options.add(DefaultDriverOption.REQUEST_THROTTLER_DRAIN_INTERVAL, drainInterval));
	}

	private List<String> mapContactPoints(CassandraProperties properties) {
		return properties.getContactPoints().stream()
				.map((candidate) -> formatContactPoint(candidate, properties.getPort())).collect(Collectors.toList());
	}

	private String formatContactPoint(String candidate, int port) {
		int i = candidate.lastIndexOf(':');
		if (i == -1 || !isPort(() -> candidate.substring(i + 1))) {
			return String.format("%s:%s", candidate, port);
		}
		return candidate;
	}

	private boolean isPort(Supplier<String> value) {
		try {
			int i = Integer.parseInt(value.get());
			return i > 0 && i < 65535;
		}
		catch (Exception ex) {
			return false;
		}
	}

	private static class CassandraDriverOptions {

		private final Map<String, String> options = new LinkedHashMap<>();

		private CassandraDriverOptions add(DriverOption option, String value) {
			String key = createKeyFor(option);
			this.options.put(key, value);
			return this;
		}

		private CassandraDriverOptions add(DriverOption option, int value) {
			return add(option, String.valueOf(value));
		}

		private CassandraDriverOptions add(DriverOption option, Enum<?> value) {
			return add(option, value.name());
		}

		private CassandraDriverOptions add(DriverOption option, List<String> values) {
			for (int i = 0; i < values.size(); i++) {
				this.options.put(String.format("%s.%s", createKeyFor(option), i), values.get(i));
			}
			return this;
		}

		private Config build() {
			return ConfigFactory.parseMap(this.options, "Environment");
		}

		private static String createKeyFor(DriverOption option) {
			return String.format("%s.%s", DefaultDriverConfigLoader.DEFAULT_ROOT_PATH, option.getPath());
		}

	}

}
