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

package org.springframework.boot.devtools.integrationtest;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.devtools.integrationtest.HttpTunnelIntegrationTests.TunnelConfiguration.TestTunnelClient;
import org.springframework.boot.devtools.remote.server.AccessManager;
import org.springframework.boot.devtools.remote.server.Dispatcher;
import org.springframework.boot.devtools.remote.server.DispatcherFilter;
import org.springframework.boot.devtools.remote.server.HandlerMapper;
import org.springframework.boot.devtools.remote.server.UrlHandlerMapper;
import org.springframework.boot.devtools.tunnel.client.HttpTunnelConnection;
import org.springframework.boot.devtools.tunnel.client.TunnelClient;
import org.springframework.boot.devtools.tunnel.client.TunnelConnection;
import org.springframework.boot.devtools.tunnel.server.HttpTunnelServer;
import org.springframework.boot.devtools.tunnel.server.HttpTunnelServerHandler;
import org.springframework.boot.devtools.tunnel.server.SocketTargetServerConnection;
import org.springframework.boot.devtools.tunnel.server.TargetServerConnection;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple integration tests for HTTP tunneling.
 *
 * @author Phillip Webb
 */
public class HttpTunnelIntegrationTests {

	@Test
	public void httpServerDirect() {
		AnnotationConfigServletWebServerApplicationContext context = new AnnotationConfigServletWebServerApplicationContext();
		context.register(ServerConfiguration.class);
		context.refresh();
		String url = "http://localhost:" + context.getWebServer().getPort() + "/hello";
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(url,
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("Hello World");
		context.close();
	}

	@Test
	public void viaTunnel() {
		AnnotationConfigServletWebServerApplicationContext serverContext = new AnnotationConfigServletWebServerApplicationContext();
		serverContext.register(ServerConfiguration.class);
		serverContext.refresh();
		AnnotationConfigApplicationContext tunnelContext = new AnnotationConfigApplicationContext();
		TestPropertyValues.of("server.port:" + serverContext.getWebServer().getPort())
				.applyTo(tunnelContext);
		tunnelContext.register(TunnelConfiguration.class);
		tunnelContext.refresh();
		String url = "http://localhost:"
				+ tunnelContext.getBean(TestTunnelClient.class).port + "/hello";
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(url,
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("Hello World");
		serverContext.close();
		tunnelContext.close();
	}

	@Configuration(proxyBeanMethods = false)
	@EnableWebMvc
	static class ServerConfiguration {

		@Bean
		public ServletWebServerFactory container() {
			return new TomcatServletWebServerFactory(0);
		}

		@Bean
		public DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

		@Bean
		public MyController myController() {
			return new MyController();
		}

		@Bean
		public DispatcherFilter filter(
				AnnotationConfigServletWebServerApplicationContext context) {
			TargetServerConnection connection = new SocketTargetServerConnection(
					() -> context.getWebServer().getPort());
			HttpTunnelServer server = new HttpTunnelServer(connection);
			HandlerMapper mapper = new UrlHandlerMapper("/httptunnel",
					new HttpTunnelServerHandler(server));
			Collection<HandlerMapper> mappers = Collections.singleton(mapper);
			Dispatcher dispatcher = new Dispatcher(AccessManager.PERMIT_ALL, mappers);
			return new DispatcherFilter(dispatcher);
		}

	}

	@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
	static class TunnelConfiguration {

		@Bean
		public TunnelClient tunnelClient(@Value("${server.port}") int serverPort) {
			String url = "http://localhost:" + serverPort + "/httptunnel";
			TunnelConnection connection = new HttpTunnelConnection(url,
					new SimpleClientHttpRequestFactory());
			return new TestTunnelClient(0, connection);
		}

		static class TestTunnelClient extends TunnelClient {

			private int port;

			TestTunnelClient(int listenPort, TunnelConnection tunnelConnection) {
				super(listenPort, tunnelConnection);
			}

			@Override
			public int start() throws IOException {
				this.port = super.start();
				return this.port;
			}

		}

	}

	@RestController
	static class MyController {

		@RequestMapping("/hello")
		public String hello() {
			return "Hello World";
		}

	}

}
