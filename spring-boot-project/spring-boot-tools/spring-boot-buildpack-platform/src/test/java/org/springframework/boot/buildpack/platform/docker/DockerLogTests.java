/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
