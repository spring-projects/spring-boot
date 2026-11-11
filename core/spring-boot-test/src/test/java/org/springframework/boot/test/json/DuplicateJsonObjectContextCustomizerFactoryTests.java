/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.json;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.testsupport.classpath.ClassPathOverrides;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DuplicateJsonObjectContextCustomizerFactory}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
@ClassPathOverrides("org.json:json:20140107")
class DuplicateJsonObjectContextCustomizerFactoryTests {

	private CapturedOutput output;

	@BeforeEach
	void setup(CapturedOutput output) {
		this.output = output;
	}

	@Test
	@SuppressWarnings("NullAway") // Uses null for quick test setup
	void warningForMultipleVersions() {
		new DuplicateJsonObjectContextCustomizerFactory().createContextCustomizer(null, null)
			.customizeContext(null, null);
		assertThat(this.output).contains("Found multiple occurrences of org.json.JSONObject on the class path:");
	}

}
