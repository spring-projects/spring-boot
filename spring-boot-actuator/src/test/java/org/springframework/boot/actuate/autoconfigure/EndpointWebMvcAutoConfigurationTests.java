/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.io.FileNotFoundException;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Valve;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMappingCustomizer;
import org.springframework.boot.actuate.endpoint.mvc.EnvironmentMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.HealthMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.LoggersMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MetricsMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.ShutdownMvcEndpoint;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.testsupport.assertj.Matched;
import org.springframework.boot.web.context.ServerPortInfoApplicationContextInitializer;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EndpointWebMvcAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Greg Turnquist
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Ben Hale
 */
public class EndpointWebMvcAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final AnnotationConfigServletWebServerApplicationContext applicationContext = new AnnotationConfigServletWebServerApplicationContext();

	private static ThreadLocal<Ports> ports = new ThreadLocal<>();

	@Before
	public void setUp() {
		Ports values = new Ports();
		ports.set(values);
		TestPropertyValues
				.of("management.security.enabled=false", "server.servlet.context-path=",
						"server.port=" + ports.get().server)
				.applyTo(this.applicationContext);
	}

	@After
	public void cleanUp() throws Exception {
		this.applicationContext.close();
	}

	@Test
	public void onSamePort() throws Exception {
		TestPropertyValues.of("management.security.enabled=false")
				.applyTo(this.applicationContext);
		this.applicationContext.register(RootConfig.class, EndpointConfig.class,
				BaseConfiguration.class, EndpointWebMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/controller", ports.get().server, "controlleroutput");
		assertContent("/application/endpoint", ports.get().server, "endpointoutput");
		assertThat(hasHeader("/application/endpoint", ports.get().server,
				"X-Application-Context")).isFalse();
		assertThat(this.applicationContext.containsBean("applicationContextIdFilter"))
				.isFalse();
	}

	@Test
	public void onSamePortWithHeader() throws Exception {
		TestPropertyValues.of("management.add-application-context-header:true")
				.applyTo(this.applicationContext);
		this.applicationContext.register(RootConfig.class, EndpointConfig.class,
				BaseConfiguration.class, EndpointWebMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertThat(hasHeader("/endpoint", ports.get().server, "X-Application-Context"))
				.isTrue();
		assertThat(this.applicationContext.containsBean("applicationContextIdFilter"))
				.isTrue();
	}

	@Test
	public void onDifferentPort() throws Exception {
		TestPropertyValues.of("management.port=" + ports.get().management)
				.applyTo(this.applicationContext);
		this.applicationContext.register(RootConfig.class, EndpointConfig.class,
				DifferentPortConfig.class, BaseConfiguration.class,
				EndpointWebMvcAutoConfiguration.class, ErrorMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/controller", ports.get().server, "controlleroutput");
		assertContent("/endpoint", ports.get().server, null);
		assertContent("/controller", ports.get().management, null);
		assertContent("/application/endpoint", ports.get().management, "endpointoutput");
		assertContent("/error", ports.get().management, startsWith("{"));
		ApplicationContext managementContext = this.applicationContext
				.getBean(ManagementContextResolver.class).getApplicationContext();
		List<?> interceptors = (List<?>) ReflectionTestUtils.getField(
				managementContext.getBean(EndpointHandlerMapping.class), "interceptors");
		assertThat(interceptors).hasSize(2);
	}

	@Test
	public void onDifferentPortAndRootContext() throws Exception {
		TestPropertyValues.of("management.port=" + ports.get().management,
				"management.context-path=/").applyTo(this.applicationContext);
		this.applicationContext.register(RootConfig.class, EndpointConfig.class,
				DifferentPortConfig.class, BaseConfiguration.class,
				EndpointWebMvcAutoConfiguration.class, ErrorMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/controller", ports.get().server, "controlleroutput");
		assertContent("/endpoint", ports.get().server, null);
		assertContent("/controller", ports.get().management, null);
		assertContent("/endpoint", ports.get().management, "endpointoutput");
		assertContent("/error", ports.get().management, startsWith("{"));
		ApplicationContext managementContext = this.applicationContext
				.getBean(ManagementContextResolver.class).getApplicationContext();
		List<?> interceptors = (List<?>) ReflectionTestUtils.getField(
				managementContext.getBean(EndpointHandlerMapping.class), "interceptors");
		assertThat(interceptors).hasSize(2);
	}

	@Test
	public void onDifferentPortWithSpecificServer() throws Exception {
		TestPropertyValues.of("management.port=" + ports.get().management)
				.applyTo(this.applicationContext);
		this.applicationContext.register(SpecificWebServerConfig.class, RootConfig.class,
				DifferentPortConfig.class, EndpointConfig.class, BaseConfiguration.class,
				EndpointWebMvcAutoConfiguration.class, ErrorMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/controller", ports.get().server, "controlleroutput");
		assertContent("/endpoint", ports.get().server, null);
		assertContent("/controller", ports.get().management, null);
		assertContent("/application/endpoint", ports.get().management, "endpointoutput");
		assertContent("/error", ports.get().management, startsWith("{"));
		ApplicationContext managementContext = this.applicationContext
				.getBean(ManagementContextResolver.class).getApplicationContext();
		List<?> interceptors = (List<?>) ReflectionTestUtils.getField(
				managementContext.getBean(EndpointHandlerMapping.class), "interceptors");
		assertThat(interceptors).hasSize(2);
		ServletWebServerFactory parentFactory = this.applicationContext
				.getBean(ServletWebServerFactory.class);
		ServletWebServerFactory managementFactory = managementContext
				.getBean(ServletWebServerFactory.class);
		assertThat(parentFactory).isInstanceOf(SpecificServletWebServerFactory.class);
		assertThat(managementFactory).isInstanceOf(SpecificServletWebServerFactory.class);
		assertThat(managementFactory).isNotSameAs(parentFactory);
	}

	@Test
	public void onDifferentPortAndContext() throws Exception {
		TestPropertyValues
				.of("management.port=" + ports.get().management,
						"management.context-path=/admin")
				.applyTo(this.applicationContext);
		this.applicationContext.register(RootConfig.class, EndpointConfig.class,
				DifferentPortConfig.class, BaseConfiguration.class,
				EndpointWebMvcAutoConfiguration.class, ErrorMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/controller", ports.get().server, "controlleroutput");
		assertContent("/admin/endpoint", ports.get().management, "endpointoutput");
		assertContent("/error", ports.get().management, startsWith("{"));
	}

	@Test
	public void onDifferentPortAndMainContext() throws Exception {
		TestPropertyValues
				.of("server.servlet.context-path=/spring",
						"management.port=" + ports.get().management,
						"management.context-path=/admin")
				.applyTo(this.applicationContext);
		this.applicationContext.register(RootConfig.class, EndpointConfig.class,
				DifferentPortConfig.class, BaseConfiguration.class,
				EndpointWebMvcAutoConfiguration.class, ErrorMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/spring/controller", ports.get().server, "controlleroutput");
		assertContent("/admin/endpoint", ports.get().management, "endpointoutput");
		assertContent("/error", ports.get().management, startsWith("{"));
	}

	@Test
	public void onDifferentPortWithoutErrorMvcAutoConfiguration() throws Exception {
		TestPropertyValues.of("management.port=" + ports.get().management)
				.applyTo(this.applicationContext);
		this.applicationContext.register(RootConfig.class, EndpointConfig.class,
				DifferentPortConfig.class, BaseConfiguration.class,
				EndpointWebMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/error", ports.get().management, null);
	}

	@Test
	public void onDifferentPortInWebServer() throws Exception {
		TestPropertyValues.of("management.port=" + ports.get().management)
				.applyTo(this.applicationContext);
		this.applicationContext.register(RootConfig.class, EndpointConfig.class,
				DifferentPortConfig.class, BaseConfiguration.class,
				EndpointWebMvcAutoConfiguration.class, ErrorMvcAutoConfiguration.class);
		ServletContext servletContext = mock(ServletContext.class);
		given(servletContext.getInitParameterNames())
				.willReturn(new Vector<String>().elements());
		given(servletContext.getAttributeNames())
				.willReturn(new Vector<String>().elements());
		this.applicationContext.setServletContext(servletContext);
		this.applicationContext.refresh();
		assertContent("/controller", ports.get().management, null);
		assertContent("/endpoint", ports.get().management, null);
	}

	@Test
	public void onDifferentPortWithPrimaryFailure() throws Exception {
		TestPropertyValues.of("management.port=" + ports.get().management)
				.applyTo(this.applicationContext);
		this.applicationContext.register(RootConfig.class, EndpointConfig.class,
				DifferentPortConfig.class, BaseConfiguration.class,
				EndpointWebMvcAutoConfiguration.class, ErrorMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		ApplicationContext managementContext = this.applicationContext
				.getBean(ManagementContextResolver.class).getApplicationContext();
		ApplicationFailedEvent event = mock(ApplicationFailedEvent.class);
		given(event.getApplicationContext()).willReturn(this.applicationContext);
		this.applicationContext.publishEvent(event);
		assertThat(((ConfigurableApplicationContext) managementContext).isActive())
				.isFalse();
	}

	@Test
	public void disabled() throws Exception {
		TestPropertyValues.of("management.port=-1").applyTo(this.applicationContext);
		this.applicationContext.register(RootConfig.class, EndpointConfig.class,
				BaseConfiguration.class, EndpointWebMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/controller", ports.get().server, "controlleroutput");
		assertContent("/endpoint", ports.get().server, null);
		assertContent("/controller", ports.get().management, null);
		assertContent("/endpoint", ports.get().management, null);
	}

	@Test
	public void specificPortsViaProperties() throws Exception {
		TestPropertyValues
				.of("server.port:" + ports.get().server,
						"management.port:" + ports.get().management,
						"management.security.enabled:false")
				.applyTo(this.applicationContext);
		this.applicationContext.register(RootConfig.class, EndpointConfig.class,
				BaseConfiguration.class, EndpointWebMvcAutoConfiguration.class,
				ErrorMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/controller", ports.get().server, "controlleroutput");
		assertContent("/endpoint", ports.get().server, null);
		assertContent("/controller", ports.get().management, null);
		assertContent("/application/endpoint", ports.get().management, "endpointoutput");
	}

	@Test
	public void managementContextFailureCausesMainContextFailure() throws Exception {
		TestPropertyValues
				.of("server.port:" + ports.get().server,
						"management.port:" + ports.get().management)
				.applyTo(this.applicationContext);
		this.applicationContext.register(RootConfig.class, EndpointConfig.class,
				BaseConfiguration.class, EndpointWebMvcAutoConfiguration.class,
				ErrorMvcAutoConfiguration.class);
		this.applicationContext.addApplicationListener(
				(ApplicationListener<ContextRefreshedEvent>) (event) -> {
					if (event.getApplicationContext().getParent() != null) {
						throw new RuntimeException();
					}
				});
		this.thrown.expect(RuntimeException.class);
		this.applicationContext.refresh();
	}

	@Test
	public void contextPath() throws Exception {
		TestPropertyValues
				.of("management.context-path:/test", "management.security.enabled:false")
				.applyTo(this.applicationContext);
		this.applicationContext.register(RootConfig.class, EndpointConfig.class,
				PropertyPlaceholderAutoConfiguration.class,
				JacksonAutoConfiguration.class,
				ServletWebServerFactoryAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
				EndpointWebMvcAutoConfiguration.class, AuditAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/controller", ports.get().server, "controlleroutput");
		assertContent("/test/endpoint", ports.get().server, "endpointoutput");
	}

	@Test
	public void overrideServerProperties() throws Exception {
		TestPropertyValues.of("server.displayName:foo").applyTo(this.applicationContext);
		this.applicationContext.register(RootConfig.class, EndpointConfig.class,
				PropertyPlaceholderAutoConfiguration.class,
				JacksonAutoConfiguration.class,
				ServletWebServerFactoryAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
				EndpointWebMvcAutoConfiguration.class, AuditAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/controller", ports.get().server, "controlleroutput");
		ServerProperties serverProperties = this.applicationContext
				.getBean(ServerProperties.class);
		assertThat(serverProperties.getDisplayName()).isEqualTo("foo");
	}

	@Test
	public void portPropertiesOnSamePort() throws Exception {
		this.applicationContext.register(RootConfig.class, BaseConfiguration.class,
				EndpointWebMvcAutoConfiguration.class);
		new ServerPortInfoApplicationContextInitializer()
				.initialize(this.applicationContext);
		this.applicationContext.refresh();
		Integer localServerPort = this.applicationContext.getEnvironment()
				.getProperty("local.server.port", Integer.class);
		Integer localManagementPort = this.applicationContext.getEnvironment()
				.getProperty("local.management.port", Integer.class);
		assertThat(localServerPort).isNotNull();
		assertThat(localManagementPort).isNotNull();
		assertThat(localServerPort).isEqualTo(localManagementPort);
	}

	@Test
	public void portPropertiesOnDifferentPort() throws Exception {
		TestPropertyValues.of("management.port=" + ports.get().management)
				.applyTo(this.applicationContext);
		new ServerPortInfoApplicationContextInitializer()
				.initialize(this.applicationContext);
		this.applicationContext.register(RootConfig.class, DifferentPortConfig.class,
				BaseConfiguration.class, EndpointWebMvcAutoConfiguration.class,
				ErrorMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		Integer localServerPort = this.applicationContext.getEnvironment()
				.getProperty("local.server.port", Integer.class);
		Integer localManagementPort = this.applicationContext.getEnvironment()
				.getProperty("local.management.port", Integer.class);
		assertThat(localServerPort).isNotNull();
		assertThat(localManagementPort).isNotNull();
		assertThat(localServerPort).isNotEqualTo(localManagementPort);
	}

	@Test
	public void singleRequestMappingInfoHandlerMappingBean() throws Exception {
		this.applicationContext.register(RootConfig.class, BaseConfiguration.class,
				EndpointWebMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		RequestMappingInfoHandlerMapping mapping = this.applicationContext
				.getBean(RequestMappingInfoHandlerMapping.class);
		assertThat(mapping).isNotEqualTo(instanceOf(EndpointHandlerMapping.class));
	}

	@Test
	public void endpointsDefaultConfiguration() throws Exception {
		this.applicationContext.register(LoggingConfig.class, RootConfig.class,
				BaseConfiguration.class, EndpointWebMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		// /health, /metrics, /loggers, /env, /actuator, /heapdump, /auditevents
		// (/shutdown is disabled by default)
		assertThat(this.applicationContext.getBeansOfType(MvcEndpoint.class)).hasSize(6);
	}

	@Test
	public void endpointsAllDisabled() throws Exception {
		this.applicationContext.register(RootConfig.class, BaseConfiguration.class,
				EndpointWebMvcAutoConfiguration.class);
		TestPropertyValues.of("endpoints.enabled:false").applyTo(this.applicationContext);
		this.applicationContext.refresh();
		assertThat(this.applicationContext.getBeansOfType(MvcEndpoint.class)).isEmpty();
	}

	@Test
	public void environmentEndpointDisabled() throws Exception {
		endpointDisabled("env", EnvironmentMvcEndpoint.class);
	}

	@Test
	public void environmentEndpointEnabledOverride() throws Exception {
		endpointEnabledOverride("env", EnvironmentMvcEndpoint.class);
	}

	@Test
	public void loggersEndpointDisabled() throws Exception {
		endpointDisabled("loggers", LoggersMvcEndpoint.class);
	}

	@Test
	public void loggersEndpointEnabledOverride() throws Exception {
		endpointEnabledOverride("loggers", LoggersMvcEndpoint.class);
	}

	@Test
	public void metricsEndpointDisabled() throws Exception {
		endpointDisabled("metrics", MetricsMvcEndpoint.class);
	}

	@Test
	public void metricsEndpointEnabledOverride() throws Exception {
		endpointEnabledOverride("metrics", MetricsMvcEndpoint.class);
	}

	@Test
	public void healthEndpointDisabled() throws Exception {
		endpointDisabled("health", HealthMvcEndpoint.class);
	}

	@Test
	public void healthEndpointEnabledOverride() throws Exception {
		endpointEnabledOverride("health", HealthMvcEndpoint.class);
	}

	@Test
	public void shutdownEndpointEnabled() {
		this.applicationContext.register(RootConfig.class, BaseConfiguration.class,
				EndpointWebMvcAutoConfiguration.class);
		TestPropertyValues.of("endpoints.shutdown.enabled:true")
				.applyTo(this.applicationContext);
		this.applicationContext.refresh();
		assertThat(this.applicationContext.getBeansOfType(ShutdownMvcEndpoint.class))
				.hasSize(1);
	}

	@Test
	public void managementSpecificSslUsingDifferentPort() throws Exception {
		TestPropertyValues
				.of("management.port=" + ports.get().management,
						"management.ssl.enabled=true",
						"management.ssl.key-store=classpath:test.jks",
						"management.ssl.key-password=password")
				.applyTo(this.applicationContext);
		this.applicationContext.register(RootConfig.class, EndpointConfig.class,
				DifferentPortConfig.class, BaseConfiguration.class,
				EndpointWebMvcAutoConfiguration.class, ErrorMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/controller", ports.get().server, "controlleroutput");
		assertContent("/endpoint", ports.get().server, null);
		assertHttpsContent("/controller", ports.get().management, null);
		assertHttpsContent("/application/endpoint", ports.get().management,
				"endpointoutput");
		assertHttpsContent("/error", ports.get().management, startsWith("{"));
		ApplicationContext managementContext = this.applicationContext
				.getBean(ManagementContextResolver.class).getApplicationContext();
		List<?> interceptors = (List<?>) ReflectionTestUtils.getField(
				managementContext.getBean(EndpointHandlerMapping.class), "interceptors");
		assertThat(interceptors).hasSize(2);
		ManagementServerProperties managementServerProperties = this.applicationContext
				.getBean(ManagementServerProperties.class);
		assertThat(managementServerProperties.getSsl()).isNotNull();
		assertThat(managementServerProperties.getSsl().isEnabled()).isTrue();
	}

	@Test
	public void managementSpecificSslUsingSamePortFails() throws Exception {
		TestPropertyValues
				.of("management.ssl.enabled=true",
						"management.ssl.key-store=classpath:test.jks",
						"management.ssl.key-password=password")
				.applyTo(this.applicationContext);
		this.applicationContext.register(RootConfig.class, EndpointConfig.class,
				BaseConfiguration.class, EndpointWebMvcAutoConfiguration.class,
				ErrorMvcAutoConfiguration.class);
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("Management-specific SSL cannot be configured as the "
				+ "management server is not listening on a separate port");
		this.applicationContext.refresh();
	}

	@Test
	public void rootManagementContextPathUsingSamePortFails() throws Exception {
		TestPropertyValues.of("management.context-path=/")
				.applyTo(this.applicationContext);
		this.applicationContext.register(RootConfig.class, EndpointConfig.class,
				BaseConfiguration.class, EndpointWebMvcAutoConfiguration.class,
				ErrorMvcAutoConfiguration.class);
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("A management context path of '/' requires the"
				+ " management server to be listening on a separate port");
		this.applicationContext.refresh();
	}

	@Test
	public void samePortCanBeUsedWhenManagementSslIsExplicitlyDisabled()
			throws Exception {
		TestPropertyValues.of("management.ssl.enabled=false")
				.applyTo(this.applicationContext);
		this.applicationContext.register(RootConfig.class, EndpointConfig.class,
				BaseConfiguration.class, EndpointWebMvcAutoConfiguration.class,
				ErrorMvcAutoConfiguration.class);
		this.applicationContext.refresh();
	}

	@Test
	public void managementServerCanDisableSslWhenUsingADifferentPort() throws Exception {
		TestPropertyValues.of("management.port=" + ports.get().management,
				"server.ssl.enabled=true", "server.ssl.key-store=classpath:test.jks",
				"server.ssl.key-password=password", "management.ssl.enabled=false")
				.applyTo(this.applicationContext);
		this.applicationContext.register(RootConfig.class, EndpointConfig.class,
				DifferentPortConfig.class, BaseConfiguration.class,
				EndpointWebMvcAutoConfiguration.class, ErrorMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertHttpsContent("/controller", ports.get().server, "controlleroutput");
		assertHttpsContent("/endpoint", ports.get().server, null);
		assertContent("/controller", ports.get().management, null);
		assertContent("/application/endpoint", ports.get().management, "endpointoutput");
		assertContent("/error", ports.get().management, startsWith("{"));
		ApplicationContext managementContext = this.applicationContext
				.getBean(ManagementContextResolver.class).getApplicationContext();
		List<?> interceptors = (List<?>) ReflectionTestUtils.getField(
				managementContext.getBean(EndpointHandlerMapping.class), "interceptors");
		assertThat(interceptors).hasSize(2);
		ManagementServerProperties managementServerProperties = this.applicationContext
				.getBean(ManagementServerProperties.class);
		assertThat(managementServerProperties.getSsl()).isNotNull();
		assertThat(managementServerProperties.getSsl().isEnabled()).isFalse();
	}

	@Test
	public void tomcatManagementAccessLogUsesCustomPrefix() throws Exception {
		TestPropertyValues.of("management.port=" + ports.get().management)
				.applyTo(this.applicationContext);
		this.applicationContext.register(TomcatWebServerConfig.class, RootConfig.class,
				EndpointConfig.class, DifferentPortConfig.class, BaseConfiguration.class,
				EndpointWebMvcAutoConfiguration.class, ErrorMvcAutoConfiguration.class);
		TestPropertyValues.of("server.tomcat.accesslog.enabled: true")
				.applyTo(this.applicationContext);
		this.applicationContext.refresh();
		ApplicationContext managementContext = this.applicationContext
				.getBean(ManagementContextResolver.class).getApplicationContext();
		ServletWebServerFactory factory = managementContext
				.getBean(ServletWebServerFactory.class);
		assertThat(factory).isInstanceOf(TomcatServletWebServerFactory.class);
		AccessLogValve accessLogValve = findAccessLogValve(
				((TomcatServletWebServerFactory) factory));
		assertThat(accessLogValve).isNotNull();
		assertThat(accessLogValve.getPrefix()).isEqualTo("management_access_log");
	}

	@Test
	public void undertowManagementAccessLogUsesCustomPrefix() throws Exception {
		TestPropertyValues
				.of("management.port=" + ports.get().management,
						"server.undertow.accesslog.enabled: true")
				.applyTo(this.applicationContext);
		this.applicationContext.register(UndertowWebServerConfig.class, RootConfig.class,
				EndpointConfig.class, DifferentPortConfig.class, BaseConfiguration.class,
				EndpointWebMvcAutoConfiguration.class, ErrorMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		ApplicationContext managementContext = this.applicationContext
				.getBean(ManagementContextResolver.class).getApplicationContext();
		ServletWebServerFactory factory = managementContext
				.getBean(ServletWebServerFactory.class);
		assertThat(factory).isInstanceOf(UndertowServletWebServerFactory.class);
		assertThat(((UndertowServletWebServerFactory) factory).getAccessLogPrefix())
				.isEqualTo("management_access_log.");
	}

	private AccessLogValve findAccessLogValve(
			TomcatServletWebServerFactory webServerFactory) {
		for (Valve engineValve : webServerFactory.getEngineValves()) {
			if (engineValve instanceof AccessLogValve) {
				return (AccessLogValve) engineValve;
			}
		}
		return null;
	}

	private void endpointDisabled(String name, Class<? extends MvcEndpoint> type) {
		this.applicationContext.register(RootConfig.class, BaseConfiguration.class,
				EndpointWebMvcAutoConfiguration.class);
		TestPropertyValues.of(String.format("endpoints.%s.enabled:false", name))
				.applyTo(this.applicationContext);
		this.applicationContext.refresh();
		assertThat(this.applicationContext.getBeansOfType(type)).isEmpty();
	}

	private void endpointEnabledOverride(String name, Class<? extends MvcEndpoint> type)
			throws Exception {
		this.applicationContext.register(LoggingConfig.class, RootConfig.class,
				BaseConfiguration.class, EndpointWebMvcAutoConfiguration.class);
		ConfigurationPropertySources.attach(this.applicationContext.getEnvironment());
		TestPropertyValues
				.of("endpoints.enabled:false",
						String.format("endpoints.%s.enabled:true", name))
				.applyTo(this.applicationContext);
		this.applicationContext.refresh();
		assertThat(this.applicationContext.getBeansOfType(type)).hasSize(1);
	}

	private void assertHttpsContent(String url, int port, Object expected)
			throws Exception {
		assertContent("https", url, port, expected);
	}

	private void assertContent(String url, int port, Object expected) throws Exception {
		assertContent("http", url, port, expected);
	}

	private void assertContent(String scheme, String url, int port, Object expected)
			throws Exception {

		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder()
						.loadTrustMaterial(null, new TrustSelfSignedStrategy()).build());
		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory)
				.build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
				httpClient);
		ClientHttpRequest request = requestFactory.createRequest(
				new URI(scheme + "://localhost:" + port + url), HttpMethod.GET);
		try (ClientHttpResponse response = request.execute()) {
			if (HttpStatus.NOT_FOUND.equals(response.getStatusCode())) {
				throw new FileNotFoundException();
			}
			String actual = StreamUtils.copyToString(response.getBody(),
					Charset.forName("UTF-8"));
			if (expected instanceof Matcher) {
				assertThat(actual).is(Matched.by((Matcher<?>) expected));
			}
			else {
				assertThat(actual).isEqualTo(expected);
			}
		}
		catch (Exception ex) {
			if (expected == null) {
				if (SocketException.class.isInstance(ex)
						|| FileNotFoundException.class.isInstance(ex)) {
					return;
				}
			}
			throw ex;
		}
	}

	public boolean hasHeader(String url, int port, String header) throws Exception {
		SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
		ClientHttpRequest request = clientHttpRequestFactory
				.createRequest(new URI("http://localhost:" + port + url), HttpMethod.GET);
		ClientHttpResponse response = request.execute();
		return response.getHeaders().containsKey(header);
	}

	private static class Ports {

		int server = 0;

		int management = 0;

	}

	@Configuration
	@Import({ PropertyPlaceholderAutoConfiguration.class,
			ServletWebServerFactoryAutoConfiguration.class,
			JacksonAutoConfiguration.class, EndpointAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
			AuditAutoConfiguration.class })
	protected static class BaseConfiguration {

	}

	@Configuration
	public static class RootConfig {

		@Bean
		public TestController testController() {
			return new TestController();
		}

		@Bean
		public ApplicationListener<WebServerInitializedEvent> serverPortListener() {
			return (event) -> {
				int port = event.getWebServer().getPort();
				if (event.getApplicationContext().getParent() == null) {
					ports.get().server = port;
				}
				else {
					ports.get().management = port;
				}
			};
		}

	}

	@Configuration
	public static class EndpointConfig {

		@Bean
		public TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

	}

	@Configuration
	public static class LoggingConfig {

		@Bean
		public LoggingSystem loggingSystem() {
			return LoggingSystem.get(getClass().getClassLoader());
		}

	}

	@Controller
	public static class TestController {

		@RequestMapping("/controller")
		@ResponseBody
		public String requestMappedMethod() {
			return "controlleroutput";
		}

	}

	@Configuration
	public static class SpecificWebServerConfig {

		@Bean
		public SpecificServletWebServerFactory webServerFactory() {
			return new SpecificServletWebServerFactory();
		}

	}

	@Configuration
	public static class TomcatWebServerConfig {

		@Bean
		public TomcatServletWebServerFactory webServerFactory() {
			return new TomcatServletWebServerFactory();
		}

	}

	@Configuration
	public static class UndertowWebServerConfig {

		@Bean
		public UndertowServletWebServerFactory webServerFactory() {
			return new UndertowServletWebServerFactory();
		}

	}

	@Configuration
	public static class DifferentPortConfig {

		@Bean
		public EndpointHandlerMappingCustomizer mappingCustomizer() {
			return (mapping) -> mapping.setInterceptors(interceptor());
		}

		@Bean
		protected TestInterceptor interceptor() {
			return new TestInterceptor();
		}

		protected static class TestInterceptor extends HandlerInterceptorAdapter {

			private int count = 0;

			@Override
			public void postHandle(HttpServletRequest request,
					HttpServletResponse response, Object handler,
					ModelAndView modelAndView) throws Exception {
				this.count++;
			}

			public int getCount() {
				return this.count;
			}

		}

	}

	public static class TestEndpoint implements MvcEndpoint {

		@RequestMapping
		@ResponseBody
		public String invoke() {
			return "endpointoutput";
		}

		@Override
		public String getPath() {
			return "/endpoint";
		}

		@Override
		@SuppressWarnings("rawtypes")
		public Class<? extends Endpoint> getEndpointType() {
			return Endpoint.class;
		}

	}

	private static class SpecificServletWebServerFactory
			extends TomcatServletWebServerFactory {

	}

}
