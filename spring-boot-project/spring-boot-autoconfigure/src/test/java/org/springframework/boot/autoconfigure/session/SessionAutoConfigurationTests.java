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

package org.springframework.boot.autoconfigure.session;

import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;

import javax.servlet.DispatcherType;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.security.web.authentication.SpringSessionRememberMeServices;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.HeaderHttpSessionIdResolver;
import org.springframework.session.web.http.HttpSessionIdResolver;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link SessionAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @author Vedran Pavic
 */
class SessionAutoConfigurationTests extends AbstractSessionAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SessionAutoConfiguration.class));

	@Test
	void contextFailsIfMultipleStoresAreAvailable() {
		this.contextRunner.run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context).getFailure().hasCauseInstanceOf(NonUniqueSessionRepositoryException.class);
			assertThat(context).getFailure()
					.hasMessageContaining("Multiple session repository candidates are available");
		});
	}

	@Test
	void contextFailsIfStoreTypeNotAvailable() {
		this.contextRunner.withPropertyValues("spring.session.store-type=jdbc").run((context) -> {
			assertThat(context).hasFailed();
			assertThat(context).getFailure().hasCauseInstanceOf(SessionRepositoryUnavailableException.class);
			assertThat(context).getFailure().hasMessageContaining("No session repository could be auto-configured");
			assertThat(context).getFailure().hasMessageContaining("session store type is 'jdbc'");
		});
	}

	@Test
	void autoConfigurationDisabledIfStoreTypeSetToNone() {
		this.contextRunner.withPropertyValues("spring.session.store-type=none")
				.run((context) -> assertThat(context).doesNotHaveBean(SessionRepository.class));
	}

	@Test
	void backOffIfSessionRepositoryIsPresent() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class)
				.withPropertyValues("spring.session.store-type=redis").run((context) -> {
					MapSessionRepository repository = validateSessionRepository(context, MapSessionRepository.class);
					assertThat(context).getBean("mySessionRepository").isSameAs(repository);
				});
	}

	@Test
	void autoConfigWhenSpringSessionTimeoutIsSetShouldUseThat() {
		this.contextRunner
				.withUserConfiguration(ServerPropertiesConfiguration.class, SessionRepositoryConfiguration.class)
				.withPropertyValues("server.servlet.session.timeout=1", "spring.session.timeout=3")
				.run((context) -> assertThat(context.getBean(SessionProperties.class).getTimeout())
						.isEqualTo(Duration.ofSeconds(3)));
	}

	@Test
	void autoConfigWhenSpringSessionTimeoutIsNotSetShouldUseServerSessionTimeout() {
		this.contextRunner
				.withUserConfiguration(ServerPropertiesConfiguration.class, SessionRepositoryConfiguration.class)
				.withPropertyValues("server.servlet.session.timeout=3")
				.run((context) -> assertThat(context.getBean(SessionProperties.class).getTimeout())
						.isEqualTo(Duration.ofSeconds(3)));
	}

	@SuppressWarnings("unchecked")
	@Test
	void filterIsRegisteredWithAsyncErrorAndRequestDispatcherTypes() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class).run((context) -> {
			FilterRegistrationBean<?> registration = context.getBean(FilterRegistrationBean.class);
			assertThat(registration.getFilter()).isSameAs(context.getBean(SessionRepositoryFilter.class));
			assertThat((EnumSet<DispatcherType>) ReflectionTestUtils.getField(registration, "dispatcherTypes"))
					.containsOnly(DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.REQUEST);
		});
	}

	@Test
	void filterOrderCanBeCustomizedWithCustomStore() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class)
				.withPropertyValues("spring.session.servlet.filter-order=123").run((context) -> {
					FilterRegistrationBean<?> registration = context.getBean(FilterRegistrationBean.class);
					assertThat(registration.getOrder()).isEqualTo(123);
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	void filterDispatcherTypesCanBeCustomized() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class)
				.withPropertyValues("spring.session.servlet.filter-dispatcher-types=error, request").run((context) -> {
					FilterRegistrationBean<?> registration = context.getBean(FilterRegistrationBean.class);
					assertThat((EnumSet<DispatcherType>) ReflectionTestUtils.getField(registration, "dispatcherTypes"))
							.containsOnly(DispatcherType.ERROR, DispatcherType.REQUEST);
				});
	}

	@Test
	void sessionCookieConfigurationIsAppliedToAutoConfiguredCookieSerializer() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class)
				.withPropertyValues("server.servlet.session.cookie.name=sid",
						"server.servlet.session.cookie.domain=spring", "server.servlet.session.cookie.path=/test",
						"server.servlet.session.cookie.httpOnly=false", "server.servlet.session.cookie.secure=false",
						"server.servlet.session.cookie.maxAge=10s")
				.run((context) -> {
					DefaultCookieSerializer cookieSerializer = context.getBean(DefaultCookieSerializer.class);
					assertThat(cookieSerializer).hasFieldOrPropertyWithValue("cookieName", "sid");
					assertThat(cookieSerializer).hasFieldOrPropertyWithValue("domainName", "spring");
					assertThat(cookieSerializer).hasFieldOrPropertyWithValue("cookiePath", "/test");
					assertThat(cookieSerializer).hasFieldOrPropertyWithValue("useHttpOnlyCookie", false);
					assertThat(cookieSerializer).hasFieldOrPropertyWithValue("useSecureCookie", false);
					assertThat(cookieSerializer).hasFieldOrPropertyWithValue("cookieMaxAge", 10);
				});
	}

	@Test
	void autoConfiguredCookieSerializerIsUsedBySessionRepositoryFilter() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class)
				.withPropertyValues("server.port=0").run((context) -> {
					SessionRepositoryFilter<?> filter = context.getBean(SessionRepositoryFilter.class);
					CookieHttpSessionIdResolver sessionIdResolver = (CookieHttpSessionIdResolver) ReflectionTestUtils
							.getField(filter, "httpSessionIdResolver");
					DefaultCookieSerializer cookieSerializer = (DefaultCookieSerializer) ReflectionTestUtils
							.getField(sessionIdResolver, "cookieSerializer");
					assertThat(cookieSerializer).isSameAs(context.getBean(DefaultCookieSerializer.class));
				});
	}

	@Test
	void autoConfiguredCookieSerializerBacksOffWhenUserConfiguresACookieSerializer() {
		this.contextRunner.withUserConfiguration(UserProvidedCookieSerializerConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(DefaultCookieSerializer.class);
			assertThat(context).hasBean("myCookieSerializer");
		});
	}

	@Test
	void cookiesSerializerIsAutoConfiguredWhenUserConfiguresCookieHttpSessionIdResolver() {
		this.contextRunner.withUserConfiguration(UserProvidedCookieHttpSessionStrategyConfiguration.class)
				.run((context) -> assertThat(context.getBeansOfType(DefaultCookieSerializer.class)).isNotEmpty());
	}

	@Test
	void autoConfiguredCookieSerializerBacksOffWhenUserConfiguresHeaderHttpSessionIdResolver() {
		this.contextRunner.withUserConfiguration(UserProvidedHeaderHttpSessionStrategyConfiguration.class)
				.run((context) -> assertThat(context.getBeansOfType(DefaultCookieSerializer.class)).isEmpty());
	}

	@Test
	void autoConfiguredCookieSerializerBacksOffWhenUserConfiguresCustomHttpSessionIdResolver() {
		this.contextRunner.withUserConfiguration(UserProvidedCustomHttpSessionStrategyConfiguration.class)
				.run((context) -> assertThat(context.getBeansOfType(DefaultCookieSerializer.class)).isEmpty());
	}

	@Test
	void autoConfiguredCookieSerializerIsConfiguredWithRememberMeRequestAttribute() {
		this.contextRunner.withBean(SpringSessionRememberMeServicesConfiguration.class).run((context) -> {
			DefaultCookieSerializer cookieSerializer = context.getBean(DefaultCookieSerializer.class);
			assertThat(cookieSerializer).hasFieldOrPropertyWithValue("rememberMeRequestAttribute",
					SpringSessionRememberMeServices.REMEMBER_ME_LOGIN_ATTR);
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringHttpSession
	static class SessionRepositoryConfiguration {

		@Bean
		public MapSessionRepository mySessionRepository() {
			return new MapSessionRepository(Collections.emptyMap());
		}

	}

	@EnableConfigurationProperties(ServerProperties.class)
	static class ServerPropertiesConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringHttpSession
	static class UserProvidedCookieSerializerConfiguration extends SessionRepositoryConfiguration {

		@Bean
		public DefaultCookieSerializer myCookieSerializer() {
			return new DefaultCookieSerializer();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringHttpSession
	static class UserProvidedCookieHttpSessionStrategyConfiguration extends SessionRepositoryConfiguration {

		@Bean
		public CookieHttpSessionIdResolver httpSessionStrategy() {
			return new CookieHttpSessionIdResolver();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringHttpSession
	static class UserProvidedHeaderHttpSessionStrategyConfiguration extends SessionRepositoryConfiguration {

		@Bean
		public HeaderHttpSessionIdResolver httpSessionStrategy() {
			return HeaderHttpSessionIdResolver.xAuthToken();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringHttpSession
	static class UserProvidedCustomHttpSessionStrategyConfiguration extends SessionRepositoryConfiguration {

		@Bean
		public HttpSessionIdResolver httpSessionStrategy() {
			return mock(HttpSessionIdResolver.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringHttpSession
	static class SpringSessionRememberMeServicesConfiguration extends SessionRepositoryConfiguration {

		@Bean
		public SpringSessionRememberMeServices rememberMeServices() {
			return new SpringSessionRememberMeServices();
		}

	}

}
