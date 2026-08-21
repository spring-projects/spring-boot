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

package org.springframework.boot.test.context;

import java.nio.file.Path;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.context.aot.AbstractAotProcessor.Settings;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootTestAotProcessor}.
 */
class SpringBootTestAotProcessorTests {

	@Test
	void processingSetsAndRestoresTestAotProcessingSystemProperty(@TempDir Path tempDir) {
		System.setProperty(SpringBootTestAotProcessor.AOT_PROCESSING, "previous");
		try {
			TestSpringBootTestAotProcessor processor = new TestSpringBootTestAotProcessor(tempDir);
			processor.process();
			assertThat(processor.testAotProcessing).isTrue();
			assertThat(System.getProperty(SpringBootTestAotProcessor.AOT_PROCESSING)).isEqualTo("previous");
		}
		finally {
			System.clearProperty(SpringBootTestAotProcessor.AOT_PROCESSING);
		}
	}

	private static final class TestSpringBootTestAotProcessor extends SpringBootTestAotProcessor {

		private boolean testAotProcessing;

		private TestSpringBootTestAotProcessor(Path tempDir) {
			super(Set.of(tempDir),
					Settings.builder()
						.sourceOutput(tempDir.resolve("source"))
						.resourceOutput(tempDir.resolve("resource"))
						.classOutput(tempDir.resolve("class"))
						.groupId("com.example")
						.artifactId("example")
						.build());
		}

		@Override
		protected void performAotProcessing() {
			this.testAotProcessing = Boolean.getBoolean(AOT_PROCESSING);
		}

	}

}
