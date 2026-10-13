/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.jdbc;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link SampleDataJdbcApplication}.
 *
 * @author Andy Wilkinson
 */
@SpringBootTest
@AutoConfigureMockMvc
class SampleDataJdbcApplicationTests {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void testCustomers() {
		assertThat(this.mvc.get().uri("/").param("name", "merEDith")).hasStatusOk().bodyText().contains("Meredith");
	}

}
