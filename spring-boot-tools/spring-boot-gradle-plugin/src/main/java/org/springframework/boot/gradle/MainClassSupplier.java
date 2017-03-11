/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Supplier;

import org.gradle.api.file.FileCollection;

import org.springframework.boot.loader.tools.MainClassFinder;

/**
 * Supplies the main class for an application by returning a configured main class if
 * available. If a main class is not available, directories in the application's classpath
 * are searched.
 *
 * @author Andy Wilkinson
 */
public class MainClassSupplier implements Supplier<String> {

	private static final String SPRING_BOOT_APPLICATION_CLASS_NAME = "org.springframework.boot.autoconfigure.SpringBootApplication";

	private final Supplier<FileCollection> classpathSupplier;

	private String mainClass;

	/**
	 * Creates a new {@code MainClassSupplier} that will fall back to searching
	 * directories in the classpath supplied by the given {@code classpathSupplier} for
	 * the application's main class.
	 *
	 * @param classpathSupplier the supplier of the classpath
	 */
	public MainClassSupplier(Supplier<FileCollection> classpathSupplier) {
		this.classpathSupplier = classpathSupplier;
	}

	@Override
	public String get() {
		if (this.mainClass == null) {
			this.mainClass = findMainClass();
		}
		return this.mainClass;
	}

	private String findMainClass() {
		FileCollection classpath = this.classpathSupplier.get();
		if (classpath == null) {
			return null;
		}
		return classpath.filter(File::isDirectory).getFiles().stream()
				.map(this::findMainClass).filter(Objects::nonNull).findFirst()
				.orElse(null);
	}

	private String findMainClass(File file) {
		try {
			String result = MainClassFinder.findSingleMainClass(file,
					SPRING_BOOT_APPLICATION_CLASS_NAME);
			return result;
		}
		catch (IOException ex) {
			return null;
		}
	}

	/**
	 * Sets the {@code mainClass} that will be supplied.
	 *
	 * @param mainClass the main class to supply
	 */
	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

}
