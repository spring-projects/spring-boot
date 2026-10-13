/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jetty.ssl;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, properties = "debug=true")
@AutoConfigureTestRestTemplate
class SampleJettySslApplicationTests {

	@Autowired
	private TestRestTemplate restTemplate;

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
		ResponseEntity<String> entity = this.restTemplate.getForEntity("/", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("Hello World");
	}

}
