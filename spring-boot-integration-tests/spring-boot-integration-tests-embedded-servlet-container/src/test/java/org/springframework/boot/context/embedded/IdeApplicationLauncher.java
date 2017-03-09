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

package org.springframework.boot.context.embedded;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.springframework.util.FileSystemUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * {@link AbstractApplicationLauncher} that launches a Spring Boot application with a
 * classpath similar to that used when run in an IDE.
 *
 * @author Andy Wilkinson
 */
class IdeApplicationLauncher extends AbstractApplicationLauncher {

	private final File exploded = new File("target/ide");

	IdeApplicationLauncher(ApplicationBuilder applicationBuilder) {
		super(applicationBuilder);
	}

	@Override
	protected File getWorkingDirectory() {
		return this.exploded;
	}

	@Override
	protected String getDescription(String packaging) {
		return "IDE run " + packaging + " project";
	}

	@Override
	protected List<String> getArguments(File archive) {
		try {
			explodeArchive(archive, this.exploded);
			deleteLauncherClasses();
			File targetClasses = populateTargetClasses(archive);
			File dependencies = populateDependencies(archive);
			File resourcesProject = explodedResourcesProject(dependencies);
			populateSrcMainWebapp();
			List<String> classpath = new ArrayList<String>();
			classpath.add(targetClasses.getAbsolutePath());
			for (File dependency : dependencies.listFiles()) {
				classpath.add(dependency.getAbsolutePath());
			}
			classpath.add(resourcesProject.getAbsolutePath());
			return Arrays.asList("-cp",
					StringUtils.collectionToDelimitedString(classpath,
							File.pathSeparator),
					"com.example.ResourceHandlingApplication");
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private File populateTargetClasses(File archive) {
		File targetClasses = new File(this.exploded, "target/classes");
		targetClasses.mkdirs();
		new File(this.exploded, getClassesPath(archive)).renameTo(targetClasses);
		return targetClasses;
	}

	private File populateDependencies(File archive) {
		File dependencies = new File(this.exploded, "dependencies");
		dependencies.mkdirs();
		List<String> libPaths = getLibPaths(archive);
		for (String libPath : libPaths) {
			for (File jar : new File(this.exploded, libPath).listFiles()) {
				jar.renameTo(new File(dependencies, jar.getName()));
			}
		}
		return dependencies;
	}

	private File explodedResourcesProject(File dependencies) throws IOException {
		File resourcesProject = new File(this.exploded,
				"resources-project/target/classes");
		File resourcesJar = new File(dependencies, "resources-1.0.jar");
		explodeArchive(resourcesJar, resourcesProject);
		resourcesJar.delete();
		return resourcesProject;
	}

	private void populateSrcMainWebapp() {
		File srcMainWebapp = new File(this.exploded, "src/main/webapp");
		srcMainWebapp.mkdirs();
		new File(this.exploded, "webapp-resource.txt")
				.renameTo(new File(srcMainWebapp, "webapp-resource.txt"));
	}

	private void deleteLauncherClasses() {
		FileSystemUtils.deleteRecursively(new File(this.exploded, "org"));
	}

	private String getClassesPath(File archive) {
		return archive.getName().endsWith(".jar") ? "BOOT-INF/classes"
				: "WEB-INF/classes";
	}

	private List<String> getLibPaths(File archive) {
		return archive.getName().endsWith(".jar")
				? Collections.singletonList("BOOT-INF/lib")
				: Arrays.asList("WEB-INF/lib", "WEB-INF/lib-provided");
	}

	private void explodeArchive(File archive, File destination) throws IOException {
		FileSystemUtils.deleteRecursively(destination);
		JarFile jarFile = new JarFile(archive);
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry jarEntry = entries.nextElement();
			File extracted = new File(destination, jarEntry.getName());
			if (jarEntry.isDirectory()) {
				extracted.mkdirs();
			}
			else {
				FileOutputStream extractedOutputStream = new FileOutputStream(extracted);
				StreamUtils.copy(jarFile.getInputStream(jarEntry), extractedOutputStream);
				extractedOutputStream.close();
			}
		}
		jarFile.close();
	}

}
