/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.lifecycle;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.SpringApplicationAotProcessor;
import org.springframework.util.ClassUtils;

/**
 * Checks if Docker Compose support should be skipped.
 *
 * @author Phillip Webb
 */
class DockerComposeSkipCheck {

	private static final Set<String> REQUIRED_CLASSES = Set.of("org.junit.jupiter.api.Test", "org.junit.Test");

	private static final Set<String> SKIPPED_STACK_ELEMENTS;

	static {
		Set<String> skipped = new LinkedHashSet<>();
		skipped.add("org.junit.runners.");
		skipped.add("org.junit.platform.");
		skipped.add("org.springframework.boot.test.");
		skipped.add(SpringApplicationAotProcessor.class.getName());
		skipped.add("cucumber.runtime.");
		SKIPPED_STACK_ELEMENTS = Collections.unmodifiableSet(skipped);
	}

	boolean shouldSkip(@Nullable ClassLoader classLoader, DockerComposeProperties.Skip properties) {
		if (properties.isInTests() && hasAtLeastOneRequiredClass(classLoader)) {
			Thread thread = Thread.currentThread();
			for (StackTraceElement element : thread.getStackTrace()) {
				if (isSkippedStackElement(element)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean hasAtLeastOneRequiredClass(@Nullable ClassLoader classLoader) {
		for (String requiredClass : REQUIRED_CLASSES) {
			if (ClassUtils.isPresent(requiredClass, classLoader)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isSkippedStackElement(StackTraceElement element) {
		for (String skipped : SKIPPED_STACK_ELEMENTS) {
			if (element.getClassName().startsWith(skipped)) {
				return true;
			}
		}
		return false;
	}

}
