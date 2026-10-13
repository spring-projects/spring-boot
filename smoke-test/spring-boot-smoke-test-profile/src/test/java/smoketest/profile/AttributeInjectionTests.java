/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.profile;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

// gh-29169
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AttributeInjectionTests {

	@Autowired(required = false)
	private org.springframework.boot.webmvc.error.ErrorAttributes errorAttributesServlet;

	@Autowired(required = false)
	private org.springframework.boot.webflux.error.ErrorAttributes errorAttributesReactive;

	@Test
	void contextLoads() {
		assertThat(this.errorAttributesServlet).isNull();
		assertThat(this.errorAttributesReactive).isNotNull();
	}

}
