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

package org.springframework.boot.gradle.bundling;

import java.io.File;
import java.io.IOException;
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
class MainClassSupplier implements Supplier<String> {

	private final Supplier<FileCollection> classpathSupplier;

	private String mainClass;

	MainClassSupplier(Supplier<FileCollection> classpathSupplier) {
		this.classpathSupplier = classpathSupplier;
	}

	@Override
	public String get() {
		if (this.mainClass != null) {
			return this.mainClass;
		}
		return findMainClass();
	}

	private String findMainClass() {
		FileCollection classpath = this.classpathSupplier.get();
		return classpath == null ? null
				: classpath.filter(File::isDirectory).getFiles().stream()
						.map(this::findMainClass).findFirst().orElse(null);
	}

	private String findMainClass(File file) {
		try {
			return MainClassFinder.findSingleMainClass(file);
		}
		catch (IOException ex) {
			return null;
		}
	}

	void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

}
