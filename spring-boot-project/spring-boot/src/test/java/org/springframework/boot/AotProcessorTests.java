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

import java.io.IOException;
import java.nio.file.Files;
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
 * @author Andy Wilkinson
 */
class AotProcessorTests {

	@BeforeEach
	void setup() {
		SampleApplication.argsHolder = null;
		SampleApplication.postRunInvoked = false;
	}

	@Test
	void processApplicationInvokesRunMethod(@TempDir Path directory) throws IOException {
		String[] arguments = new String[] { "1", "2" };
		AotProcessor processor = new AotProcessor(SampleApplication.class, arguments, directory.resolve("source"),
				directory.resolve("resource"), directory.resolve("class"), "com.example", "example");
		processor.process();
		assertThat(SampleApplication.argsHolder).isEqualTo(arguments);
		assertThat(SampleApplication.postRunInvoked).isFalse();
		assertThat(directory).satisfies(hasGeneratedAssetsForSampleApplication());
	}

	@Test
	void processApplicationWithMainMethodThatDoesNotRun(@TempDir Path directory) {
		AotProcessor processor = new AotProcessor(BrokenApplication.class, new String[0], directory.resolve("source"),
				directory.resolve("resource"), directory.resolve("class"), "com.example", "example");
		assertThatIllegalArgumentException().isThrownBy(processor::process)
				.withMessageContaining("Does it run a SpringApplication?");
		assertThat(directory).isEmptyDirectory();
	}

	@Test
	void invokeMainParseArgumentsAndInvokesRunMethod(@TempDir Path directory) throws Exception {
		String[] mainArguments = new String[] { SampleApplication.class.getName(),
				directory.resolve("source").toString(), directory.resolve("resource").toString(),
				directory.resolve("class").toString(), "com.example", "example", "1", "2" };
		AotProcessor.main(mainArguments);
		assertThat(SampleApplication.argsHolder).containsExactly("1", "2");
		assertThat(SampleApplication.postRunInvoked).isFalse();
		assertThat(directory).satisfies(hasGeneratedAssetsForSampleApplication());
	}

	@Test
	void invokeMainWithMissingArguments() {
		assertThatIllegalArgumentException().isThrownBy(() -> AotProcessor.main(new String[] { "Test" }))
				.withMessageContaining("Usage:");
	}

	@Test
	void processingDeletesExistingOutput(@TempDir Path directory) throws IOException {
		Path sourceOutput = directory.resolve("source");
		Path resourceOutput = directory.resolve("resource");
		Path classOutput = directory.resolve("class");
		Path existingSourceOutput = createExisting(sourceOutput);
		Path existingResourceOutput = createExisting(resourceOutput);
		Path existingClassOutput = createExisting(classOutput);
		AotProcessor processor = new AotProcessor(SampleApplication.class, new String[0], sourceOutput, resourceOutput,
				classOutput, "com.example", "example");
		processor.process();
		assertThat(existingSourceOutput).doesNotExist();
		assertThat(existingResourceOutput).doesNotExist();
		assertThat(existingClassOutput).doesNotExist();
	}

	private Path createExisting(Path directory) throws IOException {
		Path existing = directory.resolve("existing");
		Files.createDirectories(directory);
		Files.createFile(existing);
		return existing;
	}

	private Consumer<Path> hasGeneratedAssetsForSampleApplication() {
		return (directory) -> {
			assertThat(directory.resolve(
					"source/org/springframework/boot/AotProcessorTests_SampleApplication__ApplicationContextInitializer.java"))
							.exists().isRegularFile();
			assertThat(directory.resolve(
					"source/org/springframework/boot/AotProcessorTests_SampleApplication__BeanDefinitions.java"))
							.exists().isRegularFile();
			assertThat(directory.resolve(
					"source/org/springframework/boot/AotProcessorTests_SampleApplication__BeanFactoryRegistrations.java"))
							.exists().isRegularFile();
			assertThat(directory.resolve("resource/META-INF/native-image/com.example/example/reflect-config.json"))
					.exists().isRegularFile();
			Path nativeImagePropertiesFile = directory
					.resolve("resource/META-INF/native-image/com.example/example/native-image.properties");
			assertThat(nativeImagePropertiesFile).exists().isRegularFile().hasContent("""
					Args = -H:Class=org.springframework.boot.AotProcessorTests$SampleApplication \\
					--report-unsupported-elements-at-runtime \\
					--no-fallback \\
					--install-exit-handlers
					""");
		};
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
