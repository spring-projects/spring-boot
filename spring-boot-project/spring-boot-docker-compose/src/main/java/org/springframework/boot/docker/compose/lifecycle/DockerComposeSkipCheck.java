/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.docker.compose.lifecycle;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

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

	boolean shouldSkip(ClassLoader classLoader, DockerComposeProperties.Skip properties) {
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

	private boolean hasAtLeastOneRequiredClass(ClassLoader classLoader) {
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
