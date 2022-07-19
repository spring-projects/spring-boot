/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.neo4j;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Config.TrustStrategy;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.internal.Scheme;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.Pool;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.Security;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Neo4j.
 *
 * @author Michael J. Simons
 * @author Stephane Nicoll
 * @since 2.4.0
 */
@AutoConfiguration
@ConditionalOnClass(Driver.class)
@EnableConfigurationProperties(Neo4jProperties.class)
public class Neo4jAutoConfiguration {

	private static final URI DEFAULT_SERVER_URI = URI.create("bolt://localhost:7687");

	@Bean
	@ConditionalOnMissingBean
	public Driver neo4jDriver(Neo4jProperties properties, Environment environment,
			ObjectProvider<ConfigBuilderCustomizer> configBuilderCustomizers) {
		AuthToken authToken = mapAuthToken(properties.getAuthentication(), environment);
		Config config = mapDriverConfig(properties,
				configBuilderCustomizers.orderedStream().collect(Collectors.toList()));
		URI serverUri = determineServerUri(properties, environment);
		return GraphDatabase.driver(serverUri, authToken, config);
	}

	URI determineServerUri(Neo4jProperties properties, Environment environment) {
		return getOrFallback(properties.getUri(), () -> {
			URI deprecatedProperty = environment.getProperty("spring.data.neo4j.uri", URI.class);
			return (deprecatedProperty != null) ? deprecatedProperty : DEFAULT_SERVER_URI;
		});
	}

	AuthToken mapAuthToken(Neo4jProperties.Authentication authentication, Environment environment) {
		String username = getOrFallback(authentication.getUsername(),
				() -> environment.getProperty("spring.data.neo4j.username", String.class));
		String password = getOrFallback(authentication.getPassword(),
				() -> environment.getProperty("spring.data.neo4j.password", String.class));
		String kerberosTicket = authentication.getKerberosTicket();
		String realm = authentication.getRealm();

		boolean hasUsername = StringUtils.hasText(username);
		boolean hasPassword = StringUtils.hasText(password);
		boolean hasKerberosTicket = StringUtils.hasText(kerberosTicket);

		if (hasUsername && hasKerberosTicket) {
			throw new IllegalStateException(String.format(
					"Cannot specify both username ('%s') and kerberos ticket ('%s')", username, kerberosTicket));
		}
		if (hasUsername && hasPassword) {
			return AuthTokens.basic(username, password, realm);
		}
		if (hasKerberosTicket) {
			return AuthTokens.kerberos(kerberosTicket);
		}
		return AuthTokens.none();
	}

	private <T> T getOrFallback(T value, Supplier<T> fallback) {
		if (value != null) {
			return value;
		}
		return fallback.get();
	}

	Config mapDriverConfig(Neo4jProperties properties, List<ConfigBuilderCustomizer> customizers) {
		Config.ConfigBuilder builder = Config.builder();
		configurePoolSettings(builder, properties.getPool());
		URI uri = properties.getUri();
		String scheme = (uri != null) ? uri.getScheme() : "bolt";
		configureDriverSettings(builder, properties, isSimpleScheme(scheme));
		builder.withLogging(new Neo4jSpringJclLogging());
		customizers.forEach((customizer) -> customizer.customize(builder));
		return builder.build();
	}

	private boolean isSimpleScheme(String scheme) {
		String lowerCaseScheme = scheme.toLowerCase(Locale.ENGLISH);
		try {
			Scheme.validateScheme(lowerCaseScheme);
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException(String.format("'%s' is not a supported scheme.", scheme));
		}
		return lowerCaseScheme.equals("bolt") || lowerCaseScheme.equals("neo4j");
	}

	private void configurePoolSettings(Config.ConfigBuilder builder, Pool pool) {
		if (pool.isLogLeakedSessions()) {
			builder.withLeakedSessionsLogging();
		}
		builder.withMaxConnectionPoolSize(pool.getMaxConnectionPoolSize());
		Duration idleTimeBeforeConnectionTest = pool.getIdleTimeBeforeConnectionTest();
		if (idleTimeBeforeConnectionTest != null) {
			builder.withConnectionLivenessCheckTimeout(idleTimeBeforeConnectionTest.toMillis(), TimeUnit.MILLISECONDS);
		}
		builder.withMaxConnectionLifetime(pool.getMaxConnectionLifetime().toMillis(), TimeUnit.MILLISECONDS);
		builder.withConnectionAcquisitionTimeout(pool.getConnectionAcquisitionTimeout().toMillis(),
				TimeUnit.MILLISECONDS);
		if (pool.isMetricsEnabled()) {
			builder.withDriverMetrics();
		}
		else {
			builder.withoutDriverMetrics();
		}
	}

	private void configureDriverSettings(Config.ConfigBuilder builder, Neo4jProperties properties,
			boolean withEncryptionAndTrustSettings) {
		if (withEncryptionAndTrustSettings) {
			applyEncryptionAndTrustSettings(builder, properties.getSecurity());
		}
		builder.withConnectionTimeout(properties.getConnectionTimeout().toMillis(), TimeUnit.MILLISECONDS);
		builder.withMaxTransactionRetryTime(properties.getMaxTransactionRetryTime().toMillis(), TimeUnit.MILLISECONDS);
	}

	private void applyEncryptionAndTrustSettings(Config.ConfigBuilder builder,
			Neo4jProperties.Security securityProperties) {
		if (securityProperties.isEncrypted()) {
			builder.withEncryption();
		}
		else {
			builder.withoutEncryption();
		}
		builder.withTrustStrategy(mapTrustStrategy(securityProperties));
	}

	private Config.TrustStrategy mapTrustStrategy(Neo4jProperties.Security securityProperties) {
		String propertyName = "spring.neo4j.security.trust-strategy";
		Security.TrustStrategy strategy = securityProperties.getTrustStrategy();
		TrustStrategy trustStrategy = createTrustStrategy(securityProperties, propertyName, strategy);
		if (securityProperties.isHostnameVerificationEnabled()) {
			trustStrategy.withHostnameVerification();
		}
		else {
			trustStrategy.withoutHostnameVerification();
		}
		return trustStrategy;
	}

	private TrustStrategy createTrustStrategy(Neo4jProperties.Security securityProperties, String propertyName,
			Security.TrustStrategy strategy) {
		switch (strategy) {
			case TRUST_ALL_CERTIFICATES:
				return TrustStrategy.trustAllCertificates();
			case TRUST_SYSTEM_CA_SIGNED_CERTIFICATES:
				return TrustStrategy.trustSystemCertificates();
			case TRUST_CUSTOM_CA_SIGNED_CERTIFICATES:
				File certFile = securityProperties.getCertFile();
				if (certFile == null || !certFile.isFile()) {
					throw new InvalidConfigurationPropertyValueException(propertyName, strategy.name(),
							"Configured trust strategy requires a certificate file.");
				}
				return TrustStrategy.trustCustomCertificateSignedBy(certFile);
			default:
				throw new InvalidConfigurationPropertyValueException(propertyName, strategy.name(),
						"Unknown strategy.");
		}
	}

}
