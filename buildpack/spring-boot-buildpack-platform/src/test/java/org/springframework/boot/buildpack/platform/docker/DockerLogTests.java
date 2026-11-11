/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DockerLog}.
 *
 * @author Dmytro nosan
 */
@ExtendWith(OutputCaptureExtension.class)
class DockerLogTests {

	@Test
	void toSystemOutPrintsToSystemOut(CapturedOutput output) {
		DockerLog logger = DockerLog.toSystemOut();
		logger.log("Hello world");
		assertThat(output.getErr()).isEmpty();
		assertThat(output.getOut()).contains("Hello world");
	}

	@Test
	void toPrintsToOutput(CapturedOutput output) {
		DockerLog logger = DockerLog.to(System.err);
		logger.log("Hello world");
		assertThat(output.getOut()).isEmpty();
		assertThat(output.getErr()).contains("Hello world");
	}

}
