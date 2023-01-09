/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.devtools.env;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DevToolsHomePropertiesPostProcessor}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author HaiTao Zhang
 * @author Madhura Bhave
 */
class DevToolsHomePropertiesPostProcessorTests {

	private String configDir;

	private File home;

	private File customHome;

	@BeforeEach
	void setup(@TempDir File tempDir) {
		this.home = new File(tempDir, "default-home");
		this.customHome = new File(tempDir, "custom-home");
		this.configDir = this.home + "/.config/spring-boot/";
		new File(this.configDir).mkdirs();
	}

	@Test
	void loadsPropertiesFromHomeDirectoryUsingProperties() throws Exception {
		Properties properties = new Properties();
		properties.put("abc", "def");
		writeFile(properties, ".spring-boot-devtools.properties");
		ConfigurableEnvironment environment = getPostProcessedEnvironment();
		assertThat(environment.getProperty("abc")).isEqualTo("def");
	}

	@Test
	void loadsPropertiesFromCustomHomeDirectorySetUsingSystemProperty() throws Exception {
		Properties properties = new Properties();
		properties.put("uvw", "xyz");
		writeFile(properties, this.customHome, ".config/spring-boot/spring-boot-devtools.properties");
		Properties systemProperties = new Properties();
		systemProperties.setProperty("spring.devtools.home", this.customHome.getAbsolutePath());
		ConfigurableEnvironment environment = getPostProcessedEnvironment(systemProperties);
		assertThat(environment.getProperty("uvw")).isEqualTo("xyz");
	}

	@Test
	void loadsPropertiesFromCustomHomeDirectorySetUsingEnvironmentVariable() throws Exception {
		Properties properties = new Properties();
		properties.put("uvw", "xyz");
		writeFile(properties, this.customHome, ".config/spring-boot/spring-boot-devtools.properties");
		ConfigurableEnvironment environment = getPostProcessedEnvironment(
				Collections.singletonMap("SPRING_DEVTOOLS_HOME", this.customHome.getAbsolutePath()));
		assertThat(environment.getProperty("uvw")).isEqualTo("xyz");
	}

	@Test
	void loadsPropertiesFromConfigDirectoryUsingProperties() throws Exception {
		Properties properties = new Properties();
		properties.put("abc", "def");
		OutputStream out = new FileOutputStream(new File(this.configDir, "spring-boot-devtools.properties"));
		properties.store(out, null);
		out.close();
		ConfigurableEnvironment environment = getPostProcessedEnvironment();
		assertThat(environment.getProperty("abc")).isEqualTo("def");
	}

	@Test
	void loadsPropertiesFromConfigDirectoryUsingYml() throws Exception {
		OutputStream out = new FileOutputStream(new File(this.configDir, "spring-boot-devtools.yml"));
		File file = new ClassPathResource("spring-devtools.yaml", getClass()).getFile();
		byte[] content = Files.readAllBytes(file.toPath());
		out.write(content);
		out.close();
		ConfigurableEnvironment environment = getPostProcessedEnvironment();
		assertThat(environment.getProperty("abc.xyz")).isEqualTo("def");
	}

	@Test
	void loadsPropertiesFromConfigDirectoryUsingYaml() throws Exception {
		OutputStream out = new FileOutputStream(new File(this.configDir, "spring-boot-devtools.yaml"));
		File file = new ClassPathResource("spring-devtools.yaml", getClass()).getFile();
		byte[] content = Files.readAllBytes(file.toPath());
		out.write(content);
		out.close();
		ConfigurableEnvironment environment = getPostProcessedEnvironment();
		assertThat(environment.getProperty("abc.xyz")).isEqualTo("def");
	}

	@Test
	void loadFromConfigDirectoryWithPropertiesTakingPrecedence() throws Exception {
		OutputStream out = new FileOutputStream(new File(this.configDir, "spring-boot-devtools.yaml"));
		File file = new ClassPathResource("spring-devtools.yaml", getClass()).getFile();
		byte[] content = Files.readAllBytes(file.toPath());
		out.write(content);
		out.close();
		Properties properties2 = new Properties();
		properties2.put("abc.xyz", "jkl");
		OutputStream out2 = new FileOutputStream(new File(this.configDir, "spring-boot-devtools.properties"));
		properties2.store(out2, null);
		out2.close();
		ConfigurableEnvironment environment = getPostProcessedEnvironment();
		assertThat(environment.getProperty("abc.xyz")).isEqualTo("jkl");
		assertThat(environment.getProperty("bing")).isEqualTo("blip");
	}

	@Test
	void loadFromConfigDirectoryTakesPrecedenceOverHomeDirectory() throws Exception {
		Properties properties = new Properties();
		properties.put("abc", "def");
		properties.put("bar", "baz");
		writeFile(properties, ".spring-boot-devtools.properties");
		Properties properties2 = new Properties();
		properties2.put("abc", "jkl");
		OutputStream out2 = new FileOutputStream(new File(this.configDir, "spring-boot-devtools.properties"));
		properties2.store(out2, null);
		out2.close();
		ConfigurableEnvironment environment = getPostProcessedEnvironment();
		assertThat(environment.getProperty("abc")).isEqualTo("jkl");
		assertThat(environment.getProperty("bar")).isNull();
	}

	@Test
	void loadFromConfigDirectoryWithYamlTakesPrecedenceOverHomeDirectory() throws Exception {
		Properties properties = new Properties();
		properties.put("abc.xyz", "jkl");
		properties.put("bar", "baz");
		writeFile(properties, ".spring-boot-devtools.properties");
		OutputStream out2 = new FileOutputStream(new File(this.configDir, "spring-boot-devtools.yml"));
		File file = new ClassPathResource("spring-devtools.yaml", getClass()).getFile();
		byte[] content = Files.readAllBytes(file.toPath());
		out2.write(content);
		out2.close();
		ConfigurableEnvironment environment = getPostProcessedEnvironment();
		assertThat(environment.getProperty("abc.xyz")).isEqualTo("def");
		assertThat(environment.getProperty("bar")).isNull();
	}

	@Test
	void ignoresMissingHomeProperties() throws Exception {
		ConfigurableEnvironment environment = getPostProcessedEnvironment();
		assertThat(environment.getProperty("abc")).isNull();
	}

	private void writeFile(Properties properties, String path) throws IOException {
		writeFile(properties, this.home, path);
	}

	private void writeFile(Properties properties, File home, String path) throws IOException {
		File file = new File(home, path);
		file.getParentFile().mkdirs();
		try (OutputStream out = new FileOutputStream(file)) {
			properties.store(out, null);
		}
	}

	private ConfigurableEnvironment getPostProcessedEnvironment() throws Exception {
		return getPostProcessedEnvironment(null, null);
	}

	private ConfigurableEnvironment getPostProcessedEnvironment(Properties systemProperties) throws Exception {
		return getPostProcessedEnvironment(null, systemProperties);
	}

	private ConfigurableEnvironment getPostProcessedEnvironment(Map<String, String> env) throws Exception {
		return getPostProcessedEnvironment(env, null);
	}

	private ConfigurableEnvironment getPostProcessedEnvironment(Map<String, String> env, Properties systemProperties)
			throws Exception {
		if (systemProperties == null) {
			systemProperties = new Properties();
			systemProperties.setProperty("user.home", this.home.getAbsolutePath());
		}
		ConfigurableEnvironment environment = new MockEnvironment();
		DevToolsHomePropertiesPostProcessor postProcessor = new DevToolsHomePropertiesPostProcessor(
				(env != null) ? env : Collections.emptyMap(), systemProperties);
		runPostProcessor(() -> postProcessor.postProcessEnvironment(environment, null));
		return environment;
	}

	protected void runPostProcessor(Runnable runnable) throws Exception {
		Thread thread = new Thread(runnable);
		thread.start();
		thread.join();
	}

}
