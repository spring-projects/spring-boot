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

package org.springframework.boot.devtools.settings;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DevToolsSettings}.
 *
 * @author Phillip Webb
 */
public class DevToolsSettingsTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private static final String ROOT = DevToolsSettingsTests.class.getPackage().getName().replace('.', '/') + "/";

	@Test
	public void includePatterns() throws Exception {
		DevToolsSettings settings = DevToolsSettings.load(ROOT + "spring-devtools-include.properties");
		assertThat(settings.isRestartInclude(new URL("file://test/a"))).isTrue();
		assertThat(settings.isRestartInclude(new URL("file://test/b"))).isTrue();
		assertThat(settings.isRestartInclude(new URL("file://test/c"))).isFalse();
	}

	@Test
	public void excludePatterns() throws Exception {
		DevToolsSettings settings = DevToolsSettings.load(ROOT + "spring-devtools-exclude.properties");
		assertThat(settings.isRestartExclude(new URL("file://test/a"))).isTrue();
		assertThat(settings.isRestartExclude(new URL("file://test/b"))).isTrue();
		assertThat(settings.isRestartExclude(new URL("file://test/c"))).isFalse();
	}

	@Test
	public void defaultIncludePatterns() throws Exception {
		DevToolsSettings settings = DevToolsSettings.get();
		assertThat(settings.isRestartExclude(makeUrl("spring-boot"))).isTrue();
		assertThat(settings.isRestartExclude(makeUrl("spring-boot-autoconfigure"))).isTrue();
		assertThat(settings.isRestartExclude(makeUrl("spring-boot-actuator"))).isTrue();
		assertThat(settings.isRestartExclude(makeUrl("spring-boot-starter"))).isTrue();
		assertThat(settings.isRestartExclude(makeUrl("spring-boot-starter-some-thing"))).isTrue();
	}

	private URL makeUrl(String name) throws IOException {
		File file = this.temporaryFolder.newFolder();
		file = new File(file, name);
		file = new File(file, "target");
		file = new File(file, "classes");
		file.mkdirs();
		return file.toURI().toURL();
	}

}
