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

package org.springframework.boot.autoconfigure.security.oauth2.server.servlet;

import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OAuth2AuthorizationServerJwtConfiguration}.
 *
 * @author Steve Riesenberg
 */
public class OAuth2AuthorizationServerJwtConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void jwtConfigurationConfiguresJwtDecoderWithGeneratedKey() {
		// @formatter:off
		this.contextRunner.withUserConfiguration(TestJwtConfiguration.class)
				.run((context) -> {
					assertThat(context).hasBean("jwtDecoder");
					assertThat(context).hasBean("jwkSource");

					assertThat(context.getBean("jwtDecoder")).isInstanceOf(NimbusJwtDecoder.class);
					assertThat(context.getBean("jwkSource")).isInstanceOf(ImmutableJWKSet.class);
				});
		// @formatter:on
	}

	@Test
	void jwtDecoderBacksOffWhenBeanPresent() {
		// @formatter:off
		this.contextRunner.withUserConfiguration(TestJwtDecoderConfiguration.class, TestJwtConfiguration.class)
				.run((context) -> {
					assertThat(context).hasBean("jwtDecoder");
					assertThat(context).hasBean("jwkSource");

					assertThat(context.getBean("jwtDecoder")).isNotInstanceOf(NimbusJwtDecoder.class);
					assertThat(context.getBean("jwkSource")).isInstanceOf(ImmutableJWKSet.class);
				});
		// @formatter:on
	}

	@Test
	void jwkSourceBacksOffWhenBeanPresent() {
		// @formatter:off
		this.contextRunner.withUserConfiguration(TestJwkSourceConfiguration.class, TestJwtConfiguration.class)
				.run((context) -> {
					assertThat(context).hasBean("jwtDecoder");
					assertThat(context).hasBean("jwkSource");

					assertThat(context.getBean("jwtDecoder")).isInstanceOf(NimbusJwtDecoder.class);
					assertThat(context.getBean("jwkSource")).isNotInstanceOf(ImmutableJWKSet.class);
				});
		// @formatter:on
	}

	@Configuration
	@Import(OAuth2AuthorizationServerJwtConfiguration.class)
	static class TestJwtConfiguration {

	}

	@Configuration
	static class TestJwtDecoderConfiguration {

		@Bean
		JwtDecoder jwtDecoder() {
			return (token) -> null;
		}

	}

	@Configuration
	static class TestJwkSourceConfiguration {

		@Bean
		JWKSource<SecurityContext> jwkSource() {
			return (jwkSelector, context) -> null;
		}

	}

}
