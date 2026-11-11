/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.webapplicationtype;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for an application using an overridden web application type
 *
 * @author Andy Wilkinson
 */
@SpringBootTest(properties = "spring.main.web-application-type=servlet")
class OverriddenWebApplicationTypeApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void contextIsServlet() {
		assertThat(this.context).isInstanceOf(WebApplicationContext.class);
	}

}
