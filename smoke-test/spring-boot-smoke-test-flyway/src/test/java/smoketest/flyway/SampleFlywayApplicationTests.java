/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.flyway;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SampleFlywayApplicationTests {

	@Autowired
	private JdbcTemplate template;

	@Test
	void testDefaultSettings() {
		assertThat(this.template.queryForObject("SELECT COUNT(*) from PERSON", Integer.class)).isEqualTo(2);
	}

}
