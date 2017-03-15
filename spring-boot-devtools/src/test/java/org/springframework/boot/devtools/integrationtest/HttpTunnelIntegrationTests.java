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

package org.springframework.boot.devtools.integrationtest;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.boot.devtools.tunnel.server.PortProvider;
import org.springframework.boot.devtools.tunnel.server.SocketTargetServerConnection;
import org.springframework.boot.devtools.tunnel.server.StaticPortProvider;
import org.springframework.boot.devtools.tunnel.server.TargetServerConnection;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.SocketUtils;
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
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public class HttpTunnelIntegrationTests {

	@Autowired
	private Config config;

	@BeforeClass
	@AfterClass
	public static void uninstallUrlStreamHandlerFactory() {
		ReflectionTestUtils.setField(TomcatURLStreamHandlerFactory.class, "instance",
				null);
		ReflectionTestUtils.setField(URL.class, "factory", null);
	}

	@Test
	public void httpServerDirect() throws Exception {
		String url = "http://localhost:" + this.config.httpServerPort + "/hello";
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(url,
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("Hello World");
	}

	@Test
	public void viaTunnel() throws Exception {
		String url = "http://localhost:" + this.config.clientPort + "/hello";
		ResponseEntity<String> entity = new TestRestTemplate().getForEntity(url,
				String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("Hello World");
	}

	@Configuration
	@EnableWebMvc
	static class Config {

		private int clientPort = SocketUtils.findAvailableTcpPort();

		private int httpServerPort = SocketUtils.findAvailableTcpPort();

		@Bean
		public ServletWebServerFactory container() {
			return new TomcatServletWebServerFactory(this.httpServerPort);
		}

		@Bean
		public DispatcherFilter filter() {
			PortProvider port = new StaticPortProvider(this.httpServerPort);
			TargetServerConnection connection = new SocketTargetServerConnection(port);
			HttpTunnelServer server = new HttpTunnelServer(connection);
			HandlerMapper mapper = new UrlHandlerMapper("/httptunnel",
					new HttpTunnelServerHandler(server));
			Collection<HandlerMapper> mappers = Collections.singleton(mapper);
			Dispatcher dispatcher = new Dispatcher(AccessManager.PERMIT_ALL, mappers);
			return new DispatcherFilter(dispatcher);
		}

		@Bean
		public TunnelClient tunnelClient() {
			String url = "http://localhost:" + this.httpServerPort + "/httptunnel";
			TunnelConnection connection = new HttpTunnelConnection(url,
					new SimpleClientHttpRequestFactory());
			return new TunnelClient(this.clientPort, connection);
		}

		@Bean
		public DispatcherServlet dispatcherServlet() {
			return new DispatcherServlet();
		}

		@Bean
		public MyController myController() {
			return new MyController();
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
