/*
 * Copyright 2012-2020 the original author or authors.
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
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import com.samskivert.mustache.Mustache;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.MavenInvocationException;

import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Builds a Spring Boot application using Maven.
 *
 * @author Andy Wilkinson
 */
class ApplicationBuilder {

	private final Path temp;

	private final String packaging;

	private final String container;

	ApplicationBuilder(Path temp, String packaging, String container) {
		this.temp = temp;
		this.packaging = packaging;
		this.container = container;
	}

	File buildApplication() throws Exception {
		File containerDirectory = new File(this.temp.toFile(), this.container);
		if (containerDirectory.exists()) {
			return new File(containerDirectory, "app/target/app-0.0.1." + this.packaging);
		}
		return doBuildApplication(containerDirectory);
	}

	String getPackaging() {
		return this.packaging;
	}

	String getContainer() {
		return this.container;
	}

	private File doBuildApplication(File containerDirectory) throws IOException, MavenInvocationException {
		File resourcesJar = createResourcesJar();
		File appDirectory = new File(containerDirectory, "app");
		appDirectory.mkdirs();
		File settingsXml = writeSettingsXml(appDirectory);
		writePom(appDirectory, resourcesJar);
		copyApplicationSource(appDirectory);
		packageApplication(appDirectory, settingsXml);
		return new File(appDirectory, "target/app-0.0.1." + this.packaging);
	}

	private File createResourcesJar() throws IOException {
		File resourcesJar = new File(this.temp.toFile(), "resources.jar");
		if (resourcesJar.exists()) {
			return resourcesJar;
		}
		try (JarOutputStream resourcesJarStream = new JarOutputStream(new FileOutputStream(resourcesJar))) {
			resourcesJarStream.putNextEntry(new ZipEntry("META-INF/resources/"));
			resourcesJarStream.closeEntry();
			resourcesJarStream.putNextEntry(new ZipEntry("META-INF/resources/nested-meta-inf-resource.txt"));
			resourcesJarStream.write("nested".getBytes());
			resourcesJarStream.closeEntry();
			if (!isWindows()) {
				resourcesJarStream.putNextEntry(
						new ZipEntry("META-INF/resources/nested-reserved-!#$%&()*+,:=?@[]-meta-inf-resource.txt"));
				resourcesJarStream.write("encoded-name".getBytes());
				resourcesJarStream.closeEntry();
			}
			return resourcesJar;
		}
	}

	private void writePom(File appDirectory, File resourcesJar) throws IOException {
		Map<String, Object> context = new HashMap<>();
		context.put("packaging", this.packaging);
		context.put("container", this.container);
		context.put("bootVersion", Versions.getBootVersion());
		context.put("resourcesJarPath", resourcesJar.getAbsolutePath());
		try (FileWriter out = new FileWriter(new File(appDirectory, "pom.xml"));
				FileReader templateReader = new FileReader("src/test/resources/pom-template.xml")) {
			Mustache.compiler().escapeHTML(false).compile(templateReader).execute(context, out);
		}
	}

	private File writeSettingsXml(File appDirectory) throws IOException {
		Map<String, Object> context = new HashMap<>();
		context.put("repository", new File("build/test-repository").toURI().toURL());
		context.put("localRepository", new File("build/local-m2-repository").getAbsolutePath());
		File settingsXml = new File(appDirectory, "settings.xml");
		try (FileWriter out = new FileWriter(settingsXml);
				FileReader templateReader = new FileReader("src/test/resources/settings-template.xml")) {
			Mustache.compiler().escapeHTML(false).compile(templateReader).execute(context, out);
		}
		return settingsXml;
	}

	private void copyApplicationSource(File appDirectory) throws IOException {
		File examplePackage = new File(appDirectory, "src/main/java/com/example");
		examplePackage.mkdirs();
		FileCopyUtils.copy(new File("src/test/java/com/example/ResourceHandlingApplication.java"),
				new File(examplePackage, "ResourceHandlingApplication.java"));
		// To allow aliased resources on Concourse Windows CI (See gh-15553) to be served
		// as static resources.
		if (this.container.equals("jetty")) {
			FileCopyUtils.copy(new File("src/test/java/com/example/JettyServerCustomizerConfig.java"),
					new File(examplePackage, "JettyServerCustomizerConfig.java"));
		}
		if ("war".equals(this.packaging)) {
			File srcMainWebapp = new File(appDirectory, "src/main/webapp");
			srcMainWebapp.mkdirs();
			FileCopyUtils.copy("webapp resource", new FileWriter(new File(srcMainWebapp, "webapp-resource.txt")));
		}
		copyAutoConfigurationFiles(appDirectory);
		return;
	}

	private void copyAutoConfigurationFiles(File appDirectory) throws IOException {
		File autoConfigPackage = new File(appDirectory, "src/main/java/com/autoconfig");
		autoConfigPackage.mkdirs();
		FileCopyUtils.copy(new File("src/test/java/com/autoconfig/ExampleAutoConfiguration.java"),
				new File(autoConfigPackage, "ExampleAutoConfiguration.java"));
		File srcMainResources = new File(appDirectory, "src/main/resources");
		srcMainResources.mkdirs();
		File metaInf = new File(srcMainResources, "META-INF");
		metaInf.mkdirs();
		FileCopyUtils.copy(new File("src/test/resources/META-INF/spring.factories"),
				new File(metaInf, "spring.factories"));
		FileCopyUtils.copy(new File("src/test/resources/application.yml"),
				new File(srcMainResources, "application.yml"));
	}

	private void packageApplication(File appDirectory, File settingsXml) throws MavenInvocationException {
		InvocationRequest invocation = new DefaultInvocationRequest();
		invocation.setBaseDirectory(appDirectory);
		invocation.setGoals(Collections.singletonList("package"));
		if (settingsXml != null) {
			invocation.setUserSettingsFile(settingsXml);
		}
		DefaultInvoker invoker = new DefaultInvoker();
		invoker.setMavenHome(new File("build/maven-binaries/apache-maven-3.6.2"));
		InvocationResult execute = invoker.execute(invocation);
		assertThat(execute.getExitCode()).isEqualTo(0);
	}

	private boolean isWindows() {
		return File.separatorChar == '\\';
	}

}
