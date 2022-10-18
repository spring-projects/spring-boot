/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.aot.AbstractAotProcessor.Settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SpringApplicationAotProcessor}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class SpringApplicationAotProcessorTests {

	@BeforeEach
	void setup() {
		SampleApplication.argsHolder = null;
		SampleApplication.postRunInvoked = false;
	}

	@Test
	void processApplicationInvokesRunMethod(@TempDir Path directory) {
		String[] arguments = new String[] { "1", "2" };
		SpringApplicationAotProcessor processor = new SpringApplicationAotProcessor(SampleApplication.class,
				settings(directory), arguments);
		processor.process();
		assertThat(SampleApplication.argsHolder).isEqualTo(arguments);
		assertThat(SampleApplication.postRunInvoked).isFalse();
	}

	@Test
	void processApplicationWithMainMethodThatDoesNotRun(@TempDir Path directory) {
		SpringApplicationAotProcessor processor = new SpringApplicationAotProcessor(BrokenApplication.class,
				settings(directory), new String[0]);
		assertThatIllegalStateException().isThrownBy(processor::process)
				.withMessageContaining("Does it run a SpringApplication?");
		assertThat(directory).isEmptyDirectory();
	}

	@Test
	void invokeMainParseArgumentsAndInvokesRunMethod(@TempDir Path directory) throws Exception {
		String[] mainArguments = new String[] { SampleApplication.class.getName(),
				directory.resolve("source").toString(), directory.resolve("resource").toString(),
				directory.resolve("class").toString(), "com.example", "example", "1", "2" };
		SpringApplicationAotProcessor.main(mainArguments);
		assertThat(SampleApplication.argsHolder).containsExactly("1", "2");
		assertThat(SampleApplication.postRunInvoked).isFalse();
	}

	@Test
	void invokeMainWithMissingArguments() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> SpringApplicationAotProcessor.main(new String[] { "Test" }))
				.withMessageContaining("Usage:");
	}

	private Settings settings(Path directory) {
		return new Settings().setSourceOutput(directory.resolve("source"))
				.setResourceOutput(directory.resolve("resource")).setClassOutput(directory.resolve("class"))
				.setGroupId("com.example").setArtifactId("example");

	}

	@Configuration(proxyBeanMethods = false)
	public static class SampleApplication {

		public static String[] argsHolder;

		public static boolean postRunInvoked;

		public static void main(String[] args) {
			argsHolder = args;
			SpringApplication.run(SampleApplication.class, args);
			postRunInvoked = true;
		}

	}

	public static class BrokenApplication {

		public static void main(String[] args) {
			// Does not run an application
		}

	}

}
