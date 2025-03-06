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

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OAuth2AuthorizationServerJwtAutoConfiguration}.
 *
 * @author Steve Riesenberg
 */
class OAuth2AuthorizationServerJwtAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(OAuth2AuthorizationServerJwtAutoConfiguration.class));

	@Test
	void autoConfigurationConditionalOnClassOauth2Authorization() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(OAuth2Authorization.class))
			.run((context) -> assertThat(context).doesNotHaveBean(OAuth2AuthorizationServerJwtAutoConfiguration.class));
	}

	@Test
	void jwtDecoderConditionalOnClassJwtDecoder() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(JwtDecoder.class))
			.run((context) -> assertThat(context).doesNotHaveBean("jwtDecoder"));
	}

	@Test
	void jwtConfigurationConfiguresJwtDecoderWithGeneratedKey() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasBean("jwtDecoder");
			assertThat(context.getBean("jwtDecoder")).isInstanceOf(NimbusJwtDecoder.class);
			assertThat(context).hasBean("jwkSource");
			assertThat(context.getBean("jwkSource")).isInstanceOf(ImmutableJWKSet.class);
		});
	}

	@Test
	void jwtDecoderBacksOffWhenBeanPresent() {
		this.contextRunner.withUserConfiguration(TestJwtDecoderConfiguration.class).run((context) -> {
			assertThat(context).hasBean("jwtDecoder");
			assertThat(context.getBean("jwtDecoder")).isNotInstanceOf(NimbusJwtDecoder.class);
			assertThat(context).hasBean("jwkSource");
			assertThat(context.getBean("jwkSource")).isInstanceOf(ImmutableJWKSet.class);
		});
	}

	@Test
	void jwkSourceBacksOffWhenBeanPresent() {
		this.contextRunner.withUserConfiguration(TestJwkSourceConfiguration.class).run((context) -> {
			assertThat(context).hasBean("jwtDecoder");
			assertThat(context.getBean("jwtDecoder")).isInstanceOf(NimbusJwtDecoder.class);
			assertThat(context).hasBean("jwkSource");
			assertThat(context.getBean("jwkSource")).isNotInstanceOf(ImmutableJWKSet.class);
		});
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
