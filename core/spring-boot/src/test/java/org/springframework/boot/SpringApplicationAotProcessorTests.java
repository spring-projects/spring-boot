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

package org.springframework.boot;

import java.nio.file.Path;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.aot.AbstractAotProcessor.Settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SpringApplicationAotProcessor}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
class SpringApplicationAotProcessorTests {

	private static final ApplicationInvoker invoker = new ApplicationInvoker();

	@BeforeEach
	void setup() {
		invoker.clean();
	}

	@Test
	void processApplicationInvokesMainMethod(@TempDir Path directory) {
		String[] arguments = new String[] { "1", "2" };
		SpringApplicationAotProcessor processor = new SpringApplicationAotProcessor(PublicMainMethod.class,
				settings(directory), arguments);
		processor.process();
		assertThat(ApplicationInvoker.argsHolder).isEqualTo(arguments);
		assertThat(ApplicationInvoker.postRunInvoked).isFalse();
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
	void invokeMainParsesArgumentsAndInvokesMainMethod(@TempDir Path directory) throws Exception {
		String[] mainArguments = new String[] { PublicMainMethod.class.getName(),
				directory.resolve("source").toString(), directory.resolve("resource").toString(),
				directory.resolve("class").toString(), "com.example", "example", "1", "2" };
		SpringApplicationAotProcessor.main(mainArguments);
		assertThat(ApplicationInvoker.argsHolder).containsExactly("1", "2");
		assertThat(ApplicationInvoker.postRunInvoked).isFalse();
	}

	@Test
	void invokeMainParsesArgumentsAndInvokesPackagePrivateMainMethod(@TempDir Path directory) throws Exception {
		String[] mainArguments = new String[] { PackagePrivateMainMethod.class.getName(),
				directory.resolve("source").toString(), directory.resolve("resource").toString(),
				directory.resolve("class").toString(), "com.example", "example", "1", "2" };
		SpringApplicationAotProcessor.main(mainArguments);
		assertThat(ApplicationInvoker.argsHolder).containsExactly("1", "2");
		assertThat(ApplicationInvoker.postRunInvoked).isFalse();
	}

	@Test
	void invokeMainParsesArgumentsAndInvokesParameterLessMainMethod(@TempDir Path directory) throws Exception {
		String[] mainArguments = new String[] { PublicParameterlessMainMethod.class.getName(),
				directory.resolve("source").toString(), directory.resolve("resource").toString(),
				directory.resolve("class").toString(), "com.example", "example", "1", "2" };
		SpringApplicationAotProcessor.main(mainArguments);
		assertThat(ApplicationInvoker.argsHolder).isNull();
		assertThat(ApplicationInvoker.postRunInvoked).isFalse();
	}

	@Test
	void invokeMainParsesArgumentsAndInvokesPackagePrivateRunMethod(@TempDir Path directory) throws Exception {
		String[] mainArguments = new String[] { PackagePrivateParameterlessMainMethod.class.getName(),
				directory.resolve("source").toString(), directory.resolve("resource").toString(),
				directory.resolve("class").toString(), "com.example", "example", "1", "2" };
		SpringApplicationAotProcessor.main(mainArguments);
		assertThat(ApplicationInvoker.argsHolder).isNull();
		assertThat(ApplicationInvoker.postRunInvoked).isFalse();
	}

	@Test
	void invokeMainParsesArgumentsAndInvokesRunMethodWithoutGroupId(@TempDir Path directory) throws Exception {
		String[] mainArguments = new String[] { PublicMainMethod.class.getName(),
				directory.resolve("source").toString(), directory.resolve("resource").toString(),
				directory.resolve("class").toString(), "", "example", "1", "2" };
		SpringApplicationAotProcessor.main(mainArguments);
		assertThat(ApplicationInvoker.argsHolder).containsExactly("1", "2");
		assertThat(ApplicationInvoker.postRunInvoked).isFalse();
	}

	@Test
	void invokeMainWithMissingArguments() {
		assertThatIllegalStateException().isThrownBy(() -> SpringApplicationAotProcessor.main(new String[] { "Test" }))
			.withMessageContaining("Usage:");
	}

	private Settings settings(Path directory) {
		return Settings.builder()
			.sourceOutput(directory.resolve("source"))
			.resourceOutput(directory.resolve("resource"))
			.classOutput(directory.resolve("class"))
			.groupId("com.example")
			.artifactId("example")
			.build();

	}

	@Configuration(proxyBeanMethods = false)
	public static class PublicMainMethod {

		public static void main(String[] args) {
			invoker.invoke(args, () -> SpringApplication.run(PublicMainMethod.class, args));
		}

	}

	@Configuration(proxyBeanMethods = false)
	public static class PackagePrivateMainMethod {

		static void main(String[] args) {
			invoker.invoke(args, () -> SpringApplication.run(PackagePrivateMainMethod.class, args));
		}

	}

	@Configuration(proxyBeanMethods = false)
	public static class PublicParameterlessMainMethod {

		public static void main() {
			invoker.invoke(null, () -> SpringApplication.run(PublicParameterlessMainMethod.class));
		}

	}

	@Configuration(proxyBeanMethods = false)
	public static class PackagePrivateParameterlessMainMethod {

		static void main() {
			invoker.invoke(null, () -> SpringApplication.run(PackagePrivateParameterlessMainMethod.class));
		}

	}

	public static class BrokenApplication {

		public static void main(String[] args) {
			// Does not run an application
		}

	}

	private static final class ApplicationInvoker {

		public static String @Nullable [] argsHolder;

		public static boolean postRunInvoked;

		void invoke(String @Nullable [] args, Runnable applicationRun) {
			argsHolder = args;
			applicationRun.run();
			postRunInvoked = true;
		}

		void clean() {
			argsHolder = null;
			postRunInvoked = false;
		}

	}

}
