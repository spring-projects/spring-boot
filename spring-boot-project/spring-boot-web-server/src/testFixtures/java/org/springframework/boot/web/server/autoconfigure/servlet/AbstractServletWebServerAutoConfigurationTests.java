/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.web.server.autoconfigure.servlet;

import java.io.IOException;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpoint;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.boot.web.server.servlet.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.server.servlet.CookieSameSiteSupplier;
import org.springframework.boot.web.server.servlet.MockServletWebServerFactory;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.boot.web.server.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.ForwardedHeaderFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Base class for testing sub-classes of {@link ServletWebServerConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Raheela Aslam
 * @author Madhura Bhave
 */
public abstract class AbstractServletWebServerAutoConfigurationTests {

	private final WebApplicationContextRunner mockServerRunner;

	protected final WebApplicationContextRunner serverRunner;

	protected AbstractServletWebServerAutoConfigurationTests(Class<?> serverAutoConfiguration) {
		WebApplicationContextRunner common = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
			.withConfiguration(AutoConfigurations.of(serverAutoConfiguration));
		this.serverRunner = common.withPropertyValues("server.port=0");
		this.mockServerRunner = common.withUserConfiguration(MockWebServerConfiguration.class);
	}

	@Test
	void createFromConfigClass() {
		this.mockServerRunner
			.run((context) -> assertThat(context).hasSingleBean(ServletWebServerFactoryCustomizer.class));
	}

	@Test
	void webServerHasNoServletContext() {
		this.mockServerRunner.withUserConfiguration(EnsureWebServerHasNoServletContext.class)
			.run((context) -> assertThat(context).hasNotFailed());
	}

	@Test
	void webServerFactoryCustomizerBeansAreCalledToCustomizeWebServerFactory() {
		this.mockServerRunner
			.withBean(WebServerFactoryCustomizer.class,
					() -> (WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>) (factory) -> factory
						.setPort(9000))
			.run((context) -> assertThat(context.getBean(MockServletWebServerFactory.class).getPort()).isEqualTo(9000));
	}

	@Test
	void initParametersAreConfiguredOnTheServletContext() {
		this.mockServerRunner
			.withPropertyValues("server.servlet.context-parameters.a:alpha",
					"server.servlet.context-parameters.b:bravo")
			.run((context) -> {
				ServletContext servletContext = context.getServletContext();
				assertThat(servletContext.getInitParameter("a")).isEqualTo("alpha");
				assertThat(servletContext.getInitParameter("b")).isEqualTo("bravo");
			});
	}

	@Test
	void forwardedHeaderFilterShouldBeConfigured() {
		this.mockServerRunner.withPropertyValues("server.forward-headers-strategy=framework").run((context) -> {
			assertThat(context).hasSingleBean(FilterRegistrationBean.class);
			Filter filter = context.getBean(FilterRegistrationBean.class).getFilter();
			assertThat(filter).isInstanceOf(ForwardedHeaderFilter.class);
			assertThat(filter).extracting("relativeRedirects").isEqualTo(false);
		});
	}

	@Test
	void forwardedHeaderFilterWhenStrategyNotFilterShouldNotBeConfigured() {
		this.mockServerRunner.withPropertyValues("server.forward-headers-strategy=native")
			.run((context) -> assertThat(context).doesNotHaveBean(FilterRegistrationBean.class));
	}

	@Test
	void forwardedHeaderFilterWhenFilterAlreadyRegisteredShouldBackOff() {
		this.mockServerRunner.withUserConfiguration(ForwardedHeaderFilterConfiguration.class)
			.withPropertyValues("server.forward-headers-strategy=framework")
			.run((context) -> assertThat(context).hasSingleBean(FilterRegistrationBean.class));
	}

	@Test
	void cookieSameSiteSuppliersAreApplied() {
		this.mockServerRunner.withUserConfiguration(CookieSameSiteSupplierConfiguration.class).run((context) -> {
			ConfigurableServletWebServerFactory webServerFactory = context
				.getBean(ConfigurableServletWebServerFactory.class);
			assertThat(webServerFactory.getSettings().getCookieSameSiteSuppliers()).hasSize(2);
		});
	}

	@Test
	void webSocketServerContainerIsAvailableFromServletContext() {
		this.serverRunner.run((context) -> {
			Object serverContainer = context.getServletContext()
				.getAttribute("jakarta.websocket.server.ServerContainer");
			assertThat(serverContainer).isInstanceOf(ServerContainer.class);
		});
	}

	@Test
	void webSocketUpgradeDoesNotPreventAFilterFromRejectingTheRequest() {
		this.serverRunner
			.withBean("testEndpointRegistrar", ServletContextInitializer.class, () -> TestEndpoint::register)
			.withUserConfiguration(UnauthorizedFilterConfiguration.class)
			.run((context) -> {
				TestEndpoint.register(context.getServletContext());
				WebServer webServer = ((WebServerApplicationContext) context.getSourceApplicationContext())
					.getWebServer();
				int port = webServer.getPort();
				RestTemplate rest = new RestTemplate();
				RequestEntity<Void> request = RequestEntity.get("http://localhost:" + port)
					.header("Upgrade", "websocket")
					.header("Connection", "upgrade")
					.header("Sec-WebSocket-Version", "13")
					.header("Sec-WebSocket-Key", "key")
					.build();
				assertThatExceptionOfType(HttpClientErrorException.Unauthorized.class)
					.isThrownBy(() -> rest.exchange(request, Void.class));
			});
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnExpression("true")
	static class MockWebServerConfiguration {

		@Bean
		ServletWebServerFactory webServerFactory() {
			return new MockServletWebServerFactory();
		}

	}

	@Component
	static class EnsureWebServerHasNoServletContext implements BeanPostProcessor {

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			if (bean instanceof ConfigurableServletWebServerFactory) {
				MockServletWebServerFactory webServerFactory = (MockServletWebServerFactory) bean;
				assertThat(webServerFactory.getServletContext()).isNull();
			}
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			return bean;
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ForwardedHeaderFilterConfiguration {

		@Bean
		FilterRegistrationBean<ForwardedHeaderFilter> testForwardedHeaderFilter() {
			ForwardedHeaderFilter filter = new ForwardedHeaderFilter();
			return new FilterRegistrationBean<>(filter);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CookieSameSiteSupplierConfiguration {

		@Bean
		CookieSameSiteSupplier cookieSameSiteSupplier1() {
			return CookieSameSiteSupplier.ofLax().whenHasName("test1");
		}

		@Bean
		CookieSameSiteSupplier cookieSameSiteSupplier2() {
			return CookieSameSiteSupplier.ofNone().whenHasName("test2");
		}

	}

	@ServerEndpoint("/")
	public static class TestEndpoint {

		static void register(ServletContext context) {
			try {
				ServerContainer serverContainer = (ServerContainer) context
					.getAttribute("jakarta.websocket.server.ServerContainer");
				if (serverContainer != null) {
					serverContainer.addEndpoint(TestEndpoint.class);
				}
			}
			catch (Exception ex) {
				// Continue
			}
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class UnauthorizedFilterConfiguration {

		@Bean
		FilterRegistrationBean<Filter> unauthorizedFilter() {
			FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(new Filter() {

				@Override
				public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
						throws IOException, ServletException {
					((HttpServletResponse) response).sendError(HttpStatus.UNAUTHORIZED.value());
				}

			});
			registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
			registration.addUrlPatterns("/*");
			registration.setDispatcherTypes(DispatcherType.REQUEST);
			return registration;
		}

		@Bean
		ServletRegistrationBean<HttpServlet> basicServlet() {
			ServletRegistrationBean<HttpServlet> registration = new ServletRegistrationBean<>(new HttpServlet() {
			});
			registration.addUrlMappings("/");
			return registration;
		}

	}

}
