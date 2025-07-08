/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.security.autoconfigure.servlet;

import java.util.Collections;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport.ConditionAndOutcomes;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.SecurityProperties;
import org.springframework.boot.security.autoconfigure.servlet.UserDetailsServiceAutoConfiguration.MissingAlternativeOrUserPropertiesConfigured;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.AbstractApplicationContextRunner;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.TestingAuthenticationProvider;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UserDetailsServiceAutoConfiguration}.
 *
 * @author Madhura Bhave
 * @author HaiTao Zhang
 * @author Lasse Wulff
 * @author Moritz Halbritter
 */
@ExtendWith(OutputCaptureExtension.class)
class UserDetailsServiceAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withUserConfiguration(TestSecurityConfiguration.class)
		.withConfiguration(AutoConfigurations.of(UserDetailsServiceAutoConfiguration.class));

	@Test
	void shouldSupplyUserDetailsServiceInServletApp() {
		this.contextRunner.with(AlternativeFormOfAuthentication.nonPresent())
			.run((context) -> assertThat(context).hasSingleBean(UserDetailsService.class));
	}

	@Test
	void shouldNotSupplyUserDetailsServiceInReactiveApp() {
		new ReactiveWebApplicationContextRunner().withUserConfiguration(TestSecurityConfiguration.class)
			.withConfiguration(AutoConfigurations.of(UserDetailsServiceAutoConfiguration.class))
			.with(AlternativeFormOfAuthentication.nonPresent())
			.run((context) -> assertThat(context).doesNotHaveBean(UserDetailsService.class));
	}

	@Test
	void shouldNotSupplyUserDetailsServiceInNonWebApp() {
		new ApplicationContextRunner().withUserConfiguration(TestSecurityConfiguration.class)
			.withConfiguration(AutoConfigurations.of(UserDetailsServiceAutoConfiguration.class))
			.with(AlternativeFormOfAuthentication.nonPresent())
			.run((context) -> assertThat(context).doesNotHaveBean(UserDetailsService.class));
	}

	@Test
	void testDefaultUsernamePassword(CapturedOutput output) {
		this.contextRunner.with(AlternativeFormOfAuthentication.nonPresent()).run((context) -> {
			assertThat(outcomeOfMissingAlternativeCondition(context).isMatch()).isTrue();
			UserDetailsService manager = context.getBean(UserDetailsService.class);
			assertThat(output).contains("Using generated security password:");
			assertThat(manager.loadUserByUsername("user")).isNotNull();
		});
	}

	@Test
	void defaultUserNotCreatedIfAuthenticationManagerBeanPresent(CapturedOutput output) {
		this.contextRunner.with(AlternativeFormOfAuthentication.nonPresent())
			.withUserConfiguration(TestAuthenticationManagerConfiguration.class)
			.run((context) -> {
				assertThat(outcomeOfMissingAlternativeCondition(context).isMatch()).isTrue();
				AuthenticationManager manager = context.getBean(AuthenticationManager.class);
				assertThat(manager)
					.isEqualTo(context.getBean(TestAuthenticationManagerConfiguration.class).authenticationManager);
				assertThat(output).doesNotContain("Using generated security password: ");
				TestingAuthenticationToken token = new TestingAuthenticationToken("foo", "bar");
				assertThat(manager.authenticate(token)).isNotNull();
			});
	}

	@Test
	void defaultUserNotCreatedIfAuthenticationManagerResolverBeanPresent(CapturedOutput output) {
		this.contextRunner.with(AlternativeFormOfAuthentication.nonPresent())
			.withUserConfiguration(TestAuthenticationManagerResolverConfiguration.class)
			.run((context) -> {
				assertThat(outcomeOfMissingAlternativeCondition(context).isMatch()).isTrue();
				assertThat(output).doesNotContain("Using generated security password: ");
			});
	}

	@Test
	void defaultUserNotCreatedIfUserDetailsServiceBeanPresent(CapturedOutput output) {
		this.contextRunner.with(AlternativeFormOfAuthentication.nonPresent())
			.withUserConfiguration(TestUserDetailsServiceConfiguration.class)
			.run((context) -> {
				assertThat(outcomeOfMissingAlternativeCondition(context).isMatch()).isTrue();
				UserDetailsService userDetailsService = context.getBean(UserDetailsService.class);
				assertThat(output).doesNotContain("Using generated security password: ");
				assertThat(userDetailsService.loadUserByUsername("foo")).isNotNull();
			});
	}

	@Test
	void defaultUserNotCreatedIfAuthenticationProviderBeanPresent(CapturedOutput output) {
		this.contextRunner.with(AlternativeFormOfAuthentication.nonPresent())
			.withUserConfiguration(TestAuthenticationProviderConfiguration.class)
			.run((context) -> {
				assertThat(outcomeOfMissingAlternativeCondition(context).isMatch()).isTrue();
				AuthenticationProvider provider = context.getBean(AuthenticationProvider.class);
				assertThat(output).doesNotContain("Using generated security password: ");
				TestingAuthenticationToken token = new TestingAuthenticationToken("foo", "bar");
				assertThat(provider.authenticate(token)).isNotNull();
			});
	}

	@Test
	void defaultUserNotCreatedIfJwtDecoderBeanPresent() {
		this.contextRunner.with(AlternativeFormOfAuthentication.nonPresent())
			.withUserConfiguration(TestConfigWithJwtDecoder.class)
			.run((context) -> {
				assertThat(outcomeOfMissingAlternativeCondition(context).isMatch()).isTrue();
				assertThat(context).hasSingleBean(JwtDecoder.class);
				assertThat(context).doesNotHaveBean(UserDetailsService.class);
			});
	}

	@Test
	void userDetailsServiceWhenPasswordEncoderAbsentAndDefaultPassword() {
		this.contextRunner.with(AlternativeFormOfAuthentication.nonPresent())
			.withUserConfiguration(TestSecurityConfiguration.class)
			.run(((context) -> {
				InMemoryUserDetailsManager userDetailsService = context.getBean(InMemoryUserDetailsManager.class);
				String password = userDetailsService.loadUserByUsername("user").getPassword();
				assertThat(password).startsWith("{noop}");
			}));
	}

	@Test
	void userDetailsServiceWhenPasswordEncoderAbsentAndRawPassword() {
		testPasswordEncoding(TestSecurityConfiguration.class, "secret", "{noop}secret");
	}

	@Test
	void userDetailsServiceWhenPasswordEncoderAbsentAndEncodedPassword() {
		String password = "{bcrypt}$2a$10$sCBi9fy9814vUPf2ZRbtp.fR5/VgRk2iBFZ.ypu5IyZ28bZgxrVDa";
		testPasswordEncoding(TestSecurityConfiguration.class, password, password);
	}

	@Test
	void userDetailsServiceWhenPasswordEncoderBeanPresent() {
		testPasswordEncoding(TestConfigWithPasswordEncoder.class, "secret", "secret");
	}

	@ParameterizedTest
	@EnumSource
	void whenClassOfAlternativeIsPresentUserDetailsServiceBacksOff(AlternativeFormOfAuthentication alternative) {
		this.contextRunner.with(alternative.present())
			.run((context) -> assertThat(context).doesNotHaveBean(InMemoryUserDetailsManager.class));
	}

	@ParameterizedTest
	@EnumSource
	void whenAlternativeIsPresentAndUsernameIsConfiguredThenUserDetailsServiceIsAutoConfigured(
			AlternativeFormOfAuthentication alternative) {
		this.contextRunner.with(alternative.present())
			.withPropertyValues("spring.security.user.name=alice")
			.run(((context) -> assertThat(context).hasSingleBean(InMemoryUserDetailsManager.class)));
	}

	@ParameterizedTest
	@EnumSource
	void whenAlternativeIsPresentAndPasswordIsConfiguredThenUserDetailsServiceIsAutoConfigured(
			AlternativeFormOfAuthentication alternative) {
		this.contextRunner.with(alternative.present())
			.withPropertyValues("spring.security.user.password=secret")
			.run(((context) -> assertThat(context).hasSingleBean(InMemoryUserDetailsManager.class)));
	}

	private void testPasswordEncoding(Class<?> configClass, String providedPassword, String expectedPassword) {
		this.contextRunner.with(AlternativeFormOfAuthentication.nonPresent())
			.withUserConfiguration(configClass)
			.withPropertyValues("spring.security.user.password=" + providedPassword)
			.run(((context) -> {
				InMemoryUserDetailsManager userDetailsService = context.getBean(InMemoryUserDetailsManager.class);
				String password = userDetailsService.loadUserByUsername("user").getPassword();
				assertThat(password).isEqualTo(expectedPassword);
			}));
	}

	private ConditionOutcome outcomeOfMissingAlternativeCondition(ConfigurableApplicationContext context) {
		ConditionAndOutcomes conditionAndOutcomes = ConditionEvaluationReport.get(context.getBeanFactory())
			.getConditionAndOutcomesBySource()
			.get(UserDetailsServiceAutoConfiguration.class.getName());
		for (ConditionAndOutcome conditionAndOutcome : conditionAndOutcomes) {
			if (conditionAndOutcome.getCondition() instanceof MissingAlternativeOrUserPropertiesConfigured) {
				return conditionAndOutcome.getOutcome();
			}
		}
		return null;
	}

	@Configuration(proxyBeanMethods = false)
	static class TestAuthenticationManagerConfiguration {

		private AuthenticationManager authenticationManager;

		@Bean
		AuthenticationManager myAuthenticationManager() {
			AuthenticationProvider authenticationProvider = new TestingAuthenticationProvider();
			this.authenticationManager = new ProviderManager(Collections.singletonList(authenticationProvider));
			return this.authenticationManager;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestUserDetailsServiceConfiguration {

		@Bean
		InMemoryUserDetailsManager myUserDetailsManager() {
			return new InMemoryUserDetailsManager(User.withUsername("foo").password("bar").roles("USER").build());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestAuthenticationProviderConfiguration {

		@Bean
		AuthenticationProvider myAuthenticationProvider() {
			return new TestingAuthenticationProvider();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebSecurity
	@EnableConfigurationProperties(SecurityProperties.class)
	static class TestSecurityConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@Import(TestSecurityConfiguration.class)
	static class TestConfigWithPasswordEncoder {

		@Bean
		PasswordEncoder passwordEncoder() {
			return mock(PasswordEncoder.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(TestSecurityConfiguration.class)
	static class TestConfigWithClientRegistrationRepository {

		@Bean
		ClientRegistrationRepository clientRegistrationRepository() {
			return mock(ClientRegistrationRepository.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(TestSecurityConfiguration.class)
	static class TestConfigWithJwtDecoder {

		@Bean
		JwtDecoder jwtDecoder() {
			return mock(JwtDecoder.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@Import(TestSecurityConfiguration.class)
	static class TestConfigWithIntrospectionClient {

		@Bean
		OpaqueTokenIntrospector introspectionClient() {
			return mock(OpaqueTokenIntrospector.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TestAuthenticationManagerResolverConfiguration {

		@Bean
		AuthenticationManagerResolver<?> authenticationManagerResolver() {
			return mock(AuthenticationManagerResolver.class);
		}

	}

	private enum AlternativeFormOfAuthentication {

		CLIENT_REGISTRATION_REPOSITORY(ClientRegistrationRepository.class),

		OPAQUE_TOKEN_INTROSPECTOR(OpaqueTokenIntrospector.class),

		RELYING_PARTY_REGISTRATION_REPOSITORY(RelyingPartyRegistrationRepository.class);

		private final Class<?> type;

		AlternativeFormOfAuthentication(Class<?> type) {
			this.type = type;
		}

		private Class<?> getType() {
			return this.type;
		}

		@SuppressWarnings("unchecked")
		private <T extends AbstractApplicationContextRunner<?, ?, ?>> Function<T, T> present() {
			return (contextRunner) -> (T) contextRunner
				.withClassLoader(new FilteredClassLoader(Stream.of(AlternativeFormOfAuthentication.values())
					.filter(Predicate.not(this::equals))
					.map(AlternativeFormOfAuthentication::getType)
					.toArray(Class[]::new)));
		}

		@SuppressWarnings("unchecked")
		private static <T extends AbstractApplicationContextRunner<?, ?, ?>> Function<T, T> nonPresent() {
			return (contextRunner) -> (T) contextRunner
				.withClassLoader(new FilteredClassLoader(Stream.of(AlternativeFormOfAuthentication.values())
					.map(AlternativeFormOfAuthentication::getType)
					.toArray(Class[]::new)));
		}

	}

}
