/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.autoconfigure.security.oauth2.resource.servlet;

import java.util.List;

import javax.servlet.Filter;

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoderJwkSupport;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link OAuth2ResourceServerAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
public class OAuth2ResourceServerAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(OAuth2ResourceServerAutoConfiguration.class))
			.withUserConfiguration(TestConfig.class);

	@Test
	public void autoConfigurationShouldConfigureResourceServer() {
		this.contextRunner.withPropertyValues(
				"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://jwk-set-uri.com")
				.run((context) -> {
					assertThat(context.getBean(JwtDecoder.class))
							.isInstanceOf(NimbusJwtDecoderJwkSupport.class);
					assertThat(getBearerTokenFilter(context)).isNotNull();
				});
	}

	@Test
	public void autoConfigurationWhenJwkSetUriNullShouldNotFail() {
		this.contextRunner
				.run((context) -> assertThat(getBearerTokenFilter(context)).isNull());
	}

	@Test
	public void jwtDecoderBeanIsConditionalOnMissingBean() {
		this.contextRunner.withPropertyValues(
				"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://jwk-set-uri.com")
				.withUserConfiguration(JwtDecoderConfig.class)
				.run((context) -> assertThat(getBearerTokenFilter(context)).isNotNull());
	}

	@Test
	public void autoConfigurationShouldBeConditionalOnJwtAuthenticationTokenClass() {
		this.contextRunner.withPropertyValues(
				"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://jwk-set-uri.com")
				.withUserConfiguration(JwtDecoderConfig.class)
				.withClassLoader(new FilteredClassLoader(JwtAuthenticationToken.class))
				.run((context) -> assertThat(getBearerTokenFilter(context)).isNull());
	}

	@SuppressWarnings("unchecked")
	private Filter getBearerTokenFilter(AssertableWebApplicationContext context) {
		FilterChainProxy filterChain = (FilterChainProxy) context
				.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN);
		List<SecurityFilterChain> filterChains = filterChain.getFilterChains();
		List<Filter> filters = (List<Filter>) ReflectionTestUtils
				.getField(filterChains.get(0), "filters");
		return filters.stream()
				.filter((f) -> f instanceof BearerTokenAuthenticationFilter).findFirst()
				.orElse(null);
	}

	@Configuration
	@EnableWebSecurity
	static class TestConfig {

	}

	@Configuration
	@EnableWebSecurity
	static class JwtDecoderConfig {

		@Bean
		public JwtDecoder decoder() {
			return mock(JwtDecoder.class);
		}

	}

}
