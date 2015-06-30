/*
 * Copyright 2015 the original author or authors.
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
 */
public class FindMainClass extends Task {
	private String mainClass;
	private File classesRoot;
	private String property;

	public FindMainClass(Project project) {
		super();
		setProject(project);
	}

	@Override
	public void execute() throws BuildException {
		if (StringUtils.hasText(mainClass)) {
			handle(mainClass);
			return;
		}
		if (classesRoot == null) {
			throw new BuildException(
					"one of @mainClass or @classesRoot must be specified");
		}
		if (!classesRoot.exists()) {
			throw new BuildException("@classesRoot " + classesRoot + " does not exist");
		}
		final String foundClass;
		try {
			if (classesRoot.isDirectory()) {
				foundClass = MainClassFinder.findSingleMainClass(classesRoot);
			}
			else {
				foundClass = MainClassFinder.findSingleMainClass(
						new JarFile(classesRoot), "/");
			}
		}
		catch (IOException ex) {
			throw new BuildException(ex);
		}
		if (!StringUtils.hasText(foundClass)) {
			throw new BuildException("Could not determine main class given @classesRoot "
					+ classesRoot);
		}
		handle(foundClass);
	}

	private void handle(String mainClass) {
		if (StringUtils.hasText(property)) {
			getProject().setProperty(property, mainClass);
		}
		else {
			log("Found main class " + mainClass);
		}
	}

	/**
	 * Set the main class, which will cause the search to be bypassed.
	 * @param mainClass
	 */
	public void setMainClass(String mainClass) {
		this.mainClass = mainClass;
	}

	/**
	 * Set the root location of classes to be searched.
	 * @param classesRoot
	 */
	public void setClassesRoot(File classesRoot) {
		this.classesRoot = classesRoot;
	}

	/**
	 * Set the property to set (if unset, result will be printed to the log).
	 * @param property
	 */
	public void setProperty(String property) {
		this.property = property;
	}
}
