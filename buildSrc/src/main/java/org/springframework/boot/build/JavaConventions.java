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
package org.springframework.boot.build;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import io.spring.javaformat.gradle.FormatTask;
import io.spring.javaformat.gradle.SpringJavaFormatPlugin;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.file.CopySpec;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.CheckstylePlugin;
import org.gradle.api.resources.TextResourceFactory;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;
import org.gradle.testretry.TestRetryPlugin;
import org.gradle.testretry.TestRetryTaskExtension;

import org.springframework.boot.build.testing.TestFailuresPlugin;
import org.springframework.util.FileCopyUtils;

/**
 * Conventions that are applied in the presence of the {@link JavaBasePlugin}. When the
 * plugin is applied:
 *
 * <ul>
 * <li>{@code sourceCompatibility} is set to {@code 1.8}
 * <li>{@link SpringJavaFormatPlugin Spring Java Format}, {@link CheckstylePlugin
 * Checkstyle}, {@link TestFailuresPlugin Test Failures}, and {@link TestRetryPlugin Test
 * Retry} plugins are applied
 * <li>{@link Test} tasks are configured to use JUnit Platform and use a max heap of 1024M
 * <li>{@link JavaCompile}, {@link Javadoc}, and {@link FormatTask} tasks are configured
 * to use UTF-8 encoding
 * <li>{@link JavaCompile} tasks are configured to use {@code -parameters}
 * <li>{@link Jar} tasks are configured to produce jars with LICENSE.txt and NOTICE.txt
 * files and the following manifest entries:
 * <ul>
 * <li>{@code Automatic-Module-Name}
 * <li>{@code Build-Jdk-Spec}
 * <li>{@code Built-By}
 * <li>{@code Implementation-Title}
 * <li>{@code Implementation-Version}
 * </ul>
 * </ul>
 *
 * <p/>
 *
 * @author Andy Wilkinson
 * @author Christoph Dreis
 * @author Mike Smithson
 */
class JavaConventions {

	void apply(Project project) {
		project.getPlugins().withType(JavaBasePlugin.class, (java) -> {
			project.getPlugins().apply(TestFailuresPlugin.class);
			configureSpringJavaFormat(project);
			project.setProperty("sourceCompatibility", "1.8");
			configureJavaCompileConventions(project);
			configureJavadocConventions(project);
			configureTestConventions(project);
			configureJarManifestConventions(project);
		});
	}

	private void configureJarManifestConventions(Project project) {
		project.getTasks().withType(Jar.class, (jar) -> project.afterEvaluate((evaluated) -> {
			jar.metaInf((metaInf) -> copyLegalFiles(project, metaInf));
			jar.manifest((manifest) -> {
				Map<String, Object> attributes = new TreeMap<>();
				attributes.put("Automatic-Module-Name", project.getName().replace("-", "."));
				attributes.put("Build-Jdk-Spec", project.property("sourceCompatibility"));
				attributes.put("Built-By", "Spring");
				attributes.put("Implementation-Title", project.getDescription());
				attributes.put("Implementation-Version", project.getVersion());
				manifest.attributes(attributes);
			});
		}));
	}

	private void configureTestConventions(Project project) {
		project.getPlugins().apply(TestRetryPlugin.class);
		project.getTasks().withType(Test.class, (test) -> {
			withOptionalBuildJavaHome(project, (javaHome) -> test.setExecutable(javaHome + "/bin/java"));
			test.useJUnitPlatform();
			test.setMaxHeapSize("1024M");
			project.getPlugins().withType(TestRetryPlugin.class, (testRetryPlugin) -> {
				TestRetryTaskExtension testRetry = test.getExtensions().getByType(TestRetryTaskExtension.class);
				testRetry.getFailOnPassedAfterRetry().set(true);
				testRetry.getMaxRetries().set(3);
			});
		});
	}

	private void configureJavadocConventions(Project project) {
		project.getTasks().withType(Javadoc.class, (javadoc) -> {
			javadoc.getOptions().source("1.8").encoding("UTF-8");
			withOptionalBuildJavaHome(project, (javaHome) -> javadoc.setExecutable(javaHome + "/bin/javadoc"));
		});
	}

	private void configureJavaCompileConventions(Project project) {
		project.getTasks().withType(JavaCompile.class, (compile) -> {
			compile.getOptions().setEncoding("UTF-8");
			withOptionalBuildJavaHome(project, (javaHome) -> {
				compile.getOptions().setFork(true);
				compile.getOptions().getForkOptions().setJavaHome(new File(javaHome));
				compile.getOptions().getForkOptions().setExecutable(javaHome + "/bin/javac");
			});
			List<String> args = compile.getOptions().getCompilerArgs();
			if (!args.contains("-parameters")) {
				args.add("-parameters");
			}
		});
	}

	private void withOptionalBuildJavaHome(Project project, Consumer<String> consumer) {
		String buildJavaHome = (String) project.findProperty("buildJavaHome");
		if (buildJavaHome != null && !buildJavaHome.isEmpty()) {
			consumer.accept(buildJavaHome);
		}
	}

	private void configureSpringJavaFormat(Project project) {
		project.getPlugins().apply(SpringJavaFormatPlugin.class);
		project.getTasks().withType(FormatTask.class, (formatTask) -> formatTask.setEncoding("UTF-8"));
		project.getPlugins().apply(CheckstylePlugin.class);
		CheckstyleExtension checkstyle = project.getExtensions().getByType(CheckstyleExtension.class);
		checkstyle.setToolVersion("8.29");
		checkstyle.getConfigDirectory().set(project.getRootProject().file("src/checkstyle"));
		String version = SpringJavaFormatPlugin.class.getPackage().getImplementationVersion();
		DependencySet checkstyleDependencies = project.getConfigurations().getByName("checkstyle").getDependencies();
		checkstyleDependencies
				.add(project.getDependencies().create("io.spring.javaformat:spring-javaformat-checkstyle:" + version));
		checkstyleDependencies
				.add(project.getDependencies().create("io.spring.nohttp:nohttp-checkstyle:0.0.3.RELEASE"));
	}

	void copyLegalFiles(Project project, CopySpec metaInf) {
		copyNoticeFile(project, metaInf);
		copyLicenseFile(project, metaInf);
	}

	void copyNoticeFile(Project project, CopySpec metaInf) {
		try {
			InputStream notice = getClass().getClassLoader().getResourceAsStream("NOTICE.txt");
			String noticeContent = FileCopyUtils.copyToString(new InputStreamReader(notice, StandardCharsets.UTF_8))
					.replace("${version}", project.getVersion().toString());
			TextResourceFactory resourceFactory = project.getResources().getText();
			File file = createLegalFile(resourceFactory.fromString(noticeContent).asFile(), "NOTICE.txt");
			metaInf.from(file);
		}
		catch (IOException ex) {
			throw new GradleException("Failed to copy NOTICE.txt", ex);
		}
	}

	void copyLicenseFile(Project project, CopySpec metaInf) {
		URL license = getClass().getClassLoader().getResource("LICENSE.txt");
		try {
			TextResourceFactory resourceFactory = project.getResources().getText();
			File file = createLegalFile(resourceFactory.fromUri(license.toURI()).asFile(), "LICENSE.txt");
			metaInf.from(file);
		}
		catch (URISyntaxException ex) {
			throw new GradleException("Failed to copy LICENSE.txt", ex);
		}
	}

	File createLegalFile(File source, String filename) {
		File legalFile = new File(source.getParentFile(), filename);
		source.renameTo(legalFile);
		return legalFile;
	}

}
