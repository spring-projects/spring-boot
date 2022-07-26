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
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.aot.generate.ClassNameGenerator;
import org.springframework.aot.generate.DefaultGenerationContext;
import org.springframework.aot.generate.FileSystemGeneratedFiles;
import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.aot.hint.ExecutableHint;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.nativex.FileNativeConfigurationWriter;
import org.springframework.boot.SpringApplicationHooks.Hook;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;
import org.springframework.util.Assert;
import org.springframework.util.FileSystemUtils;

/**
 * Entry point for AOT processing of a {@link SpringApplication}.
 * <p>
 * <strong>For internal use only.</strong>
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.0.0
 */
public class AotProcessor {

	private static final Consumer<ExecutableHint.Builder> INVOKE_CONSTRUCTOR_HINT = (hint) -> hint
			.setModes(ExecutableMode.INVOKE);

	private final Class<?> application;

	private final String[] applicationArgs;

	private final Path sourceOutput;

	private final Path resourceOutput;

	private final Path classOutput;

	private final String groupId;

	private final String artifactId;

	/**
	 * Create a new processor for the specified application and settings.
	 * @param application the application main class
	 * @param applicationArgs the arguments to provide to the main method
	 * @param sourceOutput the location of generated sources
	 * @param resourceOutput the location of generated resources
	 * @param classOutput the location of generated classes
	 * @param groupId the group ID of the application, used to locate
	 * native-image.properties
	 * @param artifactId the artifact ID of the application, used to locate
	 * native-image.properties
	 */
	public AotProcessor(Class<?> application, String[] applicationArgs, Path sourceOutput, Path resourceOutput,
			Path classOutput, String groupId, String artifactId) {
		this.application = application;
		this.applicationArgs = applicationArgs;
		this.sourceOutput = sourceOutput;
		this.resourceOutput = resourceOutput;
		this.classOutput = classOutput;
		this.groupId = groupId;
		this.artifactId = artifactId;
	}

	/**
	 * Trigger the processing of the application managed by this instance.
	 */
	public void process() throws IOException {
		deleteExistingOutput();
		AotProcessorHook hook = new AotProcessorHook();
		SpringApplicationHooks.withHook(hook, this::callApplicationMainMethod);
		GenericApplicationContext applicationContext = hook.getApplicationContext();
		Assert.notNull(applicationContext, "No application context available after calling main method of '"
				+ this.application.getName() + "'. Does it run a SpringApplication?");
		performAotProcessing(applicationContext);
	}

	private void deleteExistingOutput() {
		deleteExistingOutput(this.sourceOutput, this.resourceOutput, this.classOutput);
	}

	private void deleteExistingOutput(Path... paths) {
		for (Path path : paths) {
			try {
				FileSystemUtils.deleteRecursively(path);
			}
			catch (IOException ex) {
				throw new RuntimeException("Failed to delete existing output in '" + path + "'");
			}
		}
	}

	private void callApplicationMainMethod() {
		try {
			this.application.getMethod("main", String[].class).invoke(null, new Object[] { this.applicationArgs });
		}
		catch (InvocationTargetException ex) {
			Throwable targetException = ex.getTargetException();
			if (!(targetException instanceof MainMethodSilentExitException)) {
				throw (targetException instanceof RuntimeException runtimeEx) ? runtimeEx
						: new RuntimeException(targetException);
			}
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private void performAotProcessing(GenericApplicationContext applicationContext) throws IOException {
		FileSystemGeneratedFiles generatedFiles = new FileSystemGeneratedFiles(this::getRoot);
		DefaultGenerationContext generationContext = new DefaultGenerationContext(
				new ClassNameGenerator(this.application), generatedFiles);
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		ClassName generatedInitializerClassName = generator.generateApplicationContext(applicationContext,
				generationContext);
		registerEntryPointHint(generationContext, generatedInitializerClassName);
		generationContext.writeGeneratedContent();
		writeHints(generationContext.getRuntimeHints());
		writeNativeImageProperties();
	}

	private void registerEntryPointHint(DefaultGenerationContext generationContext,
			ClassName generatedInitializerClassName) {
		TypeReference generatedType = TypeReference.of(generatedInitializerClassName.canonicalName());
		TypeReference applicationType = TypeReference.of(this.application);
		ReflectionHints reflection = generationContext.getRuntimeHints().reflection();
		reflection.registerType(applicationType, (hint) -> {
		});
		reflection.registerType(generatedType, (hint) -> hint.onReachableType(applicationType)
				.withConstructor(Collections.emptyList(), INVOKE_CONSTRUCTOR_HINT));
	}

	private Path getRoot(Kind kind) {
		return switch (kind) {
			case SOURCE -> this.sourceOutput;
			case RESOURCE -> this.resourceOutput;
			case CLASS -> this.classOutput;
		};
	}

	private void writeHints(RuntimeHints hints) {
		FileNativeConfigurationWriter writer = new FileNativeConfigurationWriter(this.resourceOutput, this.groupId,
				this.artifactId);
		writer.write(hints);
	}

	private void writeNativeImageProperties() {
		List<String> args = new ArrayList<>();
		args.add("-H:Class=" + this.application.getName());
		args.add("--report-unsupported-elements-at-runtime");
		args.add("--no-fallback");
		args.add("--install-exit-handlers");
		StringBuilder sb = new StringBuilder();
		sb.append("Args = ");
		sb.append(String.join(String.format(" \\%n"), args));
		Path file = this.resourceOutput
				.resolve("META-INF/native-image/" + this.groupId + "/" + this.artifactId + "/native-image.properties");
		try {
			if (!Files.exists(file)) {
				Files.createDirectories(file.getParent());
				Files.createFile(file);
			}
			Files.writeString(file, sb.toString());
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to write native-image properties", ex);
		}
	}

	public static void main(String[] args) throws Exception {
		int requiredArgs = 6;
		Assert.isTrue(args.length >= requiredArgs, () -> "Usage: " + AotProcessor.class.getName()
				+ " <applicationName> <sourceOutput> <resourceOutput> <classOutput> <groupId> <artifactId> <originalArgs...>");
		String applicationName = args[0];
		Path sourceOutput = Paths.get(args[1]);
		Path resourceOutput = Paths.get(args[2]);
		Path classOutput = Paths.get(args[3]);
		String groupId = args[4];
		String artifactId = args[5];
		String[] applicationArgs = (args.length > requiredArgs) ? Arrays.copyOfRange(args, requiredArgs, args.length)
				: new String[0];
		Class<?> application = Class.forName(applicationName);
		new AotProcessor(application, applicationArgs, sourceOutput, resourceOutput, classOutput, groupId, artifactId)
				.process();
	}

	/**
	 * Hook used to capture the {@link ApplicationContext} and trigger early exit of main
	 * method.
	 */
	private static class AotProcessorHook implements Hook {

		private GenericApplicationContext context;

		@Override
		public boolean preRefresh(SpringApplication application, ConfigurableApplicationContext context) {
			Assert.isInstanceOf(GenericApplicationContext.class, context,
					() -> "AOT processing requires a GenericApplicationContext but got a "
							+ context.getClass().getName());
			this.context = (GenericApplicationContext) context;
			return false;
		}

		@Override
		public void postRun(SpringApplication application, ConfigurableApplicationContext context) {
			throw new MainMethodSilentExitException();
		}

		GenericApplicationContext getApplicationContext() {
			return this.context;
		}

	}

	/**
	 * Internal exception used to prevent main method to continue once
	 * {@code SpringApplication#run} completes.
	 */
	private static class MainMethodSilentExitException extends RuntimeException {

	}

}
