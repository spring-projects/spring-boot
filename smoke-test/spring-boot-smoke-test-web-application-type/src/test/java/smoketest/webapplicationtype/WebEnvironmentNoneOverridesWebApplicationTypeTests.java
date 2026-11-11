/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.webapplicationtype;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.context.reactive.ReactiveWebApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for a web environment of none overriding the configured web
 * application type.
 *
 * @author Andy Wilkinson
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class WebEnvironmentNoneOverridesWebApplicationTypeTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void contextIsPlain() {
		assertThat(this.context).isNotInstanceOf(ReactiveWebApplicationContext.class);
		assertThat(this.context).isNotInstanceOf(WebApplicationContext.class);
	}

}
