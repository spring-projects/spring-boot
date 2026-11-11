/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.context;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.test.context.aot.TestAotProcessor;
import org.springframework.util.Assert;

/**
 * Entry point for AOT processing of a Spring Boot application's tests.
 *
 * <strong>For internal use only.</strong>
 *
 * @author Andy Wilkinson
 * @since 3.0.0
 */
public class SpringBootTestAotProcessor extends TestAotProcessor {

	/**
	 * Create a new processor for the specified test classpath roots and general settings.
	 * @param classpathRoots the classpath roots to scan for test classes
	 * @param settings the general AOT processor settings
	 */
	public SpringBootTestAotProcessor(Set<Path> classpathRoots, Settings settings) {
		super(classpathRoots, settings);
	}

	public static void main(String[] args) {
		int requiredArgs = 6;
		Assert.isTrue(args.length >= requiredArgs,
				() -> "Usage: %s <classpathRoots> <sourceOutput> <resourceOutput> <classOutput> <groupId> <artifactId>"
					.formatted(TestAotProcessor.class.getName()));
		Set<Path> classpathRoots = Arrays.stream(args[0].split(File.pathSeparator))
			.map(Paths::get)
			.collect(Collectors.toSet());
		Settings settings = Settings.builder()
			.sourceOutput(Paths.get(args[1]))
			.resourceOutput(Paths.get(args[2]))
			.classOutput(Paths.get(args[3]))
			.groupId(args[4])
			.artifactId(args[5])
			.build();
		new SpringBootTestAotProcessor(classpathRoots, settings).process();
	}

}
