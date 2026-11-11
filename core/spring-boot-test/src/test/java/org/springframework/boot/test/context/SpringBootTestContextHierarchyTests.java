/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.context.SpringBootTestContextHierarchyTests.ChildConfiguration;
import org.springframework.boot.test.context.SpringBootTestContextHierarchyTests.ParentConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTest @SpringBootTest} and
 * {@link ContextHierarchy @ContextHierarchy}.
 *
 * @author Andy Wilkinson
 */
@SpringBootTest
@ContextHierarchy({ @ContextConfiguration(classes = ParentConfiguration.class),
		@ContextConfiguration(classes = ChildConfiguration.class) })
@ExtendWith(OutputCaptureExtension.class)
class SpringBootTestContextHierarchyTests {

	@Test
	void contextLoads(CapturedOutput capturedOutput) {
		assertThat(capturedOutput).containsOnlyOnce(":: Spring Boot ::");
	}

	@Configuration(proxyBeanMethods = false)
	static class ParentConfiguration {

		@Bean
		MyBean myBean() {
			return new MyBean();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class ChildConfiguration {

		ChildConfiguration(MyBean myBean) {

		}

	}

	static class MyBean {

	}

}
