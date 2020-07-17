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

package org.springframework.boot.autoconfigure.neo4j;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.internal.Scheme;
import org.neo4j.driver.net.ServerAddressResolver;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Automatic configuration of Neo4j's Java Driver.
 *
 * @author Michael J. Simons
 * @since 2.4.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(Driver.class)
@EnableConfigurationProperties(Neo4jProperties.class)
public class Neo4jAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(Driver.class)
	Driver neo4jDriver(Neo4jProperties properties) {
		AuthToken authToken = asAuthToken(properties.getAuthentication());
		Config config = asDriverConfig(properties);
		return GraphDatabase.driver(properties.getUri(), authToken, config);
	}

	static AuthToken asAuthToken(Neo4jProperties.Authentication authentication) {
		String username = authentication.getUsername();
		String password = authentication.getPassword();
		String kerberosTicket = authentication.getKerberosTicket();
		String realm = authentication.getRealm();

		boolean hasUsername = StringUtils.hasText(username);
		boolean hasPassword = StringUtils.hasText(password);
		boolean hasKerberosTicket = StringUtils.hasText(kerberosTicket);

		if (hasUsername && hasKerberosTicket) {
			throw new InvalidConfigurationPropertyValueException("org.neo4j.driver.authentication",
					"username=" + username + ",kerberos-ticket=" + kerberosTicket,
					"Cannot specify both username and kerberos ticket.");
		}

		if (hasUsername && hasPassword) {
			return AuthTokens.basic(username, password, realm);
		}

		if (hasKerberosTicket) {
			return AuthTokens.kerberos(kerberosTicket);
		}

		return AuthTokens.none();
	}

	static Config asDriverConfig(Neo4jProperties properties) {
		Config.ConfigBuilder builder = Config.builder();
		applyTo(builder, properties.getPool());
		URI uri = properties.getUri();
		String scheme = (uri != null) ? uri.getScheme() : "bolt";
		applyTo(builder, properties.getConfig(), isSimpleScheme(scheme));

		return builder.withLogging(new Neo4jSpringJclLogging()).build();
	}

	static boolean isSimpleScheme(String scheme) {
		String lowerCaseScheme = scheme.toLowerCase(Locale.ENGLISH);
		try {
			Scheme.validateScheme(lowerCaseScheme);
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException(String.format("'%s' is not a supported scheme.", scheme));
		}

		return lowerCaseScheme.equals("bolt") || lowerCaseScheme.equals("neo4j");
	}

	private static void applyTo(Config.ConfigBuilder builder, Neo4jProperties.PoolSettings poolSettings) {
		if (poolSettings.isLogLeakedSessions()) {
			builder.withLeakedSessionsLogging();
		}
		builder.withMaxConnectionPoolSize(poolSettings.getMaxConnectionPoolSize());
		Duration idleTimeBeforeConnectionTest = poolSettings.getIdleTimeBeforeConnectionTest();
		if (idleTimeBeforeConnectionTest != null) {
			builder.withConnectionLivenessCheckTimeout(idleTimeBeforeConnectionTest.toMillis(), TimeUnit.MILLISECONDS);
		}
		builder.withMaxConnectionLifetime(poolSettings.getMaxConnectionLifetime().toMillis(), TimeUnit.MILLISECONDS);
		builder.withConnectionAcquisitionTimeout(poolSettings.getConnectionAcquisitionTimeout().toMillis(),
				TimeUnit.MILLISECONDS);

		if (poolSettings.isMetricsEnabled()) {
			builder.withDriverMetrics();
		}
		else {
			builder.withoutDriverMetrics();
		}
	}

	private static void applyTo(Config.ConfigBuilder builder, Neo4jProperties.DriverSettings driverSettings,
			boolean withEncryptionAndTrustSettings) {
		if (withEncryptionAndTrustSettings) {
			applyEncryptionAndTrustSettings(builder, driverSettings);
		}

		builder.withConnectionTimeout(driverSettings.getConnectionTimeout().toMillis(), TimeUnit.MILLISECONDS);
		builder.withMaxTransactionRetryTime(driverSettings.getMaxTransactionRetryTime().toMillis(),
				TimeUnit.MILLISECONDS);

		Class<? extends ServerAddressResolver> serverAddressResolverClass = driverSettings
				.getServerAddressResolverClass();
		if (serverAddressResolverClass != null) {
			builder.withResolver(BeanUtils.instantiateClass(serverAddressResolverClass));
		}
	}

	private static void applyEncryptionAndTrustSettings(Config.ConfigBuilder builder,
			Neo4jProperties.DriverSettings driverSettings) {
		if (driverSettings.isEncrypted()) {
			builder.withEncryption();
		}
		else {
			builder.withoutEncryption();
		}
		builder.withTrustStrategy(toInternalRepresentation(driverSettings.getTrustSettings()));
	}

	static Config.TrustStrategy toInternalRepresentation(Neo4jProperties.TrustSettings trustSettings) {
		String propertyName = "org.neo4j.driver.config.trust-settings";

		Config.TrustStrategy internalRepresentation;
		Neo4jProperties.TrustSettings.Strategy strategy = trustSettings.getStrategy();
		switch (strategy) {
		case TRUST_ALL_CERTIFICATES:
			internalRepresentation = Config.TrustStrategy.trustAllCertificates();
			break;
		case TRUST_SYSTEM_CA_SIGNED_CERTIFICATES:
			internalRepresentation = Config.TrustStrategy.trustSystemCertificates();
			break;
		case TRUST_CUSTOM_CA_SIGNED_CERTIFICATES:
			File certFile = trustSettings.getCertFile();
			if (certFile == null || !certFile.isFile()) {
				throw new InvalidConfigurationPropertyValueException(propertyName, strategy.name(),
						"Configured trust strategy requires a certificate file.");
			}
			internalRepresentation = Config.TrustStrategy.trustCustomCertificateSignedBy(certFile);
			break;
		default:
			throw new InvalidConfigurationPropertyValueException(propertyName, strategy.name(), "Unknown strategy.");
		}

		if (trustSettings.isHostnameVerificationEnabled()) {
			internalRepresentation.withHostnameVerification();
		}
		else {
			internalRepresentation.withoutHostnameVerification();
		}

		return internalRepresentation;
	}

}
