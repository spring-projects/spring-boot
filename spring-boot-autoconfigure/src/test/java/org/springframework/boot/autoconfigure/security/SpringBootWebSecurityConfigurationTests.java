/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.servlet.Filter;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ErrorMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for {@link SpringBootWebSecurityConfiguration}.
 *
 * @author Dave Syer
 * @author Rob Winch
 * @author Andy Wilkinson
 */
public class SpringBootWebSecurityConfigurationTests {

	private ConfigurableApplicationContext context;

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void testWebConfigurationOverrideGlobalAuthentication() throws Exception {
		this.context = SpringApplication.run(TestWebConfiguration.class,
				"--server.port=0");
		assertThat(this.context.getBean(AuthenticationManagerBuilder.class)).isNotNull();
		assertThat(this.context.getBean(AuthenticationManager.class)
				.authenticate(new UsernamePasswordAuthenticationToken("dave", "secret")))
						.isNotNull();
	}

	@Test
	public void testWebConfigurationFilterChainUnauthenticated() throws Exception {
		this.context = SpringApplication.run(VanillaWebConfiguration.class,
				"--server.port=0");
		MockMvc mockMvc = MockMvcBuilders
				.webAppContextSetup((WebApplicationContext) this.context)
				.addFilters(
						this.context.getBean("springSecurityFilterChain", Filter.class))
				.build();
		mockMvc.perform(MockMvcRequestBuilders.get("/"))
				.andExpect(MockMvcResultMatchers.status().isUnauthorized())
				.andExpect(MockMvcResultMatchers.header().string("www-authenticate",
						Matchers.containsString("realm=\"Spring\"")));
	}

	@Test
	public void testWebConfigurationFilterChainUnauthenticatedWithAuthorizeModeNone()
			throws Exception {
		this.context = SpringApplication.run(VanillaWebConfiguration.class,
				"--server.port=0", "--security.basic.authorize-mode=none");
		MockMvc mockMvc = MockMvcBuilders
				.webAppContextSetup((WebApplicationContext) this.context)
				.addFilters(
						this.context.getBean("springSecurityFilterChain", Filter.class))
				.build();
		mockMvc.perform(MockMvcRequestBuilders.get("/"))
				.andExpect(MockMvcResultMatchers.status().isNotFound());
	}

	@Test
	public void testWebConfigurationFilterChainUnauthenticatedWithAuthorizeModeAuthenticated()
			throws Exception {
		this.context = SpringApplication.run(VanillaWebConfiguration.class,
				"--server.port=0", "--security.basic.authorize-mode=authenticated");
		MockMvc mockMvc = MockMvcBuilders
				.webAppContextSetup((WebApplicationContext) this.context)
				.addFilters(
						this.context.getBean("springSecurityFilterChain", Filter.class))
				.build();
		mockMvc.perform(MockMvcRequestBuilders.get("/"))
				.andExpect(MockMvcResultMatchers.status().isUnauthorized())
				.andExpect(MockMvcResultMatchers.header().string("www-authenticate",
						Matchers.containsString("realm=\"Spring\"")));
	}

	@Test
	public void testWebConfigurationFilterChainBadCredentials() throws Exception {
		this.context = SpringApplication.run(VanillaWebConfiguration.class,
				"--server.port=0");
		MockMvc mockMvc = MockMvcBuilders
				.webAppContextSetup((WebApplicationContext) this.context)
				.addFilters(
						this.context.getBean("springSecurityFilterChain", Filter.class))
				.build();
		mockMvc.perform(
				MockMvcRequestBuilders.get("/").header("authorization", "Basic xxx"))
				.andExpect(MockMvcResultMatchers.status().isUnauthorized())
				.andExpect(MockMvcResultMatchers.header().string("www-authenticate",
						Matchers.containsString("realm=\"Spring\"")));
	}

	@Test
	public void testWebConfigurationInjectGlobalAuthentication() throws Exception {
		this.context = SpringApplication.run(TestInjectWebConfiguration.class,
				"--server.port=0");
		assertThat(this.context.getBean(AuthenticationManagerBuilder.class)).isNotNull();
		assertThat(this.context.getBean(AuthenticationManager.class)
				.authenticate(new UsernamePasswordAuthenticationToken("dave", "secret")))
						.isNotNull();
	}

	// gh-3447
	@Test
	public void testHiddenHttpMethodFilterOrderedFirst() throws Exception {
		this.context = SpringApplication.run(DenyPostRequestConfig.class,
				"--server.port=0");
		int port = Integer
				.parseInt(this.context.getEnvironment().getProperty("local.server.port"));
		TestRestTemplate rest = new TestRestTemplate();

		// not overriding causes forbidden
		MultiValueMap<String, String> form = new LinkedMultiValueMap<String, String>();

		ResponseEntity<Object> result = rest
				.postForEntity("http://localhost:" + port + "/", form, Object.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

		// override method with DELETE
		form = new LinkedMultiValueMap<String, String>();
		form.add("_method", "DELETE");

		result = rest.postForEntity("http://localhost:" + port + "/", form, Object.class);
		assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void defaultHeaderConfiguration() throws Exception {
		this.context = SpringApplication.run(VanillaWebConfiguration.class,
				"--server.port=0");
		MockMvc mockMvc = MockMvcBuilders
				.webAppContextSetup((WebApplicationContext) this.context)
				.addFilters((FilterChainProxy) this.context
						.getBean("springSecurityFilterChain", Filter.class))
				.build();
		mockMvc.perform(MockMvcRequestBuilders.get("/"))
				.andExpect(MockMvcResultMatchers.header().string("X-Content-Type-Options",
						is(notNullValue())))
				.andExpect(MockMvcResultMatchers.header().string("X-XSS-Protection",
						is(notNullValue())))
				.andExpect(MockMvcResultMatchers.header().string("Cache-Control",
						is(notNullValue())))
				.andExpect(MockMvcResultMatchers.header().string("X-Frame-Options",
						is(notNullValue())))
				.andExpect(MockMvcResultMatchers.header()
						.doesNotExist("Content-Security-Policy"));
	}

	@Test
	public void securityHeadersCanBeDisabled() throws Exception {
		this.context = SpringApplication.run(VanillaWebConfiguration.class,
				"--server.port=0", "--security.headers.content-type=false",
				"--security.headers.xss=false", "--security.headers.cache=false",
				"--security.headers.frame=false");

		MockMvc mockMvc = MockMvcBuilders
				.webAppContextSetup((WebApplicationContext) this.context)
				.addFilters(
						this.context.getBean("springSecurityFilterChain", Filter.class))
				.build();
		mockMvc.perform(MockMvcRequestBuilders.get("/"))
				.andExpect(MockMvcResultMatchers.status().isUnauthorized())
				.andExpect(MockMvcResultMatchers.header()
						.doesNotExist("X-Content-Type-Options"))
				.andExpect(
						MockMvcResultMatchers.header().doesNotExist("X-XSS-Protection"))
				.andExpect(MockMvcResultMatchers.header().doesNotExist("Cache-Control"))
				.andExpect(
						MockMvcResultMatchers.header().doesNotExist("X-Frame-Options"));
	}

	@Test
	public void contentSecurityPolicyConfiguration() throws Exception {
		this.context = SpringApplication.run(VanillaWebConfiguration.class,
				"--security.headers.content-security-policy=default-src 'self';",
				"--server.port=0");
		MockMvc mockMvc = MockMvcBuilders
				.webAppContextSetup((WebApplicationContext) this.context)
				.addFilters((FilterChainProxy) this.context
						.getBean("springSecurityFilterChain", Filter.class))
				.build();
		mockMvc.perform(MockMvcRequestBuilders.get("/"))
				.andExpect(MockMvcResultMatchers.header()
						.string("Content-Security-Policy", is("default-src 'self';")))
				.andExpect(MockMvcResultMatchers.header()
						.doesNotExist("Content-Security-Policy-Report-Only"));
	}

	@Test
	public void contentSecurityPolicyReportOnlyConfiguration() throws Exception {
		this.context = SpringApplication.run(VanillaWebConfiguration.class,
				"--security.headers.content-security-policy=default-src 'self';",
				"--security.headers.content-security-policy-mode=report-only",
				"--server.port=0");
		MockMvc mockMvc = MockMvcBuilders
				.webAppContextSetup((WebApplicationContext) this.context)
				.addFilters((FilterChainProxy) this.context
						.getBean("springSecurityFilterChain", Filter.class))
				.build();
		mockMvc.perform(MockMvcRequestBuilders.get("/"))
				.andExpect(MockMvcResultMatchers.header().string(
						"Content-Security-Policy-Report-Only", is("default-src 'self';")))
				.andExpect(MockMvcResultMatchers.header()
						.doesNotExist("Content-Security-Policy"));
	}

	@Configuration
	@Import(TestWebConfiguration.class)
	@Order(Ordered.LOWEST_PRECEDENCE)
	protected static class TestInjectWebConfiguration
			extends WebSecurityConfigurerAdapter {

		private final AuthenticationManagerBuilder auth;

		// It's a bad idea to inject an AuthenticationManager into a
		// WebSecurityConfigurerAdapter because it can cascade early instantiation,
		// unless you explicitly want the Boot default AuthenticationManager. It's
		// better to inject the builder, if you want the global AuthenticationManager. It
		// might even be necessary to wrap the builder in a lazy AuthenticationManager
		// (that calls getOrBuild() only when the AuthenticationManager is actually
		// called).
		protected TestInjectWebConfiguration(AuthenticationManagerBuilder auth) {
			this.auth = auth;
		}

		@Override
		public void init(WebSecurity web) throws Exception {
			this.auth.getOrBuild();
		}

	}

	@MinimalWebConfiguration
	@Import(SecurityAutoConfiguration.class)
	protected static class VanillaWebConfiguration {

	}

	@MinimalWebConfiguration
	@Import(SecurityAutoConfiguration.class)
	@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
	protected static class TestWebConfiguration extends WebSecurityConfigurerAdapter {

		@Autowired
		public void init(AuthenticationManagerBuilder auth) throws Exception {
			auth.inMemoryAuthentication().withUser("dave").password("secret")
					.roles("USER");
		}

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.authorizeRequests().anyRequest().denyAll();
		}

	}

	@Configuration
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Import({ EmbeddedServletContainerAutoConfiguration.class,
			ServerPropertiesAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class, ErrorMvcAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	protected @interface MinimalWebConfiguration {

	}

	@MinimalWebConfiguration
	@Import(SecurityAutoConfiguration.class)
	protected static class DenyPostRequestConfig extends WebSecurityConfigurerAdapter {

		@Override
		protected void configure(HttpSecurity http) throws Exception {
			http.authorizeRequests().mvcMatchers(HttpMethod.POST, "/**").denyAll().and()
					.csrf().disable();
		}

	}

}
