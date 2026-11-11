/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jetty;

import org.junit.jupiter.api.Test;
import smoketest.jetty.util.StringUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Florian Storz
 * @author Michael Weidmann
 * @author Moritz Halbritter
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class SampleJettyApplicationTests {

	@Autowired
	private RestTestClient restTestClient;

	@Value("${server.max-http-request-header-size}")
	private int maxHttpRequestHeaderSize;

	@Test
	void testHome() {
		this.restTestClient.get().uri("/").exchangeSuccessfully().expectBody(String.class).isEqualTo("Hello World");
	}

	@Test
	void testCompression() {
		// Jetty HttpClient sends Accept-Encoding: gzip by default
		this.restTestClient.get().uri("/").exchangeSuccessfully().expectBody(String.class).isEqualTo("Hello World");
		// Jetty HttpClient decodes gzip responses automatically and removes the
		// Content-Encoding header. We have to assume that the response was gzipped.
	}

	@Test
	void testMaxHttpResponseHeaderSize() {
		this.restTestClient.get()
			.uri("/max-http-response-header")
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	@Test
	void testMaxHttpRequestHeaderSize() {
		String headerValue = StringUtil.repeat('A', this.maxHttpRequestHeaderSize + 1);
		this.restTestClient.get()
			.uri("/")
			.headers((headers) -> headers.add("x-max-request-header", headerValue))
			.exchange()
			.expectStatus()
			.isEqualTo(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE);
	}

}
