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

package org.springframework.boot.autoconfigure.security.servlet;

import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.TestingAuthenticationProvider;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link UserDetailsServiceAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
public class UserDetailsServiceAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestSecurityConfiguration.class)
			.withConfiguration(AutoConfigurations.of(UserDetailsServiceAutoConfiguration.class));

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Test
	public void testDefaultUsernamePassword() {
		this.contextRunner.run((context) -> {
			UserDetailsService manager = context.getBean(UserDetailsService.class);
			assertThat(this.outputCapture.toString()).contains("Using generated security password:");
			assertThat(manager.loadUserByUsername("user")).isNotNull();
		});
	}

	@Test
	public void defaultUserNotCreatedIfAuthenticationManagerBeanPresent() {
		this.contextRunner.withUserConfiguration(TestAuthenticationManagerConfiguration.class).run((context) -> {
			AuthenticationManager manager = context.getBean(AuthenticationManager.class);
			assertThat(manager)
					.isEqualTo(context.getBean(TestAuthenticationManagerConfiguration.class).authenticationManager);
			assertThat(this.outputCapture.toString()).doesNotContain("Using generated security password: ");
			TestingAuthenticationToken token = new TestingAuthenticationToken("foo", "bar");
			assertThat(manager.authenticate(token)).isNotNull();
		});
	}

	@Test
	public void defaultUserNotCreatedIfUserDetailsServiceBeanPresent() {
		this.contextRunner.withUserConfiguration(TestUserDetailsServiceConfiguration.class).run((context) -> {
			UserDetailsService userDetailsService = context.getBean(UserDetailsService.class);
			assertThat(this.outputCapture.toString()).doesNotContain("Using generated security password: ");
			assertThat(userDetailsService.loadUserByUsername("foo")).isNotNull();
		});
	}

	@Test
	public void defaultUserNotCreatedIfAuthenticationProviderBeanPresent() {
		this.contextRunner.withUserConfiguration(TestAuthenticationProviderConfiguration.class).run((context) -> {
			AuthenticationProvider provider = context.getBean(AuthenticationProvider.class);
			assertThat(this.outputCapture.toString()).doesNotContain("Using generated security password: ");
			TestingAuthenticationToken token = new TestingAuthenticationToken("foo", "bar");
			assertThat(provider.authenticate(token)).isNotNull();
		});
	}

	@Test
	public void userDetailsServiceWhenPasswordEncoderAbsentAndDefaultPassword() {
		this.contextRunner.withUserConfiguration(TestSecurityConfiguration.class).run(((context) -> {
			InMemoryUserDetailsManager userDetailsService = context.getBean(InMemoryUserDetailsManager.class);
			String password = userDetailsService.loadUserByUsername("user").getPassword();
			assertThat(password).startsWith("{noop}");
		}));
	}

	@Test
	public void userDetailsServiceWhenPasswordEncoderAbsentAndRawPassword() {
		testPasswordEncoding(TestSecurityConfiguration.class, "secret", "{noop}secret");
	}

	@Test
	public void userDetailsServiceWhenPasswordEncoderAbsentAndEncodedPassword() {
		String password = "{bcrypt}$2a$10$sCBi9fy9814vUPf2ZRbtp.fR5/VgRk2iBFZ.ypu5IyZ28bZgxrVDa";
		testPasswordEncoding(TestSecurityConfiguration.class, password, password);
	}

	@Test
	public void userDetailsServiceWhenPasswordEncoderBeanPresent() {
		testPasswordEncoding(TestConfigWithPasswordEncoder.class, "secret", "secret");
	}

	@Test
	public void userDetailsServiceWhenClientRegistrationRepositoryBeanPresent() {
		this.contextRunner.withUserConfiguration(TestConfigWithClientRegistrationRepository.class)
				.run(((context) -> assertThat(context).doesNotHaveBean(InMemoryUserDetailsManager.class)));
	}

	@Test
	public void generatedPasswordShouldNotBePrintedIfAuthenticationManagerBuilderIsUsed() {
		this.contextRunner.withUserConfiguration(TestConfigWithAuthenticationManagerBuilder.class)
				.run(((context) -> assertThat(this.outputCapture.toString())
						.doesNotContain("Using generated security password: ")));
	}

	private void testPasswordEncoding(Class<?> configClass, String providedPassword, String expectedPassword) {
		this.contextRunner.withUserConfiguration(configClass)
				.withPropertyValues("spring.security.user.password=" + providedPassword).run(((context) -> {
					InMemoryUserDetailsManager userDetailsService = context.getBean(InMemoryUserDetailsManager.class);
					String password = userDetailsService.loadUserByUsername("user").getPassword();
					assertThat(password).isEqualTo(expectedPassword);
				}));
	}

	@Configuration
	protected static class TestAuthenticationManagerConfiguration {

		private AuthenticationManager authenticationManager;

		@Bean
		public AuthenticationManager myAuthenticationManager() {
			AuthenticationProvider authenticationProvider = new TestingAuthenticationProvider();
			this.authenticationManager = new ProviderManager(Collections.singletonList(authenticationProvider));
			return this.authenticationManager;
		}

	}

	@Configuration
	protected static class TestUserDetailsServiceConfiguration {

		@Bean
		public InMemoryUserDetailsManager myUserDetailsManager() {
			return new InMemoryUserDetailsManager(User.withUsername("foo").password("bar").roles("USER").build());
		}

	}

	@Configuration
	protected static class TestAuthenticationProviderConfiguration {

		@Bean
		public AuthenticationProvider myAuthenticationProvider() {
			return new TestingAuthenticationProvider();
		}

	}

	@Configuration
	@EnableWebSecurity
	@EnableConfigurationProperties(SecurityProperties.class)
	protected static class TestSecurityConfiguration {

	}

	@Configuration
	@Import(TestSecurityConfiguration.class)
	protected static class TestConfigWithPasswordEncoder {

		@Bean
		public PasswordEncoder passwordEncoder() {
			return mock(PasswordEncoder.class);
		}

	}

	@Configuration
	@Import(TestSecurityConfiguration.class)
	protected static class TestConfigWithClientRegistrationRepository {

		@Bean
		public ClientRegistrationRepository clientRegistrationRepository() {
			return mock(ClientRegistrationRepository.class);
		}

	}

	@Configuration
	@Import(TestSecurityConfiguration.class)
	protected static class TestConfigWithAuthenticationManagerBuilder {

		@Bean
		public WebSecurityConfigurerAdapter webSecurityConfigurerAdapter() {
			return new WebSecurityConfigurerAdapter() {
				@Override
				protected void configure(AuthenticationManagerBuilder auth) throws Exception {
					auth.inMemoryAuthentication().withUser("hero").password("{noop}hero").roles("HERO", "USER").and()
							.withUser("user").password("{noop}user").roles("USER");
				}
			};
		}

	}

}
