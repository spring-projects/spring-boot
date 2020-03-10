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
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.asciidoctor.gradle.jvm.AsciidoctorJPlugin;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.file.CopySpec;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.CheckstylePlugin;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPom;
import org.gradle.api.publish.maven.MavenPomDeveloperSpec;
import org.gradle.api.publish.maven.MavenPomIssueManagement;
import org.gradle.api.publish.maven.MavenPomLicenseSpec;
import org.gradle.api.publish.maven.MavenPomOrganization;
import org.gradle.api.publish.maven.MavenPomScm;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.resources.TextResourceFactory;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;

import org.springframework.boot.build.testing.TestFailuresPlugin;
import org.springframework.util.FileCopyUtils;

/**
 * Plugin to apply conventions to projects that are part of Spring Boot's build.
 * Conventions are applied in response to various plugins being applied.
 *
 * <p/>
 *
 * When the {@link JavaBasePlugin Java base plugin} is applied:
 *
 * <ul>
 * <li>{@code sourceCompatibility} is set to {@code 1.8}
 * <li>{@link SpringJavaFormatPlugin Spring Java Format}, {@link CheckstylePlugin
 * Checkstyle}, and {@link TestFailuresPlugin Test Failures} plugins are applied
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
 * When the {@link MavenPublishPlugin Maven Publish plugin} is applied:
 *
 * <ul>
 * <li>If the {@code deploymentRepository} property has been set, a
 * {@link MavenArtifactRepository Maven artifact repository} is configured to publish to
 * it.
 * <li>The poms of all {@link MavenPublication Maven publications} are customized to meet
 * Maven Central's requirements.
 * <li>If the {@link JavaPlugin Java plugin} has also been applied, creation of Javadoc
 * and source jars is enabled.
 * </ul>
 *
 * <p/>
 *
 * When the {@link AsciidoctorJPlugin} is applied, the conventions in
 * {@link AsciidoctorConventions} are applied.
 *
 * @author Andy Wilkinson
 * @author Christoph Dreis
 */
public class ConventionsPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		applyJavaConventions(project);
		applyAsciidoctorConventions(project);
		applyMavenPublishingConventions(project);
	}

	private void applyJavaConventions(Project project) {
		project.getPlugins().withType(JavaBasePlugin.class, (java) -> {
			project.getPlugins().apply(TestFailuresPlugin.class);
			configureSpringJavaFormat(project);
			project.setProperty("sourceCompatibility", "1.8");
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
			project.getTasks().withType(Javadoc.class, (javadoc) -> {
				javadoc.getOptions().source("1.8").encoding("UTF-8");
				withOptionalBuildJavaHome(project, (javaHome) -> javadoc.setExecutable(javaHome + "/bin/javadoc"));
			});
			project.getTasks().withType(Test.class, (test) -> {
				withOptionalBuildJavaHome(project, (javaHome) -> test.setExecutable(javaHome + "/bin/java"));
				test.useJUnitPlatform();
				test.setMaxHeapSize("1024M");
			});
			project.getTasks().withType(Jar.class, (jar) -> {
				project.afterEvaluate((evaluated) -> {
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
				});
			});
		});
	}

	private void copyLegalFiles(Project project, CopySpec metaInf) {
		copyNoticeFile(project, metaInf);
		copyLicenseFile(project, metaInf);
	}

	private void copyNoticeFile(Project project, CopySpec metaInf) {
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

	private void copyLicenseFile(Project project, CopySpec metaInf) {
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

	private File createLegalFile(File source, String filename) {
		File legalFile = new File(source.getParentFile(), filename);
		source.renameTo(legalFile);
		return legalFile;
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

	private void applyAsciidoctorConventions(Project project) {
		new AsciidoctorConventions().apply(project);
	}

	private void applyMavenPublishingConventions(Project project) {
		project.getPlugins().withType(MavenPublishPlugin.class).all((mavenPublish) -> {
			PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
			if (project.hasProperty("deploymentRepository")) {
				publishing.getRepositories().maven((mavenRepository) -> {
					mavenRepository.setUrl(project.property("deploymentRepository"));
					mavenRepository.setName("deployment");
				});
			}
			publishing.getPublications().withType(MavenPublication.class)
					.all((mavenPublication) -> customizePom(mavenPublication.getPom(), project));
			project.getPlugins().withType(JavaPlugin.class).all((javaPlugin) -> {
				JavaPluginExtension extension = project.getExtensions().getByType(JavaPluginExtension.class);
				extension.withJavadocJar();
				extension.withSourcesJar();
			});
		});
	}

	private void customizePom(MavenPom pom, Project project) {
		pom.getUrl().set("https://projects.spring.io/spring-boot/#");
		pom.getDescription().set(project.provider(project::getDescription));
		pom.organization(this::customizeOrganization);
		pom.licenses(this::customizeLicences);
		pom.developers(this::customizeDevelopers);
		pom.scm(this::customizeScm);
		pom.issueManagement(this::customizeIssueManagement);
	}

	private void customizeOrganization(MavenPomOrganization organization) {
		organization.getName().set("Pivotal Software, Inc.");
		organization.getUrl().set("https://spring.io");
	}

	private void customizeLicences(MavenPomLicenseSpec licences) {
		licences.license((licence) -> {
			licence.getName().set("Apache License, Version 2.0");
			licence.getUrl().set("http://www.apache.org/licenses/LICENSE-2.0");
		});
	}

	private void customizeDevelopers(MavenPomDeveloperSpec developers) {
		developers.developer((developer) -> {
			developer.getName().set("Pivotal");
			developer.getEmail().set("info@pivotal.io");
			developer.getOrganization().set("Pivotal Software, Inc.");
			developer.getOrganizationUrl().set("https://www.spring.io");
		});
	}

	private void customizeScm(MavenPomScm scm) {
		scm.getConnection().set("scm:git:git://github.com/spring-projects/spring-boot.git");
		scm.getDeveloperConnection().set("scm:git:ssh://git@github.com/spring-projects/spring-boot.git");
		scm.getUrl().set("https://github.com/spring-projects/spring-boot");

	}

	private void customizeIssueManagement(MavenPomIssueManagement issueManagement) {
		issueManagement.getSystem().set("GitHub");
		issueManagement.getUrl().set("https://github.com/spring-projects/spring-boot/issues");
	}

}
