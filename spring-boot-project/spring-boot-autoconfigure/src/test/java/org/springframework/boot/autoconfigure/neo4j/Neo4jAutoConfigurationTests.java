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
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Config.ConfigBuilder;
import org.neo4j.driver.Driver;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.Authentication;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties.Security.TrustStrategy;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link Neo4jAutoConfiguration}.
 *
 * @author Michael J. Simons
 * @author Stephane Nicoll
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
				.run((ctx) -> assertThat(ctx).hasFailed().getFailure()
						.hasMessageContaining("'%s' is not a supported scheme.", invalidScheme));
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
		assertThat(mapDriverConfig(properties)).extracting("retrySettings")
				.hasFieldOrPropertyWithValue("maxRetryTimeMs", 2000L);
	}

	@Test
	void determineServerUriShouldDefaultToLocalhost() {
		assertThat(determineServerUri(new Neo4jProperties(), new MockEnvironment()))
				.isEqualTo(URI.create("bolt://localhost:7687"));
	}

	@Test
	void determineServerUriWithCustomUriShouldOverrideDefault() {
		URI customUri = URI.create("bolt://localhost:4242");
		Neo4jProperties properties = new Neo4jProperties();
		properties.setUri(customUri);
		assertThat(determineServerUri(properties, new MockEnvironment())).isEqualTo(customUri);
	}

	@Test
	@Deprecated
	void determineServerUriWithDeprecatedPropertyShouldOverrideDefault() {
		URI customUri = URI.create("bolt://localhost:4242");
		MockEnvironment environment = new MockEnvironment().withProperty("spring.data.neo4j.uri", customUri.toString());
		assertThat(determineServerUri(new Neo4jProperties(), environment)).isEqualTo(customUri);
	}

	@Test
	@Deprecated
	void determineServerUriWithCustoUriShouldTakePrecedenceOverDeprecatedProperty() {
		URI customUri = URI.create("bolt://localhost:4242");
		URI anotherCustomURI = URI.create("bolt://localhost:2424");
		Neo4jProperties properties = new Neo4jProperties();
		properties.setUri(customUri);
		MockEnvironment environment = new MockEnvironment().withProperty("spring.data.neo4j.uri",
				anotherCustomURI.toString());
		assertThat(determineServerUri(properties, environment)).isEqualTo(customUri);
	}

	@Test
	void authenticationShouldDefaultToNone() {
		assertThat(mapAuthToken(new Authentication())).isEqualTo(AuthTokens.none());
	}

	@Test
	void authenticationWithUsernameShouldEnableBasicAuth() {
		Authentication authentication = new Authentication();
		authentication.setUsername("Farin");
		authentication.setPassword("Urlaub");
		assertThat(mapAuthToken(authentication)).isEqualTo(AuthTokens.basic("Farin", "Urlaub"));
	}

	@Test
	void authenticationWithUsernameAndRealmShouldEnableBasicAuth() {
		Authentication authentication = new Authentication();
		authentication.setUsername("Farin");
		authentication.setPassword("Urlaub");
		authentication.setRealm("Test Realm");
		assertThat(mapAuthToken(authentication)).isEqualTo(AuthTokens.basic("Farin", "Urlaub", "Test Realm"));
	}

	@Test
	@Deprecated
	void authenticationWithUsernameUsingDeprecatedPropertiesShouldEnableBasicAuth() {
		MockEnvironment environment = new MockEnvironment().withProperty("spring.data.neo4j.username", "user")
				.withProperty("spring.data.neo4j.password", "secret");
		assertThat(mapAuthToken(new Authentication(), environment)).isEqualTo(AuthTokens.basic("user", "secret"));
	}

	@Test
	@Deprecated
	void authenticationWithUsernameShouldTakePrecedenceOverDeprecatedPropertiesAndEnableBasicAuth() {
		MockEnvironment environment = new MockEnvironment().withProperty("spring.data.neo4j.username", "user")
				.withProperty("spring.data.neo4j.password", "secret");
		Authentication authentication = new Authentication();
		authentication.setUsername("Farin");
		authentication.setPassword("Urlaub");
		assertThat(mapAuthToken(authentication, environment)).isEqualTo(AuthTokens.basic("Farin", "Urlaub"));
	}

	@Test
	void authenticationWithKerberosTicketShouldEnableKerberos() {
		Authentication authentication = new Authentication();
		authentication.setKerberosTicket("AABBCCDDEE");
		assertThat(mapAuthToken(authentication)).isEqualTo(AuthTokens.kerberos("AABBCCDDEE"));
	}

	@Test
	void authenticationWithBothUsernameAndKerberosShouldNotBeAllowed() {
		Authentication authentication = new Authentication();
		authentication.setUsername("Farin");
		authentication.setKerberosTicket("AABBCCDDEE");
		assertThatIllegalStateException().isThrownBy(() -> mapAuthToken(authentication))
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
		assertThat(certFile.createNewFile());

		Neo4jProperties properties = new Neo4jProperties();
		properties.getSecurity().setTrustStrategy(TrustStrategy.TRUST_CUSTOM_CA_SIGNED_CERTIFICATES);
		properties.getSecurity().setCertFile(certFile);
		Config.TrustStrategy trustStrategy = mapDriverConfig(properties).trustStrategy();
		assertThat(trustStrategy.strategy())
				.isEqualTo(Config.TrustStrategy.Strategy.TRUST_CUSTOM_CA_SIGNED_CERTIFICATES);
		assertThat(trustStrategy.certFile()).isEqualTo(certFile);
	}

	@Test
	void securityWithCustomCertificatesShouldFailWithoutCertificate() {
		Neo4jProperties properties = new Neo4jProperties();
		properties.getSecurity().setTrustStrategy(TrustStrategy.TRUST_CUSTOM_CA_SIGNED_CERTIFICATES);
		assertThatExceptionOfType(InvalidConfigurationPropertyValueException.class)
				.isThrownBy(() -> mapDriverConfig(properties)).withMessage(
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
		assertThat(mapDriverConfig(new Neo4jProperties()).logging()).isNotNull()
				.isInstanceOf(Neo4jSpringJclLogging.class);
	}

	private URI determineServerUri(Neo4jProperties properties, Environment environment) {
		return new Neo4jAutoConfiguration().determineServerUri(properties, environment);
	}

	private AuthToken mapAuthToken(Authentication authentication, Environment environment) {
		return new Neo4jAutoConfiguration().mapAuthToken(authentication, environment);
	}

	private AuthToken mapAuthToken(Authentication authentication) {
		return mapAuthToken(authentication, new MockEnvironment());
	}

	private Config mapDriverConfig(Neo4jProperties properties, ConfigBuilderCustomizer... customizers) {
		return new Neo4jAutoConfiguration().mapDriverConfig(properties, Arrays.asList(customizers));
	}

}
