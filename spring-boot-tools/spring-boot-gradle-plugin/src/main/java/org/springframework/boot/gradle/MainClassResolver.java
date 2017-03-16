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

import org.gradle.api.file.FileCollection;

import org.springframework.boot.loader.tools.MainClassFinder;

/**
 * Resolves the main class for an application.
 *
 * @author Andy Wilkinson
 */
public class MainClassResolver {

	private static final String SPRING_BOOT_APPLICATION_CLASS_NAME = "org.springframework.boot.autoconfigure.SpringBootApplication";

	private final FileCollection classpath;

	/**
	 * Creates a new {@code MainClassResolver} that will search
	 * directories in the given {@code classpath} for
	 * the application's main class.
	 *
	 * @param classpath the classpath
	 */
	public MainClassResolver(FileCollection classpath) {
		this.classpath = classpath;
	}

	/**
	 * Resolves the main class.
	 *
	 * @return the main class or {@code null}
	 */
	public String resolveMainClass() {
		return this.classpath.filter(File::isDirectory).getFiles().stream()
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

}
