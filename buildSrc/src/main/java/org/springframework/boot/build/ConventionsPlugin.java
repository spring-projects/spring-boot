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

import io.spring.javaformat.gradle.FormatTask;
import io.spring.javaformat.gradle.SpringJavaFormatPlugin;
import org.asciidoctor.gradle.jvm.AsciidoctorJPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.quality.CheckstylePlugin;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;
import org.gradle.testretry.TestRetryPlugin;

import org.springframework.boot.build.testing.TestFailuresPlugin;

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
 * When the {@link MavenPublishPlugin} is applied, the conventions in
 * {@link MavenPublishingConventions} are applied.
 *
 * When the {@link AsciidoctorJPlugin} is applied, the conventions in
 * {@link AsciidoctorConventions} are applied.
 *
 * @author Andy Wilkinson
 * @author Christoph Dreis
 * @author Mike Smithson
 */
public class ConventionsPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		applyJavaConventions(project);
		applyAsciidoctorConventions(project);
		applyMavenPublishingConventions(project);
	}

	private void applyJavaConventions(Project project) {
		new JavaConventions().apply(project);
	}

	private void applyAsciidoctorConventions(Project project) {
		new AsciidoctorConventions().apply(project);
	}

	private void applyMavenPublishingConventions(Project project) {
		new MavenPublishingConventions().applyPublishingConventions(project);
	}

}
