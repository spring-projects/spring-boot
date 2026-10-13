/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.tomcat.multiconnector;

import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.junit.jupiter.api.Test;
import smoketest.tomcat.multiconnector.SampleTomcatTwoConnectorsApplicationTests.Ports;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TrustAllTlsRequestFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;

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
	private AbstractConfigurableWebServerFactory webServerFactory;

	@Test
	void testSsl() {
		Ssl ssl = this.webServerFactory.getSsl();
		assertThat(ssl).isNotNull();
		assertThat(ssl.isEnabled()).isTrue();
	}

	@Test
	void testHello() {
		assertThat(this.ports.getHttpsPort()).isEqualTo(this.port);
		assertThat(this.ports.getHttpPort()).isNotEqualTo(this.port);
		RestTestClient httpClient = RestTestClient.bindToServer()
			.baseUrl("http://localhost:" + this.ports.getHttpPort())
			.build();
		httpClient.get().uri("/hello").exchangeSuccessfully().expectBody(String.class).isEqualTo("hello");
		RestTestClient httpsClient = RestTestClient.bindToServer(TrustAllTlsRequestFactory.create())
			.baseUrl("https://localhost:" + this.port)
			.build();
		httpsClient.get().uri("/hello").exchangeSuccessfully().expectBody(String.class).isEqualTo("hello");
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
