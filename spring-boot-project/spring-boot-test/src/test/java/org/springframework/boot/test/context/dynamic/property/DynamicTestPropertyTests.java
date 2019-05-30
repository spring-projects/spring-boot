package org.springframework.boot.test.context.dynamic.property;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Integration test for {@link DynamicTestProperty}
 *
 * @author Anatoliy Korovin
 */
@SpringBootTest(classes = DynamicTestPropertyTests.class)
class DynamicTestPropertyTests {

	@DynamicTestProperty
	private static TestPropertyValues dynamicProperties() {
		return TestPropertyValues.of("a=123",
									 String.format("b=%d", 456));
	}

	@Value("${a}")
	public String a;

	@Value("${b}")
	public String b;

	@Test
	void testPropertyValues() {
		assertThat(a).isEqualTo("123");
		assertThat(b).isEqualTo("456");
	}
}