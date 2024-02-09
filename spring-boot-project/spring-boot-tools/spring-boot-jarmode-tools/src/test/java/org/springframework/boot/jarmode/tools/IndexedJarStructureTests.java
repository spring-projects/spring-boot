/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.jarmode.tools;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.jarmode.tools.JarStructure.Entry;
import org.springframework.boot.jarmode.tools.JarStructure.Entry.Type;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link IndexedJarStructure}.
 *
 * @author Moritz Halbritter
 */
class IndexedJarStructureTests {

	@Test
	void shouldResolveLibraryEntry() throws IOException {
		IndexedJarStructure structure = createStructure();
		Entry entry = structure.resolve("BOOT-INF/lib/spring-webmvc-6.1.4.jar");
		assertThat(entry.location()).isEqualTo("spring-webmvc-6.1.4.jar");
		assertThat(entry.originalLocation()).isEqualTo("BOOT-INF/lib/spring-webmvc-6.1.4.jar");
		assertThat(entry.type()).isEqualTo(Type.LIBRARY);
	}

	@Test
	void shouldResolveApplicationEntry() throws IOException {
		IndexedJarStructure structure = createStructure();
		Entry entry = structure.resolve("BOOT-INF/classes/application.properties");
		assertThat(entry.location()).isEqualTo("application.properties");
		assertThat(entry.originalLocation()).isEqualTo("BOOT-INF/classes/application.properties");
		assertThat(entry.type()).isEqualTo(Type.APPLICATION_CLASS_OR_RESOURCE);
	}

	@Test
	void shouldResolveLoaderEntry() throws IOException {
		IndexedJarStructure structure = createStructure();
		Entry entry = structure.resolve("org/springframework/boot/loader/launch/JarLauncher");
		assertThat(entry.location()).isEqualTo("org/springframework/boot/loader/launch/JarLauncher");
		assertThat(entry.originalLocation()).isEqualTo("org/springframework/boot/loader/launch/JarLauncher");
		assertThat(entry.type()).isEqualTo(Type.LOADER);
	}

	@Test
	void shouldNotResolveNonExistingLibs() throws IOException {
		IndexedJarStructure structure = createStructure();
		Entry entry = structure.resolve("BOOT-INF/lib/doesnt-exists.jar");
		assertThat(entry).isNull();
	}

	@Test
	void shouldCreateLauncherManifest() throws IOException {
		IndexedJarStructure structure = createStructure();
		Manifest manifest = structure.createLauncherManifest(UnaryOperator.identity());
		Map<String, String> attributes = getAttributes(manifest);
		assertThat(attributes).containsEntry("Manifest-Version", "1.0")
			.containsEntry("Implementation-Title", "IndexedJarStructureTests")
			.containsEntry("Spring-Boot-Version", "3.3.0-SNAPSHOT")
			.containsEntry("Implementation-Version", "0.0.1-SNAPSHOT")
			.containsEntry("Build-Jdk-Spec", "17")
			.containsEntry("Class-Path",
					"spring-webmvc-6.1.4.jar spring-web-6.1.4.jar spring-boot-autoconfigure-3.3.0-SNAPSHOT.jar spring-boot-3.3.0-SNAPSHOT.jar jakarta.annotation-api-2.1.1.jar spring-context-6.1.4.jar spring-aop-6.1.4.jar spring-beans-6.1.4.jar spring-expression-6.1.4.jar spring-core-6.1.4.jar snakeyaml-2.2.jar jackson-datatype-jdk8-2.16.1.jar jackson-datatype-jsr310-2.16.1.jar jackson-module-parameter-names-2.16.1.jar jackson-databind-2.16.1.jar tomcat-embed-websocket-10.1.19.jar tomcat-embed-core-10.1.19.jar tomcat-embed-el-10.1.19.jar micrometer-observation-1.13.0-M1.jar logback-classic-1.4.14.jar log4j-to-slf4j-2.23.0.jar jul-to-slf4j-2.0.12.jar spring-jcl-6.1.4.jar jackson-annotations-2.16.1.jar jackson-core-2.16.1.jar micrometer-commons-1.13.0-M1.jar logback-core-1.4.14.jar slf4j-api-2.0.12.jar log4j-api-2.23.0.jar")
			.containsEntry("Main-Class", "org.springframework.boot.jarmode.tools.IndexedJarStructureTests")
			.doesNotContainKeys("Start-Class", "Spring-Boot-Classes", "Spring-Boot-Lib", "Spring-Boot-Classpath-Index",
					"Spring-Boot-Layers-Index");
	}

	@Test
	void shouldLoadFromFile(@TempDir File tempDir) throws IOException {
		File jarFile = new File(tempDir, "test.jar");
		try (JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(jarFile), createManifest())) {
			outputStream.putNextEntry(new ZipEntry("BOOT-INF/classpath.idx"));
			outputStream.write(createIndexFile().getBytes(StandardCharsets.UTF_8));
			outputStream.closeEntry();
		}
		IndexedJarStructure structure = IndexedJarStructure.get(jarFile);
		assertThat(structure).isNotNull();
		assertThat(structure.resolve("BOOT-INF/lib/spring-webmvc-6.1.4.jar")).extracting(Entry::type)
			.isEqualTo(Type.LIBRARY);
		assertThat(structure.resolve("BOOT-INF/classes/application.properties")).extracting(Entry::type)
			.isEqualTo(Type.APPLICATION_CLASS_OR_RESOURCE);
	}

	private Map<String, String> getAttributes(Manifest manifest) {
		Map<String, String> result = new HashMap<>();
		manifest.getMainAttributes().forEach((key, value) -> result.put(key.toString(), value.toString()));
		return result;
	}

	private IndexedJarStructure createStructure() throws IOException {
		return new IndexedJarStructure(createManifest(), createIndexFile());
	}

	private String createIndexFile() {
		return """
				- "BOOT-INF/lib/spring-webmvc-6.1.4.jar"
				- "BOOT-INF/lib/spring-web-6.1.4.jar"
				- "BOOT-INF/lib/spring-boot-autoconfigure-3.3.0-SNAPSHOT.jar"
				- "BOOT-INF/lib/spring-boot-3.3.0-SNAPSHOT.jar"
				- "BOOT-INF/lib/jakarta.annotation-api-2.1.1.jar"
				- "BOOT-INF/lib/spring-context-6.1.4.jar"
				- "BOOT-INF/lib/spring-aop-6.1.4.jar"
				- "BOOT-INF/lib/spring-beans-6.1.4.jar"
				- "BOOT-INF/lib/spring-expression-6.1.4.jar"
				- "BOOT-INF/lib/spring-core-6.1.4.jar"
				- "BOOT-INF/lib/snakeyaml-2.2.jar"
				- "BOOT-INF/lib/jackson-datatype-jdk8-2.16.1.jar"
				- "BOOT-INF/lib/jackson-datatype-jsr310-2.16.1.jar"
				- "BOOT-INF/lib/jackson-module-parameter-names-2.16.1.jar"
				- "BOOT-INF/lib/jackson-databind-2.16.1.jar"
				- "BOOT-INF/lib/tomcat-embed-websocket-10.1.19.jar"
				- "BOOT-INF/lib/tomcat-embed-core-10.1.19.jar"
				- "BOOT-INF/lib/tomcat-embed-el-10.1.19.jar"
				- "BOOT-INF/lib/micrometer-observation-1.13.0-M1.jar"
				- "BOOT-INF/lib/logback-classic-1.4.14.jar"
				- "BOOT-INF/lib/log4j-to-slf4j-2.23.0.jar"
				- "BOOT-INF/lib/jul-to-slf4j-2.0.12.jar"
				- "BOOT-INF/lib/spring-jcl-6.1.4.jar"
				- "BOOT-INF/lib/jackson-annotations-2.16.1.jar"
				- "BOOT-INF/lib/jackson-core-2.16.1.jar"
				- "BOOT-INF/lib/micrometer-commons-1.13.0-M1.jar"
				- "BOOT-INF/lib/logback-core-1.4.14.jar"
				- "BOOT-INF/lib/slf4j-api-2.0.12.jar"
				- "BOOT-INF/lib/log4j-api-2.23.0.jar"
				""";
	}

	private Manifest createManifest() throws IOException {
		return new Manifest(new ByteArrayInputStream("""
				Manifest-Version: 1.0
				Main-Class: org.springframework.boot.loader.launch.JarLauncher
				Start-Class: org.springframework.boot.jarmode.tools.IndexedJarStructureTests
				Spring-Boot-Version: 3.3.0-SNAPSHOT
				Spring-Boot-Classes: BOOT-INF/classes/
				Spring-Boot-Lib: BOOT-INF/lib/
				Spring-Boot-Classpath-Index: BOOT-INF/classpath.idx
				Spring-Boot-Layers-Index: BOOT-INF/layers.idx
				Build-Jdk-Spec: 17
				Implementation-Title: IndexedJarStructureTests
				Implementation-Version: 0.0.1-SNAPSHOT
				""".getBytes(StandardCharsets.UTF_8)));
	}

}
