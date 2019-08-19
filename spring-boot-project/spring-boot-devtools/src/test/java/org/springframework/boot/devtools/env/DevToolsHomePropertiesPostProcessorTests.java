/*
 * Copyright 2012-2019 the original author or authors.
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
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DevToolsHomePropertiesPostProcessor}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author HaiTao Zhang
 */
class DevToolsHomePropertiesPostProcessorTests {

	private File home;

	@BeforeEach
	void setup(@TempDir File tempDir) throws IOException {
		this.home = tempDir;
	}

	@Test
	void loadsPropertiesFromHomeFolderUsingProperties() throws Exception {
		Properties properties = new Properties();
		properties.put("abc", "def");
		OutputStream out = new FileOutputStream(new File(this.home, ".spring-boot-devtools.properties"));
		properties.store(out, null);
		out.close();
		ConfigurableEnvironment environment = new MockEnvironment();
		MockDevToolHomePropertiesPostProcessor postProcessor = new MockDevToolHomePropertiesPostProcessor();
		runPostProcessor(() -> postProcessor.postProcessEnvironment(environment, null));
		assertThat(environment.getProperty("abc")).isEqualTo("def");
	}

	@Test
	void loadsPropertiesFromHomeFolderUsingYml() throws Exception {
		Properties properties = new Properties();
		properties.put("abc", "def");
		OutputStream out = new FileOutputStream(new File(this.home, ".spring-boot-devtools.yml"));
		properties.store(out, null);
		out.close();
		ConfigurableEnvironment environment = new MockEnvironment();
		MockDevToolHomePropertiesPostProcessor postProcessor = new MockDevToolHomePropertiesPostProcessor();
		runPostProcessor(() -> postProcessor.postProcessEnvironment(environment, null));
		assertThat(environment.getProperty("abc")).isEqualTo("def");
	}

	@Test
	void loadsPropertiesFromHomeFolderUsingYaml() throws Exception {
		Properties properties = new Properties();
		properties.put("abc", "def");
		OutputStream out = new FileOutputStream(new File(this.home, ".spring-boot-devtools.yaml"));
		properties.store(out, null);
		out.close();
		ConfigurableEnvironment environment = new MockEnvironment();
		MockDevToolHomePropertiesPostProcessor postProcessor = new MockDevToolHomePropertiesPostProcessor();
		runPostProcessor(() -> postProcessor.postProcessEnvironment(environment, null));
		assertThat(environment.getProperty("abc")).isEqualTo("def");
	}

	@Test
	void loadsPropertiesFromConfigFolderUsingProperties() throws Exception {
		Properties properties = new Properties();
		new File(this.home + "/.config/spring-boot").mkdirs();
		properties.put("abc", "def");
		OutputStream out = new FileOutputStream(
				new File(this.home + "/.config/spring-boot", ".spring-boot-devtools.properties"));
		properties.store(out, null);
		out.close();
		ConfigurableEnvironment environment = new MockEnvironment();
		MockDevToolHomePropertiesPostProcessor postProcessor = new MockDevToolHomePropertiesPostProcessor();
		runPostProcessor(() -> postProcessor.postProcessEnvironment(environment, null));
		assertThat(environment.getProperty("abc")).isEqualTo("def");
	}

	@Test
	void loadsPropertiesFromConfigFolderUsingYml() throws Exception {
		Properties properties = new Properties();
		new File(this.home + "/.config/spring-boot").mkdirs();
		properties.put("abc", "def");
		OutputStream out = new FileOutputStream(
				new File(this.home + "/.config/spring-boot", ".spring-boot-devtools.yml"));
		properties.store(out, null);
		out.close();
		ConfigurableEnvironment environment = new MockEnvironment();
		MockDevToolHomePropertiesPostProcessor postProcessor = new MockDevToolHomePropertiesPostProcessor();
		runPostProcessor(() -> postProcessor.postProcessEnvironment(environment, null));
		assertThat(environment.getProperty("abc")).isEqualTo("def");
	}

	@Test
	void loadsPropertiesFromConfigFolderUsingYaml() throws Exception {
		Properties properties = new Properties();
		new File(this.home + "/.config/spring-boot").mkdirs();
		properties.put("abc", "def");
		OutputStream out = new FileOutputStream(
				new File(this.home + "/.config/spring-boot", ".spring-boot-devtools.yaml"));
		properties.store(out, null);
		out.close();
		ConfigurableEnvironment environment = new MockEnvironment();
		MockDevToolHomePropertiesPostProcessor postProcessor = new MockDevToolHomePropertiesPostProcessor();
		runPostProcessor(() -> postProcessor.postProcessEnvironment(environment, null));
		assertThat(environment.getProperty("abc")).isEqualTo("def");
	}

	@Test
	void loadFromConfigFolderWithPropertiesTakingPrecedence() throws Exception {
		Properties properties = new Properties();
		properties.put("abc", "def");
		new File(this.home + "/.config/spring-boot").mkdirs();
		OutputStream out = new FileOutputStream(
				new File(this.home + "/.config/spring-boot/", ".spring-boot-devtools.yaml"));
		properties.store(out, null);
		out.close();
		Properties properties2 = new Properties();
		properties2.put("abc", "jkl");
		OutputStream out2 = new FileOutputStream(
				new File(this.home + "/.config/spring-boot/", ".spring-boot-devtools.properties"));
		properties2.store(out2, null);
		out2.close();
		ConfigurableEnvironment environment = new MockEnvironment();
		MockDevToolHomePropertiesPostProcessor postProcessor = new MockDevToolHomePropertiesPostProcessor();
		runPostProcessor(() -> postProcessor.postProcessEnvironment(environment, null));
		assertThat(environment.getProperty("abc")).isEqualTo("jkl");
	}

	@Test
	void loadFromHomeFolderWithPropertiesTakingPrecedence() throws Exception {
		Properties properties = new Properties();
		properties.put("abc", "def");
		new File(this.home + "/.config/spring-boot").mkdirs();
		OutputStream out = new FileOutputStream(new File(this.home, ".spring-boot-devtools.yaml"));
		properties.store(out, null);
		out.close();
		Properties properties2 = new Properties();
		properties2.put("abc", "jkl");
		OutputStream out2 = new FileOutputStream(new File(this.home, ".spring-boot-devtools.properties"));
		properties2.store(out2, null);
		out2.close();
		ConfigurableEnvironment environment = new MockEnvironment();
		MockDevToolHomePropertiesPostProcessor postProcessor = new MockDevToolHomePropertiesPostProcessor();
		runPostProcessor(() -> postProcessor.postProcessEnvironment(environment, null));
		assertThat(environment.getProperty("abc")).isEqualTo("jkl");
	}

	@Test
	void loadFromConfigFolderTakesPrecedenceOverHomeFolder() throws Exception {
		Properties properties = new Properties();
		properties.put("abc", "def");
		new File(this.home + "/.config/spring-boot").mkdirs();
		OutputStream out = new FileOutputStream(new File(this.home, ".spring-boot-devtools.properties"));
		properties.store(out, null);
		out.close();
		Properties properties2 = new Properties();
		properties2.put("abc", "jkl");
		OutputStream out2 = new FileOutputStream(
				new File(this.home + "/.config/spring-boot/", ".spring-boot-devtools.properties"));
		properties2.store(out2, null);
		out2.close();
		ConfigurableEnvironment environment = new MockEnvironment();
		MockDevToolHomePropertiesPostProcessor postProcessor = new MockDevToolHomePropertiesPostProcessor();
		runPostProcessor(() -> postProcessor.postProcessEnvironment(environment, null));
		assertThat(environment.getProperty("abc")).isEqualTo("jkl");
	}

	@Test
	void ignoresMissingHomeProperties() throws Exception {
		ConfigurableEnvironment environment = new MockEnvironment();
		MockDevToolHomePropertiesPostProcessor postProcessor = new MockDevToolHomePropertiesPostProcessor();
		runPostProcessor(() -> postProcessor.postProcessEnvironment(environment, null));
		assertThat(environment.getProperty("abc")).isNull();
	}

	protected void runPostProcessor(Runnable runnable) throws Exception {
		Thread thread = new Thread(runnable);
		thread.start();
		thread.join();
	}

	private class MockDevToolHomePropertiesPostProcessor extends DevToolsHomePropertiesPostProcessor {

		@Override
		protected File getHomeFolder() {
			return DevToolsHomePropertiesPostProcessorTests.this.home;
		}

	}

}
