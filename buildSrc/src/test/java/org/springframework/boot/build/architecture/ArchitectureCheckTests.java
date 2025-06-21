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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileSystemUtils;

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

	private Path projectDir;

	private Path buildFile;

	@BeforeEach
	void setup(@TempDir Path projectDir) {
		this.projectDir = projectDir;
		this.buildFile = projectDir.resolve("build.gradle");
	}

	@Test
	void whenPackagesAreTangledTaskFailsAndWritesAReport() throws IOException {
		runGradleWithCompiledClasses("tangled",
				shouldHaveFailureReportWithMessage("slices matching '(**)' should be free of cycles"));
	}

	@Test
	void whenPackagesAreNotTangledTaskSucceedsAndWritesAnEmptyReport() throws IOException {
		runGradleWithCompiledClasses("untangled", shouldHaveEmptyFailureReport());
	}

	@Test
	void whenBeanPostProcessorBeanMethodIsNotStaticTaskFailsAndWritesAReport() throws IOException {
		runGradleWithCompiledClasses("bpp/nonstatic",
				shouldHaveFailureReportWithMessage(
						"methods that are annotated with @Bean and have raw return type assignable "
								+ "to org.springframework.beans.factory.config.BeanPostProcessor"));
	}

	@Test
	void whenBeanPostProcessorBeanMethodIsStaticAndHasUnsafeParametersTaskFailsAndWritesAReport() throws IOException {
		runGradleWithCompiledClasses("bpp/unsafeparameters",
				shouldHaveFailureReportWithMessage(
						"methods that are annotated with @Bean and have raw return type assignable "
								+ "to org.springframework.beans.factory.config.BeanPostProcessor"));
	}

	@Test
	void whenBeanPostProcessorBeanMethodIsStaticAndHasSafeParametersTaskSucceedsAndWritesAnEmptyReport()
			throws IOException {
		runGradleWithCompiledClasses("bpp/safeparameters", shouldHaveEmptyFailureReport());
	}

	@Test
	void whenBeanPostProcessorBeanMethodIsStaticAndHasNoParametersTaskSucceedsAndWritesAnEmptyReport()
			throws IOException {
		runGradleWithCompiledClasses("bpp/noparameters", shouldHaveEmptyFailureReport());
	}

	@Test
	void whenBeanFactoryPostProcessorBeanMethodIsNotStaticTaskFailsAndWritesAReport() throws IOException {
		runGradleWithCompiledClasses("bfpp/nonstatic",
				shouldHaveFailureReportWithMessage("methods that are annotated with @Bean and have raw return "
						+ "type assignable to org.springframework.beans.factory.config.BeanFactoryPostProcessor"));
	}

	@Test
	void whenBeanFactoryPostProcessorBeanMethodIsStaticAndHasParametersTaskFailsAndWritesAReport() throws IOException {
		runGradleWithCompiledClasses("bfpp/parameters",
				shouldHaveFailureReportWithMessage("methods that are annotated with @Bean and have raw return "
						+ "type assignable to org.springframework.beans.factory.config.BeanFactoryPostProcessor"));
	}

	@Test
	void whenBeanFactoryPostProcessorBeanMethodIsStaticAndHasNoParametersTaskSucceedsAndWritesAnEmptyReport()
			throws IOException {
		runGradleWithCompiledClasses("bfpp/noparameters", shouldHaveEmptyFailureReport());
	}

	@Test
	void whenClassLoadsResourceUsingResourceUtilsTaskFailsAndWritesReport() throws IOException {
		runGradleWithCompiledClasses("resources/loads", shouldHaveFailureReportWithMessage(
				"no classes should call method where target owner type org.springframework.util.ResourceUtils and target name 'getURL'"));
	}

	@Test
	void whenClassUsesResourceUtilsWithoutLoadingResourcesTaskSucceedsAndWritesAnEmptyReport() throws IOException {
		runGradleWithCompiledClasses("resources/noloads", shouldHaveEmptyFailureReport());
	}

	@Test
	void whenClassDoesNotCallObjectsRequireNonNullTaskSucceedsAndWritesAnEmptyReport() throws IOException {
		runGradleWithCompiledClasses("objects/noRequireNonNull", shouldHaveEmptyFailureReport());
	}

	@Test
	void whenClassCallsObjectsRequireNonNullWithMessageTaskFailsAndWritesReport() throws IOException {
		runGradleWithCompiledClasses("objects/requireNonNullWithString", shouldHaveFailureReportWithMessage(
				"no classes should call method Objects.requireNonNull(Object, String)"));
	}

	@Test
	void whenClassCallsObjectsRequireNonNullWithSupplierTaskFailsAndWritesReport() throws IOException {
		runGradleWithCompiledClasses("objects/requireNonNullWithSupplier", shouldHaveFailureReportWithMessage(
				"no classes should call method Objects.requireNonNull(Object, Supplier)"));
	}

	@Test
	void whenClassCallsStringToUpperCaseWithoutLocaleFailsAndWritesReport() throws IOException {
		runGradleWithCompiledClasses("string/toUpperCase",
				shouldHaveFailureReportWithMessage("because String.toUpperCase(Locale.ROOT) should be used instead"));
	}

	@Test
	void whenClassCallsStringToLowerCaseWithoutLocaleFailsAndWritesReport() throws IOException {
		runGradleWithCompiledClasses("string/toLowerCase",
				shouldHaveFailureReportWithMessage("because String.toLowerCase(Locale.ROOT) should be used instead"));
	}

	@Test
	void whenClassCallsStringToLowerCaseWithLocaleShouldNotFail() throws IOException {
		runGradleWithCompiledClasses("string/toLowerCaseWithLocale", shouldHaveEmptyFailureReport());
	}

	@Test
	void whenClassCallsStringToUpperCaseWithLocaleShouldNotFail() throws IOException {
		runGradleWithCompiledClasses("string/toUpperCaseWithLocale", shouldHaveEmptyFailureReport());
	}

	@Test
	void whenBeanPostProcessorBeanMethodIsNotStaticWithExternalClass() throws IOException {
		Files.writeString(this.buildFile, """
				plugins {
					id 'java'
					id 'org.springframework.boot.architecture'
				}
				repositories {
					mavenCentral()
				}
				java {
					sourceCompatibility = 17
				}
				dependencies {
					implementation("org.springframework.integration:spring-integration-jmx:6.3.9")
				}
				""");
		Path testClass = this.projectDir.resolve("src/main/java/boot/architecture/bpp/external/TestClass.java");
		Files.createDirectories(testClass.getParent());
		Files.writeString(testClass, """
				package org.springframework.boot.build.architecture.bpp.external;
				import org.springframework.context.annotation.Bean;
				import org.springframework.integration.monitor.IntegrationMBeanExporter;
				public class TestClass {
					@Bean
					IntegrationMBeanExporter integrationMBeanExporter() {
						return new IntegrationMBeanExporter();
					}
				}
				""");
		runGradle(shouldHaveFailureReportWithMessage("methods that are annotated with @Bean and have raw return "
				+ "type assignable to org.springframework.beans.factory.config.BeanPostProcessor "));
	}

	private Consumer<GradleRunner> shouldHaveEmptyFailureReport() {
		return (gradleRunner) -> {
			assertThat(gradleRunner.build().getOutput()).contains("BUILD SUCCESSFUL")
				.contains("Task :checkArchitectureMain");
			assertThat(failureReport()).isEmptyFile();
		};
	}

	private Consumer<GradleRunner> shouldHaveFailureReportWithMessage(String message) {
		return (gradleRunner) -> {
			assertThat(gradleRunner.buildAndFail().getOutput()).contains("BUILD FAILED")
				.contains("Task :checkArchitectureMain FAILED");
			assertThat(failureReport()).content().contains(message);
		};
	}

	private void runGradleWithCompiledClasses(String path, Consumer<GradleRunner> callback) throws IOException {
		ClassPathResource classPathResource = new ClassPathResource(path, getClass());
		FileSystemUtils.copyRecursively(classPathResource.getFile().toPath(),
				this.projectDir.resolve("classes").resolve(classPathResource.getPath()));
		Files.writeString(this.buildFile, """
				plugins {
					 id 'java'
					 id 'org.springframework.boot.architecture'
				}
				sourceSets {
					main {
						  output.classesDirs.setFrom(file("classes"))
					  }
				}
				""");
		runGradle(callback);
	}

	private void runGradle(Consumer<GradleRunner> callback) {
		callback.accept(GradleRunner.create()
			.withProjectDir(this.projectDir.toFile())
			.withArguments("checkArchitectureMain")
			.withPluginClasspath());
	}

	private Path failureReport() {
		return this.projectDir.resolve("build/checkArchitectureMain/failure-report.txt");
	}

}
