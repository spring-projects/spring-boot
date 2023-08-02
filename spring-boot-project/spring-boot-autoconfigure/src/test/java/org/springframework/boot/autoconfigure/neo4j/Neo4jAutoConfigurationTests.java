/*
 * Copyright 2012-2023 the original author or authors.
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
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.AuthTokenManagers;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Config.ConfigBuilder;
import org.neo4j.driver.Driver;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.neo4j.Neo4jAutoConfiguration.PropertiesNeo4jConnectionDetails;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.Authentication;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.Security.TrustStrategy;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link Neo4jAutoConfiguration}.
 *
 * @author Michael J. Simons
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class Neo4jAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(Neo4jAutoConfiguration.class));

	@Test
	void driverNotConfiguredWithoutDriverApi() {
		this.contextRunner.withPropertyValues("spring.neo4j.uri=bolt://localhost:4711")
			.withClassLoader(new FilteredClassLoader(Driver.class))
			.run((ctx) -> assertThat(ctx).doesNotHaveBean(Driver.class));
	}

	@Test
	void driverShouldNotRequireUri() {
		this.contextRunner.run((ctx) -> assertThat(ctx).hasSingleBean(Driver.class));
	}

	@Test
	void driverShouldInvokeConfigBuilderCustomizers() {
		this.contextRunner.withPropertyValues("spring.neo4j.uri=bolt://localhost:4711")
			.withBean(ConfigBuilderCustomizer.class, () -> ConfigBuilder::withEncryption)
			.run((ctx) -> assertThat(ctx.getBean(Driver.class).isEncrypted()).isTrue());
	}

	@ParameterizedTest
	@ValueSource(strings = { "bolt", "neo4j" })
	void uriWithSimpleSchemeAreDetected(String scheme) {
		this.contextRunner.withPropertyValues("spring.neo4j.uri=" + scheme + "://localhost:4711").run((ctx) -> {
			assertThat(ctx).hasSingleBean(Driver.class);
			assertThat(ctx.getBean(Driver.class).isEncrypted()).isFalse();
		});
	}

	@ParameterizedTest
	@ValueSource(strings = { "bolt+s", "bolt+ssc", "neo4j+s", "neo4j+ssc" })
	void uriWithAdvancedSchemesAreDetected(String scheme) {
		this.contextRunner.withPropertyValues("spring.neo4j.uri=" + scheme + "://localhost:4711").run((ctx) -> {
			assertThat(ctx).hasSingleBean(Driver.class);
			Driver driver = ctx.getBean(Driver.class);
			assertThat(driver.isEncrypted()).isTrue();
		});
	}

	@ParameterizedTest
	@ValueSource(strings = { "bolt+routing", "bolt+x", "neo4j+wth" })
	void uriWithInvalidSchemesAreDetected(String invalidScheme) {
		this.contextRunner.withPropertyValues("spring.neo4j.uri=" + invalidScheme + "://localhost:4711")
			.run((ctx) -> assertThat(ctx).hasFailed()
				.getFailure()
				.hasMessageContaining("'%s' is not a supported scheme.", invalidScheme));
	}

	@Test
	void definesPropertiesBasedConnectionDetailsByDefault() {
		this.contextRunner.run((context) -> assertThat(context).hasSingleBean(PropertiesNeo4jConnectionDetails.class));
	}

	@Test
	void shouldUseCustomConnectionDetailsWhenDefined() {
		this.contextRunner.withBean(Neo4jConnectionDetails.class, () -> new Neo4jConnectionDetails() {

			@Override
			public URI getUri() {
				return URI.create("bolt+ssc://localhost:12345");
			}

		}).run((context) -> {
			assertThat(context).hasSingleBean(Driver.class)
				.hasSingleBean(Neo4jConnectionDetails.class)
				.doesNotHaveBean(PropertiesNeo4jConnectionDetails.class);
			Driver driver = context.getBean(Driver.class);
			assertThat(driver.isEncrypted()).isTrue();
		});
	}

	@Test
	void connectionTimeout() {
		Neo4jProperties properties = new Neo4jProperties();
		properties.setConnectionTimeout(Duration.ofMillis(500));
		assertThat(mapDriverConfig(properties).connectionTimeoutMillis()).isEqualTo(500);
	}

	@Test
	void maxTransactionRetryTime() {
		Neo4jProperties properties = new Neo4jProperties();
		properties.setMaxTransactionRetryTime(Duration.ofSeconds(2));
		assertThat(mapDriverConfig(properties).maxTransactionRetryTimeMillis()).isEqualTo(2000L);
	}

	@Test
	void uriShouldDefaultToLocalhost() {
		assertThat(new PropertiesNeo4jConnectionDetails(new Neo4jProperties(), null).getUri())
			.isEqualTo(URI.create("bolt://localhost:7687"));
	}

	@Test
	void determineServerUriWithCustomUriShouldOverrideDefault() {
		URI customUri = URI.create("bolt://localhost:4242");
		Neo4jProperties properties = new Neo4jProperties();
		properties.setUri(customUri);
		assertThat(new PropertiesNeo4jConnectionDetails(properties, null).getUri()).isEqualTo(customUri);
	}

	@Test
	void authenticationShouldDefaultToNone() {
		assertThat(new PropertiesNeo4jConnectionDetails(new Neo4jProperties(), null).getAuthToken())
			.isEqualTo(AuthTokens.none());
	}

	@Test
	void authenticationWithUsernameShouldEnableBasicAuth() {
		Neo4jProperties properties = new Neo4jProperties();
		properties.getAuthentication().setUsername("Farin");
		properties.getAuthentication().setPassword("Urlaub");
		PropertiesNeo4jConnectionDetails connectionDetails = new PropertiesNeo4jConnectionDetails(properties, null);
		assertThat(connectionDetails.getAuthToken()).isEqualTo(AuthTokens.basic("Farin", "Urlaub"));
		assertThat(connectionDetails.getAuthTokenManager()).isNull();
	}

	@Test
	void authenticationWithUsernameAndRealmShouldEnableBasicAuth() {
		Neo4jProperties properties = new Neo4jProperties();
		Authentication authentication = properties.getAuthentication();
		authentication.setUsername("Farin");
		authentication.setPassword("Urlaub");
		authentication.setRealm("Test Realm");
		PropertiesNeo4jConnectionDetails connectionDetails = new PropertiesNeo4jConnectionDetails(properties, null);
		assertThat(connectionDetails.getAuthToken()).isEqualTo(AuthTokens.basic("Farin", "Urlaub", "Test Realm"));
		assertThat(connectionDetails.getAuthTokenManager()).isNull();
	}

	@Test
	void authenticationWithAuthTokenManagerAndUsernameShouldProvideAuthTokenManger() {
		Neo4jProperties properties = new Neo4jProperties();
		Authentication authentication = properties.getAuthentication();
		authentication.setUsername("Farin");
		authentication.setPassword("Urlaub");
		authentication.setRealm("Test Realm");
		assertThat(new PropertiesNeo4jConnectionDetails(properties,
				AuthTokenManagers.expirationBased(
						() -> AuthTokens.basic("username", "password").expiringAt(System.currentTimeMillis() + 5000)))
			.getAuthTokenManager()).isNotNull();
	}

	@Test
	void authenticationWithKerberosTicketShouldEnableKerberos() {
		Neo4jProperties properties = new Neo4jProperties();
		Authentication authentication = properties.getAuthentication();
		authentication.setKerberosTicket("AABBCCDDEE");
		assertThat(new PropertiesNeo4jConnectionDetails(properties, null).getAuthToken())
			.isEqualTo(AuthTokens.kerberos("AABBCCDDEE"));
	}

	@Test
	void authenticationWithBothUsernameAndKerberosShouldNotBeAllowed() {
		Neo4jProperties properties = new Neo4jProperties();
		Authentication authentication = properties.getAuthentication();
		authentication.setUsername("Farin");
		authentication.setKerberosTicket("AABBCCDDEE");
		assertThatIllegalStateException()
			.isThrownBy(() -> new PropertiesNeo4jConnectionDetails(properties, null).getAuthToken())
			.withMessage("Cannot specify both username ('Farin') and kerberos ticket ('AABBCCDDEE')");
	}

	@Test
	void poolWithMetricsEnabled() {
		Neo4jProperties properties = new Neo4jProperties();
		properties.getPool().setMetricsEnabled(true);
		assertThat(mapDriverConfig(properties).isMetricsEnabled()).isTrue();
	}

	@Test
	void poolWithLogLeakedSessions() {
		Neo4jProperties properties = new Neo4jProperties();
		properties.getPool().setLogLeakedSessions(true);
		assertThat(mapDriverConfig(properties).logLeakedSessions()).isTrue();
	}

	@Test
	void poolWithMaxConnectionPoolSize() {
		Neo4jProperties properties = new Neo4jProperties();
		properties.getPool().setMaxConnectionPoolSize(4711);
		assertThat(mapDriverConfig(properties).maxConnectionPoolSize()).isEqualTo(4711);
	}

	@Test
	void poolWithIdleTimeBeforeConnectionTest() {
		Neo4jProperties properties = new Neo4jProperties();
		properties.getPool().setIdleTimeBeforeConnectionTest(Duration.ofSeconds(23));
		assertThat(mapDriverConfig(properties).idleTimeBeforeConnectionTest()).isEqualTo(23000);
	}

	@Test
	void poolWithMaxConnectionLifetime() {
		Neo4jProperties properties = new Neo4jProperties();
		properties.getPool().setMaxConnectionLifetime(Duration.ofSeconds(30));
		assertThat(mapDriverConfig(properties).maxConnectionLifetimeMillis()).isEqualTo(30000);
	}

	@Test
	void poolWithConnectionAcquisitionTimeout() {
		Neo4jProperties properties = new Neo4jProperties();
		properties.getPool().setConnectionAcquisitionTimeout(Duration.ofSeconds(5));
		assertThat(mapDriverConfig(properties).connectionAcquisitionTimeoutMillis()).isEqualTo(5000);
	}

	@Test
	void securityWithEncrypted() {
		Neo4jProperties properties = new Neo4jProperties();
		properties.getSecurity().setEncrypted(true);
		assertThat(mapDriverConfig(properties).encrypted()).isTrue();
	}

	@Test
	void securityWithTrustSignedCertificates() {
		Neo4jProperties properties = new Neo4jProperties();
		properties.getSecurity().setTrustStrategy(TrustStrategy.TRUST_SYSTEM_CA_SIGNED_CERTIFICATES);
		assertThat(mapDriverConfig(properties).trustStrategy().strategy())
			.isEqualTo(Config.TrustStrategy.Strategy.TRUST_SYSTEM_CA_SIGNED_CERTIFICATES);
	}

	@Test
	void securityWithTrustAllCertificates() {
		Neo4jProperties properties = new Neo4jProperties();
		properties.getSecurity().setTrustStrategy(TrustStrategy.TRUST_ALL_CERTIFICATES);
		assertThat(mapDriverConfig(properties).trustStrategy().strategy())
			.isEqualTo(Config.TrustStrategy.Strategy.TRUST_ALL_CERTIFICATES);
	}

	@Test
	void securityWitHostnameVerificationEnabled() {
		Neo4jProperties properties = new Neo4jProperties();
		properties.getSecurity().setTrustStrategy(TrustStrategy.TRUST_ALL_CERTIFICATES);
		properties.getSecurity().setHostnameVerificationEnabled(true);
		assertThat(mapDriverConfig(properties).trustStrategy().isHostnameVerificationEnabled()).isTrue();
	}

	@Test
	void securityWithCustomCertificates(@TempDir File directory) throws IOException {
		File certFile = new File(directory, "neo4j-driver.cert");
		assertThat(certFile.createNewFile()).isTrue();

		Neo4jProperties properties = new Neo4jProperties();
		properties.getSecurity().setTrustStrategy(TrustStrategy.TRUST_CUSTOM_CA_SIGNED_CERTIFICATES);
		properties.getSecurity().setCertFile(certFile);
		Config.TrustStrategy trustStrategy = mapDriverConfig(properties).trustStrategy();
		assertThat(trustStrategy.strategy())
			.isEqualTo(Config.TrustStrategy.Strategy.TRUST_CUSTOM_CA_SIGNED_CERTIFICATES);
		assertThat(trustStrategy.certFiles()).containsOnly(certFile);
	}

	@Test
	void securityWithCustomCertificatesShouldFailWithoutCertificate() {
		Neo4jProperties properties = new Neo4jProperties();
		properties.getSecurity().setTrustStrategy(TrustStrategy.TRUST_CUSTOM_CA_SIGNED_CERTIFICATES);
		assertThatExceptionOfType(InvalidConfigurationPropertyValueException.class)
			.isThrownBy(() -> mapDriverConfig(properties))
			.withMessage(
					"Property spring.neo4j.security.trust-strategy with value 'TRUST_CUSTOM_CA_SIGNED_CERTIFICATES' is invalid: Configured trust strategy requires a certificate file.");
	}

	@Test
	void securityWithTrustSystemCertificates() {
		Neo4jProperties properties = new Neo4jProperties();
		properties.getSecurity().setTrustStrategy(TrustStrategy.TRUST_SYSTEM_CA_SIGNED_CERTIFICATES);
		assertThat(mapDriverConfig(properties).trustStrategy().strategy())
			.isEqualTo(Config.TrustStrategy.Strategy.TRUST_SYSTEM_CA_SIGNED_CERTIFICATES);
	}

	@Test
	void driverConfigShouldBeConfiguredToUseUseSpringJclLogging() {
		assertThat(mapDriverConfig(new Neo4jProperties()).logging()).isInstanceOf(Neo4jSpringJclLogging.class);
	}

	private Config mapDriverConfig(Neo4jProperties properties, ConfigBuilderCustomizer... customizers) {
		return new Neo4jAutoConfiguration().mapDriverConfig(properties,
				new PropertiesNeo4jConnectionDetails(properties, null), Arrays.asList(customizers));
	}

}
