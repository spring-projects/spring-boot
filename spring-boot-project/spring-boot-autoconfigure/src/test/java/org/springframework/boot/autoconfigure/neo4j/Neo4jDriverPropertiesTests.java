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

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.neo4j.Neo4jDriverProperties.Authentication;
import org.springframework.boot.autoconfigure.neo4j.Neo4jDriverProperties.DriverSettings;
import org.springframework.boot.autoconfigure.neo4j.Neo4jDriverProperties.PoolSettings;
import org.springframework.boot.autoconfigure.neo4j.Neo4jDriverProperties.TrustSettings;
import org.springframework.boot.autoconfigure.neo4j.Neo4jDriverProperties.TrustSettings.Strategy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.internal.retry.RetrySettings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Michael J. Simons
 */
class Neo4jDriverPropertiesTests {

	private AnnotationConfigApplicationContext context;

	private Neo4jDriverProperties load(String... properties) {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(properties).applyTo(ctx);
		ctx.register(TestConfiguration.class);
		ctx.refresh();
		this.context = ctx;
		return this.context.getBean(Neo4jDriverProperties.class);
	}

	@AfterEach
	void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	private static void assertDuration(Duration duration, long expectedValueInMillis) {
		if (expectedValueInMillis == org.neo4j.driver.internal.async.pool.PoolSettings.NOT_CONFIGURED) {
			assertThat(duration).isNull();
		}
		else {
			assertThat(duration.toMillis()).isEqualTo(expectedValueInMillis);
		}
	}

	@Nested
	@DisplayName("Configuration of authentication")
	class AuthenticationTest {

		@Test
		@DisplayName("…should not be empty by default")
		void shouldAllowEmptyListOfURIs() {

			Neo4jDriverProperties driverProperties = load();
			assertThat(driverProperties.getAuthentication()).isNotNull();
		}

		@Test
		@DisplayName("…should default to none")
		void noAuthenticationShouldWork() {

			Authentication authentication = new Authentication();
			assertThat(authentication.asAuthToken()).isEqualTo(AuthTokens.none());
		}

		@Test
		@DisplayName("…should configure basic auth")
		void basicAuthShouldWork() {

			Authentication authentication = new Authentication();
			authentication.setUsername("Farin");
			authentication.setPassword("Urlaub");

			assertThat(authentication.asAuthToken()).isEqualTo(AuthTokens.basic("Farin", "Urlaub"));
		}

		@Test
		@DisplayName("…should configure basic auth with realm")
		void basicAuthWithRealmShouldWork() {

			Authentication authentication = new Authentication();
			authentication.setUsername("Farin");
			authentication.setPassword("Urlaub");
			authentication.setRealm("Die Ärzte");

			assertThat(authentication.asAuthToken()).isEqualTo(AuthTokens.basic("Farin", "Urlaub", "Die Ärzte"));
		}

		@Test
		@DisplayName("…should configure kerberos")
		void kerberosAuthShouldWork() {

			Authentication authentication = new Authentication();
			authentication.setKerberosTicket("AABBCCDDEE");

			assertThat(authentication.asAuthToken()).isEqualTo(AuthTokens.kerberos("AABBCCDDEE"));
		}

		@Test
		@DisplayName("…should not allow ambiguous config")
		void ambiguousShouldNotBeAllowed() {

			Authentication authentication = new Authentication();
			authentication.setUsername("Farin");
			authentication.setKerberosTicket("AABBCCDDEE");

			assertThatExceptionOfType(InvalidConfigurationPropertyValueException.class)
					.isThrownBy(() -> authentication.asAuthToken()).withMessage(
							"Property org.neo4j.driver.authentication with value 'username=Farin,kerberos-ticket=AABBCCDDEE' is invalid: Cannot specify both username and kerberos ticket.");
		}

	}

	@Nested
	@DisplayName("Pool properties")
	class PoolSettingsTest {

		@Test
		@DisplayName("…should default to drivers values")
		void shouldDefaultToDriversValues() {

			Config defaultConfig = Config.defaultConfig();

			Neo4jDriverProperties driverProperties = load();

			PoolSettings poolSettings = driverProperties.getPool();
			assertThat(poolSettings.isLogLeakedSessions()).isEqualTo(defaultConfig.logLeakedSessions());
			assertThat(poolSettings.getMaxConnectionPoolSize()).isEqualTo(defaultConfig.maxConnectionPoolSize());
			assertDuration(poolSettings.getIdleTimeBeforeConnectionTest(),
					defaultConfig.idleTimeBeforeConnectionTest());
			assertDuration(poolSettings.getMaxConnectionLifetime(), defaultConfig.maxConnectionLifetimeMillis());
			assertDuration(poolSettings.getConnectionAcquisitionTimeout(),
					defaultConfig.connectionAcquisitionTimeoutMillis());
			assertThat(poolSettings.isMetricsEnabled()).isFalse();
		}

		@Test
		void logLeakedSessionsSettingsShouldWork() {

			Neo4jDriverProperties driverProperties;

			driverProperties = new Neo4jDriverProperties();
			driverProperties.getPool().setLogLeakedSessions(true);
			assertThat(driverProperties.asDriverConfig().logLeakedSessions()).isTrue();

			driverProperties = new Neo4jDriverProperties();
			driverProperties.getPool().setLogLeakedSessions(false);
			assertThat(driverProperties.asDriverConfig().logLeakedSessions()).isFalse();
		}

		@Test
		void maxConnectionPoolSizeSettingsShouldWork() {

			Neo4jDriverProperties driverProperties = new Neo4jDriverProperties();
			driverProperties.getPool().setMaxConnectionPoolSize(4711);
			assertThat(driverProperties.asDriverConfig().maxConnectionPoolSize()).isEqualTo(4711);
		}

		@Test
		void idleTimeBeforeConnectionTestSettingsShouldWork() {

			Neo4jDriverProperties driverProperties;

			driverProperties = new Neo4jDriverProperties();
			assertThat(driverProperties.asDriverConfig().idleTimeBeforeConnectionTest()).isEqualTo(-1);

			driverProperties = new Neo4jDriverProperties();
			driverProperties.getPool().setIdleTimeBeforeConnectionTest(Duration.ofSeconds(23));
			assertThat(driverProperties.asDriverConfig().idleTimeBeforeConnectionTest()).isEqualTo(23_000);
		}

		@Test
		void connectionAcquisitionTimeoutSettingsShouldWork() {

			Neo4jDriverProperties driverProperties = new Neo4jDriverProperties();
			driverProperties.getPool().setConnectionAcquisitionTimeout(Duration.ofSeconds(23));
			assertThat(driverProperties.asDriverConfig().connectionAcquisitionTimeoutMillis()).isEqualTo(23_000);
		}

		@Test
		void enableMetricsShouldWork() {

			Neo4jDriverProperties driverProperties = new Neo4jDriverProperties();
			assertThat(driverProperties.asDriverConfig().isMetricsEnabled()).isFalse();

			driverProperties.getPool().setMetricsEnabled(true);
			assertThat(driverProperties.asDriverConfig().isMetricsEnabled()).isTrue();
		}

	}

	@Nested
	@DisplayName("Config properties")
	class DriverSettingsTest {

		@Test
		@DisplayName("…should default to drivers values")
		void shouldDefaultToDriversValues() {

			Config defaultConfig = Config.defaultConfig();

			Neo4jDriverProperties driverProperties = load();

			DriverSettings driverSettings = driverProperties.getConfig();
			assertThat(driverSettings.isEncrypted()).isEqualTo(defaultConfig.encrypted());
			assertThat(driverSettings.getTrustSettings().getStrategy().name())
					.isEqualTo(defaultConfig.trustStrategy().strategy().name());
			assertDuration(driverSettings.getConnectionTimeout(), defaultConfig.connectionTimeoutMillis());
			assertDuration(driverSettings.getMaxTransactionRetryTime(), RetrySettings.DEFAULT.maxRetryTimeMs());
			assertThat(driverSettings.getServerAddressResolverClass()).isNull();
		}

		@Test
		void encryptedSettingsShouldWork() {

			Neo4jDriverProperties driverProperties;

			driverProperties = new Neo4jDriverProperties();
			driverProperties.getConfig().setEncrypted(true);
			assertThat(driverProperties.asDriverConfig().encrypted()).isTrue();

			driverProperties = new Neo4jDriverProperties();
			driverProperties.getConfig().setEncrypted(false);
			assertThat(driverProperties.asDriverConfig().encrypted()).isFalse();
		}

		@Test
		void trustSettingsShouldWork() {

			Neo4jDriverProperties driverProperties = new Neo4jDriverProperties();
			TrustSettings trustSettings = new TrustSettings();
			trustSettings.setStrategy(Strategy.TRUST_SYSTEM_CA_SIGNED_CERTIFICATES);
			driverProperties.getConfig().setTrustSettings(trustSettings);
			assertThat(driverProperties.asDriverConfig().trustStrategy().strategy())
					.isEqualTo(Config.TrustStrategy.Strategy.TRUST_SYSTEM_CA_SIGNED_CERTIFICATES);
		}

		@Test
		void connectionTimeoutSettingsShouldWork() {

			Neo4jDriverProperties driverProperties = new Neo4jDriverProperties();
			driverProperties.getConfig().setConnectionTimeout(Duration.ofSeconds(23));
			assertThat(driverProperties.asDriverConfig().connectionTimeoutMillis()).isEqualTo(23_000);
		}

		@Test
		@Disabled("The internal driver has no means of retrieving that value back again")
		void maxTransactionRetryTimeSettingsShouldWork() {

			DriverSettings driverSettings = new DriverSettings();
			driverSettings.setMaxTransactionRetryTime(Duration.ofSeconds(23));
		}

		@Test
		void serverAddressResolverClassSettingsShouldWork() {

			Neo4jDriverProperties driverProperties = new Neo4jDriverProperties();
			driverProperties.getConfig().setServerAddressResolverClass(TestServerAddressResolver.class);
			assertThat(driverProperties.asDriverConfig().resolver()).isNotNull()
					.isInstanceOf(TestServerAddressResolver.class);
		}

		@Test
		void shouldUseSpringJclLogging() {

			Neo4jDriverProperties driverProperties = new Neo4jDriverProperties();
			assertThat(driverProperties.asDriverConfig().logging()).isNotNull()
					.isInstanceOf(Neo4jSpringJclLogging.class);
		}

	}

	@Nested
	@DisplayName("Trust settings")
	class TrustSettingsTest {

		@Test
		void trustAllCertificatesShouldWork() {

			TrustSettings settings = new TrustSettings();
			settings.setStrategy(Strategy.TRUST_ALL_CERTIFICATES);

			assertThat(settings.toInternalRepresentation().strategy())
					.isEqualTo(Config.TrustStrategy.Strategy.TRUST_ALL_CERTIFICATES);
		}

		@Test
		void shouldEnableHostnameVerification() {

			TrustSettings settings = new TrustSettings();
			settings.setStrategy(Strategy.TRUST_ALL_CERTIFICATES);
			settings.setHostnameVerificationEnabled(true);

			assertThat(settings.toInternalRepresentation().isHostnameVerificationEnabled()).isTrue();
		}

		@Test
		void trustSystemCertificatesShouldWork() {

			TrustSettings settings = new TrustSettings();
			settings.setStrategy(Strategy.TRUST_SYSTEM_CA_SIGNED_CERTIFICATES);

			assertThat(settings.toInternalRepresentation().strategy())
					.isEqualTo(Config.TrustStrategy.Strategy.TRUST_SYSTEM_CA_SIGNED_CERTIFICATES);
		}

		@Test
		@DisplayName("…should recognize correctly configured custom certificates")
		void trustCustomCertificatesShouldWork1() throws IOException {

			File certFile = File.createTempFile("sdnrx", ".cert");

			TrustSettings settings = new TrustSettings();
			settings.setStrategy(Strategy.TRUST_CUSTOM_CA_SIGNED_CERTIFICATES);
			settings.setCertFile(certFile);

			Config.TrustStrategy trustStrategy = settings.toInternalRepresentation();
			assertThat(trustStrategy.strategy())
					.isEqualTo(Config.TrustStrategy.Strategy.TRUST_CUSTOM_CA_SIGNED_CERTIFICATES);
			assertThat(trustStrategy.certFile()).isEqualTo(certFile);
		}

		@Test
		@DisplayName("…should fail on custom certificates without cert file")
		void trustCustomCertificatesShouldWork2() {

			TrustSettings settings = new TrustSettings();
			settings.setStrategy(Strategy.TRUST_CUSTOM_CA_SIGNED_CERTIFICATES);

			assertThatExceptionOfType(InvalidConfigurationPropertyValueException.class)
					.isThrownBy(() -> settings.toInternalRepresentation()).withMessage(
							"Property org.neo4j.driver.config.trust-settings with value 'TRUST_CUSTOM_CA_SIGNED_CERTIFICATES' is invalid: Configured trust strategy requires a certificate file.");
		}

	}

	@Test
	@DisplayName("Should assume default value for the URL")
	void shouldAssumeDefaultValuesForUrl() {

		Neo4jDriverProperties driverProperties = new Neo4jDriverProperties();
		assertThat(driverProperties.getUri()).isEqualTo(URI.create("bolt://localhost:7687"));
	}

	@Nested
	class SchemeDetection {

		@ParameterizedTest
		@ValueSource(strings = { "bolt", "Bolt", "neo4j", "Neo4J" })
		void shouldDetectSimpleSchemes(String aSimpleScheme) {

			assertThat(Neo4jDriverProperties.isSimpleScheme(aSimpleScheme)).isTrue();
		}

		@ParameterizedTest
		@ValueSource(strings = { "bolt+s", "Bolt+ssc", "neo4j+s", "Neo4J+ssc" })
		void shouldDetectAdvancedSchemes(String anAdvancedScheme) {

			assertThat(Neo4jDriverProperties.isSimpleScheme(anAdvancedScheme)).isFalse();
		}

		@ParameterizedTest
		@ValueSource(strings = { "bolt+routing", "bolt+x", "neo4j+wth" })
		void shouldFailEarlyOnInvalidSchemes(String invalidScheme) {

			assertThatIllegalArgumentException().isThrownBy(() -> Neo4jDriverProperties.isSimpleScheme(invalidScheme))
					.withMessage("'%s' is not a supported scheme.", invalidScheme);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(Neo4jDriverProperties.class)
	static class TestConfiguration {

	}

}
