/*
 * Copyright 2012-2018 the original author or authors.
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

import org.springframework.util.FileCopyUtils;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * {@link AbstractApplicationLauncher} that launches a Spring Boot application with a
 * classpath similar to that used when run with Maven or Gradle.
 *
 * @author Andy Wilkinson
 */
class BootRunApplicationLauncher extends AbstractApplicationLauncher {

	private final File exploded = new File("target/run");

	BootRunApplicationLauncher(ApplicationBuilder applicationBuilder) {
		super(applicationBuilder);
	}

	@Override
	protected List<String> getArguments(File archive) {
		try {
			explodeArchive(archive);
			deleteLauncherClasses();
			File targetClasses = populateTargetClasses(archive);
			File dependencies = populateDependencies(archive);
			if (archive.getName().endsWith(".war")) {
				populateSrcMainWebapp();
			}
			List<String> classpath = new ArrayList<String>();
			classpath.add(targetClasses.getAbsolutePath());
			for (File dependency : dependencies.listFiles()) {
				classpath.add(dependency.getAbsolutePath());
			}
			return Arrays.asList("-cp",
					StringUtils.collectionToDelimitedString(classpath,
							File.pathSeparator),
					"com.example.ResourceHandlingApplication");
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	private void deleteLauncherClasses() {
		FileSystemUtils.deleteRecursively(new File(this.exploded, "org"));
	}

	private File populateTargetClasses(File archive) throws IOException {
		File targetClasses = new File(this.exploded, "target/classes");
		targetClasses.mkdirs();
		File source = new File(this.exploded, getClassesPath(archive));
		FileSystemUtils.copyRecursively(source, targetClasses);
		FileSystemUtils.deleteRecursively(source);
		return targetClasses;
	}

	private File populateDependencies(File archive) throws IOException {
		File dependencies = new File(this.exploded, "dependencies");
		dependencies.mkdirs();
		List<String> libPaths = getLibPaths(archive);
		for (String libPath : libPaths) {
			File libDirectory = new File(this.exploded, libPath);
			for (File jar : libDirectory.listFiles()) {
				FileCopyUtils.copy(jar, new File(dependencies, jar.getName()));
			}
			FileSystemUtils.deleteRecursively(libDirectory);
		}
		return dependencies;
	}

	private void populateSrcMainWebapp() throws IOException {
		File srcMainWebapp = new File(this.exploded, "src/main/webapp");
		srcMainWebapp.mkdirs();
		File source = new File(this.exploded, "webapp-resource.txt");
		FileCopyUtils.copy(source, new File(srcMainWebapp, "webapp-resource.txt"));
		source.delete();
	}

	private String getClassesPath(File archive) {
		return (archive.getName().endsWith(".jar") ? "BOOT-INF/classes"
				: "WEB-INF/classes");
	}

	private List<String> getLibPaths(File archive) {
		return (archive.getName().endsWith(".jar")
				? Collections.singletonList("BOOT-INF/lib")
				: Arrays.asList("WEB-INF/lib", "WEB-INF/lib-provided"));
	}

	private void explodeArchive(File archive) throws IOException {
		FileSystemUtils.deleteRecursively(this.exploded);
		JarFile jarFile = new JarFile(archive);
		Enumeration<JarEntry> entries = jarFile.entries();
		while (entries.hasMoreElements()) {
			JarEntry jarEntry = entries.nextElement();
			File extracted = new File(this.exploded, jarEntry.getName());
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

	@Override
	protected File getWorkingDirectory() {
		return this.exploded;
	}

	@Override
	protected String getDescription(String packaging) {
		return "build system run " + packaging + " project";
	}

}
