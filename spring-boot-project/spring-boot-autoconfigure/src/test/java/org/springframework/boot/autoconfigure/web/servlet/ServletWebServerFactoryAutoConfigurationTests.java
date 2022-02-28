/*
 * Copyright 2012-2022 the original author or authors.
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

import io.undertow.Undertow.Builder;
import io.undertow.servlet.api.DeploymentInfo;
import jakarta.servlet.Filter;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.Test;
import reactor.netty.http.server.HttpServer;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowBuilderCustomizer;
import org.springframework.boot.web.embedded.undertow.UndertowDeploymentInfoCustomizer;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.FrameworkServlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ServletWebServerFactoryAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Raheela Aslam
 * @author Madhura Bhave
 */
class ServletWebServerFactoryAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner(
			AnnotationConfigServletWebServerApplicationContext::new)
					.withConfiguration(AutoConfigurations.of(ServletWebServerFactoryAutoConfiguration.class,
							DispatcherServletAutoConfiguration.class))
					.withUserConfiguration(WebServerConfiguration.class);

	@Test
	void createFromConfigClass() {
		this.contextRunner.run(verifyContext());
	}

	@Test
	void contextAlreadyHasDispatcherServletWithDefaultName() {
		this.contextRunner.withUserConfiguration(DispatcherServletConfiguration.class).run(verifyContext());
	}

	@Test
	void contextAlreadyHasDispatcherServlet() {
		this.contextRunner.withUserConfiguration(SpringServletConfiguration.class).run((context) -> {
			verifyContext(context);
			assertThat(context.getBeanNamesForType(DispatcherServlet.class)).hasSize(2);
		});
	}

	@Test
	void contextAlreadyHasNonDispatcherServlet() {
		this.contextRunner.withUserConfiguration(NonSpringServletConfiguration.class).run((context) -> {
			verifyContext(context); // the non default servlet is still registered
			assertThat(context).doesNotHaveBean(DispatcherServlet.class);
		});
	}

	@Test
	void contextAlreadyHasNonServlet() {
		this.contextRunner.withUserConfiguration(NonServletConfiguration.class).run((context) -> {
			assertThat(context).doesNotHaveBean(DispatcherServlet.class);
			assertThat(context).doesNotHaveBean(Servlet.class);
		});
	}

	@Test
	void contextAlreadyHasDispatcherServletAndRegistration() {
		this.contextRunner.withUserConfiguration(DispatcherServletWithRegistrationConfiguration.class)
				.run((context) -> {
					verifyContext(context);
					assertThat(context).hasSingleBean(DispatcherServlet.class);
				});
	}

	@Test
	void webServerHasNoServletContext() {
		this.contextRunner.withUserConfiguration(EnsureWebServerHasNoServletContext.class).run(verifyContext());
	}

	@Test
	void customizeWebServerFactoryThroughCallback() {
		this.contextRunner.withUserConfiguration(CallbackEmbeddedServerFactoryCustomizer.class).run((context) -> {
			verifyContext(context);
			assertThat(context.getBean(MockServletWebServerFactory.class).getPort()).isEqualTo(9000);
		});
	}

	@Test
	void initParametersAreConfiguredOnTheServletContext() {
		this.contextRunner.withPropertyValues("server.servlet.context-parameters.a:alpha",
				"server.servlet.context-parameters.b:bravo").run((context) -> {
					ServletContext servletContext = context.getServletContext();
					assertThat(servletContext.getInitParameter("a")).isEqualTo("alpha");
					assertThat(servletContext.getInitParameter("b")).isEqualTo("bravo");
				});
	}

	@Test
	void jettyServerCustomizerBeanIsAddedToFactory() {
		WebApplicationContextRunner runner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withClassLoader(new FilteredClassLoader(Tomcat.class, HttpServer.class))
						.withConfiguration(AutoConfigurations.of(ServletWebServerFactoryAutoConfiguration.class))
						.withUserConfiguration(JettyServerCustomizerConfiguration.class)
						.withPropertyValues("server.port:0");
		runner.run((context) -> {
			JettyServletWebServerFactory factory = context.getBean(JettyServletWebServerFactory.class);
			assertThat(factory.getServerCustomizers()).hasSize(1);
		});
	}

	@Test
	void jettyServerCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
		WebApplicationContextRunner runner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withClassLoader(new FilteredClassLoader(Tomcat.class, HttpServer.class))
						.withConfiguration(AutoConfigurations.of(ServletWebServerFactoryAutoConfiguration.class))
						.withUserConfiguration(DoubleRegistrationJettyServerCustomizerConfiguration.class)
						.withPropertyValues("server.port: 0");
		runner.run((context) -> {
			JettyServletWebServerFactory factory = context.getBean(JettyServletWebServerFactory.class);
			JettyServerCustomizer customizer = context.getBean("serverCustomizer", JettyServerCustomizer.class);
			assertThat(factory.getServerCustomizers()).contains(customizer);
			then(customizer).should().customize(any(Server.class));
		});
	}

	@Test
	void undertowDeploymentInfoCustomizerBeanIsAddedToFactory() {
		WebApplicationContextRunner runner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withClassLoader(new FilteredClassLoader(Tomcat.class, HttpServer.class, Server.class))
						.withConfiguration(AutoConfigurations.of(ServletWebServerFactoryAutoConfiguration.class))
						.withUserConfiguration(UndertowDeploymentInfoCustomizerConfiguration.class)
						.withPropertyValues("server.port:0");
		runner.run((context) -> {
			UndertowServletWebServerFactory factory = context.getBean(UndertowServletWebServerFactory.class);
			assertThat(factory.getDeploymentInfoCustomizers()).hasSize(1);
		});
	}

	@Test
	void undertowDeploymentInfoCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
		WebApplicationContextRunner runner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withClassLoader(new FilteredClassLoader(Tomcat.class, HttpServer.class, Server.class))
						.withConfiguration(AutoConfigurations.of(ServletWebServerFactoryAutoConfiguration.class))
						.withUserConfiguration(DoubleRegistrationUndertowDeploymentInfoCustomizerConfiguration.class)
						.withPropertyValues("server.port: 0");
		runner.run((context) -> {
			UndertowServletWebServerFactory factory = context.getBean(UndertowServletWebServerFactory.class);
			UndertowDeploymentInfoCustomizer customizer = context.getBean("deploymentInfoCustomizer",
					UndertowDeploymentInfoCustomizer.class);
			assertThat(factory.getDeploymentInfoCustomizers()).contains(customizer);
			then(customizer).should().customize(any(DeploymentInfo.class));
		});
	}

	@Test
	void undertowBuilderCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
		WebApplicationContextRunner runner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withClassLoader(new FilteredClassLoader(Tomcat.class, HttpServer.class, Server.class))
						.withConfiguration(AutoConfigurations.of(ServletWebServerFactoryAutoConfiguration.class))
						.withUserConfiguration(DoubleRegistrationUndertowBuilderCustomizerConfiguration.class)
						.withPropertyValues("server.port: 0");
		runner.run((context) -> {
			UndertowServletWebServerFactory factory = context.getBean(UndertowServletWebServerFactory.class);
			UndertowBuilderCustomizer customizer = context.getBean("builderCustomizer",
					UndertowBuilderCustomizer.class);
			assertThat(factory.getBuilderCustomizers()).contains(customizer);
			then(customizer).should().customize(any(Builder.class));
		});
	}

	@Test
	void undertowBuilderCustomizerBeanIsAddedToFactory() {
		WebApplicationContextRunner runner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withClassLoader(new FilteredClassLoader(Tomcat.class, HttpServer.class, Server.class))
						.withConfiguration(AutoConfigurations.of(ServletWebServerFactoryAutoConfiguration.class))
						.withUserConfiguration(UndertowBuilderCustomizerConfiguration.class)
						.withPropertyValues("server.port:0");
		runner.run((context) -> {
			UndertowServletWebServerFactory factory = context.getBean(UndertowServletWebServerFactory.class);
			assertThat(factory.getBuilderCustomizers()).hasSize(1);
		});
	}

	@Test
	void undertowServletWebServerFactoryCustomizerIsAutoConfigured() {
		WebApplicationContextRunner runner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withClassLoader(new FilteredClassLoader(Tomcat.class, HttpServer.class, Server.class))
						.withConfiguration(AutoConfigurations.of(ServletWebServerFactoryAutoConfiguration.class))
						.withUserConfiguration(UndertowBuilderCustomizerConfiguration.class)
						.withPropertyValues("server.port:0");
		runner.run((context) -> assertThat(context).hasSingleBean(UndertowServletWebServerFactoryCustomizer.class));
	}

	@Test
	void tomcatConnectorCustomizerBeanIsAddedToFactory() {
		WebApplicationContextRunner runner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withConfiguration(AutoConfigurations.of(ServletWebServerFactoryAutoConfiguration.class))
						.withUserConfiguration(TomcatConnectorCustomizerConfiguration.class)
						.withPropertyValues("server.port: 0");
		runner.run((context) -> {
			TomcatServletWebServerFactory factory = context.getBean(TomcatServletWebServerFactory.class);
			TomcatConnectorCustomizer customizer = context.getBean("connectorCustomizer",
					TomcatConnectorCustomizer.class);
			assertThat(factory.getTomcatConnectorCustomizers()).contains(customizer);
			then(customizer).should().customize(any(Connector.class));
		});
	}

	@Test
	void tomcatConnectorCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
		WebApplicationContextRunner runner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withConfiguration(AutoConfigurations.of(ServletWebServerFactoryAutoConfiguration.class))
						.withUserConfiguration(DoubleRegistrationTomcatConnectorCustomizerConfiguration.class)
						.withPropertyValues("server.port: 0");
		runner.run((context) -> {
			TomcatServletWebServerFactory factory = context.getBean(TomcatServletWebServerFactory.class);
			TomcatConnectorCustomizer customizer = context.getBean("connectorCustomizer",
					TomcatConnectorCustomizer.class);
			assertThat(factory.getTomcatConnectorCustomizers()).contains(customizer);
			then(customizer).should().customize(any(Connector.class));
		});
	}

	@Test
	void tomcatContextCustomizerBeanIsAddedToFactory() {
		WebApplicationContextRunner runner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withConfiguration(AutoConfigurations.of(ServletWebServerFactoryAutoConfiguration.class))
						.withUserConfiguration(TomcatContextCustomizerConfiguration.class)
						.withPropertyValues("server.port: 0");
		runner.run((context) -> {
			TomcatServletWebServerFactory factory = context.getBean(TomcatServletWebServerFactory.class);
			TomcatContextCustomizer customizer = context.getBean("contextCustomizer", TomcatContextCustomizer.class);
			assertThat(factory.getTomcatContextCustomizers()).contains(customizer);
			then(customizer).should().customize(any(Context.class));
		});
	}

	@Test
	void tomcatContextCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
		WebApplicationContextRunner runner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withConfiguration(AutoConfigurations.of(ServletWebServerFactoryAutoConfiguration.class))
						.withUserConfiguration(DoubleRegistrationTomcatContextCustomizerConfiguration.class)
						.withPropertyValues("server.port: 0");
		runner.run((context) -> {
			TomcatServletWebServerFactory factory = context.getBean(TomcatServletWebServerFactory.class);
			TomcatContextCustomizer customizer = context.getBean("contextCustomizer", TomcatContextCustomizer.class);
			assertThat(factory.getTomcatContextCustomizers()).contains(customizer);
			then(customizer).should().customize(any(Context.class));
		});
	}

	@Test
	void tomcatProtocolHandlerCustomizerBeanIsAddedToFactory() {
		WebApplicationContextRunner runner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withConfiguration(AutoConfigurations.of(ServletWebServerFactoryAutoConfiguration.class))
						.withUserConfiguration(TomcatProtocolHandlerCustomizerConfiguration.class)
						.withPropertyValues("server.port: 0");
		runner.run((context) -> {
			TomcatServletWebServerFactory factory = context.getBean(TomcatServletWebServerFactory.class);
			TomcatProtocolHandlerCustomizer<?> customizer = context.getBean("protocolHandlerCustomizer",
					TomcatProtocolHandlerCustomizer.class);
			assertThat(factory.getTomcatProtocolHandlerCustomizers()).contains(customizer);
			then(customizer).should().customize(any());
		});
	}

	@Test
	void tomcatProtocolHandlerCustomizerRegisteredAsBeanAndViaFactoryIsOnlyCalledOnce() {
		WebApplicationContextRunner runner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withConfiguration(AutoConfigurations.of(ServletWebServerFactoryAutoConfiguration.class))
						.withUserConfiguration(DoubleRegistrationTomcatProtocolHandlerCustomizerConfiguration.class)
						.withPropertyValues("server.port: 0");
		runner.run((context) -> {
			TomcatServletWebServerFactory factory = context.getBean(TomcatServletWebServerFactory.class);
			TomcatProtocolHandlerCustomizer<?> customizer = context.getBean("protocolHandlerCustomizer",
					TomcatProtocolHandlerCustomizer.class);
			assertThat(factory.getTomcatProtocolHandlerCustomizers()).contains(customizer);
			then(customizer).should().customize(any());
		});
	}

	@Test
	void forwardedHeaderFilterShouldBeConfigured() {
		this.contextRunner.withPropertyValues("server.forward-headers-strategy=framework").run((context) -> {
			assertThat(context).hasSingleBean(FilterRegistrationBean.class);
			Filter filter = context.getBean(FilterRegistrationBean.class).getFilter();
			assertThat(filter).isInstanceOf(ForwardedHeaderFilter.class);
			assertThat(filter).extracting("relativeRedirects").isEqualTo(false);
		});
	}

	@Test
	void forwardedHeaderFilterWhenStrategyNotFilterShouldNotBeConfigured() {
		this.contextRunner.withPropertyValues("server.forward-headers-strategy=native")
				.run((context) -> assertThat(context).doesNotHaveBean(FilterRegistrationBean.class));
	}

	@Test
	void forwardedHeaderFilterWhenFilterAlreadyRegisteredShouldBackOff() {
		this.contextRunner.withUserConfiguration(ForwardedHeaderFilterConfiguration.class)
				.withPropertyValues("server.forward-headers-strategy=framework")
				.run((context) -> assertThat(context).hasSingleBean(FilterRegistrationBean.class));
	}

	@Test
	void cookieSameSiteSuppliersAreApplied() {
		this.contextRunner.withUserConfiguration(CookieSameSiteSupplierConfiguration.class).run((context) -> {
			AbstractServletWebServerFactory webServerFactory = context.getBean(AbstractServletWebServerFactory.class);
			assertThat(webServerFactory.getCookieSameSiteSuppliers()).hasSize(2);
		});
	}

	@Test
	void relativeRedirectsShouldBeEnabledWhenUsingTomcatContainerAndUseRelativeRedirects() {
		WebApplicationContextRunner runner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withConfiguration(AutoConfigurations.of(ServletWebServerFactoryAutoConfiguration.class))
						.withPropertyValues("server.forward-headers-strategy=framework",
								"server.tomcat.use-relative-redirects=true", "server.port=0");
		runner.run((context) -> {
			Filter filter = context.getBean(FilterRegistrationBean.class).getFilter();
			assertThat(filter).isInstanceOf(ForwardedHeaderFilter.class);
			assertThat(filter).extracting("relativeRedirects").isEqualTo(true);
		});
	}

	@Test
	void relativeRedirectsShouldNotBeEnabledWhenUsingTomcatContainerAndNotUsingRelativeRedirects() {
		WebApplicationContextRunner runner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withConfiguration(AutoConfigurations.of(ServletWebServerFactoryAutoConfiguration.class))
						.withPropertyValues("server.forward-headers-strategy=framework",
								"server.tomcat.use-relative-redirects=false", "server.port=0");
		runner.run((context) -> {
			Filter filter = context.getBean(FilterRegistrationBean.class).getFilter();
			assertThat(filter).isInstanceOf(ForwardedHeaderFilter.class);
			assertThat(filter).extracting("relativeRedirects").isEqualTo(false);
		});
	}

	@Test
	void relativeRedirectsShouldNotBeEnabledWhenNotUsingTomcatContainer() {
		WebApplicationContextRunner runner = new WebApplicationContextRunner(
				AnnotationConfigServletWebServerApplicationContext::new)
						.withClassLoader(new FilteredClassLoader(Tomcat.class))
						.withConfiguration(AutoConfigurations.of(ServletWebServerFactoryAutoConfiguration.class))
						.withPropertyValues("server.forward-headers-strategy=framework", "server.port=0");
		runner.run((context) -> {
			Filter filter = context.getBean(FilterRegistrationBean.class).getFilter();
			assertThat(filter).isInstanceOf(ForwardedHeaderFilter.class);
			assertThat(filter).extracting("relativeRedirects").isEqualTo(false);
		});
	}

	private ContextConsumer<AssertableWebApplicationContext> verifyContext() {
		return this::verifyContext;
	}

	private void verifyContext(ApplicationContext context) {
		MockServletWebServerFactory factory = context.getBean(MockServletWebServerFactory.class);
		Servlet servlet = context.getBean(DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME,
				Servlet.class);
		then(factory.getServletContext()).should().addServlet("dispatcherServlet", servlet);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnExpression("true")
	static class WebServerConfiguration {

		@Bean
		ServletWebServerFactory webServerFactory() {
			return new MockServletWebServerFactory();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DispatcherServletConfiguration {

		@Bean
		DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class SpringServletConfiguration {

		@Bean
		DispatcherServlet springServlet() {
			return new DispatcherServlet();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NonSpringServletConfiguration {

		@Bean
		FrameworkServlet dispatcherServlet() {
			return new FrameworkServlet() {
				@Override
				protected void doService(HttpServletRequest request, HttpServletResponse response) {
				}
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class NonServletConfiguration {

		@Bean
		String dispatcherServlet() {
			return "foo";
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DispatcherServletWithRegistrationConfiguration {

		@Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
		DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

		@Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)
		ServletRegistrationBean<DispatcherServlet> dispatcherRegistration(DispatcherServlet dispatcherServlet) {
			return new ServletRegistrationBean<>(dispatcherServlet, "/app/*");
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

	@Component
	static class CallbackEmbeddedServerFactoryCustomizer
			implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

		@Override
		public void customize(ConfigurableServletWebServerFactory serverFactory) {
			serverFactory.setPort(9000);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TomcatConnectorCustomizerConfiguration {

		@Bean
		TomcatConnectorCustomizer connectorCustomizer() {
			return mock(TomcatConnectorCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DoubleRegistrationTomcatConnectorCustomizerConfiguration {

		private final TomcatConnectorCustomizer customizer = mock(TomcatConnectorCustomizer.class);

		@Bean
		TomcatConnectorCustomizer connectorCustomizer() {
			return this.customizer;
		}

		@Bean
		WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
			return (tomcat) -> tomcat.addConnectorCustomizers(this.customizer);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TomcatContextCustomizerConfiguration {

		@Bean
		TomcatContextCustomizer contextCustomizer() {
			return mock(TomcatContextCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DoubleRegistrationTomcatContextCustomizerConfiguration {

		private final TomcatContextCustomizer customizer = mock(TomcatContextCustomizer.class);

		@Bean
		TomcatContextCustomizer contextCustomizer() {
			return this.customizer;
		}

		@Bean
		WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
			return (tomcat) -> tomcat.addContextCustomizers(this.customizer);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class TomcatProtocolHandlerCustomizerConfiguration {

		@Bean
		TomcatProtocolHandlerCustomizer<?> protocolHandlerCustomizer() {
			return mock(TomcatProtocolHandlerCustomizer.class);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DoubleRegistrationTomcatProtocolHandlerCustomizerConfiguration {

		private final TomcatProtocolHandlerCustomizer<?> customizer = mock(TomcatProtocolHandlerCustomizer.class);

		@Bean
		TomcatProtocolHandlerCustomizer<?> protocolHandlerCustomizer() {
			return this.customizer;
		}

		@Bean
		WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
			return (tomcat) -> tomcat.addProtocolHandlerCustomizers(this.customizer);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class JettyServerCustomizerConfiguration {

		@Bean
		JettyServerCustomizer serverCustomizer() {
			return (server) -> {
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DoubleRegistrationJettyServerCustomizerConfiguration {

		private final JettyServerCustomizer customizer = mock(JettyServerCustomizer.class);

		@Bean
		JettyServerCustomizer serverCustomizer() {
			return this.customizer;
		}

		@Bean
		WebServerFactoryCustomizer<JettyServletWebServerFactory> jettyCustomizer() {
			return (jetty) -> jetty.addServerCustomizers(this.customizer);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class UndertowBuilderCustomizerConfiguration {

		@Bean
		UndertowBuilderCustomizer builderCustomizer() {
			return (builder) -> {
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DoubleRegistrationUndertowBuilderCustomizerConfiguration {

		private final UndertowBuilderCustomizer customizer = mock(UndertowBuilderCustomizer.class);

		@Bean
		UndertowBuilderCustomizer builderCustomizer() {
			return this.customizer;
		}

		@Bean
		WebServerFactoryCustomizer<UndertowServletWebServerFactory> undertowCustomizer() {
			return (undertow) -> undertow.addBuilderCustomizers(this.customizer);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class UndertowDeploymentInfoCustomizerConfiguration {

		@Bean
		UndertowDeploymentInfoCustomizer deploymentInfoCustomizer() {
			return (deploymentInfo) -> {
			};
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class DoubleRegistrationUndertowDeploymentInfoCustomizerConfiguration {

		private final UndertowDeploymentInfoCustomizer customizer = mock(UndertowDeploymentInfoCustomizer.class);

		@Bean
		UndertowDeploymentInfoCustomizer deploymentInfoCustomizer() {
			return this.customizer;
		}

		@Bean
		WebServerFactoryCustomizer<UndertowServletWebServerFactory> undertowCustomizer() {
			return (undertow) -> undertow.addDeploymentInfoCustomizers(this.customizer);
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

}
