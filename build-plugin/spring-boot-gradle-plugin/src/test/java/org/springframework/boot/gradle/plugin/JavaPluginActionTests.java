/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.gradle.plugin;

import org.gradle.api.NamedDomainObjectProvider;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Attribute;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JavaPluginActionTests {

	private static final Attribute<String> testAttribute =
			Attribute.of("test", String.class);

	@Test
	public void productionRuntimeClasspathCopiesRuntimeClasspathAttributes() {
		Project project = ProjectBuilder.builder().build();
		project.getPluginManager().apply("application");
		JavaPluginAction.configureProductionRuntimeClasspathConfiguration(project);

		Configuration runtimeClasspath = project.getConfigurations().getByName("runtimeClasspath");
		Configuration productionRuntimeClasspath =
				project.getConfigurations().getByName(SpringBootPlugin.PRODUCTION_RUNTIME_CLASSPATH_CONFIGURATION_NAME);
		runtimeClasspath.getAttributes().keySet().forEach(attribute ->
				assertThat(productionRuntimeClasspath.getAttributes().getAttribute(attribute))
						.as(attribute.getName() + " is copied from runtime")
						.isEqualTo(runtimeClasspath.getAttributes().getAttribute(attribute)));
	}

	@Test
	public void productionRuntimeClasspathHasProperUsage() {
		Project project = ProjectBuilder.builder().build();
		project.getPluginManager().apply("application");
		JavaPluginAction.configureProductionRuntimeClasspathConfiguration(project);
		Configuration productionRuntimeClasspath =
				project.getConfigurations().getByName(SpringBootPlugin.PRODUCTION_RUNTIME_CLASSPATH_CONFIGURATION_NAME);
		assertThat(productionRuntimeClasspath.isCanBeResolved()).isTrue();
		assertThat(productionRuntimeClasspath.isCanBeConsumed()).isFalse();
		assertThat(productionRuntimeClasspath.isCanBeDeclared())
				.as("is declarable (dependencyScope)")
				.isTrue();
	}

	@Test
	public void developmentOnlyCopiesRuntimeClasspathAttributes() {
		Project project = ProjectBuilder.builder().build();
		project.getPluginManager().apply("application");
		JavaPluginAction.configureDevelopmentOnlyConfiguration(project);

		Configuration runtimeClasspath = project.getConfigurations().getByName("runtimeClasspath");
		Configuration developmentOnly =
				project.getConfigurations().getByName(SpringBootPlugin.DEVELOPMENT_ONLY_CONFIGURATION_NAME);
		runtimeClasspath.getAttributes().keySet().forEach(attribute ->
				assertThat(developmentOnly.getAttributes().getAttribute(attribute))
						.as(attribute.getName() + " is copied from runtime")
						.isEqualTo(runtimeClasspath.getAttributes().getAttribute(attribute)));
	}

	@Test
	public void developmentOnlyHasProperUsage() {
		Project project = ProjectBuilder.builder().build();
		project.getPluginManager().apply("application");
		JavaPluginAction.configureDevelopmentOnlyConfiguration(project);
		Configuration productionRuntimeClasspath =
				project.getConfigurations().getByName(SpringBootPlugin.DEVELOPMENT_ONLY_CONFIGURATION_NAME);
		assertThat(productionRuntimeClasspath.isCanBeResolved()).isTrue();
		assertThat(productionRuntimeClasspath.isCanBeConsumed()).isFalse();
		assertThat(productionRuntimeClasspath.isCanBeDeclared())
				.as("is declarable (dependencyScope)")
				.isTrue();
	}

	@Test
	public void testAndDevelopmentOnlyCopiesRuntimeClasspathAttributes() {
		Project project = ProjectBuilder.builder().build();
		project.getPluginManager().apply("application");
		JavaPluginAction.configureTestAndDevelopmentOnlyConfiguration(project);

		Configuration runtimeClasspath = project.getConfigurations().getByName("runtimeClasspath");
		Configuration testAndDevelopmentOnly =
				project.getConfigurations().getByName(SpringBootPlugin.TEST_AND_DEVELOPMENT_ONLY_CONFIGURATION_NAME);
		runtimeClasspath.getAttributes().keySet().forEach(attribute ->
				assertThat(testAndDevelopmentOnly.getAttributes().getAttribute(attribute))
						.as(attribute.getName() + " is copied from runtime")
						.isEqualTo(runtimeClasspath.getAttributes().getAttribute(attribute)));
	}

	@Test
	public void testAndDevelopmentOnlyHasProperUsage() {
		Project project = ProjectBuilder.builder().build();
		project.getPluginManager().apply("application");
		JavaPluginAction.configureTestAndDevelopmentOnlyConfiguration(project);
		Configuration productionRuntimeClasspath =
				project.getConfigurations().getByName(SpringBootPlugin.TEST_AND_DEVELOPMENT_ONLY_CONFIGURATION_NAME);
		assertThat(productionRuntimeClasspath.isCanBeResolved()).isTrue();
		assertThat(productionRuntimeClasspath.isCanBeConsumed()).isFalse();
		assertThat(productionRuntimeClasspath.isCanBeDeclared())
				.as("is declarable (dependencyScope)")
				.isTrue();
	}

	@Test
	public void copyAttributesIsLazy() {
		Project project = ProjectBuilder.builder().build();
		Configuration a = project.getConfigurations().create("a");
		Configuration b = project.getConfigurations().create("b");
		JavaPluginAction.copyAttributes(project.getConfigurations().named("a"), b);
		a.getAttributes().attribute(testAttribute, "value");
		assertThat(b.getAttributes().getAttribute(testAttribute))
				.as("Ideal implementation is always lazy, even if configuration is resolved early")
				.isEqualTo("value");
	}

	@Test
	public void copyAttributesLegacyIsNotLazy() {
		Project project = ProjectBuilder.builder().build();
		NamedDomainObjectProvider<Configuration> a = project.getConfigurations().register("a");
		Configuration b = project.getConfigurations().create("b");
		JavaPluginAction.copyAttributesLegacy(a, b);
		a.configure( it -> it.getAttributes().attribute(testAttribute, "value"));

		assertThat(b.getAttributes().getAttribute(testAttribute))
				.as("implementation for Gradle < 9 is not properly lazy (known limitation)")
				.isNull();
	}
}
