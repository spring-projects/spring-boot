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
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link AotProcessor}.
 *
 * @author Stephane Nicoll
 */
class AotProcessorTests {

	@BeforeEach
	void setup() {
		SampleApplication.argsHolder = null;
	}

	@Test
	void processApplicationInvokesRunMethod(@TempDir Path directory) {
		String[] arguments = new String[] { "1", "2" };
		AotProcessor processor = new AotProcessor(SampleApplication.class, arguments, directory.resolve("source"),
				directory.resolve("resource"));
		processor.process();
		assertThat(SampleApplication.argsHolder).isEqualTo(arguments);
		assertThat(directory).satisfies(hasGeneratedAssetsForSampleApplication());
	}

	@Test
	void processApplicationWithMainMethodThatDoesNotRun(@TempDir Path directory) {
		AotProcessor processor = new AotProcessor(BrokenApplication.class, new String[0], directory.resolve("source"),
				directory.resolve("resource"));
		assertThatIllegalArgumentException().isThrownBy(processor::process)
				.withMessageContaining("Does it run a SpringApplication?");
		assertThat(directory).isEmptyDirectory();
	}

	@Test
	void invokeMainParseArgumentsAndInvokesRunMethod(@TempDir Path directory) throws Exception {
		String[] mainArguments = new String[] { SampleApplication.class.getName(),
				directory.resolve("source").toString(), directory.resolve("resource").toString(), "1", "2" };
		AotProcessor.main(mainArguments);
		assertThat(SampleApplication.argsHolder).containsExactly("1", "2");
		assertThat(directory).satisfies(hasGeneratedAssetsForSampleApplication());
	}

	@Test
	void invokeMainWithMissingArguments() {
		assertThatIllegalArgumentException().isThrownBy(() -> AotProcessor.main(new String[] { "Test" }))
				.withMessageContaining("Usage:");
	}

	private Consumer<Path> hasGeneratedAssetsForSampleApplication() {
		return (directory) -> {
			assertThat(directory
					.resolve("source/org/springframework/boot/SampleApplication__ApplicationContextInitializer.java"))
							.exists().isRegularFile();
			assertThat(directory.resolve("resource/META-INF/native-image/reflect-config.json")).exists()
					.isRegularFile();
			Path nativeImagePropertiesFile = directory
					.resolve("resource/META-INF/native-image/native-image.properties");
			assertThat(nativeImagePropertiesFile).exists().isRegularFile().hasContent("""
					Args = -H:Class=org.springframework.boot.AotProcessorTests$SampleApplication \\
					--allow-incomplete-classpath \\
					--report-unsupported-elements-at-runtime \\
					--no-fallback \\
					--install-exit-handlers
					""");
		};
	}

	@Configuration(proxyBeanMethods = false)
	public static class SampleApplication {

		public static String[] argsHolder;

		public static void main(String[] args) {
			argsHolder = args;
			SpringApplication.run(SampleApplication.class, args);
		}

	}

	public static class BrokenApplication {

		public static void main(String[] args) {
			// Does not run an application
		}

	}

}
