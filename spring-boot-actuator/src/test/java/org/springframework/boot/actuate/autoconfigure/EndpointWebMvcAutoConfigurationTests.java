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

package org.springframework.boot.actuate.autoconfigure;

import java.io.FileNotFoundException;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMapping;
import org.springframework.boot.actuate.endpoint.mvc.EndpointHandlerMappingCustomizer;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.web.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ErrorMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.web.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.ServerPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.embedded.AnnotationConfigEmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerInitializedEvent;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.boot.test.ServerPortInfoApplicationContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Controller;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.SocketUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link EndpointWebMvcAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Greg Turnquist
 */
public class EndpointWebMvcAutoConfigurationTests {

	private final AnnotationConfigEmbeddedWebApplicationContext applicationContext = new AnnotationConfigEmbeddedWebApplicationContext();

	private static ThreadLocal<Ports> ports = new ThreadLocal<Ports>();

	@Before
	public void grabPorts() {
		ports.set(new Ports());
	}

	@After
	public void close() {
		if (this.applicationContext != null) {
			this.applicationContext.close();
		}
	}

	@Test
	public void onSamePort() throws Exception {
		this.applicationContext.register(RootConfig.class, BaseConfiguration.class,
				ServerPortConfig.class, EndpointWebMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/controller", ports.get().server, "controlleroutput");
		assertContent("/endpoint", ports.get().server, "endpointoutput");
		assertContent("/controller", ports.get().management, null);
		assertContent("/endpoint", ports.get().management, null);
		assertTrue(hasHeader("/endpoint", ports.get().server, "X-Application-Context"));
		this.applicationContext.close();
		assertAllClosed();
	}

	@Test
	public void onSamePortWithoutHeader() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.applicationContext,
				"management.add-application-context-header:false");
		this.applicationContext.register(RootConfig.class, BaseConfiguration.class,
				ServerPortConfig.class, EndpointWebMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertFalse(hasHeader("/endpoint", ports.get().server, "X-Application-Context"));
		this.applicationContext.close();
		assertAllClosed();
	}

	@Test
	public void onDifferentPort() throws Exception {
		this.applicationContext.register(RootConfig.class, DifferentPortConfig.class,
				BaseConfiguration.class, EndpointWebMvcAutoConfiguration.class,
				ErrorMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/controller", ports.get().server, "controlleroutput");
		assertContent("/endpoint", ports.get().server, null);
		assertContent("/controller", ports.get().management, null);
		assertContent("/endpoint", ports.get().management, "endpointoutput");
		List<?> interceptors = (List<?>) ReflectionTestUtils.getField(
				this.applicationContext.getBean(EndpointHandlerMapping.class),
				"interceptors");
		assertEquals(1, interceptors.size());
		this.applicationContext.close();
		assertAllClosed();
	}

	@Test
	public void onRandomPort() throws Exception {
		this.applicationContext.register(RootConfig.class, RandomPortConfig.class,
				BaseConfiguration.class, EndpointWebMvcAutoConfiguration.class,
				ErrorMvcAutoConfiguration.class);
		GrabManagementPort grabManagementPort = new GrabManagementPort(
				this.applicationContext);
		this.applicationContext.addApplicationListener(grabManagementPort);
		this.applicationContext.refresh();
		int managementPort = grabManagementPort.getServletContainer().getPort();
		assertThat(managementPort, not(equalTo(ports.get().server)));
		assertContent("/controller", ports.get().server, "controlleroutput");
		assertContent("/endpoint", ports.get().server, null);
		assertContent("/controller", managementPort, null);
		assertContent("/endpoint", managementPort, "endpointoutput");
	}

	@Test
	public void disabled() throws Exception {
		this.applicationContext.register(RootConfig.class, DisableConfig.class,
				BaseConfiguration.class, EndpointWebMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/controller", ports.get().server, "controlleroutput");
		assertContent("/endpoint", ports.get().server, null);
		assertContent("/controller", ports.get().management, null);
		assertContent("/endpoint", ports.get().management, null);
		this.applicationContext.close();
		assertAllClosed();
	}

	@Test
	public void specificPortsViaProperties() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.applicationContext, "server.port:"
				+ ports.get().server, "management.port:" + ports.get().management);
		this.applicationContext.register(RootConfig.class, BaseConfiguration.class,
				EndpointWebMvcAutoConfiguration.class, ErrorMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/controller", ports.get().server, "controlleroutput");
		assertContent("/endpoint", ports.get().server, null);
		assertContent("/controller", ports.get().management, null);
		assertContent("/endpoint", ports.get().management, "endpointoutput");
		this.applicationContext.close();
		assertAllClosed();
	}

	@Test
	public void contextPath() throws Exception {
		EnvironmentTestUtils.addEnvironment(this.applicationContext,
				"management.contextPath:/test");
		this.applicationContext.register(RootConfig.class, ServerPortConfig.class,
				PropertyPlaceholderAutoConfiguration.class,
				ManagementServerPropertiesAutoConfiguration.class,
				ServerPropertiesAutoConfiguration.class,
				EmbeddedServletContainerAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class,
				DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
				EndpointWebMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		assertContent("/controller", ports.get().server, "controlleroutput");
		assertContent("/test/endpoint", ports.get().server, "endpointoutput");
		this.applicationContext.close();
		assertAllClosed();
	}

	@Test
	public void portPropertiesOnSamePort() throws Exception {
		this.applicationContext.register(RootConfig.class, BaseConfiguration.class,
				ServerPortConfig.class, EndpointWebMvcAutoConfiguration.class);
		new ServerPortInfoApplicationContextInitializer()
				.initialize(this.applicationContext);
		this.applicationContext.refresh();
		Integer localServerPort = this.applicationContext.getEnvironment().getProperty(
				"local.server.port", Integer.class);
		Integer localManagementPort = this.applicationContext.getEnvironment()
				.getProperty("local.management.port", Integer.class);
		assertThat(localServerPort, notNullValue());
		assertThat(localManagementPort, notNullValue());
		assertThat(localServerPort, equalTo(localManagementPort));
		this.applicationContext.close();
		assertAllClosed();
	}

	@Test
	public void portPropertiesOnDifferentPort() throws Exception {
		new ServerPortInfoApplicationContextInitializer()
				.initialize(this.applicationContext);
		this.applicationContext.register(RootConfig.class, DifferentPortConfig.class,
				BaseConfiguration.class, EndpointWebMvcAutoConfiguration.class,
				ErrorMvcAutoConfiguration.class);
		this.applicationContext.refresh();
		Integer localServerPort = this.applicationContext.getEnvironment().getProperty(
				"local.server.port", Integer.class);
		Integer localManagementPort = this.applicationContext.getEnvironment()
				.getProperty("local.management.port", Integer.class);
		assertThat(localServerPort, notNullValue());
		assertThat(localManagementPort, notNullValue());
		assertThat(localServerPort, not(equalTo(localManagementPort)));
		assertThat(this.applicationContext.getBean(ServerPortConfig.class).getCount(),
				equalTo(2));
		this.applicationContext.close();
		assertAllClosed();
	}

	private void assertAllClosed() throws Exception {
		assertContent("/controller", ports.get().server, null);
		assertContent("/endpoint", ports.get().server, null);
		assertContent("/controller", ports.get().management, null);
		assertContent("/endpoint", ports.get().management, null);
	}

	public void assertContent(String url, int port, Object expected) throws Exception {
		SimpleClientHttpRequestFactory clientHttpRequestFactory = new SimpleClientHttpRequestFactory();
		ClientHttpRequest request = clientHttpRequestFactory.createRequest(new URI(
				"http://localhost:" + port + url), HttpMethod.GET);
		try {
			ClientHttpResponse response = request.execute();
			try {
				String actual = StreamUtils.copyToString(response.getBody(),
						Charset.forName("UTF-8"));
				assertThat(actual, equalTo(expected));
			}
			finally {
				response.close();
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
		ClientHttpRequest request = clientHttpRequestFactory.createRequest(new URI(
				"http://localhost:" + port + url), HttpMethod.GET);
		ClientHttpResponse response = request.execute();
		return response.getHeaders().containsKey(header);
	}

	private static class Ports {

		int server = SocketUtils.findAvailableTcpPort();

		int management = SocketUtils.findAvailableTcpPort();

	}

	@Configuration
	@Import({ PropertyPlaceholderAutoConfiguration.class,
			EmbeddedServletContainerAutoConfiguration.class,
			HttpMessageConvertersAutoConfiguration.class,
			DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
			ManagementServerPropertiesAutoConfiguration.class,
			ServerPropertiesAutoConfiguration.class, WebMvcAutoConfiguration.class })
	protected static class BaseConfiguration {

	}

	@Configuration
	public static class RootConfig {

		@Bean
		public TestController testController() {
			return new TestController();
		}

		@Bean
		public TestEndpoint testEndpoint() {
			return new TestEndpoint();
		}

	}

	@Configuration
	public static class ServerPortConfig {

		private int count = 0;

		public int getCount() {
			return this.count;
		}

		@Bean
		public ServerProperties serverProperties() {
			ServerProperties properties = new ServerProperties() {
				@Override
				public void customize(ConfigurableEmbeddedServletContainer container) {
					ServerPortConfig.this.count++;
					super.customize(container);
				}
			};
			properties.setPort(ports.get().server);
			return properties;
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
	@Import(ServerPortConfig.class)
	public static class DifferentPortConfig {

		@Bean
		public ManagementServerProperties managementServerProperties() {
			ManagementServerProperties properties = new ManagementServerProperties();
			properties.setPort(ports.get().management);
			return properties;
		}

		@Bean
		public EndpointHandlerMappingCustomizer mappingCustomizer() {
			return new EndpointHandlerMappingCustomizer() {

				@Override
				public void customize(EndpointHandlerMapping mapping) {
					mapping.setInterceptors(new Object[] { interceptor() });
				}

			};
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

	@Configuration
	@Import(ServerPortConfig.class)
	public static class RandomPortConfig {

		@Bean
		public ManagementServerProperties managementServerProperties() {
			ManagementServerProperties properties = new ManagementServerProperties();
			properties.setPort(0);
			return properties;
		}

	}

	@Configuration
	@Import(ServerPortConfig.class)
	public static class DisableConfig {

		@Bean
		public ManagementServerProperties managementServerProperties() {
			ManagementServerProperties properties = new ManagementServerProperties();
			properties.setPort(-1);
			return properties;
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
		public boolean isSensitive() {
			return true;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public Class<? extends Endpoint> getEndpointType() {
			return Endpoint.class;
		}

	}

	private static class GrabManagementPort implements
			ApplicationListener<EmbeddedServletContainerInitializedEvent> {

		private ApplicationContext rootContext;

		private EmbeddedServletContainer servletContainer;

		public GrabManagementPort(ApplicationContext rootContext) {
			this.rootContext = rootContext;
		}

		@Override
		public void onApplicationEvent(EmbeddedServletContainerInitializedEvent event) {
			if (event.getApplicationContext() != this.rootContext) {
				this.servletContainer = event.getEmbeddedServletContainer();
			}
		}

		public EmbeddedServletContainer getServletContainer() {
			return this.servletContainer;
		}

	}

}
