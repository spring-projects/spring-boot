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

package smoketest.tomcat.multiconnector;

import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.junit.jupiter.api.Test;
import smoketest.tomcat.multiconnector.SampleTomcatTwoConnectorsApplicationTests.Ports;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for {@link SampleTomcatTwoConnectorsApplication}.
 *
 * @author Brock Mills
 * @author Andy Wilkinson
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(Ports.class)
class SampleTomcatTwoConnectorsApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private Ports ports;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private AbstractConfigurableWebServerFactory webServerFactory;

	@Test
	void testSsl() {
		assertThat(this.webServerFactory.getSsl().isEnabled()).isTrue();
	}

	@Test
	void testHello() {
		assertThat(this.ports.getHttpsPort()).isEqualTo(this.port);
		assertThat(this.ports.getHttpPort()).isNotEqualTo(this.port);
		ResponseEntity<String> entity = this.restTemplate
				.getForEntity("http://localhost:" + this.ports.getHttpPort() + "/hello", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("hello");
		ResponseEntity<String> httpsEntity = this.restTemplate.getForEntity("https://localhost:" + this.port + "/hello",
				String.class);
		assertThat(httpsEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(httpsEntity.getBody()).isEqualTo("hello");
	}

	@TestConfiguration
	static class Ports implements ApplicationListener<WebServerInitializedEvent> {

		private int httpPort;

		private int httpsPort;

		@Override
		public void onApplicationEvent(WebServerInitializedEvent event) {
			Service service = ((TomcatWebServer) event.getWebServer()).getTomcat().getService();
			for (Connector connector : service.findConnectors()) {
				if (connector.getSecure()) {
					this.httpsPort = connector.getLocalPort();
				}
				else {
					this.httpPort = connector.getLocalPort();
				}
			}
		}

		int getHttpPort() {
			return this.httpPort;
		}

		int getHttpsPort() {
			return this.httpsPort;
		}

	}

}
