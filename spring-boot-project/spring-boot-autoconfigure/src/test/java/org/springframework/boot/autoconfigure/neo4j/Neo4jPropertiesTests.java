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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.internal.retry.RetrySettings;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.Authentication;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.DriverSettings;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.PoolSettings;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.TrustSettings;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.TrustSettings.Strategy;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;

/**
 * @author Michael J. Simons
 */
class Neo4jPropertiesTests {

	private static void assertDuration(Duration duration, long expectedValueInMillis) {
		if (expectedValueInMillis == org.neo4j.driver.internal.async.pool.PoolSettings.NOT_CONFIGURED) {
			assertThat(duration).isNull();
		}
		else {
			assertThat(duration.toMillis()).isEqualTo(expectedValueInMillis);
		}
	}

	@Test
	void shouldAllowEmptyListOfURIs() {
		Neo4jProperties driverProperties = new Neo4jProperties();
		assertThat(driverProperties.getAuthentication()).isNotNull();
	}

	@Test
	void noAuthenticationShouldWork() {
		Authentication authentication = new Authentication();
		assertThat(asAuthToken(authentication)).isEqualTo(AuthTokens.none());
	}

	@Test
	void basicAuthShouldWork() {
		Authentication authentication = new Authentication();
		authentication.setUsername("Farin");
		authentication.setPassword("Urlaub");

		assertThat(asAuthToken(authentication)).isEqualTo(AuthTokens.basic("Farin", "Urlaub"));
	}

	@Test
	void basicAuthWithRealmShouldWork() {
		Authentication authentication = new Authentication();
		authentication.setUsername("Farin");
		authentication.setPassword("Urlaub");
		authentication.setRealm("Die Ärzte");

		assertThat(asAuthToken(authentication)).isEqualTo(AuthTokens.basic("Farin", "Urlaub", "Die Ärzte"));
	}

	@Test
	void kerberosAuthShouldWork() {
		Authentication authentication = new Authentication();
		authentication.setKerberosTicket("AABBCCDDEE");

		assertThat(asAuthToken(authentication)).isEqualTo(AuthTokens.kerberos("AABBCCDDEE"));
	}

	@Test
	void ambiguousShouldNotBeAllowed() {
		Authentication authentication = new Authentication();
		authentication.setUsername("Farin");
		authentication.setKerberosTicket("AABBCCDDEE");

		assertThatExceptionOfType(InvalidConfigurationPropertyValueException.class)
				.isThrownBy(() -> asAuthToken(authentication)).withMessage(
						"Property org.neo4j.driver.authentication with value 'username=Farin,kerberos-ticket=AABBCCDDEE' is invalid: Cannot specify both username and kerberos ticket.");
	}

	@Test
	void poolSettingsShouldDefaultToDriversValues() {
		Config defaultConfig = Config.defaultConfig();

		Neo4jProperties driverProperties = new Neo4jProperties();

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
		Neo4jProperties driverProperties;

		driverProperties = new Neo4jProperties();
		driverProperties.getPool().setLogLeakedSessions(true);
		assertThat(asDriverConfig(driverProperties).logLeakedSessions()).isTrue();

		driverProperties = new Neo4jProperties();
		driverProperties.getPool().setLogLeakedSessions(false);
		assertThat(asDriverConfig(driverProperties).logLeakedSessions()).isFalse();
	}

	@Test
	void maxConnectionPoolSizeSettingsShouldWork() {
		Neo4jProperties driverProperties = new Neo4jProperties();
		driverProperties.getPool().setMaxConnectionPoolSize(4711);
		assertThat(asDriverConfig(driverProperties).maxConnectionPoolSize()).isEqualTo(4711);
	}

	@Test
	void idleTimeBeforeConnectionTestSettingsShouldWork() {
		Neo4jProperties driverProperties;

		driverProperties = new Neo4jProperties();
		assertThat(asDriverConfig(driverProperties).idleTimeBeforeConnectionTest()).isEqualTo(-1);

		driverProperties = new Neo4jProperties();
		driverProperties.getPool().setIdleTimeBeforeConnectionTest(Duration.ofSeconds(23));
		assertThat(asDriverConfig(driverProperties).idleTimeBeforeConnectionTest()).isEqualTo(23_000);
	}

	@Test
	void connectionAcquisitionTimeoutSettingsShouldWork() {
		Neo4jProperties driverProperties = new Neo4jProperties();
		driverProperties.getPool().setConnectionAcquisitionTimeout(Duration.ofSeconds(23));
		assertThat(asDriverConfig(driverProperties).connectionAcquisitionTimeoutMillis()).isEqualTo(23_000);
	}

	@Test
	void enableMetricsShouldWork() {
		Neo4jProperties driverProperties = new Neo4jProperties();
		assertThat(asDriverConfig(driverProperties).isMetricsEnabled()).isFalse();

		driverProperties.getPool().setMetricsEnabled(true);
		assertThat(asDriverConfig(driverProperties).isMetricsEnabled()).isTrue();
	}

	@Test
	void driverSettingsShouldDefaultToDriversValues() {
		Config defaultConfig = Config.defaultConfig();

		Neo4jProperties driverProperties = new Neo4jProperties();

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
		Neo4jProperties driverProperties;

		driverProperties = new Neo4jProperties();
		driverProperties.getConfig().setEncrypted(true);
		assertThat(asDriverConfig(driverProperties).encrypted()).isTrue();

		driverProperties = new Neo4jProperties();
		driverProperties.getConfig().setEncrypted(false);
		assertThat(asDriverConfig(driverProperties).encrypted()).isFalse();
	}

	@Test
	void trustSettingsShouldWork() {
		Neo4jProperties driverProperties = new Neo4jProperties();
		TrustSettings trustSettings = new TrustSettings();
		trustSettings.setStrategy(Strategy.TRUST_SYSTEM_CA_SIGNED_CERTIFICATES);
		driverProperties.getConfig().setTrustSettings(trustSettings);
		assertThat(asDriverConfig(driverProperties).trustStrategy().strategy())
				.isEqualTo(Config.TrustStrategy.Strategy.TRUST_SYSTEM_CA_SIGNED_CERTIFICATES);
	}

	@Test
	void connectionTimeoutSettingsShouldWork() {
		Neo4jProperties driverProperties = new Neo4jProperties();
		driverProperties.getConfig().setConnectionTimeout(Duration.ofSeconds(23));
		assertThat(asDriverConfig(driverProperties).connectionTimeoutMillis()).isEqualTo(23_000);
	}

	@Test
	@Disabled("The internal driver has no means of retrieving that value back again")
	void maxTransactionRetryTimeSettingsShouldWork() {
		DriverSettings driverSettings = new DriverSettings();
		driverSettings.setMaxTransactionRetryTime(Duration.ofSeconds(23));
	}

	@Test
	void serverAddressResolverClassSettingsShouldWork() {
		Neo4jProperties driverProperties = new Neo4jProperties();
		driverProperties.getConfig().setServerAddressResolverClass(TestServerAddressResolver.class);
		assertThat(asDriverConfig(driverProperties).resolver()).isNotNull()
				.isInstanceOf(TestServerAddressResolver.class);
	}

	@Test
	void shouldUseSpringJclLogging() {
		Neo4jProperties driverProperties = new Neo4jProperties();
		assertThat(asDriverConfig(driverProperties).logging()).isNotNull()
				.isInstanceOf(Neo4jSpringJclLogging.class);
	}

	@Test
	void trustAllCertificatesShouldWork() {
		TrustSettings settings = new TrustSettings();
		settings.setStrategy(Strategy.TRUST_ALL_CERTIFICATES);

		assertThat(toInternalRepresentation(settings).strategy())
				.isEqualTo(Config.TrustStrategy.Strategy.TRUST_ALL_CERTIFICATES);
	}

	@Test
	void shouldEnableHostnameVerification() {
		TrustSettings settings = new TrustSettings();
		settings.setStrategy(Strategy.TRUST_ALL_CERTIFICATES);
		settings.setHostnameVerificationEnabled(true);

		assertThat(toInternalRepresentation(settings).isHostnameVerificationEnabled()).isTrue();
	}

	@Test
	void trustSystemCertificatesShouldWork() {

		TrustSettings settings = new TrustSettings();
		settings.setStrategy(Strategy.TRUST_SYSTEM_CA_SIGNED_CERTIFICATES);

		assertThat(toInternalRepresentation(settings).strategy())
				.isEqualTo(Config.TrustStrategy.Strategy.TRUST_SYSTEM_CA_SIGNED_CERTIFICATES);
	}

	@Test
	void trustCustomCertificatesShouldWork() throws IOException {
		File certFile = File.createTempFile("neo4j-driver", ".cert");

		TrustSettings settings = new TrustSettings();
		settings.setStrategy(Strategy.TRUST_CUSTOM_CA_SIGNED_CERTIFICATES);
		settings.setCertFile(certFile);

		Config.TrustStrategy trustStrategy = toInternalRepresentation(settings);
		assertThat(trustStrategy.strategy())
				.isEqualTo(Config.TrustStrategy.Strategy.TRUST_CUSTOM_CA_SIGNED_CERTIFICATES);
		assertThat(trustStrategy.certFile()).isEqualTo(certFile);
	}

	@Test
	void trustCustomCertificatesShouldFailWithoutCertificate() {
		TrustSettings settings = new TrustSettings();
		settings.setStrategy(Strategy.TRUST_CUSTOM_CA_SIGNED_CERTIFICATES);

		assertThatExceptionOfType(InvalidConfigurationPropertyValueException.class)
				.isThrownBy(() -> toInternalRepresentation(settings)).withMessage(
				"Property org.neo4j.driver.config.trust-settings with value 'TRUST_CUSTOM_CA_SIGNED_CERTIFICATES' is invalid: Configured trust strategy requires a certificate file.");
	}

	@Test
	void shouldAssumeDefaultValuesForUrl() {
		Neo4jProperties driverProperties = new Neo4jProperties();
		assertThat(driverProperties.getUri()).isEqualTo(URI.create("bolt://localhost:7687"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "bolt", "Bolt", "neo4j", "Neo4J" })
	void shouldDetectSimpleSchemes(String aSimpleScheme) {
		assertThat(isSimpleScheme(aSimpleScheme)).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = { "bolt+s", "Bolt+ssc", "neo4j+s", "Neo4J+ssc" })
	void shouldDetectAdvancedSchemes(String anAdvancedScheme) {
		assertThat(isSimpleScheme(anAdvancedScheme)).isFalse();
	}

	@ParameterizedTest
	@ValueSource(strings = { "bolt+routing", "bolt+x", "neo4j+wth" })
	void shouldFailEarlyOnInvalidSchemes(String invalidScheme) {
		assertThatIllegalArgumentException().isThrownBy(() -> isSimpleScheme(invalidScheme))
				.withMessage("'%s' is not a supported scheme.", invalidScheme);
	}

}
