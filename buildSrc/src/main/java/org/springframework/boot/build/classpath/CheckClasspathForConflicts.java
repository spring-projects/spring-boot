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

package org.springframework.boot.build.classpath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.TaskAction;

/**
 * A {@link Task} for checking the classpath for conflicting classes and resources.
 *
 * @author Andy Wilkinson
 */
public class CheckClasspathForConflicts extends DefaultTask {

	private final List<Predicate<String>> ignores = new ArrayList<>();

	private FileCollection classpath;

	public void setClasspath(FileCollection classpath) {
		this.classpath = classpath;
	}

	@Classpath
	public FileCollection getClasspath() {
		return this.classpath;
	}

	@TaskAction
	public void checkForConflicts() throws IOException {
		ClasspathContents classpathContents = new ClasspathContents();
		for (File file : this.classpath) {
			if (file.isDirectory()) {
				Path root = file.toPath();
				try (Stream<Path> pathStream = Files.walk(root)) {
					pathStream.filter(Files::isRegularFile)
						.forEach((entry) -> classpathContents.add(root.relativize(entry).toString(), root.toString()));
				}
			}
			else {
				try (JarFile jar = new JarFile(file)) {
					for (JarEntry entry : Collections.list(jar.entries())) {
						if (!entry.isDirectory()) {
							classpathContents.add(entry.getName(), file.getAbsolutePath());
						}
					}
				}
			}
		}
		Map<String, List<String>> conflicts = classpathContents.getConflicts(this.ignores);
		if (!conflicts.isEmpty()) {
			StringBuilder message = new StringBuilder(String.format("Found classpath conflicts:%n"));
			conflicts.forEach((entry, locations) -> {
				message.append(String.format("    %s%n", entry));
				locations.forEach((location) -> message.append(String.format("        %s%n", location)));
			});
			throw new GradleException(message.toString());
		}
	}

	public void ignore(Predicate<String> predicate) {
		this.ignores.add(predicate);
	}

	private static final class ClasspathContents {

		private static final Set<String> IGNORED_NAMES = new HashSet<>(Arrays.asList("about.html", "changelog.txt",
				"LICENSE", "license.txt", "module-info.class", "notice.txt", "readme.txt"));

		private final Map<String, List<String>> classpathContents = new HashMap<>();

		private void add(String name, String source) {
			this.classpathContents.computeIfAbsent(name, (key) -> new ArrayList<>()).add(source);
		}

		private Map<String, List<String>> getConflicts(List<Predicate<String>> ignores) {
			return this.classpathContents.entrySet()
				.stream()
				.filter((entry) -> entry.getValue().size() > 1)
				.filter((entry) -> canConflict(entry.getKey(), ignores))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (v1, v2) -> v1, TreeMap::new));
		}

		private boolean canConflict(String name, List<Predicate<String>> ignores) {
			if (name.startsWith("META-INF/")) {
				return false;
			}
			for (String ignoredName : IGNORED_NAMES) {
				if (name.equals(ignoredName)) {
					return false;
				}
			}
			for (Predicate<String> ignore : ignores) {
				if (ignore.test(name)) {
					return false;
				}
			}
			return true;
		}

	}

}
