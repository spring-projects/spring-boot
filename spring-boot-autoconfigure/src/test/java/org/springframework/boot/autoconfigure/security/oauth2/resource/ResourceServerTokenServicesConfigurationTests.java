/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.oauth2.resource;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.OAuth2ClientProperties;
import org.springframework.boot.autoconfigure.social.FacebookAutoConfiguration;
import org.springframework.boot.autoconfigure.social.SocialWebAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.social.connect.ConnectionFactoryLocator;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ResourceServerTokenServicesConfiguration}.
 *
 * @author Dave Syer
 */
public class ResourceServerTokenServicesConfigurationTests {

	private static String PUBLIC_KEY = "-----BEGIN PUBLIC KEY-----\n"
			+ "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnGp/Q5lh0P8nPL21oMMrt2RrkT9"
			+ "AW5jgYwLfSUnJVc9G6uR3cXRRDCjHqWU5WYwivcF180A6CWp/ireQFFBNowgc5XaA0kPpzE"
			+ "tgsA5YsNX7iSnUibB004iBTfU9hZ2Rbsc8cWqynT0RyN4TP1RYVSeVKvMQk4GT1r7JCEC+T"
			+ "Nu1ELmbNwMQyzKjsfBXyIOCFU/E94ktvsTZUHF4Oq44DBylCDsS1k7/sfZC2G5EU7Oz0mhG"
			+ "8+Uz6MSEQHtoIi6mc8u64Rwi3Z3tscuWG2ShtsUFuNSAFNkY7LkLn+/hxLCu2bNISMaESa8"
			+ "dG22CIMuIeRLVcAmEWEWH5EEforTg+QIDAQAB\n-----END PUBLIC KEY-----";

	private ConfigurableApplicationContext context;

	private ConfigurableEnvironment environment = new StandardEnvironment();

	@After
	public void close() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void defaultIsRemoteTokenServices() {
		this.context = new SpringApplicationBuilder(ResourceConfiguration.class).web(
				false).run();
		RemoteTokenServices services = this.context.getBean(RemoteTokenServices.class);
		assertNotNull(services);
	}

	@Test
	public void useRemoteTokenServices() {
		EnvironmentTestUtils.addEnvironment(this.environment,
				"spring.oauth2.resource.tokenInfoUri:http://example.com",
				"spring.oauth2.resource.clientId=acme");
		this.context = new SpringApplicationBuilder(ResourceConfiguration.class)
				.environment(this.environment).web(false).run();
		RemoteTokenServices services = this.context.getBean(RemoteTokenServices.class);
		assertNotNull(services);
	}

	@Test
	public void switchToUserInfo() {
		EnvironmentTestUtils.addEnvironment(this.environment,
				"spring.oauth2.resource.userInfoUri:http://example.com");
		this.context = new SpringApplicationBuilder(ResourceConfiguration.class)
				.environment(this.environment).web(false).run();
		UserInfoTokenServices services = this.context
				.getBean(UserInfoTokenServices.class);
		assertNotNull(services);
	}

	@Test
	public void preferUserInfo() {
		EnvironmentTestUtils.addEnvironment(this.environment,
				"spring.oauth2.resource.userInfoUri:http://example.com",
				"spring.oauth2.resource.tokenInfoUri:http://example.com",
				"spring.oauth2.resource.preferTokenInfo:false");
		this.context = new SpringApplicationBuilder(ResourceConfiguration.class)
				.environment(this.environment).web(false).run();
		UserInfoTokenServices services = this.context
				.getBean(UserInfoTokenServices.class);
		assertNotNull(services);
	}

	@Test
	public void switchToJwt() {
		EnvironmentTestUtils.addEnvironment(this.environment,
				"spring.oauth2.resource.jwt.keyValue=FOOBAR");
		this.context = new SpringApplicationBuilder(ResourceConfiguration.class)
				.environment(this.environment).web(false).run();
		DefaultTokenServices services = this.context.getBean(DefaultTokenServices.class);
		assertNotNull(services);
	}

	@Test
	public void asymmetricJwt() {
		EnvironmentTestUtils.addEnvironment(this.environment,
				"spring.oauth2.resource.jwt.keyValue=" + PUBLIC_KEY);
		this.context = new SpringApplicationBuilder(ResourceConfiguration.class)
				.environment(this.environment).web(false).run();
		DefaultTokenServices services = this.context.getBean(DefaultTokenServices.class);
		assertNotNull(services);
	}

	@Test
	public void springSocialUserInfo() {
		EnvironmentTestUtils.addEnvironment(this.environment,
				"spring.oauth2.resource.userInfoUri:http://example.com",
				"spring.social.facebook.app-id=foo",
				"spring.social.facebook.app-secret=bar");
		this.context = new SpringApplicationBuilder(SocialResourceConfiguration.class)
				.environment(this.environment).web(true).run();
		ConnectionFactoryLocator connectionFactory = this.context
				.getBean(ConnectionFactoryLocator.class);
		assertNotNull(connectionFactory);
		SpringSocialTokenServices services = this.context
				.getBean(SpringSocialTokenServices.class);
		assertNotNull(services);
	}

	@Configuration
	@Import({ ResourceServerTokenServicesConfiguration.class,
			ResourceServerPropertiesConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	@EnableConfigurationProperties(OAuth2ClientProperties.class)
	protected static class ResourceConfiguration {

	}

	@Configuration
	protected static class ResourceServerPropertiesConfiguration {

		@Autowired
		private OAuth2ClientProperties credentials;

		@Bean
		public ResourceServerProperties resourceServerProperties() {
			return new ResourceServerProperties(this.credentials.getClientId(),
					this.credentials.getClientSecret());
		}

	}

	@Import({ FacebookAutoConfiguration.class, SocialWebAutoConfiguration.class })
	protected static class SocialResourceConfiguration extends ResourceConfiguration {

		@Bean
		public EmbeddedServletContainerFactory embeddedServletContainerFactory() {
			return mock(EmbeddedServletContainerFactory.class);
		}

	}

}
