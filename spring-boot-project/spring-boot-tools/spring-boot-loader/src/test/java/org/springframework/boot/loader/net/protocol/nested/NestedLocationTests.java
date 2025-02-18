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

package org.springframework.boot.loader.net.protocol.nested;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.loader.net.protocol.Handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link NestedLocation}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class NestedLocationTests {

	@TempDir
	File temp;

	@BeforeAll
	static void registerHandlers() {
		Handlers.register();
	}

	@Test
	void createWhenPathIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new NestedLocation(null, "nested.jar"))
			.withMessageContaining("'path' must not be null");
	}

	@Test
	void createWhenNestedEntryNameIsNull() {
		NestedLocation location = new NestedLocation(Path.of("test.jar"), null);
		assertThat(location.path().toString()).contains("test.jar");
		assertThat(location.nestedEntryName()).isNull();
	}

	@Test
	void createWhenNestedEntryNameIsEmpty() {
		NestedLocation location = new NestedLocation(Path.of("test.jar"), "");
		assertThat(location.path().toString()).contains("test.jar");
		assertThat(location.nestedEntryName()).isNull();
	}

	@Test
	void fromUrlWhenUrlIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> NestedLocation.fromUrl(null))
			.withMessageContaining("'url' must not be null");
	}

	@Test
	void fromUrlWhenNotNestedProtocolThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> NestedLocation.fromUrl(new URL("file://test.jar")))
			.withMessageContaining("must use 'nested' protocol");
	}

	@Test
	void fromUrlWhenNoPathThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> NestedLocation.fromUrl(new URL("nested:")))
			.withMessageContaining("'location' must not be empty");
	}

	@Test
	void fromUrlWhenNoSeparator() throws Exception {
		File file = new File(this.temp, "test.jar");
		NestedLocation location = NestedLocation.fromUrl(new URL("nested:" + file.getAbsolutePath() + "/"));
		assertThat(location.path()).isEqualTo(file.toPath());
		assertThat(location.nestedEntryName()).isNull();
	}

	@Test
	void fromUrlReturnsNestedLocation() throws Exception {
		File file = new File(this.temp, "test.jar");
		NestedLocation location = NestedLocation
			.fromUrl(new URL("nested:" + file.getAbsolutePath() + "/!lib/nested.jar"));
		assertThat(location.path()).isEqualTo(file.toPath());
		assertThat(location.nestedEntryName()).isEqualTo("lib/nested.jar");
	}

	@Test
	void fromUriWhenUrlIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> NestedLocation.fromUri(null))
			.withMessageContaining("'uri' must not be null");
	}

	@Test
	void fromUriWhenNotNestedProtocolThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> NestedLocation.fromUri(new URI("file://test.jar")))
			.withMessageContaining("must use 'nested' scheme");
	}

	@Test
	@Disabled
	void fromUriWhenNoSeparator() throws Exception {
		NestedLocation location = NestedLocation.fromUri(new URI("nested:test.jar!nested.jar"));
		assertThat(location.path().toString()).contains("test.jar!nested.jar");
		assertThat(location.nestedEntryName()).isNull();
	}

	@Test
	void fromUriReturnsNestedLocation() throws Exception {
		File file = new File(this.temp, "test.jar");
		NestedLocation location = NestedLocation
			.fromUri(new URI("nested:" + file.getAbsoluteFile().toURI().getPath() + "/!lib/nested.jar"));
		assertThat(location.path()).isEqualTo(file.toPath());
		assertThat(location.nestedEntryName()).isEqualTo("lib/nested.jar");
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	void windowsUncPathIsHandledCorrectly() throws MalformedURLException {
		NestedLocation location = NestedLocation.fromUrl(
				new URL("nested://localhost/c$/dev/temp/demo/build/libs/demo-0.0.1-SNAPSHOT.jar/!BOOT-INF/classes/"));
		assertThat(location.path()).asString()
			.isEqualTo("\\\\localhost\\c$\\dev\\temp\\demo\\build\\libs\\demo-0.0.1-SNAPSHOT.jar");
	}

}
