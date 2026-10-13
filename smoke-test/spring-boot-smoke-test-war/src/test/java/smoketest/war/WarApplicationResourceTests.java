/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.war;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class WarApplicationResourceTests {

	// gh-6371

	@Value("${demo.string.value}")
	private @Nullable String demoStringValue;

	@Test
	void contextLoads() {
		assertThat(this.demoStringValue).isEqualTo("demo");
	}

}
