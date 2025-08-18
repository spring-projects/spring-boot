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

package org.springframework.boot.build.architecture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.gradle.api.tasks.SourceSet;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.gradle.testkit.runner.UnexpectedBuildSuccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.springframework.util.ClassUtils;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ArchitectureCheck}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author Ivan Malutin
 * @author Dmytro Nosan
 */
class ArchitectureCheckTests {

	private static final String SPRING_CONTEXT = "org.springframework:spring-context:6.2.9";

	private static final String SPRING_INTEGRATION_JMX = "org.springframework.integration:spring-integration-jmx:6.5.1";

	private GradleBuild gradleBuild;

	@BeforeEach
	void setup(@TempDir Path projectDir) {
		this.gradleBuild = new GradleBuild(projectDir).withNullMarked(false);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenPackagesAreTangledShouldFailAndWriteReport(Task task) throws IOException {
		prepareTask(task, "tangled");
		buildAndFail(this.gradleBuild, task, "slices matching '(**)' should be free of cycles");
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenPackagesAreNotTangledShouldSucceedAndWriteEmptyReport(Task task) throws IOException {
		prepareTask(task, "untangled");
		build(this.gradleBuild, task);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenBeanPostProcessorBeanMethodIsNotStaticShouldFailAndWriteReport(Task task) throws IOException {
		prepareTask(task, "bpp/nonstatic");
		buildAndFail(this.gradleBuild.withDependencies(SPRING_CONTEXT), task,
				"methods that are annotated with @Bean and have raw return type assignable"
						+ " to org.springframework.beans.factory.config.BeanPostProcessor");
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenBeanPostProcessorBeanMethodIsStaticAndHasUnsafeParametersShouldFailAndWriteReport(Task task)
			throws IOException {
		prepareTask(task, "bpp/unsafeparameters");
		buildAndFail(this.gradleBuild.withDependencies(SPRING_CONTEXT), task,
				"methods that are annotated with @Bean and have raw return type assignable"
						+ " to org.springframework.beans.factory.config.BeanPostProcessor");
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenBeanPostProcessorBeanMethodIsStaticAndHasSafeParametersShouldSucceedAndWriteEmptyReport(Task task)
			throws IOException {
		prepareTask(task, "bpp/safeparameters");
		build(this.gradleBuild.withDependencies(SPRING_CONTEXT), task);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenBeanPostProcessorBeanMethodIsStaticAndHasNoParametersShouldSucceedAndWriteEmptyReport(Task task)
			throws IOException {
		prepareTask(task, "bpp/noparameters");
		build(this.gradleBuild.withDependencies(SPRING_CONTEXT), task);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenBeanFactoryPostProcessorBeanMethodIsNotStaticShouldFailAndWriteReport(Task task) throws IOException {
		prepareTask(task, "bfpp/nonstatic");
		buildAndFail(this.gradleBuild.withDependencies(SPRING_CONTEXT), task,
				"methods that are annotated with @Bean and have raw return type assignable"
						+ " to org.springframework.beans.factory.config.BeanFactoryPostProcessor");
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenBeanFactoryPostProcessorBeanMethodIsStaticAndHasParametersShouldFailAndWriteReport(Task task)
			throws IOException {
		prepareTask(task, "bfpp/parameters");
		buildAndFail(this.gradleBuild.withDependencies(SPRING_CONTEXT), task,
				"methods that are annotated with @Bean and have raw return type assignable"
						+ " to org.springframework.beans.factory.config.BeanFactoryPostProcessor");
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenBeanFactoryPostProcessorBeanMethodIsStaticAndHasNoParametersShouldSucceedAndWriteEmptyReport(Task task)
			throws IOException {
		prepareTask(task, "bfpp/noparameters");
		build(this.gradleBuild.withDependencies(SPRING_CONTEXT), task);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenClassLoadsResourceUsingResourceUtilsShouldFailAndWriteReport(Task task) throws IOException {
		prepareTask(task, "resources/loads");
		buildAndFail(this.gradleBuild.withDependencies(SPRING_CONTEXT), task,
				"no classes should call method where target owner type"
						+ " org.springframework.util.ResourceUtils and target name 'getURL'");
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenClassUsesResourceUtilsWithoutLoadingResourcesShouldSucceedAndWriteEmptyReport(Task task)
			throws IOException {
		prepareTask(task, "resources/noloads");
		build(this.gradleBuild.withDependencies(SPRING_CONTEXT), task);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenClassDoesNotCallObjectsRequireNonNullShouldSucceedAndWriteEmptyReport(Task task) throws IOException {
		prepareTask(task, "objects/noRequireNonNull");
		build(this.gradleBuild.withDependencies(SPRING_CONTEXT), task);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenClassCallsObjectsRequireNonNullWithMessageShouldFailAndWriteReport(Task task) throws IOException {
		prepareTask(task, "objects/requireNonNullWithString");
		buildAndFail(this.gradleBuild, task, "no classes should call method Objects.requireNonNull(Object, String)");
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenClassCallsObjectsRequireNonNullWithMessageAndProhibitObjectsRequireNonNullIsFalseShouldSucceedAndWriteEmptyReport(
			Task task) throws IOException {
		prepareTask(task, "objects/requireNonNullWithString");
		build(this.gradleBuild.withProhibitObjectsRequireNonNull(task, false), task);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenClassCallsObjectsRequireNonNullWithSupplierShouldFailAndWriteReport(Task task) throws IOException {
		prepareTask(task, "objects/requireNonNullWithSupplier");
		buildAndFail(this.gradleBuild, task, "no classes should call method Objects.requireNonNull(Object, Supplier)");
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenClassCallsObjectsRequireNonNullWithSupplierAndProhibitObjectsRequireNonNullIsFalseShouldSucceedAndWriteEmptyReport(
			Task task) throws IOException {
		prepareTask(task, "objects/requireNonNullWithSupplier");
		build(this.gradleBuild.withProhibitObjectsRequireNonNull(task, false), task);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenClassCallsStringToUpperCaseWithoutLocaleShouldFailAndWriteReport(Task task) throws IOException {
		prepareTask(task, "string/toUpperCase");
		buildAndFail(this.gradleBuild, task, "because String.toUpperCase(Locale.ROOT) should be used instead");
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenClassCallsStringToLowerCaseWithoutLocaleShouldFailAndWriteReport(Task task) throws IOException {
		prepareTask(task, "string/toLowerCase");
		buildAndFail(this.gradleBuild, task, "because String.toLowerCase(Locale.ROOT) should be used instead");
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenClassCallsStringToLowerCaseWithLocaleShouldSucceedAndWriteEmptyReport(Task task) throws IOException {
		prepareTask(task, "string/toLowerCaseWithLocale");
		build(this.gradleBuild, task);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenClassCallsStringToUpperCaseWithLocaleShouldSucceedAndWriteEmptyReport(Task task) throws IOException {
		prepareTask(task, "string/toUpperCaseWithLocale");
		build(this.gradleBuild, task);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenBeanPostProcessorBeanMethodIsNotStaticWithExternalClassShouldFailAndWriteReport(Task task)
			throws IOException {
		Path sourceDirectory = task.getSourceDirectory(this.gradleBuild.getProjectDir())
			.resolve(ClassUtils.classPackageAsResourcePath(getClass()));
		Files.createDirectories(sourceDirectory);
		Files.writeString(sourceDirectory.resolve("TestClass.java"), """
				package %s;
				import org.springframework.context.annotation.Bean;
				import org.springframework.integration.monitor.IntegrationMBeanExporter;
				public class TestClass {
					@Bean
					IntegrationMBeanExporter integrationMBeanExporter() {
						return new IntegrationMBeanExporter();
					}
				}
				""".formatted(ClassUtils.getPackageName(getClass())));
		buildAndFail(this.gradleBuild.withDependencies(SPRING_INTEGRATION_JMX), task,
				"methods that are annotated with @Bean and have raw return type assignable "
						+ "to org.springframework.beans.factory.config.BeanPostProcessor");
	}

	@Test
	void whenBeanMethodExposesPrivateTypeWithMainSourcesShouldFailAndWriteReport() throws IOException {
		prepareTask(Task.CHECK_ARCHITECTURE_MAIN, "beans/privatebean");
		buildAndFail(this.gradleBuild.withDependencies(SPRING_CONTEXT), Task.CHECK_ARCHITECTURE_MAIN,
				"methods that are annotated with @Bean should not return types declared "
						+ "with the PRIVATE modifier, as such types are incompatible with Spring AOT processing",
				"returns Class <org.springframework.boot.build.architecture.beans.privatebean.PrivateBean$MyBean>"
						+ " which is declared as [PRIVATE, STATIC, FINAL]");
	}

	@Test
	void whenBeanMethodExposesPrivateTypeWithTestsSourcesShouldSucceedAndWriteEmptyReport() throws IOException {
		prepareTask(Task.CHECK_ARCHITECTURE_TEST, "beans/privatebean");
		build(this.gradleBuild.withDependencies(SPRING_CONTEXT), Task.CHECK_ARCHITECTURE_TEST);
	}

	@ParameterizedTest(name = "{0}")
	@EnumSource(Task.class)
	void whenBeanMethodExposesNonPrivateTypeShouldSucceedAndWriteEmptyReport(Task task) throws IOException {
		prepareTask(task, "beans/regular");
		build(this.gradleBuild.withDependencies(SPRING_CONTEXT), task);
	}

	private void prepareTask(Task task, String... sourceDirectories) throws IOException {
		for (String sourceDirectory : sourceDirectories) {
			FileSystemUtils.copyRecursively(
					Paths.get("src/test/java")
						.resolve(ClassUtils.classPackageAsResourcePath(getClass()))
						.resolve(sourceDirectory),
					task.getSourceDirectory(this.gradleBuild.getProjectDir())
						.resolve(ClassUtils.classPackageAsResourcePath(getClass()))
						.resolve(sourceDirectory));
		}
	}

	private void build(GradleBuild gradleBuild, Task task) throws IOException {
		try {
			BuildResult buildResult = gradleBuild.build(task.toString());
			assertThat(buildResult.taskPaths(TaskOutcome.SUCCESS)).contains(":" + task);
			assertThat(task.getFailureReport(gradleBuild.getProjectDir())).isEmpty();
		}
		catch (UnexpectedBuildFailure ex) {
			StringBuilder message = new StringBuilder("Expected build to succeed but it failed");
			if (Files.exists(task.getFailureReportFile(gradleBuild.getProjectDir()))) {
				message.append('\n').append(task.getFailureReport(gradleBuild.getProjectDir()));
			}
			message.append('\n').append(ex.getBuildResult().getOutput());
			throw new AssertionError(message.toString(), ex);
		}
	}

	private void buildAndFail(GradleBuild gradleBuild, Task task, String... messages) throws IOException {
		try {
			BuildResult buildResult = gradleBuild.buildAndFail(task.toString());
			assertThat(buildResult.taskPaths(TaskOutcome.FAILED)).contains(":" + task);
			assertThat(task.getFailureReport(gradleBuild.getProjectDir())).contains(messages);
		}
		catch (UnexpectedBuildSuccess ex) {
			throw new AssertionError("Expected build to fail but it succeeded\n" + ex.getBuildResult().getOutput(), ex);
		}
	}

	private enum Task {

		CHECK_ARCHITECTURE_MAIN(SourceSet.MAIN_SOURCE_SET_NAME),

		CHECK_ARCHITECTURE_TEST(SourceSet.TEST_SOURCE_SET_NAME);

		private final String sourceSetName;

		Task(String sourceSetName) {
			this.sourceSetName = sourceSetName;
		}

		String getFailureReport(Path projectDir) throws IOException {
			return Files.readString(getFailureReportFile(projectDir), StandardCharsets.UTF_8);
		}

		Path getFailureReportFile(Path projectDir) {
			return projectDir.resolve("build/%s/failure-report.txt".formatted(toString()));
		}

		Path getSourceDirectory(Path projectDir) {
			return projectDir.resolve("src/%s/java".formatted(this.sourceSetName));
		}

		@Override
		public String toString() {
			return "checkArchitecture" + StringUtils.capitalize(this.sourceSetName);
		}

	}

	private static final class GradleBuild {

		private final Path projectDir;

		private final Set<String> dependencies = new LinkedHashSet<>();

		private final Map<Task, Boolean> prohibitObjectsRequireNonNull = new LinkedHashMap<>();

		private Boolean nullMarked;

		private GradleBuild(Path projectDir) {
			this.projectDir = projectDir;
		}

		Path getProjectDir() {
			return this.projectDir;
		}

		GradleBuild withNullMarked(Boolean nullMarked) {
			this.nullMarked = nullMarked;
			return this;
		}

		GradleBuild withProhibitObjectsRequireNonNull(Task task, boolean prohibitObjectsRequireNonNull) {
			this.prohibitObjectsRequireNonNull.put(task, prohibitObjectsRequireNonNull);
			return this;
		}

		GradleBuild withDependencies(String... dependencies) {
			this.dependencies.addAll(Arrays.asList(dependencies));
			return this;
		}

		BuildResult build(String... arguments) throws IOException {
			return prepareRunner(arguments).build();
		}

		BuildResult buildAndFail(String... arguments) throws IOException {
			return prepareRunner(arguments).buildAndFail();
		}

		private GradleRunner prepareRunner(String... arguments) throws IOException {
			StringBuilder buildFile = new StringBuilder();
			buildFile.append("plugins {\n")
				.append("    id 'java'\n")
				.append("    id 'org.springframework.boot.architecture'\n")
				.append("}\n\n")
				.append("repositories {\n")
				.append("    mavenCentral()\n")
				.append("}\n\n")
				.append("java {\n")
				.append("    sourceCompatibility = '17'\n")
				.append("    targetCompatibility = '17'\n")
				.append("}\n\n");
			if (!this.dependencies.isEmpty()) {
				buildFile.append("dependencies {\n");
				for (String dependency : this.dependencies) {
					buildFile.append("    implementation '%s'\n".formatted(dependency));
				}
				buildFile.append("}\n");
			}
			this.prohibitObjectsRequireNonNull.forEach((task, prohibitObjectsRequireNonNull) -> buildFile.append(task)
				.append(" {\n")
				.append("    prohibitObjectsRequireNonNull = ")
				.append(prohibitObjectsRequireNonNull)
				.append("\n}\n\n"));
			if (this.nullMarked != null) {
				buildFile.append("architectureCheck {\n")
					.append("    nullMarked = ")
					.append(this.nullMarked)
					.append("\n}\n");
			}
			Files.writeString(this.projectDir.resolve("build.gradle"), buildFile, StandardCharsets.UTF_8);
			return GradleRunner.create()
				.withProjectDir(this.projectDir.toFile())
				.withArguments(arguments)
				.withPluginClasspath();
		}

	}

}
