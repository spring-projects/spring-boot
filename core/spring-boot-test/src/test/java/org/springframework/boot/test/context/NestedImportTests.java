package org.springframework.boot.test.context;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
@Import(NestedImportTests.ConfigA.class)
class NestedImportTests {

	@Nested
	@SpringBootTest
	@Import(ConfigB.class)
	class Inner extends NestedImportTests {

		@Autowired(required = false)
		ConfigA configA;

		@Test
		void shouldLoadOuterImport() {
			assertThat(this.configA).isNotNull();
		}
	}

	@Configuration
	static class ConfigA {}

	@Configuration
	static class ConfigB {}
}
