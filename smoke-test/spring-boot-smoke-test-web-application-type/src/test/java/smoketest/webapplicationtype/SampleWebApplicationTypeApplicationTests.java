/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.webapplicationtype;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.context.reactive.ReactiveWebApplicationContext;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for an application using a configured web application type
 *
 * @author Andy Wilkinson
 */
@SpringBootTest
class SampleWebApplicationTypeApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void contextIsReactive() {
		assertThat(this.context).isInstanceOf(ReactiveWebApplicationContext.class);
	}

}
