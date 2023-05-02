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

package org.springframework.boot.autoconfigure.session;

import java.util.Collections;

import jakarta.servlet.DispatcherType;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.AbstractFilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.ReactiveMapSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.config.annotation.web.server.EnableSpringWebSession;
import org.springframework.session.data.mongo.MongoIndexedSessionRepository;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.hazelcast.HazelcastIndexedSessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.security.web.authentication.SpringSessionRememberMeServices;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.session.web.http.HeaderHttpSessionIdResolver;
import org.springframework.session.web.http.HttpSessionIdResolver;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.filter.DelegatingFilterProxy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
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
	void autoConfigurationDisabledIfNoImplementationMatches() {
		this.contextRunner
			.withClassLoader(new FilteredClassLoader(RedisIndexedSessionRepository.class,
					HazelcastIndexedSessionRepository.class, JdbcIndexedSessionRepository.class,
					MongoIndexedSessionRepository.class))
			.run((context) -> assertThat(context).doesNotHaveBean(SessionRepository.class));
	}

	@Test
	void backOffIfSessionRepositoryIsPresent() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class).run((context) -> {
			MapSessionRepository repository = validateSessionRepository(context, MapSessionRepository.class);
			assertThat(context).getBean("mySessionRepository").isSameAs(repository);
		});
	}

	@Test
	void backOffIfReactiveSessionRepositoryIsPresent() {
		ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SessionAutoConfiguration.class));
		contextRunner.withUserConfiguration(ReactiveSessionRepositoryConfiguration.class).run((context) -> {
			ReactiveMapSessionRepository repository = validateSessionRepository(context,
					ReactiveMapSessionRepository.class);
			assertThat(context).getBean("mySessionRepository").isSameAs(repository);
		});
	}

	@Test
	void filterIsRegisteredWithAsyncErrorAndRequestDispatcherTypes() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class).run((context) -> {
			AbstractFilterRegistrationBean<?> registration = context.getBean(AbstractFilterRegistrationBean.class);
			DelegatingFilterProxy delegatingFilterProxy = (DelegatingFilterProxy) registration.getFilter();
			try {
				// Trigger actual initialization
				delegatingFilterProxy.doFilter(null, null, null);
			}
			catch (Exception ex) {
			}
			assertThat(delegatingFilterProxy).extracting("delegate")
				.isSameAs(context.getBean(SessionRepositoryFilter.class));
			assertThat(registration)
				.extracting("dispatcherTypes", InstanceOfAssertFactories.iterable(DispatcherType.class))
				.containsOnly(DispatcherType.ASYNC, DispatcherType.ERROR, DispatcherType.REQUEST);
		});
	}

	@Test
	void filterOrderCanBeCustomizedWithCustomStore() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class)
			.withPropertyValues("spring.session.servlet.filter-order=123")
			.run((context) -> {
				AbstractFilterRegistrationBean<?> registration = context.getBean(AbstractFilterRegistrationBean.class);
				assertThat(registration.getOrder()).isEqualTo(123);
			});
	}

	@Test
	void filterDispatcherTypesCanBeCustomized() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class)
			.withPropertyValues("spring.session.servlet.filter-dispatcher-types=error, request")
			.run((context) -> {
				AbstractFilterRegistrationBean<?> registration = context.getBean(AbstractFilterRegistrationBean.class);
				assertThat(registration)
					.extracting("dispatcherTypes", InstanceOfAssertFactories.iterable(DispatcherType.class))
					.containsOnly(DispatcherType.ERROR, DispatcherType.REQUEST);
			});
	}

	@Test
	void emptyFilterDispatcherTypesDoNotThrowException() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class)
			.withPropertyValues("spring.session.servlet.filter-dispatcher-types=")
			.run((context) -> {
				AbstractFilterRegistrationBean<?> registration = context.getBean(AbstractFilterRegistrationBean.class);
				assertThat(registration)
					.extracting("dispatcherTypes", InstanceOfAssertFactories.iterable(DispatcherType.class))
					.isEmpty();
			});
	}

	@Test
	void sessionCookieConfigurationIsAppliedToAutoConfiguredCookieSerializer() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class)
			.withPropertyValues("server.servlet.session.cookie.name=sid", "server.servlet.session.cookie.domain=spring",
					"server.servlet.session.cookie.path=/test", "server.servlet.session.cookie.httpOnly=false",
					"server.servlet.session.cookie.secure=false", "server.servlet.session.cookie.maxAge=10s",
					"server.servlet.session.cookie.sameSite=strict")
			.run((context) -> {
				DefaultCookieSerializer cookieSerializer = context.getBean(DefaultCookieSerializer.class);
				assertThat(cookieSerializer).hasFieldOrPropertyWithValue("cookieName", "sid");
				assertThat(cookieSerializer).hasFieldOrPropertyWithValue("domainName", "spring");
				assertThat(cookieSerializer).hasFieldOrPropertyWithValue("cookiePath", "/test");
				assertThat(cookieSerializer).hasFieldOrPropertyWithValue("useHttpOnlyCookie", false);
				assertThat(cookieSerializer).hasFieldOrPropertyWithValue("useSecureCookie", false);
				assertThat(cookieSerializer).hasFieldOrPropertyWithValue("cookieMaxAge", 10);
				assertThat(cookieSerializer).hasFieldOrPropertyWithValue("sameSite", "Strict");
			});
	}

	@Test
	void autoConfiguredCookieSerializerIsUsedBySessionRepositoryFilter() {
		this.contextRunner.withUserConfiguration(SessionRepositoryConfiguration.class)
			.withPropertyValues("server.port=0")
			.run((context) -> {
				SessionRepositoryFilter<?> filter = context.getBean(SessionRepositoryFilter.class);
				assertThat(filter).extracting("httpSessionIdResolver.cookieSerializer")
					.isSameAs(context.getBean(DefaultCookieSerializer.class));
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

	@Test
	void cookieSerializerCustomization() {
		this.contextRunner.withBean(CookieSerializerCustomization.class).run((context) -> {
			CookieSerializerCustomization customization = context.getBean(CookieSerializerCustomization.class);
			InOrder inOrder = inOrder(customization.customizer1, customization.customizer2);
			inOrder.verify(customization.customizer1).customize(any());
			inOrder.verify(customization.customizer2).customize(any());
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringHttpSession
	static class SessionRepositoryConfiguration {

		@Bean
		MapSessionRepository mySessionRepository() {
			return new MapSessionRepository(Collections.emptyMap());
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringWebSession
	static class ReactiveSessionRepositoryConfiguration {

		@Bean
		ReactiveMapSessionRepository mySessionRepository() {
			return new ReactiveMapSessionRepository(Collections.emptyMap());
		}

	}

	@EnableConfigurationProperties(ServerProperties.class)
	static class ServerPropertiesConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringHttpSession
	static class UserProvidedCookieSerializerConfiguration extends SessionRepositoryConfiguration {

		@Bean
		DefaultCookieSerializer myCookieSerializer() {
			return new DefaultCookieSerializer();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringHttpSession
	static class UserProvidedCookieHttpSessionStrategyConfiguration extends SessionRepositoryConfiguration {

		@Bean
		CookieHttpSessionIdResolver httpSessionStrategy() {
			return new CookieHttpSessionIdResolver();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringHttpSession
	static class UserProvidedHeaderHttpSessionStrategyConfiguration extends SessionRepositoryConfiguration {

		@Bean
		HeaderHttpSessionIdResolver httpSessionStrategy() {
			return HeaderHttpSessionIdResolver.xAuthToken();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringHttpSession
	static class UserProvidedCustomHttpSessionStrategyConfiguration extends SessionRepositoryConfiguration {

		@Bean
		HttpSessionIdResolver httpSessionStrategy() {
			return mock(HttpSessionIdResolver.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringHttpSession
	static class SpringSessionRememberMeServicesConfiguration extends SessionRepositoryConfiguration {

		@Bean
		SpringSessionRememberMeServices rememberMeServices() {
			return new SpringSessionRememberMeServices();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringHttpSession
	static class CookieSerializerCustomization extends SessionRepositoryConfiguration {

		private final DefaultCookieSerializerCustomizer customizer1 = mock(DefaultCookieSerializerCustomizer.class);

		private final DefaultCookieSerializerCustomizer customizer2 = mock(DefaultCookieSerializerCustomizer.class);

		@Bean
		@Order(1)
		DefaultCookieSerializerCustomizer customizer1() {
			return this.customizer1;
		}

		@Bean
		@Order(2)
		DefaultCookieSerializerCustomizer customizer2() {
			return this.customizer2;
		}

	}

}
