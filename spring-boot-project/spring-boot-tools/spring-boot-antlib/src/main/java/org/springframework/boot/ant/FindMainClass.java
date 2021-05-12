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

package org.springframework.boot.ant;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import org.springframework.boot.loader.tools.MainClassFinder;
import org.springframework.util.StringUtils;

/**
 * Ant task to find a main class.
 *
 * @author Matt Benson
 * @since 1.3.0
 */
public class FindMainClass extends Task {

	private static final String SPRING_BOOT_APPLICATION_CLASS_NAME = "org.springframework.boot.autoconfigure.SpringBootApplication";

	private String mainClass;

	private File classesRoot;

	private String property;

	public FindMainClass(Project project) {
		setProject(project);
	}

	@Override
	public void execute() throws BuildException {
		String mainClass = this.mainClass;
		if (!StringUtils.hasText(mainClass)) {
			mainClass = findMainClass();
			if (!StringUtils.hasText(mainClass)) {
				throw new BuildException("Could not determine main class given @classesRoot " + this.classesRoot);
			}
		}
		handle(mainClass);
	}

	private String findMainClass() {
		if (this.classesRoot == null) {
			throw new BuildException("one of @mainClass or @classesRoot must be specified");
		}
		if (!this.classesRoot.exists()) {
			throw new BuildException("@classesRoot " + this.classesRoot + " does not exist");
		}
		try {
			if (this.classesRoot.isDirectory()) {
				return MainClassFinder.findSingleMainClass(this.classesRoot, SPRING_BOOT_APPLICATION_CLASS_NAME);
			}
			return MainClassFinder.findSingleMainClass(new JarFile(this.classesRoot), "/",
					SPRING_BOOT_APPLICATION_CLASS_NAME);
		}
		catch (IOException ex) {
			throw new BuildException(ex);
		}
	}

	private void handle(String mainClass) {
		if (StringUtils.hasText(this.property)) {
			getProject().setProperty(this.property, mainClass);
		}
		else {
			log("Found main class " + mainClass);
		}
	}

	/**
	 * Set the main class, which will cause the search to be bypassed.
	 * @param mainClass the main class name
	 */
	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	/**
	 * Set the root location of classes to be searched.
	 * @param classesRoot the root location
	 */
	public void setClassesRoot(File classesRoot) {
		this.classesRoot = classesRoot;
	}

	/**
	 * Set the ANT property to set (if left unset, result will be printed to the log).
	 * @param property the ANT property to set
	 */
	public void setProperty(String property) {
		this.property = property;
	}

}
