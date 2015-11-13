/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link DevToolsSettings}.
 *
 * @author Phillip Webb
 */
public class DevToolsSettingsTests {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private static final String ROOT = DevToolsSettingsTests.class.getPackage().getName()
			.replace(".", "/") + "/";

	@Test
	public void includePatterns() throws Exception {
		DevToolsSettings settings = DevToolsSettings
				.load(ROOT + "spring-devtools-include.properties");
		assertThat(settings.isRestartInclude(new URL("file://test/a")), equalTo(true));
		assertThat(settings.isRestartInclude(new URL("file://test/b")), equalTo(true));
		assertThat(settings.isRestartInclude(new URL("file://test/c")), equalTo(false));
	}

	@Test
	public void excludePatterns() throws Exception {
		DevToolsSettings settings = DevToolsSettings
				.load(ROOT + "spring-devtools-exclude.properties");
		assertThat(settings.isRestartExclude(new URL("file://test/a")), equalTo(true));
		assertThat(settings.isRestartExclude(new URL("file://test/b")), equalTo(true));
		assertThat(settings.isRestartExclude(new URL("file://test/c")), equalTo(false));
	}

	@Test
	public void defaultIncludePatterns() throws Exception {
		DevToolsSettings settings = DevToolsSettings.get();
		assertTrue(settings.isRestartExclude(makeUrl("spring-boot")));
		assertTrue(settings.isRestartExclude(makeUrl("spring-boot-autoconfigure")));
		assertTrue(settings.isRestartExclude(makeUrl("spring-boot-actuator")));
		assertTrue(settings.isRestartExclude(makeUrl("spring-boot-starter")));
		assertTrue(settings.isRestartExclude(makeUrl("spring-boot-starter-some-thing")));
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
