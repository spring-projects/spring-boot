/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.features.springapplication.fluentbuilderapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MyApplication}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
class MyApplicationTests {

	@Test
	void contextHierarchyWithDisabledBanner(CapturedOutput output) {
		System.setProperty("spring.main.web-application-type", "none");
		try {
			new MyApplication().hierarchyWithDisabledBanner(new String[0]);
			assertThat(output).doesNotContain(":: Spring Boot ::");
		}
		finally {
			System.clearProperty("spring.main.web-application-type");
		}
	}

}
