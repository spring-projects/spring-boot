/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.testng;

import org.testng.annotations.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Basic integration tests for demo application.
 *
 * @author Phillip Webb
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
public class SampleTestNGApplicationTests extends AbstractTestNGSpringContextTests {

	@Autowired
	private RestTestClient restTestClient;

	@Test
	public void testHome() {
		this.restTestClient.get().uri("/").exchangeSuccessfully().expectBody(String.class).isEqualTo("Hello World");
	}

}
