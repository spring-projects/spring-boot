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

package org.springframework.boot.gradle.plugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;

import org.springframework.boot.gradle.dsl.SpringBootExtension;
import org.springframework.boot.loader.tools.MainClassFinder;

/**
 * A {@link Callable} that provides a convention for the project's main class name.
 *
 * @author Andy Wilkinson
 */
final class MainClassConvention implements Callable<Object> {

	private static final String SPRING_BOOT_APPLICATION_CLASS_NAME = "org.springframework.boot.autoconfigure.SpringBootApplication";

	private final Project project;

	private final Supplier<FileCollection> classpathSupplier;

	MainClassConvention(Project project, Supplier<FileCollection> classpathSupplier) {
		this.project = project;
		this.classpathSupplier = classpathSupplier;
	}

	@Override
	public Object call() throws Exception {
		SpringBootExtension springBootExtension = this.project.getExtensions().findByType(SpringBootExtension.class);
		if (springBootExtension != null && springBootExtension.getMainClassName() != null) {
			return springBootExtension.getMainClassName();
		}
		if (this.project.hasProperty("mainClassName")) {
			Object mainClassName = this.project.property("mainClassName");
			if (mainClassName != null) {
				return mainClassName;
			}
		}
		return resolveMainClass();
	}

	private String resolveMainClass() {
		return this.classpathSupplier.get().filter(File::isDirectory).getFiles().stream().map(this::findMainClass)
				.filter(Objects::nonNull).findFirst().orElseThrow(() -> new InvalidUserDataException(
						"Main class name has not been configured and it could not be resolved"));
	}

	private String findMainClass(File file) {
		try {
			return MainClassFinder.findSingleMainClass(file, SPRING_BOOT_APPLICATION_CLASS_NAME);
		}
		catch (IOException ex) {
			return null;
		}
	}

}
