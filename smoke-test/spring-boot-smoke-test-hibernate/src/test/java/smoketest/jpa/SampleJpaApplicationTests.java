/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jpa;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

/**
 * Integration test to run the application.
 *
 * @author Oliver Gierke
 * @author Dave Syer
 */
@SpringBootTest
@AutoConfigureMockMvc
class SampleJpaApplicationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void testHome() throws Exception {
		assertThat(this.mvc.get().uri("/")).hasStatusOk().matches(xpath("//tbody/tr").nodeCount(4));
	}

}
