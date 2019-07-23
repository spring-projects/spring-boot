/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.diagnostics.analyzer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.util.ClassUtils;

/**
 * An {@link AbstractFailureAnalyzer} that analyzes {@link NoSuchMethodError
 * NoSuchMethodErrors}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
class NoSuchMethodFailureAnalyzer extends AbstractFailureAnalyzer<NoSuchMethodError> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, NoSuchMethodError cause) {
		NoSuchMethodDescriptor descriptor = getNoSuchMethodDescriptor(cause.getMessage());
		if (descriptor == null) {
			return null;
		}
		String description = getDescription(cause, descriptor);
		return new FailureAnalysis(description,
				"Correct the classpath of your application so that it contains a single, compatible version of "
						+ descriptor.getClassName(),
				cause);
	}

	protected NoSuchMethodDescriptor getNoSuchMethodDescriptor(String cause) {
		String message = cleanMessage(cause);
		String className = extractClassName(message);
		if (className == null) {
			return null;
		}
		List<URL> candidates = findCandidates(className);
		if (candidates == null) {
			return null;
		}
		URL actual = getActual(className);
		if (actual == null) {
			return null;
		}
		return new NoSuchMethodDescriptor(message, className, candidates, actual);
	}

	private String cleanMessage(String message) {
		int loadedFromIndex = message.indexOf(" (loaded from");
		if (loadedFromIndex == -1) {
			return message;
		}
		return message.substring(0, loadedFromIndex);
	}

	private String extractClassName(String message) {
		if (message.startsWith("'") && message.endsWith("'")) {
			int splitIndex = message.indexOf(' ');
			if (splitIndex == -1) {
				return null;
			}
			message = message.substring(splitIndex + 1);
		}
		int descriptorIndex = message.indexOf('(');
		if (descriptorIndex == -1) {
			return null;
		}
		String classAndMethodName = message.substring(0, descriptorIndex);
		int methodNameIndex = classAndMethodName.lastIndexOf('.');
		if (methodNameIndex == -1) {
			return null;
		}
		String className = classAndMethodName.substring(0, methodNameIndex);
		return className.replace('/', '.');
	}

	private List<URL> findCandidates(String className) {
		try {
			return Collections.list(NoSuchMethodFailureAnalyzer.class.getClassLoader()
					.getResources(ClassUtils.convertClassNameToResourcePath(className) + ".class"));
		}
		catch (Throwable ex) {
			return null;
		}
	}

	private URL getActual(String className) {
		try {
			return getClass().getClassLoader().loadClass(className).getProtectionDomain().getCodeSource().getLocation();
		}
		catch (Throwable ex) {
			return null;
		}
	}

	private String getDescription(NoSuchMethodError cause, NoSuchMethodDescriptor descriptor) {
		StringWriter description = new StringWriter();
		PrintWriter writer = new PrintWriter(description);
		writer.println("An attempt was made to call a method that does not"
				+ " exist. The attempt was made from the following location:");
		writer.println();
		writer.print("    ");
		writer.println(cause.getStackTrace()[0]);
		writer.println();
		writer.println("The following method did not exist:");
		writer.println();
		writer.print("    ");
		writer.println(descriptor.getErrorMessage());
		writer.println();
		writer.println(
				"The method's class, " + descriptor.getClassName() + ", is available from the following locations:");
		writer.println();
		for (URL candidate : descriptor.getCandidateLocations()) {
			writer.print("    ");
			writer.println(candidate);
		}
		writer.println();
		writer.println("It was loaded from the following location:");
		writer.println();
		writer.print("    ");
		writer.println(descriptor.getActualLocation());
		return description.toString();
	}

	protected static class NoSuchMethodDescriptor {

		private final String errorMessage;

		private final String className;

		private final List<URL> candidateLocations;

		private final URL actualLocation;

		public NoSuchMethodDescriptor(String errorMessage, String className, List<URL> candidateLocations,
				URL actualLocation) {
			this.errorMessage = errorMessage;
			this.className = className;
			this.candidateLocations = candidateLocations;
			this.actualLocation = actualLocation;
		}

		public String getErrorMessage() {
			return this.errorMessage;
		}

		public String getClassName() {
			return this.className;
		}

		public List<URL> getCandidateLocations() {
			return this.candidateLocations;
		}

		public URL getActualLocation() {
			return this.actualLocation;
		}

	}

}
