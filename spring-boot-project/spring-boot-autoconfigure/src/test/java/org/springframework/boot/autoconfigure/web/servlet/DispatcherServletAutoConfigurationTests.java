/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DispatcherServletAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Brian Clozel
 */
class DispatcherServletAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(DispatcherServletAutoConfiguration.class));

	@Test
	void registrationProperties() {
		this.contextRunner.run((context) -> {
			assertThat(context.getBean(DispatcherServlet.class)).isNotNull();
			ServletRegistrationBean<?> registration = context.getBean(ServletRegistrationBean.class);
			assertThat(registration.getUrlMappings()).containsExactly("/");
		});
	}

	@Test
	void registrationNonServletBean() {
		this.contextRunner.withUserConfiguration(NonServletConfiguration.class).run((context) -> {
			assertThat(context).doesNotHaveBean(ServletRegistrationBean.class);
			assertThat(context).doesNotHaveBean(DispatcherServlet.class);
			assertThat(context).doesNotHaveBean(DispatcherServletPath.class);
		});
	}

	// If a DispatcherServlet instance is registered with a name different
	// from the default one, we're registering one anyway
	@Test
	void registrationOverrideWithDispatcherServletWrongName() {
		this.contextRunner
				.withUserConfiguration(CustomDispatcherServletDifferentName.class, CustomDispatcherServletPath.class)
				.run((context) -> {
					ServletRegistrationBean<?> registration = context.getBean(ServletRegistrationBean.class);
					assertThat(registration.getUrlMappings()).containsExactly("/");
					assertThat(registration.getServletName()).isEqualTo("dispatcherServlet");
					assertThat(context).getBeanNames(DispatcherServlet.class).hasSize(2);
				});
	}

	@Test
	void registrationOverrideWithAutowiredServlet() {
		this.contextRunner.withUserConfiguration(CustomAutowiredRegistration.class).run((context) -> {
			ServletRegistrationBean<?> registration = context.getBean(ServletRegistrationBean.class);
			assertThat(registration.getUrlMappings()).containsExactly("/foo");
			assertThat(registration.getServletName()).isEqualTo("customDispatcher");
			assertThat(context).hasSingleBean(DispatcherServlet.class);
		});
	}

	@Test
	void servletPath() {
		this.contextRunner.withPropertyValues("spring.mvc.servlet.path:/spring").run((context) -> {
			assertThat(context.getBean(DispatcherServlet.class)).isNotNull();
			ServletRegistrationBean<?> registration = context.getBean(ServletRegistrationBean.class);
			assertThat(registration.getUrlMappings()).containsExactly("/spring/*");
			assertThat(registration.getMultipartConfig()).isNull();
			assertThat(context.getBean(DispatcherServletPath.class).getPath()).isEqualTo("/spring");
		});
	}

	@Test
	void dispatcherServletPathWhenCustomDispatcherServletSameNameShouldReturnConfiguredServletPath() {
		this.contextRunner.withUserConfiguration(CustomDispatcherServletSameName.class)
				.withPropertyValues("spring.mvc.servlet.path:/spring")
				.run((context) -> assertThat(context.getBean(DispatcherServletPath.class).getPath())
						.isEqualTo("/spring"));
	}

	@Test
	void dispatcherServletPathNotCreatedWhenDefaultDispatcherServletNotAvailable() {
		this.contextRunner
				.withUserConfiguration(CustomDispatcherServletDifferentName.class, NonServletConfiguration.class)
				.run((context) -> assertThat(context).doesNotHaveBean(DispatcherServletPath.class));
	}

	@Test
	void dispatcherServletPathNotCreatedWhenCustomRegistrationBeanPresent() {
		this.contextRunner.withUserConfiguration(CustomDispatcherServletRegistration.class)
				.run((context) -> assertThat(context).doesNotHaveBean(DispatcherServletPath.class));
	}

	@Test
	void multipartConfig() {
		this.contextRunner.withUserConfiguration(MultipartConfiguration.class).run((context) -> {
			ServletRegistrationBean<?> registration = context.getBean(ServletRegistrationBean.class);
			assertThat(registration.getMultipartConfig()).isNotNull();
		});
	}

	@Test
	void renamesMultipartResolver() {
		this.contextRunner.withUserConfiguration(MultipartResolverConfiguration.class).run((context) -> {
			DispatcherServlet dispatcherServlet = context.getBean(DispatcherServlet.class);
			dispatcherServlet.onApplicationEvent(new ContextRefreshedEvent(context));
			assertThat(dispatcherServlet.getMultipartResolver()).isInstanceOf(MockMultipartResolver.class);
		});
	}

	@Test
	void dispatcherServletDefaultConfig() {
		this.contextRunner.run((context) -> {
			DispatcherServlet dispatcherServlet = context.getBean(DispatcherServlet.class);
			assertThat(dispatcherServlet).extracting("throwExceptionIfNoHandlerFound").isEqualTo(false);
			assertThat(dispatcherServlet).extracting("dispatchOptionsRequest").isEqualTo(true);
			assertThat(dispatcherServlet).extracting("dispatchTraceRequest").isEqualTo(false);
			assertThat(dispatcherServlet).extracting("enableLoggingRequestDetails").isEqualTo(false);
			assertThat(dispatcherServlet).extracting("publishEvents").isEqualTo(true);
			assertThat(context.getBean("dispatcherServletRegistration")).hasFieldOrPropertyWithValue("loadOnStartup",
					-1);
		});
	}

	@Test
	void dispatcherServletCustomConfig() {
		this.contextRunner
				.withPropertyValues("spring.mvc.throw-exception-if-no-handler-found:true",
						"spring.mvc.dispatch-options-request:false", "spring.mvc.dispatch-trace-request:true",
						"spring.mvc.publish-request-handled-events:false", "spring.mvc.servlet.load-on-startup=5")
				.run((context) -> {
					DispatcherServlet dispatcherServlet = context.getBean(DispatcherServlet.class);
					assertThat(dispatcherServlet).extracting("throwExceptionIfNoHandlerFound").isEqualTo(true);
					assertThat(dispatcherServlet).extracting("dispatchOptionsRequest").isEqualTo(false);
					assertThat(dispatcherServlet).extracting("dispatchTraceRequest").isEqualTo(true);
					assertThat(dispatcherServlet).extracting("publishEvents").isEqualTo(false);
					assertThat(context.getBean("dispatcherServletRegistration"))
							.hasFieldOrPropertyWithValue("loadOnStartup", 5);
				});
	}

	@Configuration(proxyBeanMethods = false)
	static class MultipartConfiguration {

		@Bean
		MultipartConfigElement multipartConfig() {
			MultipartConfigFactory factory = new MultipartConfigFactory();
			factory.setMaxFileSize(DataSize.ofKilobytes(128));
			factory.setMaxRequestSize(DataSize.ofKilobytes(128));
			return factory.createMultipartConfig();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomDispatcherServletDifferentName {

		@Bean
		DispatcherServlet customDispatcherServlet() {
			return new DispatcherServlet();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomDispatcherServletPath {

		@Bean
		DispatcherServletPath dispatcherServletPath() {
			return mock(DispatcherServletPath.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomAutowiredRegistration {

		@Bean
		ServletRegistrationBean<?> dispatcherServletRegistration(DispatcherServlet dispatcherServlet) {
			ServletRegistrationBean<DispatcherServlet> registration = new ServletRegistrationBean<>(dispatcherServlet,
					"/foo");
			registration.setName("customDispatcher");
			return registration;
		}

		@Bean
		DispatcherServletPath dispatcherServletPath() {
			return mock(DispatcherServletPath.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NonServletConfiguration {

		@Bean
		String dispatcherServlet() {
			return "spring";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class MultipartResolverConfiguration {

		@Bean
		MultipartResolver getMultipartResolver() {
			return new MockMultipartResolver();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomDispatcherServletSameName {

		@Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
		DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomDispatcherServletRegistration {

		@Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)
		ServletRegistrationBean<DispatcherServlet> dispatcherServletRegistration(DispatcherServlet dispatcherServlet) {
			ServletRegistrationBean<DispatcherServlet> registration = new ServletRegistrationBean<>(dispatcherServlet,
					"/foo");
			registration.setName("customDispatcher");
			return registration;
		}

	}

	static class MockMultipartResolver implements MultipartResolver {

		@Override
		public boolean isMultipart(HttpServletRequest request) {
			return false;
		}

		@Override
		public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException {
			return null;
		}

		@Override
		public void cleanupMultipart(MultipartHttpServletRequest request) {
		}

	}

}
