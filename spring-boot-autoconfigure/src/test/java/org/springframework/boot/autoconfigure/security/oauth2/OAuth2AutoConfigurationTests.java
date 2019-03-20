/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.oauth2.authserver.OAuth2AuthorizationServerConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.method.OAuth2MethodSecurityConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.annotation.Jsr250MethodSecurityMetadataSource;
import org.springframework.security.access.annotation.SecuredAnnotationSecurityMetadataSource;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.method.DelegatingMethodSecurityMetadataSource;
import org.springframework.security.access.method.MethodSecurityMetadataSource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PreInvocationAuthorizationAdvice;
import org.springframework.security.access.prepost.PrePostAnnotationSecurityMetadataSource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
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
import org.springframework.security.oauth2.provider.client.InMemoryClientDetailsService;
import org.springframework.security.oauth2.provider.endpoint.AuthorizationEndpoint;
import org.springframework.security.oauth2.provider.expression.OAuth2MethodSecurityExpressionHandler;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verify Spring Security OAuth2 auto-configuration secures end points properly, accepts
 * environmental overrides, and also backs off in the presence of other
 * resource/authorization components.
 *
 * @author Greg Turnquist
 * @author Dave Syer
 */
public class OAuth2AutoConfigurationTests {

	private static final Class<?> RESOURCE_SERVER_CONFIG = OAuth2ResourceServerConfiguration.class;

	private static final Class<?> AUTHORIZATION_SERVER_CONFIG = OAuth2AuthorizationServerConfiguration.class;

	private AnnotationConfigEmbeddedWebApplicationContext context;

	@Test
	public void testDefaultConfiguration() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(AuthorizationAndResourceServerConfiguration.class,
				MinimalSecureWebApplication.class);
		this.context.refresh();
		this.context.getBean(AUTHORIZATION_SERVER_CONFIG);
		this.context.getBean(RESOURCE_SERVER_CONFIG);
		this.context.getBean(OAuth2MethodSecurityConfiguration.class);
		ClientDetails config = this.context.getBean(BaseClientDetails.class);
		AuthorizationEndpoint endpoint = this.context
				.getBean(AuthorizationEndpoint.class);
		UserApprovalHandler handler = (UserApprovalHandler) ReflectionTestUtils
				.getField(endpoint, "userApprovalHandler");
		ClientDetailsService clientDetailsService = this.context
				.getBean(ClientDetailsService.class);
		ClientDetails clientDetails = clientDetailsService
				.loadClientByClientId(config.getClientId());
		assertThat(AopUtils.isJdkDynamicProxy(clientDetailsService)).isTrue();
		assertThat(AopUtils.getTargetClass(clientDetailsService).getName())
				.isEqualTo(InMemoryClientDetailsService.class.getName());
		assertThat(handler).isInstanceOf(ApprovalStoreUserApprovalHandler.class);
		assertThat(clientDetails).isEqualTo(config);
		verifyAuthentication(config);
		assertThat(this.context.getBeanNamesForType(OAuth2RestOperations.class))
				.isEmpty();
	}

	@Test
	public void methodSecurityExpressionHandlerIsConfiguredWithRoleHierarchyFromTheContext() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(RoleHierarchyConfiguration.class,
				AuthorizationAndResourceServerConfiguration.class,
				MinimalSecureWebApplication.class);
		this.context.refresh();
		PreInvocationAuthorizationAdvice advice = this.context
				.getBean(PreInvocationAuthorizationAdvice.class);
		MethodSecurityExpressionHandler expressionHandler = (MethodSecurityExpressionHandler) ReflectionTestUtils
				.getField(advice, "expressionHandler");
		RoleHierarchy roleHierarchy = (RoleHierarchy) ReflectionTestUtils
				.getField(expressionHandler, "roleHierarchy");
		assertThat(roleHierarchy).isSameAs(this.context.getBean(RoleHierarchy.class));
	}

	@Test
	public void methodSecurityExpressionHandlerIsConfiguredWithPermissionEvaluatorFromTheContext() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(PermissionEvaluatorConfiguration.class,
				AuthorizationAndResourceServerConfiguration.class,
				MinimalSecureWebApplication.class);
		this.context.refresh();
		PreInvocationAuthorizationAdvice advice = this.context
				.getBean(PreInvocationAuthorizationAdvice.class);
		MethodSecurityExpressionHandler expressionHandler = (MethodSecurityExpressionHandler) ReflectionTestUtils
				.getField(advice, "expressionHandler");
		PermissionEvaluator permissionEvaluator = (PermissionEvaluator) ReflectionTestUtils
				.getField(expressionHandler, "permissionEvaluator");
		assertThat(permissionEvaluator)
				.isSameAs(this.context.getBean(PermissionEvaluator.class));
	}

	@Test
	public void testEnvironmentalOverrides() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"security.oauth2.client.clientId:myclientid",
				"security.oauth2.client.clientSecret:mysecret",
				"security.oauth2.client.autoApproveScopes:read,write",
				"security.oauth2.client.accessTokenValiditySeconds:40",
				"security.oauth2.client.refreshTokenValiditySeconds:80");
		this.context.register(AuthorizationAndResourceServerConfiguration.class,
				MinimalSecureWebApplication.class);
		this.context.refresh();
		ClientDetails config = this.context.getBean(ClientDetails.class);
		assertThat(config.getClientId()).isEqualTo("myclientid");
		assertThat(config.getClientSecret()).isEqualTo("mysecret");
		assertThat(config.isAutoApprove("read")).isTrue();
		assertThat(config.isAutoApprove("write")).isTrue();
		assertThat(config.isAutoApprove("foo")).isFalse();
		assertThat(config.getAccessTokenValiditySeconds()).isEqualTo(40);
		assertThat(config.getRefreshTokenValiditySeconds()).isEqualTo(80);
		verifyAuthentication(config);
	}

	@Test
	public void testDisablingResourceServer() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(AuthorizationServerConfiguration.class,
				MinimalSecureWebApplication.class);
		this.context.refresh();
		assertThat(countBeans(RESOURCE_SERVER_CONFIG)).isEqualTo(0);
		assertThat(countBeans(AUTHORIZATION_SERVER_CONFIG)).isEqualTo(1);
	}

	@Test
	public void testClientIsNotResourceServer() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(ClientConfiguration.class,
				MinimalSecureWebApplication.class);
		this.context.refresh();
		assertThat(countBeans(RESOURCE_SERVER_CONFIG)).isEqualTo(0);
		assertThat(countBeans(AUTHORIZATION_SERVER_CONFIG)).isEqualTo(0);
		// Scoped target and proxy:
		assertThat(countBeans(OAuth2ClientContext.class)).isEqualTo(2);
	}

	@Test
	public void testCanUseClientCredentials() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(TestSecurityConfiguration.class,
				MinimalSecureWebApplication.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"security.oauth2.client.clientId=client",
				"security.oauth2.client.grantType=client_credentials");
		this.context.refresh();
		OAuth2ClientContext bean = this.context.getBean(OAuth2ClientContext.class);
		assertThat(bean.getAccessTokenRequest()).isNotNull();
		assertThat(countBeans(ClientCredentialsResourceDetails.class)).isEqualTo(1);
		assertThat(countBeans(OAuth2ClientContext.class)).isEqualTo(1);
	}

	@Test
	public void testCanUseClientCredentialsWithEnableOAuth2Client() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(ClientConfiguration.class,
				MinimalSecureWebApplication.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"security.oauth2.client.clientId=client",
				"security.oauth2.client.grantType=client_credentials");
		this.context.refresh();
		// The primary context is fine (not session scoped):
		OAuth2ClientContext bean = this.context.getBean(OAuth2ClientContext.class);
		assertThat(bean.getAccessTokenRequest()).isNotNull();
		assertThat(countBeans(ClientCredentialsResourceDetails.class)).isEqualTo(1);
		// Kind of a bug (should ideally be 1), but the cause is in Spring OAuth2 (there
		// is no need for the extra session-scoped bean). What this test proves is that
		// even if the user screws up and does @EnableOAuth2Client for client credentials,
		// it will still just about work (because of the @Primary annotation on the
		// Boot-created instance of OAuth2ClientContext).
		assertThat(countBeans(OAuth2ClientContext.class)).isEqualTo(2);
	}

	@Test
	public void testClientIsNotAuthCode() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(MinimalSecureNonWebApplication.class);
		EnvironmentTestUtils.addEnvironment(context,
				"security.oauth2.client.clientId=client");
		context.refresh();
		assertThat(countBeans(context, ClientCredentialsResourceDetails.class))
				.isEqualTo(1);
		context.close();
	}

	@Test
	public void testDisablingAuthorizationServer() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(ResourceServerConfiguration.class,
				MinimalSecureWebApplication.class);
		EnvironmentTestUtils.addEnvironment(this.context,
				"security.oauth2.resource.jwt.keyValue:DEADBEEF");
		this.context.refresh();
		assertThat(countBeans(RESOURCE_SERVER_CONFIG)).isEqualTo(1);
		assertThat(countBeans(AUTHORIZATION_SERVER_CONFIG)).isEqualTo(0);
		assertThat(countBeans(UserApprovalHandler.class)).isEqualTo(0);
		assertThat(countBeans(DefaultTokenServices.class)).isEqualTo(1);
	}

	@Test
	public void testResourceServerOverride() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(AuthorizationAndResourceServerConfiguration.class,
				CustomResourceServer.class, MinimalSecureWebApplication.class);
		this.context.refresh();
		ClientDetails config = this.context.getBean(ClientDetails.class);
		assertThat(countBeans(AUTHORIZATION_SERVER_CONFIG)).isEqualTo(1);
		assertThat(countBeans(CustomResourceServer.class)).isEqualTo(1);
		assertThat(countBeans(RESOURCE_SERVER_CONFIG)).isEqualTo(1);
		verifyAuthentication(config);
	}

	@Test
	public void testAuthorizationServerOverride() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"security.oauth2.resourceId:resource-id");
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
		assertThat(countBeans(AUTHORIZATION_SERVER_CONFIG)).isEqualTo(0);
		assertThat(countBeans(RESOURCE_SERVER_CONFIG)).isEqualTo(1);
		verifyAuthentication(config);
	}

	@Test
	public void testDefaultPrePostSecurityAnnotations() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(AuthorizationAndResourceServerConfiguration.class,
				MinimalSecureWebApplication.class);
		this.context.refresh();
		this.context.getBean(OAuth2MethodSecurityConfiguration.class);
		ClientDetails config = this.context.getBean(ClientDetails.class);
		DelegatingMethodSecurityMetadataSource source = this.context
				.getBean(DelegatingMethodSecurityMetadataSource.class);
		List<MethodSecurityMetadataSource> sources = source
				.getMethodSecurityMetadataSources();
		assertThat(sources.size()).isEqualTo(1);
		assertThat(sources.get(0).getClass().getName())
				.isEqualTo(PrePostAnnotationSecurityMetadataSource.class.getName());
		verifyAuthentication(config);
	}

	@Test
	public void testClassicSecurityAnnotationOverride() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(SecuredEnabledConfiguration.class,
				MinimalSecureWebApplication.class);
		this.context.refresh();
		this.context.getBean(OAuth2MethodSecurityConfiguration.class);
		ClientDetails config = this.context.getBean(ClientDetails.class);
		DelegatingMethodSecurityMetadataSource source = this.context
				.getBean(DelegatingMethodSecurityMetadataSource.class);
		List<MethodSecurityMetadataSource> sources = source
				.getMethodSecurityMetadataSources();
		assertThat(sources.size()).isEqualTo(1);
		assertThat(sources.get(0).getClass().getName())
				.isEqualTo(SecuredAnnotationSecurityMetadataSource.class.getName());
		verifyAuthentication(config, HttpStatus.OK);
	}

	@Test
	public void testJsr250SecurityAnnotationOverride() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(Jsr250EnabledConfiguration.class,
				MinimalSecureWebApplication.class);
		this.context.refresh();
		this.context.getBean(OAuth2MethodSecurityConfiguration.class);
		ClientDetails config = this.context.getBean(ClientDetails.class);
		DelegatingMethodSecurityMetadataSource source = this.context
				.getBean(DelegatingMethodSecurityMetadataSource.class);
		List<MethodSecurityMetadataSource> sources = source
				.getMethodSecurityMetadataSources();
		assertThat(sources.size()).isEqualTo(1);
		assertThat(sources.get(0).getClass().getName())
				.isEqualTo(Jsr250MethodSecurityMetadataSource.class.getName());
		verifyAuthentication(config, HttpStatus.OK);
	}

	@Test
	public void testMethodSecurityBackingOff() {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		this.context.register(CustomMethodSecurity.class, TestSecurityConfiguration.class,
				MinimalSecureWebApplication.class);
		this.context.refresh();
		DelegatingMethodSecurityMetadataSource source = this.context
				.getBean(DelegatingMethodSecurityMetadataSource.class);
		List<MethodSecurityMetadataSource> sources = source
				.getMethodSecurityMetadataSources();
		assertThat(sources.size()).isEqualTo(1);
		assertThat(sources.get(0).getClass().getName())
				.isEqualTo(PrePostAnnotationSecurityMetadataSource.class.getName());
	}

	@Test
	public void resourceServerConditionWhenJwkConfigurationPresentShouldMatch()
			throws Exception {
		this.context = new AnnotationConfigEmbeddedWebApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"security.oauth2.resource.jwk.key-set-uri:http://my-auth-server/token_keys");
		this.context.register(ResourceServerConfiguration.class,
				MinimalSecureWebApplication.class);
		this.context.refresh();
		assertThat(countBeans(RESOURCE_SERVER_CONFIG)).isEqualTo(1);
	}

	/**
	 * Connect to the oauth service, get a token, and then attempt some operations using
	 * it.
	 * @param config the client details.
	 */
	private void verifyAuthentication(ClientDetails config) {
		verifyAuthentication(config, HttpStatus.FORBIDDEN);
	}

	private void verifyAuthentication(ClientDetails config, HttpStatus finalStatus) {
		String baseUrl = "http://localhost:"
				+ this.context.getEmbeddedServletContainer().getPort();
		TestRestTemplate rest = new TestRestTemplate();
		// First, verify the web endpoint can't be reached
		assertEndpointUnauthorized(baseUrl, rest);
		// Since we can't reach it, need to collect an authorization token
		HttpHeaders headers = getHeaders(config);
		String url = baseUrl + "/oauth/token";
		JsonNode tokenResponse = rest.postForObject(url,
				new HttpEntity<MultiValueMap<String, Object>>(getBody(), headers),
				JsonNode.class);
		String authorizationToken = tokenResponse.findValue("access_token").asText();
		String tokenType = tokenResponse.findValue("token_type").asText();
		String scope = tokenResponse.findValues("scope").get(0).toString();
		assertThat(tokenType).isEqualTo("bearer");
		assertThat(scope).isEqualTo("\"read\"");
		// Now we should be able to see that endpoint.
		headers.set("Authorization", "BEARER " + authorizationToken);
		ResponseEntity<String> securedResponse = rest
				.exchange(new RequestEntity<Void>(headers, HttpMethod.GET,
						URI.create(baseUrl + "/securedFind")), String.class);
		assertThat(securedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(securedResponse.getBody()).isEqualTo(
				"You reached an endpoint " + "secured by Spring Security OAuth2");
		ResponseEntity<String> entity = rest.exchange(new RequestEntity<Void>(headers,
				HttpMethod.POST, URI.create(baseUrl + "/securedSave")), String.class);
		assertThat(entity.getStatusCode()).isEqualTo(finalStatus);
	}

	private HttpHeaders getHeaders(ClientDetails config) {
		HttpHeaders headers = new HttpHeaders();
		String token = new String(Base64.encode(
				(config.getClientId() + ":" + config.getClientSecret()).getBytes()));
		headers.set("Authorization", "Basic " + token);
		return headers;
	}

	private MultiValueMap<String, Object> getBody() {
		MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>();
		body.set("grant_type", "password");
		body.set("username", "foo");
		body.set("password", "bar");
		body.set("scope", "read");
		return body;
	}

	private void assertEndpointUnauthorized(String baseUrl, TestRestTemplate rest) {
		URI uri = URI.create(baseUrl + "/secured");
		ResponseEntity<String> entity = rest
				.exchange(new RequestEntity<Void>(HttpMethod.GET, uri), String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	private int countBeans(Class<?> type) {
		return countBeans(this.context, type);
	}

	private int countBeans(ApplicationContext context, Class<?> type) {
		return context.getBeanNamesForType(type).length;
	}

	@Configuration
	@Import({ UseFreePortEmbeddedContainerConfiguration.class,
			SecurityAutoConfiguration.class, ServerPropertiesAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, OAuth2AutoConfiguration.class,
			WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class })
	protected static class MinimalSecureWebApplication {

	}

	@Configuration
	@Import({ SecurityAutoConfiguration.class, OAuth2AutoConfiguration.class })
	protected static class MinimalSecureNonWebApplication {

	}

	@Configuration
	@Order(SecurityProperties.ACCESS_OVERRIDE_ORDER)
	protected static class TestSecurityConfiguration
			extends WebSecurityConfigurerAdapter {

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
	@EnableOAuth2Client
	protected static class ClientConfiguration extends TestSecurityConfiguration {

	}

	@Configuration
	@EnableAuthorizationServer
	@EnableResourceServer
	@EnableGlobalMethodSecurity(prePostEnabled = true)
	protected static class AuthorizationAndResourceServerConfiguration
			extends TestSecurityConfiguration {

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
	protected static class AuthorizationServerConfiguration
			extends TestSecurityConfiguration {

	}

	@Configuration
	@EnableResourceServer
	protected static class ResourceServerConfiguration extends TestSecurityConfiguration {

	}

	@RestController
	protected static class TestWebApp {

		@GetMapping("/securedFind")
		@PreAuthorize("#oauth2.hasScope('read')")
		public String secureFind() {
			return "You reached an endpoint secured by Spring Security OAuth2";
		}

		@PostMapping("/securedSave")
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

		private final ResourceServerProperties config;

		protected CustomResourceServer(ResourceServerProperties config) {
			this.config = config;
		}

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
	protected static class CustomAuthorizationServer
			extends AuthorizationServerConfigurerAdapter {

		private final AuthenticationManager authenticationManager;

		protected CustomAuthorizationServer(AuthenticationManager authenticationManager) {
			this.authenticationManager = authenticationManager;
		}

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
			endpoints.tokenStore(tokenStore())
					.authenticationManager(this.authenticationManager);
		}

	}

	@Configuration
	@EnableGlobalMethodSecurity(prePostEnabled = true)
	protected static class CustomMethodSecurity
			extends GlobalMethodSecurityConfiguration {

		@Override
		protected MethodSecurityExpressionHandler createExpressionHandler() {
			return new OAuth2MethodSecurityExpressionHandler();
		}

	}

	@Configuration
	protected static class RoleHierarchyConfiguration {

		@Bean
		public RoleHierarchy roleHierarchy() {
			return mock(RoleHierarchy.class);
		}

	}

	@Configuration
	protected static class PermissionEvaluatorConfiguration {

		@Bean
		public PermissionEvaluator permissionEvaluator() {
			return mock(PermissionEvaluator.class);
		}

	}

}
