package org.springframework.boot.autoconfigure;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AutoConfigurationLocatorTest {

	private AutoConfigurationLocator sut;

	@BeforeEach
	void setUp() {
		sut = new AutoConfigurationLocator();
	}

	@Test
	void locate() {
		List<String> classNames = sut.locate(TestAutoConfiguration.class, null);

		assertThat(classNames).containsExactly("class1", "class2", "class3");
	}

	@AutoConfiguration
	@Target(ElementType.TYPE)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface TestAutoConfiguration {

	}

}