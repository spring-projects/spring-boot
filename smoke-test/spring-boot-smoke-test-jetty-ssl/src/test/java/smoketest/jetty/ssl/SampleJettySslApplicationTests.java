/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jetty.ssl;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TrustAllTlsRequestFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "debug=true")
class SampleJettySslApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private AbstractConfigurableWebServerFactory webServerFactory;

	@Test
	void testSsl() {
		Ssl ssl = this.webServerFactory.getSsl();
		assertThat(ssl).isNotNull();
		assertThat(ssl.isEnabled()).isTrue();
	}

	@Test
	void testHome() {
		RestTestClient client = RestTestClient.bindToServer(TrustAllTlsRequestFactory.create())
			.baseUrl("https://localhost:" + this.port)
			.build();
		client.get().uri("/").exchangeSuccessfully().expectBody(String.class).isEqualTo("Hello World");
	}

}
