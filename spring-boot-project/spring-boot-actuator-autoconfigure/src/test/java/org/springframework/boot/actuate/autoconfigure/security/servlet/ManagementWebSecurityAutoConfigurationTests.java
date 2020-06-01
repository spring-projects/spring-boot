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

package org.springframework.boot.actuate.autoconfigure.security.servlet;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.endpoint.EndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.env.EnvironmentEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.info.InfoEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.saml2.Saml2RelyingPartyAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ManagementWebSecurityAutoConfiguration}.
 *
 * @author Madhura Bhave
 */
class ManagementWebSecurityAutoConfigurationTests {

	private WebApplicationContextRunner contextRunner = new WebApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(HealthContributorAutoConfiguration.class, HealthEndpointAutoConfiguration.class,
					InfoEndpointAutoConfiguration.class, EnvironmentEndpointAutoConfiguration.class,
					EndpointAutoConfiguration.class, WebEndpointAutoConfiguration.class,
					SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class));

	@Test
	void permitAllForHealth() {
		this.contextRunner.run((context) -> {
			HttpStatus status = getResponseStatus(context, "/actuator/health");
			assertThat(status).isEqualTo(HttpStatus.OK);
		});
	}

	@Test
	void permitAllForInfo() {
		this.contextRunner.run((context) -> {
			HttpStatus status = getResponseStatus(context, "/actuator/info");
			assertThat(status).isEqualTo(HttpStatus.OK);
		});
	}

	@Test
	void securesEverythingElse() {
		this.contextRunner.run((context) -> {
			HttpStatus status = getResponseStatus(context, "/actuator");
			assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);
			status = getResponseStatus(context, "/foo");
			assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);
		});
	}

	@Test
	void usesMatchersBasedOffConfiguredActuatorBasePath() {
		this.contextRunner.withPropertyValues("management.endpoints.web.base-path=/").run((context) -> {
			HttpStatus status = getResponseStatus(context, "/health");
			assertThat(status).isEqualTo(HttpStatus.OK);
		});
	}

	@Test
	void backOffIfCustomSecurityIsAdded() {
		this.contextRunner.withUserConfiguration(CustomSecurityConfiguration.class).run((context) -> {
			HttpStatus status = getResponseStatus(context, "/actuator/health");
			assertThat(status).isEqualTo(HttpStatus.UNAUTHORIZED);
			status = getResponseStatus(context, "/foo");
			assertThat(status).isEqualTo(HttpStatus.OK);
		});
	}

	@Test
	void backOffIfOAuth2ResourceServerAutoConfigurationPresent() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(OAuth2ResourceServerAutoConfiguration.class))
				.withPropertyValues("spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://authserver")
				.run((context) -> assertThat(context).doesNotHaveBean(ManagementWebSecurityConfigurerAdapter.class));
	}

	@Test
	void backOffIfSaml2RelyingPartyAutoConfigurationPresent() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(Saml2RelyingPartyAutoConfiguration.class))
				.withPropertyValues(
						"spring.security.saml2.relyingparty.registration.simplesamlphp.identity-provider.single-sign-on.url=https://simplesaml-for-spring-saml/SSOService.php",
						"spring.security.saml2.relyingparty.registration.simplesamlphp.identity-provider.single-sign-on.sign-request=false",
						"spring.security.saml2.relyingparty.registration.simplesamlphp.identityprovider.entity-id=https://simplesaml-for-spring-saml.cfapps.io/saml2/idp/metadata.php",
						"spring.security.saml2.relyingparty.registration.simplesamlphp.identityprovider.verification.credentials[0].certificate-location=classpath:saml/certificate-location")
				.run((context) -> assertThat(context).doesNotHaveBean(ManagementWebSecurityConfigurerAdapter.class));
	}

	private HttpStatus getResponseStatus(AssertableWebApplicationContext context, String path)
			throws IOException, javax.servlet.ServletException {
		FilterChainProxy filterChainProxy = context.getBean(FilterChainProxy.class);
		MockServletContext servletContext = new MockServletContext();
		MockHttpServletResponse response = new MockHttpServletResponse();
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, context);
		MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
		request.setServletPath(path);
		request.setMethod("GET");
		filterChainProxy.doFilter(request, response, new MockFilterChain());
		return HttpStatus.valueOf(response.getStatus());
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomSecurityConfiguration extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.authorizeRequests((requests) -> {
				requests.antMatchers("/foo").permitAll();
				requests.anyRequest().authenticated();
			});
			http.formLogin(Customizer.withDefaults());
			http.httpBasic();
		}

	}

}
