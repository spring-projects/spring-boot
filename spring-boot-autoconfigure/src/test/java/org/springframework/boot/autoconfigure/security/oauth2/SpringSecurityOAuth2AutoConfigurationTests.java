/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.authserver.SpringSecurityOAuth2AuthorizationServerConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.SpringSecurityOAuth2ResourceServerConfiguration;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Jsr250MethodSecurityMetadataSource;
import org.springframework.security.access.annotation.SecuredAnnotationSecurityMetadataSource;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.method.DelegatingMethodSecurityMetadataSource;
import org.springframework.security.access.method.MethodSecurityMetadataSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PrePostAnnotationSecurityMetadataSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.approval.ApprovalStore;
import org.springframework.security.oauth2.provider.approval.ApprovalStoreUserApprovalHandler;
import org.springframework.security.oauth2.provider.approval.TokenApprovalStore;
import org.springframework.security.oauth2.provider.approval.UserApprovalHandler;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.endpoint.AuthorizationEndpoint;
import org.springframework.security.oauth2.provider.expression.OAuth2MethodSecurityExpressionHandler;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Verify Spring Security OAuth2 auto-configuration secures end points properly, accepts
 * environmental overrides, and also backs off in the presence of other
 * resource/authorization components.
 *
 * @author Greg Turnquist
 * @author Dave Syer
 */
public class SpringSecurityOAuth2AutoConfigurationTests {

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@Test
	public void testDefaultConfiguration() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(AuthorizationAndResourceServerConfiguration.class,
				MinimalSecureWebApplication.class);
		this.context.refresh();

		this.context.getBean(SpringSecurityOAuth2AuthorizationServerConfiguration.class);
		this.context.getBean(SpringSecurityOAuth2ResourceServerConfiguration.class);
		this.context.getBean(SpringSecurityOAuth2MethodSecurityConfiguration.class);

		ClientDetails config = this.context.getBean(BaseClientDetails.class);
		AuthorizationEndpoint endpoint = this.context
				.getBean(AuthorizationEndpoint.class);
		UserApprovalHandler handler = (UserApprovalHandler) ReflectionTestUtils.getField(
				endpoint, "userApprovalHandler");
		ClientDetailsService clientDetailsService = this.context
				.getBean(ClientDetailsService.class);
		ClientDetails clientDetails = clientDetailsService.loadClientByClientId(config
				.getClientId());

		assertThat(AopUtils.isJdkDynamicProxy(clientDetailsService), is(true));
		assertThat(AopUtils.getTargetClass(clientDetailsService).getName(),
				is(ClientDetailsService.class.getName()));

		assertThat(handler instanceof ApprovalStoreUserApprovalHandler, is(true));

		assertThat(clientDetails, equalTo(config));

		verifyAuthentication(config);
	}

	@Test
	public void testEnvironmentalOverrides() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.oauth2.client.clientId:myclientid",
				"spring.oauth2.client.clientSecret:mysecret");
		this.context.register(AuthorizationAndResourceServerConfiguration.class,
				MinimalSecureWebApplication.class);
		this.context.refresh();

		ClientDetails config = this.context.getBean(ClientDetails.class);

		assertThat(config.getClientId(), is("myclientid"));
		assertThat(config.getClientSecret(), is("mysecret"));

		verifyAuthentication(config);
	}

	@Test
	public void testDisablingResourceServer() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(AuthorizationServerConfiguration.class,
				MinimalSecureWebApplication.class);
		this.context.refresh();

		assertThat(
				this.context
						.getBeanNamesForType(SpringSecurityOAuth2ResourceServerConfiguration.class).length,
				is(0));

		assertThat(
				this.context
						.getBeanNamesForType(SpringSecurityOAuth2AuthorizationServerConfiguration.class).length,
				is(1));
	}

	@Test
	public void testDisablingAuthorizationServer() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(ResourceServerConfiguration.class,
				MinimalSecureWebApplication.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.oauth2.resource.jwt.keyValue:DEADBEEF");
		this.context.refresh();

		assertThat(
				this.context
						.getBeanNamesForType(SpringSecurityOAuth2ResourceServerConfiguration.class).length,
				is(1));

		assertThat(
				this.context
						.getBeanNamesForType(SpringSecurityOAuth2AuthorizationServerConfiguration.class).length,
				is(0));

		assertThat(this.context.getBeanNamesForType(UserApprovalHandler.class).length,
				is(0));
		assertThat(this.context.getBeanNamesForType(DefaultTokenServices.class).length,
				is(1));
	}

	@Test
	public void testResourceServerOverride() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(AuthorizationAndResourceServerConfiguration.class,
				CustomResourceServer.class, MinimalSecureWebApplication.class);
		this.context.refresh();

		ClientDetails config = this.context.getBean(ClientDetails.class);

		assertThat(
				this.context
						.getBeanNamesForType(SpringSecurityOAuth2AuthorizationServerConfiguration.class).length,
				is(1));

		assertThat(this.context.getBeanNamesForType(CustomResourceServer.class).length,
				is(1));

		assertThat(
				this.context
						.getBeanNamesForType(SpringSecurityOAuth2ResourceServerConfiguration.class).length,
				is(1));

		verifyAuthentication(config);

	}

	@Test
	public void testAuthorizationServerOverride() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.oauth2.resourceId:resource-id");
		this.context.register(AuthorizationAndResourceServerConfiguration.class,
				CustomAuthorizationServer.class, MinimalSecureWebApplication.class);
		this.context.refresh();

		BaseClientDetails config = new BaseClientDetails();
		config.setClientId("client");
		config.setClientSecret("secret");
		config.setResourceIds(Arrays.asList("resource-id"));
		config.setAuthorizedGrantTypes(Arrays.asList("password"));
		config.setAuthorities(AuthorityUtils.commaSeparatedStringToAuthorityList("USER"));
		config.setScope(Arrays.asList("read"));

		assertThat(
				this.context
						.getBeanNamesForType(SpringSecurityOAuth2AuthorizationServerConfiguration.class).length,
				is(0));

		assertThat(
				this.context
						.getBeanNamesForType(SpringSecurityOAuth2ResourceServerConfiguration.class).length,
				is(1));

		verifyAuthentication(config);
	}

	@Test
	public void testDefaultPrePostSecurityAnnotations() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(AuthorizationAndResourceServerConfiguration.class,
				MinimalSecureWebApplication.class);
		this.context.refresh();

		this.context.getBean(SpringSecurityOAuth2MethodSecurityConfiguration.class);

		ClientDetails config = this.context.getBean(ClientDetails.class);

		DelegatingMethodSecurityMetadataSource source = this.context
				.getBean(DelegatingMethodSecurityMetadataSource.class);
		List<MethodSecurityMetadataSource> sources = source
				.getMethodSecurityMetadataSources();

		assertThat(sources.size(), is(1));
		assertThat(sources.get(0).getClass().getName(),
				is(PrePostAnnotationSecurityMetadataSource.class.getName()));

		verifyAuthentication(config);
	}

	@Test
	public void testClassicSecurityAnnotationOverride() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(SecuredEnabledConfiguration.class,
				MinimalSecureWebApplication.class);
		this.context.refresh();

		this.context.getBean(SpringSecurityOAuth2MethodSecurityConfiguration.class);

		ClientDetails config = this.context.getBean(ClientDetails.class);

		DelegatingMethodSecurityMetadataSource source = this.context
				.getBean(DelegatingMethodSecurityMetadataSource.class);
		List<MethodSecurityMetadataSource> sources = source
				.getMethodSecurityMetadataSources();

		assertThat(sources.size(), is(1));
		assertThat(sources.get(0).getClass().getName(),
				is(SecuredAnnotationSecurityMetadataSource.class.getName()));

		verifyAuthentication(config, HttpStatus.OK);
	}

	@Test
	public void testJsr250SecurityAnnotationOverride() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(Jsr250EnabledConfiguration.class,
				MinimalSecureWebApplication.class);
		this.context.refresh();

		this.context.getBean(SpringSecurityOAuth2MethodSecurityConfiguration.class);

		ClientDetails config = this.context.getBean(ClientDetails.class);

		DelegatingMethodSecurityMetadataSource source = this.context
				.getBean(DelegatingMethodSecurityMetadataSource.class);
		List<MethodSecurityMetadataSource> sources = source
				.getMethodSecurityMetadataSources();

		assertThat(sources.size(), is(1));
		assertThat(sources.get(0).getClass().getName(),
				is(Jsr250MethodSecurityMetadataSource.class.getName()));

		verifyAuthentication(config, HttpStatus.OK);
	}

	@Test
	public void testMethodSecurityBackingOff() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(CustomMethodSecurity.class,
				TestSecurityConfiguration.class, MinimalSecureWebApplication.class);
		this.context.refresh();

		DelegatingMethodSecurityMetadataSource source = this.context
				.getBean(DelegatingMethodSecurityMetadataSource.class);
		List<MethodSecurityMetadataSource> sources = source
				.getMethodSecurityMetadataSources();
		assertThat(sources.size(), is(1));
		assertThat(sources.get(0).getClass().getName(),
				is(PrePostAnnotationSecurityMetadataSource.class.getName()));
	}

	/**
	 * Connect to the oauth service, get a token, and then attempt some operations using
	 * it.
	 *
	 * @param config
	 */
	private void verifyAuthentication(ClientDetails config) {
		verifyAuthentication(config, HttpStatus.FORBIDDEN);
	}

	private void verifyAuthentication(ClientDetails config, HttpStatus finalStatus) {
		String baseUrl = "http://localhost:"
				+ this.context.getEmbeddedServletContainer().getPort();

		RestTemplate rest = new TestRestTemplate();
		HttpHeaders headers = new HttpHeaders();

		// First, verify the web endpoint can't be reached
		ResponseEntity<String> entity = rest.exchange(new RequestEntity<Void>(headers,
				HttpMethod.GET, URI.create(baseUrl + "/secured")), String.class);
		assertThat(entity.getStatusCode(), is(HttpStatus.UNAUTHORIZED));

		// Since we can't reach it, need to collect an authorization token
		String base64Creds = new String(
				Base64.encode((config.getClientId() + ":" + config.getClientSecret())
						.getBytes()));
		headers.set("Authorization", "Basic " + base64Creds);

		MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
		body.set("grant_type", "password");
		body.set("username", "foo");
		body.set("password", "bar");
		body.set("scope", "read");

		HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<MultiValueMap<String, Object>>(
				body, headers);

		JsonNode response = rest.postForObject(baseUrl + "/oauth/token", request,
				JsonNode.class);
		String authorizationToken = response.findValue("access_token").asText();
		String tokenType = response.findValue("token_type").asText();
		String scope = response.findValues("scope").get(0).toString();
		assertThat(tokenType, is("bearer"));
		assertThat(scope, is("\"read\""));

		// Now we should be able to see that endpoint.
		headers.set("Authorization", "BEARER " + authorizationToken);

		ResponseEntity<String> securedResponse = rest.exchange(new RequestEntity<Void>(
				headers, HttpMethod.GET, URI.create(baseUrl + "/securedFind")),
				String.class);
		assertThat(securedResponse.getStatusCode(), is(HttpStatus.OK));
		assertThat(securedResponse.getBody(),
				is("You reached an endpoint secured by Spring Security OAuth2"));

		entity = rest.exchange(
				new RequestEntity<Void>(headers, HttpMethod.POST, URI.create(baseUrl
						+ "/securedSave")), String.class);
		assertThat(entity.getStatusCode(), is(finalStatus));
	}

	@Configuration
	@Import({ UseFreePortEmbeddedContainerConfiguration.class,
			SecurityAutoConfiguration.class, ServerPropertiesAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class,
			SpringSecurityOAuth2AutoConfiguration.class, WebMvcAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class })
	protected static class MinimalSecureWebApplication {

	}

	@Configuration
	protected static class TestSecurityConfiguration extends WebSecurityConfigurerAdapter {

		@Override
		@Bean
		public AuthenticationManager authenticationManagerBean() throws Exception {
			return super.authenticationManagerBean();
		}

		@Autowired
		public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
			auth.inMemoryAuthentication().withUser("foo").password("bar").roles("USER");
		}

		@Bean
		TestWebApp testWebApp() {
			return new TestWebApp();
		}
	}

	@Configuration
	@EnableAuthorizationServer
	@EnableResourceServer
	@EnableGlobalMethodSecurity(prePostEnabled = true)
	protected static class AuthorizationAndResourceServerConfiguration extends
			TestSecurityConfiguration {
	}

	@Configuration
	@EnableAuthorizationServer
	@EnableResourceServer
	@EnableGlobalMethodSecurity(securedEnabled = true)
	protected static class SecuredEnabledConfiguration extends TestSecurityConfiguration {
	}

	@Configuration
	@EnableAuthorizationServer
	@EnableResourceServer
	@EnableGlobalMethodSecurity(jsr250Enabled = true)
	protected static class Jsr250EnabledConfiguration extends TestSecurityConfiguration {
	}

	@Configuration
	@EnableAuthorizationServer
	protected static class AuthorizationServerConfiguration extends
			TestSecurityConfiguration {
	}

	@Configuration
	@EnableResourceServer
	protected static class ResourceServerConfiguration extends TestSecurityConfiguration {
	}

	@RestController
	protected static class TestWebApp {

		@RequestMapping(value = "/securedFind", method = RequestMethod.GET)
		@PreAuthorize("#oauth2.hasScope('read')")
		public String secureFind() {
			return "You reached an endpoint secured by Spring Security OAuth2";
		}

		@RequestMapping(value = "/securedSave", method = RequestMethod.POST)
		@PreAuthorize("#oauth2.hasScope('write')")
		public String secureSave() {
			return "You reached an endpoint secured by Spring Security OAuth2";
		}
	}

	@Configuration
	protected static class UseFreePortEmbeddedContainerConfiguration {
		@Bean
		TomcatEmbeddedServletContainerFactory containerFactory() {
			return new TomcatEmbeddedServletContainerFactory(0);
		}
	}

	@Configuration
	@EnableResourceServer
	protected static class CustomResourceServer extends ResourceServerConfigurerAdapter {

		@Autowired
		private ResourceServerProperties config;

		@Override
		public void configure(ResourceServerSecurityConfigurer resources)
				throws Exception {
			if (this.config.getId() != null) {
				resources.resourceId(this.config.getId());
			}
		}

		@Override
		public void configure(HttpSecurity http) throws Exception {
			http.authorizeRequests().anyRequest().authenticated().and().httpBasic().and()
					.csrf().disable();
		}

	}

	@Configuration
	@EnableAuthorizationServer
	protected static class CustomAuthorizationServer extends
			AuthorizationServerConfigurerAdapter {

		@Autowired
		private AuthenticationManager authenticationManager;

		@Bean
		public TokenStore tokenStore() {
			return new InMemoryTokenStore();
		}

		@Bean
		public ApprovalStore approvalStore(final TokenStore tokenStore) {
			TokenApprovalStore approvalStore = new TokenApprovalStore();
			approvalStore.setTokenStore(tokenStore);
			return approvalStore;
		}

		@Override
		public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
			clients.inMemory().withClient("client").secret("secret")
					.resourceIds("resource-id").authorizedGrantTypes("password")
					.authorities("USER").scopes("read")
					.redirectUris("http://localhost:8080");
		}

		@Override
		public void configure(AuthorizationServerEndpointsConfigurer endpoints)
				throws Exception {
			endpoints.tokenStore(tokenStore()).authenticationManager(
					this.authenticationManager);
		}
	}

	@Configuration
	@EnableGlobalMethodSecurity(prePostEnabled = true)
	protected static class CustomMethodSecurity extends GlobalMethodSecurityConfiguration {
		@Override
		protected MethodSecurityExpressionHandler createExpressionHandler() {
			return new OAuth2MethodSecurityExpressionHandler();
		}
	}
}
