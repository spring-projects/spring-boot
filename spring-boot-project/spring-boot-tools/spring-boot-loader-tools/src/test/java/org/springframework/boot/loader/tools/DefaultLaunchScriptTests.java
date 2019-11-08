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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultLaunchScript}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Justin Rosenberg
 */
class DefaultLaunchScriptTests {

	@TempDir
	File tempDir;

	@Test
	void loadsDefaultScript() throws Exception {
		DefaultLaunchScript script = new DefaultLaunchScript(null, null);
		String content = new String(script.toByteArray());
		assertThat(content).contains("Spring Boot Startup Script");
	}

	@Test
	void logFilenameCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("logFilename");
	}

	@Test
	void pidFilenameCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("pidFilename");
	}

	@Test
	void initInfoProvidesCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("initInfoProvides");
	}

	@Test
	void initInfoRequiredStartCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("initInfoRequiredStart");
	}

	@Test
	void initInfoRequiredStopCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("initInfoRequiredStop");
	}

	@Test
	void initInfoDefaultStartCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("initInfoDefaultStart");
	}

	@Test
	void initInfoDefaultStopCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("initInfoDefaultStop");
	}

	@Test
	void initInfoShortDescriptionCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("initInfoShortDescription");
	}

	@Test
	void initInfoDescriptionCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("initInfoDescription");
	}

	@Test
	void initInfoChkconfigCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("initInfoChkconfig");
	}

	@Test
	void modeCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("mode");
	}

	@Test
	void useStartStopDaemonCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("useStartStopDaemon");
	}

	@Test
	void logFolderCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("logFolder");
	}

	@Test
	void pidFolderCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("pidFolder");
	}

	@Test
	void confFolderCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("confFolder");
	}

	@Test
	void stopWaitTimeCanBeReplaced() throws Exception {
		assertThatPlaceholderCanBeReplaced("stopWaitTime");
	}

	@Test
	void inlinedConfScriptFileLoad() throws IOException {
		DefaultLaunchScript script = new DefaultLaunchScript(null,
				createProperties("inlinedConfScript:src/test/resources/example.script"));
		String content = new String(script.toByteArray());
		assertThat(content).contains("FOO=BAR");
	}

	@Test
	void defaultForUseStartStopDaemonIsTrue() throws Exception {
		DefaultLaunchScript script = new DefaultLaunchScript(null, null);
		String content = new String(script.toByteArray());
		assertThat(content).contains("USE_START_STOP_DAEMON=\"true\"");
	}

	@Test
	void defaultForModeIsAuto() throws Exception {
		DefaultLaunchScript script = new DefaultLaunchScript(null, null);
		String content = new String(script.toByteArray());
		assertThat(content).contains("MODE=\"auto\"");
	}

	@Test
	void defaultForStopWaitTimeIs60() throws Exception {
		DefaultLaunchScript script = new DefaultLaunchScript(null, null);
		String content = new String(script.toByteArray());
		assertThat(content).contains("STOP_WAIT_TIME=\"60\"");
	}

	@Test
	void loadFromFile() throws Exception {
		File file = new File(this.tempDir, "script");
		FileCopyUtils.copy("ABC".getBytes(), file);
		DefaultLaunchScript script = new DefaultLaunchScript(file, null);
		String content = new String(script.toByteArray());
		assertThat(content).isEqualTo("ABC");
	}

	@Test
	void expandVariables() throws Exception {
		File file = new File(this.tempDir, "script");
		FileCopyUtils.copy("h{{a}}ll{{b}}".getBytes(), file);
		DefaultLaunchScript script = new DefaultLaunchScript(file, createProperties("a:e", "b:o"));
		String content = new String(script.toByteArray());
		assertThat(content).isEqualTo("hello");
	}

	@Test
	void expandVariablesMultiLine() throws Exception {
		File file = new File(this.tempDir, "script");
		FileCopyUtils.copy("h{{a}}l\nl{{b}}".getBytes(), file);
		DefaultLaunchScript script = new DefaultLaunchScript(file, createProperties("a:e", "b:o"));
		String content = new String(script.toByteArray());
		assertThat(content).isEqualTo("hel\nlo");
	}

	@Test
	void expandVariablesWithDefaults() throws Exception {
		File file = new File(this.tempDir, "script");
		FileCopyUtils.copy("h{{a:e}}ll{{b:o}}".getBytes(), file);
		DefaultLaunchScript script = new DefaultLaunchScript(file, null);
		String content = new String(script.toByteArray());
		assertThat(content).isEqualTo("hello");
	}

	@Test
	void expandVariablesCanDefaultToBlank() throws Exception {
		File file = new File(this.tempDir, "script");
		FileCopyUtils.copy("s{{p:}}{{r:}}ing".getBytes(), file);
		DefaultLaunchScript script = new DefaultLaunchScript(file, null);
		String content = new String(script.toByteArray());
		assertThat(content).isEqualTo("sing");
	}

	@Test
	void expandVariablesWithDefaultsOverride() throws Exception {
		File file = new File(this.tempDir, "script");
		FileCopyUtils.copy("h{{a:e}}ll{{b:o}}".getBytes(), file);
		DefaultLaunchScript script = new DefaultLaunchScript(file, createProperties("a:a"));
		String content = new String(script.toByteArray());
		assertThat(content).isEqualTo("hallo");
	}

	@Test
	void expandVariablesMissingAreUnchanged() throws Exception {
		File file = new File(this.tempDir, "script");
		FileCopyUtils.copy("h{{a}}ll{{b}}".getBytes(), file);
		DefaultLaunchScript script = new DefaultLaunchScript(file, null);
		String content = new String(script.toByteArray());
		assertThat(content).isEqualTo("h{{a}}ll{{b}}");
	}

	private void assertThatPlaceholderCanBeReplaced(String placeholder) throws Exception {
		DefaultLaunchScript script = new DefaultLaunchScript(null, createProperties(placeholder + ":__test__"));
		String content = new String(script.toByteArray());
		assertThat(content).contains("__test__");
	}

	private Map<?, ?> createProperties(String... pairs) {
		Map<Object, Object> properties = new HashMap<>();
		for (String pair : pairs) {
			String[] keyValue = pair.split(":");
			properties.put(keyValue[0], keyValue[1]);
		}
		return properties;
	}

}
