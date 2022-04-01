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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.aot.generator.DefaultGeneratedTypeContext;
import org.springframework.aot.generator.GeneratedType;
import org.springframework.aot.generator.GeneratedTypeReference;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.nativex.FileNativeConfigurationWriter;
import org.springframework.context.generator.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.JavaFile;
import org.springframework.util.Assert;

/**
 * Entry point for AOT processing of a {@link SpringApplication}.
 * <p>
 * <strong>For internal use only.</strong>
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @since 3.0
 */
public class AotProcessor {

	private final Class<?> application;

	private final String[] applicationArgs;

	private final Path sourceOutput;

	private final Path resourceOutput;

	/**
	 * Create a new processor for the specified application and settings.
	 * @param application the application main class
	 * @param applicationArgs the arguments to provide to the main method
	 * @param sourceOutput the location of generated sources
	 * @param resourceOutput the location of generated resources
	 */
	public AotProcessor(Class<?> application, String[] applicationArgs, Path sourceOutput, Path resourceOutput) {
		this.application = application;
		this.applicationArgs = applicationArgs;
		this.sourceOutput = sourceOutput;
		this.resourceOutput = resourceOutput;
	}

	/**
	 * Trigger the processing of the application managed by this instance.
	 */
	public void process() {
		AotProcessingHook hook = new AotProcessingHook();
		SpringApplicationHooks.withHook(hook, this::callApplicationMainMethod);
		GenericApplicationContext applicationContext = hook.getApplicationContext();
		Assert.notNull(applicationContext, "No application context available after calling main method of '"
				+ this.application.getName() + "'. Does it run a SpringApplication?");
		performAotProcessing(applicationContext);
	}

	private void callApplicationMainMethod() {
		try {
			this.application.getMethod("main", String[].class).invoke(null, new Object[] { this.applicationArgs });
		}
		catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	private void performAotProcessing(GenericApplicationContext applicationContext) {
		DefaultGeneratedTypeContext generationContext = new DefaultGeneratedTypeContext(
				this.application.getPackageName(), (packageName) -> GeneratedType.of(ClassName.get(packageName,
						this.application.getSimpleName() + "__ApplicationContextInitializer")));
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		generator.generateApplicationContext(applicationContext, generationContext);

		// Register reflection hint for entry point as we access it via reflection
		generationContext.runtimeHints().reflection()
				.registerType(GeneratedTypeReference.of(generationContext.getMainGeneratedType().getClassName()),
						(hint) -> hint.onReachableType(TypeReference.of(this.application)).withConstructor(
								Collections.emptyList(),
								(constructorHint) -> constructorHint.setModes(ExecutableMode.INVOKE)));

		writeGeneratedSources(generationContext.toJavaFiles());
		writeGeneratedResources(generationContext.runtimeHints());
		writeNativeImageProperties();
	}

	private void writeGeneratedSources(List<JavaFile> sources) {
		for (JavaFile source : sources) {
			try {
				source.writeTo(this.sourceOutput);
			}
			catch (IOException ex) {
				throw new IllegalStateException("Failed to write " + source.typeSpec.name, ex);
			}
		}
	}

	private void writeGeneratedResources(RuntimeHints hints) {
		FileNativeConfigurationWriter writer = new FileNativeConfigurationWriter(this.resourceOutput);
		writer.write(hints);
	}

	private void writeNativeImageProperties() {
		List<String> args = new ArrayList<>();
		args.add("-H:Class=" + this.application.getName());
		args.add("--allow-incomplete-classpath");
		args.add("--report-unsupported-elements-at-runtime");
		args.add("--no-fallback");
		args.add("--install-exit-handlers");
		StringBuilder sb = new StringBuilder();
		sb.append("Args = ");
		sb.append(String.join(String.format(" \\%n"), args));
		Path file = this.resourceOutput.resolve("META-INF/native-image/native-image.properties");
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
		if (args.length < 3) {
			throw new IllegalArgumentException("Usage: " + AotProcessor.class.getName()
					+ " <applicationName> <sourceOutput> <resourceOutput> <originalArgs...>");
		}
		String applicationName = args[0];
		Path sourceOutput = Paths.get(args[1]);
		Path resourceOutput = Paths.get(args[2]);
		String[] applicationArgs = (args.length > 3) ? Arrays.copyOfRange(args, 3, args.length) : new String[0];

		Class<?> application = Class.forName(applicationName);
		AotProcessor aotProcess = new AotProcessor(application, applicationArgs, sourceOutput, resourceOutput);
		aotProcess.process();
	}

}
