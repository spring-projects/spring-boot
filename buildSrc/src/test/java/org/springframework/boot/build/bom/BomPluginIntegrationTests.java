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
package org.springframework.boot.build.bom;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.function.Consumer;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.build.DeployedPlugin;
import org.springframework.boot.build.assertj.NodeAssert;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BomPlugin}.
 *
 * @author Andy Wilkinson
 */
public class BomPluginIntegrationTests {

	private File projectDir;

	private File buildFile;

	@BeforeEach
	public void setup(@TempDir File projectDir) throws IOException {
		this.projectDir = projectDir;
		this.buildFile = new File(this.projectDir, "build.gradle");
	}

	@Test
	void libraryModulesAreIncludedInDependencyManagementOfGeneratedPom() throws IOException {
		try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
			out.println("plugins {");
			out.println("    id 'org.springframework.boot.bom'");
			out.println("}");
			out.println("bom {");
			out.println("    library('ActiveMQ', '5.15.10') {");
			out.println("        group('org.apache.activemq') {");
			out.println("            modules = [");
			out.println("                'activemq-amqp',");
			out.println("                'activemq-blueprint'");
			out.println("            ]");
			out.println("        }");
			out.println("    }");
			out.println("}");
		}
		generatePom((pom) -> {
			assertThat(pom).textAtPath("//properties/activemq.version").isEqualTo("5.15.10");
			NodeAssert dependency = pom.nodeAtPath("//dependencyManagement/dependencies/dependency[1]");
			assertThat(dependency).textAtPath("groupId").isEqualTo("org.apache.activemq");
			assertThat(dependency).textAtPath("artifactId").isEqualTo("activemq-amqp");
			assertThat(dependency).textAtPath("version").isEqualTo("${activemq.version}");
			assertThat(dependency).textAtPath("scope").isNullOrEmpty();
			assertThat(dependency).textAtPath("type").isNullOrEmpty();
			dependency = pom.nodeAtPath("//dependencyManagement/dependencies/dependency[2]");
			assertThat(dependency).textAtPath("groupId").isEqualTo("org.apache.activemq");
			assertThat(dependency).textAtPath("artifactId").isEqualTo("activemq-blueprint");
			assertThat(dependency).textAtPath("version").isEqualTo("${activemq.version}");
			assertThat(dependency).textAtPath("scope").isNullOrEmpty();
			assertThat(dependency).textAtPath("type").isNullOrEmpty();
		});
	}

	@Test
	void libraryPluginsAreIncludedInPluginManagementOfGeneratedPom() throws IOException {
		try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
			out.println("plugins {");
			out.println("    id 'org.springframework.boot.bom'");
			out.println("}");
			out.println("bom {");
			out.println("    library('Flyway', '6.0.8') {");
			out.println("        group('org.flywaydb') {");
			out.println("            plugins = [");
			out.println("                'flyway-maven-plugin'");
			out.println("            ]");
			out.println("        }");
			out.println("    }");
			out.println("}");
		}
		generatePom((pom) -> {
			assertThat(pom).textAtPath("//properties/flyway.version").isEqualTo("6.0.8");
			NodeAssert plugin = pom.nodeAtPath("//pluginManagement/plugins/plugin");
			assertThat(plugin).textAtPath("groupId").isEqualTo("org.flywaydb");
			assertThat(plugin).textAtPath("artifactId").isEqualTo("flyway-maven-plugin");
			assertThat(plugin).textAtPath("version").isEqualTo("${flyway.version}");
			assertThat(plugin).textAtPath("scope").isNullOrEmpty();
			assertThat(plugin).textAtPath("type").isNullOrEmpty();
		});
	}

	@Test
	void libraryImportsAreIncludedInDependencyManagementOfGeneratedPom() throws Exception {
		try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
			out.println("plugins {");
			out.println("    id 'org.springframework.boot.bom'");
			out.println("}");
			out.println("bom {");
			out.println("    library('Jackson Bom', '2.10.0') {");
			out.println("        group('com.fasterxml.jackson') {");
			out.println("            imports = [");
			out.println("                'jackson-bom'");
			out.println("            ]");
			out.println("        }");
			out.println("    }");
			out.println("}");
		}
		generatePom((pom) -> {
			assertThat(pom).textAtPath("//properties/jackson-bom.version").isEqualTo("2.10.0");
			NodeAssert dependency = pom.nodeAtPath("//dependencyManagement/dependencies/dependency");
			assertThat(dependency).textAtPath("groupId").isEqualTo("com.fasterxml.jackson");
			assertThat(dependency).textAtPath("artifactId").isEqualTo("jackson-bom");
			assertThat(dependency).textAtPath("version").isEqualTo("${jackson-bom.version}");
			assertThat(dependency).textAtPath("scope").isEqualTo("import");
			assertThat(dependency).textAtPath("type").isEqualTo("pom");
		});
	}

	@Test
	void moduleExclusionsAreIncludedInDependencyManagementOfGeneratedPom() throws IOException {
		try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
			out.println("plugins {");
			out.println("    id 'org.springframework.boot.bom'");
			out.println("}");
			out.println("bom {");
			out.println("    library('MySQL', '8.0.18') {");
			out.println("        group('mysql') {");
			out.println("            modules = [");
			out.println("                'mysql-connector-java' {");
			out.println("                    exclude group: 'com.google.protobuf', module: 'protobuf-java'");
			out.println("                }");
			out.println("            ]");
			out.println("        }");
			out.println("    }");
			out.println("}");
		}
		generatePom((pom) -> {
			assertThat(pom).textAtPath("//properties/mysql.version").isEqualTo("8.0.18");
			NodeAssert dependency = pom.nodeAtPath("//dependencyManagement/dependencies/dependency");
			assertThat(dependency).textAtPath("groupId").isEqualTo("mysql");
			assertThat(dependency).textAtPath("artifactId").isEqualTo("mysql-connector-java");
			assertThat(dependency).textAtPath("version").isEqualTo("${mysql.version}");
			assertThat(dependency).textAtPath("scope").isNullOrEmpty();
			assertThat(dependency).textAtPath("type").isNullOrEmpty();
			NodeAssert exclusion = dependency.nodeAtPath("exclusions/exclusion");
			assertThat(exclusion).textAtPath("groupId").isEqualTo("com.google.protobuf");
			assertThat(exclusion).textAtPath("artifactId").isEqualTo("protobuf-java");
		});
	}

	@Test
	void libraryNamedSpringBootHasNoVersionProperty() throws IOException {
		try (PrintWriter out = new PrintWriter(new FileWriter(this.buildFile))) {
			out.println("plugins {");
			out.println("    id 'org.springframework.boot.bom'");
			out.println("}");
			out.println("bom {");
			out.println("    library('Spring Boot', '1.2.3') {");
			out.println("        group('org.springframework.boot') {");
			out.println("            modules = [");
			out.println("                'spring-boot'");
			out.println("            ]");
			out.println("        }");
			out.println("    }");
			out.println("}");
		}
		generatePom((pom) -> {
			assertThat(pom).textAtPath("//properties/spring-boot.version").isEmpty();
			NodeAssert dependency = pom.nodeAtPath("//dependencyManagement/dependencies/dependency[1]");
			assertThat(dependency).textAtPath("groupId").isEqualTo("org.springframework.boot");
			assertThat(dependency).textAtPath("artifactId").isEqualTo("spring-boot");
			assertThat(dependency).textAtPath("version").isEqualTo("1.2.3");
			assertThat(dependency).textAtPath("scope").isNullOrEmpty();
			assertThat(dependency).textAtPath("type").isNullOrEmpty();
		});
	}

	private BuildResult runGradle(String... args) {
		return GradleRunner.create().withDebug(true).withProjectDir(this.projectDir).withArguments(args)
				.withPluginClasspath().build();
	}

	private void generatePom(Consumer<NodeAssert> consumer) {
		runGradle(DeployedPlugin.GENERATE_POM_TASK_NAME, "-s");
		File generatedPomXml = new File(this.projectDir, "build/publications/maven/pom-default.xml");
		assertThat(generatedPomXml).isFile();
		consumer.accept(new NodeAssert(generatedPomXml));
	}

}
