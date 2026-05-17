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

package org.springframework.boot.context;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.context.event.PortBound;
import org.springframework.context.ApplicationEvent;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * Tests for {@link NetworkServerPortFileWriter}.
 *
 * @author Somil Jain
 */
class NetworkServerPortFileWriterTests {

	@TempDir
	@SuppressWarnings("NullAway.Init")
	File tempDir;

	@BeforeEach
	@AfterEach
	void reset() {
		System.clearProperty("PORTFILE");
	}

	@Test
	void createPortFile() {
		File file = new File(this.tempDir, "port.file");
		NetworkServerPortFileWriter listener = new NetworkServerPortFileWriter(file);
		listener.onApplicationEvent(new TestPortBoundEvent(this, 8080, ""));
		assertThat(contentOf(file)).isEqualTo("8080");
	}

	@Test
	void createsParentDirectories() {
		File file = new File(this.tempDir, "nested/path/port.file");
		NetworkServerPortFileWriter listener = new NetworkServerPortFileWriter(file);
		listener.onApplicationEvent(new TestPortBoundEvent(this, 8080, ""));
		assertThat(contentOf(file)).isEqualTo("8080");
		assertThat(file.getParentFile()).exists().isDirectory();
	}

	@Test
	void systemPropertyOverridesDefaultValue() {
		System.setProperty("PORTFILE", new File(this.tempDir, "override.file").getAbsolutePath());
		NetworkServerPortFileWriter listener = new NetworkServerPortFileWriter();
		listener.onApplicationEvent(new TestPortBoundEvent(this, 8080, ""));
		String content = contentOf(new File(System.getProperty("PORTFILE")));
		assertThat(content).isEqualTo("8080");
	}

	@Test
	void systemPropertyOverridesConstructorValue() {
		File constructorFile = new File(this.tempDir, "port.file");
		System.setProperty("PORTFILE", new File(this.tempDir, "override.file").getAbsolutePath());
		NetworkServerPortFileWriter listener = new NetworkServerPortFileWriter(constructorFile);
		listener.onApplicationEvent(new TestPortBoundEvent(this, 8080, ""));
		String content = contentOf(new File(System.getProperty("PORTFILE")));

		assertThat(content).isEqualTo("8080");
		assertThat(constructorFile).doesNotExist();
	}

	@Test
	void createNamespacePortFile() {
		File file = new File(this.tempDir, "port.file");
		NetworkServerPortFileWriter listener = new NetworkServerPortFileWriter(file);
		listener.onApplicationEvent(new TestPortBoundEvent(this, 8080, ""));
		listener.onApplicationEvent(new TestPortBoundEvent(this, 9090, "management"));
		assertThat(contentOf(file)).isEqualTo("8080");

		String managementFile = file.getName();
		String extension = StringUtils.getFilenameExtension(managementFile);
		assertThat(extension).isNotNull();
		managementFile = managementFile.substring(0, managementFile.length() - extension.length() - 1);
		managementFile = managementFile + "-management." + StringUtils.getFilenameExtension(file.getName());

		String content = contentOf(new File(file.getParentFile(), managementFile));
		assertThat(content).isEqualTo("9090");
		assertThat(collectFileNames(file.getParentFile())).contains(managementFile);
	}

	@Test
	void createUpperCaseNamespacePortFile() {
		File file = new File(this.tempDir, "port.file");
		file = new File(file.getParentFile(), file.getName().toUpperCase(Locale.ENGLISH));
		NetworkServerPortFileWriter listener = new NetworkServerPortFileWriter(file);
		listener.onApplicationEvent(new TestPortBoundEvent(this, 9090, "management"));

		String managementFile = file.getName();
		String extension = StringUtils.getFilenameExtension(managementFile);
		assertThat(extension).isNotNull();
		managementFile = managementFile.substring(0, managementFile.length() - extension.length() - 1);
		managementFile = managementFile + "-MANAGEMENT." + StringUtils.getFilenameExtension(file.getName());

		String content = contentOf(new File(file.getParentFile(), managementFile));
		assertThat(content).isEqualTo("9090");
		assertThat(collectFileNames(file.getParentFile())).contains(managementFile);
	}

	@Test
	void getPortFileWhenPortFileNameDoesNotHaveExtension() {
		File file = new File(this.tempDir, "portfile");
		NetworkServerPortFileWriter listener = new NetworkServerPortFileWriter(file);
		TestPortBoundEvent event = new TestPortBoundEvent(this, 9090, "management");
		assertThat(listener.getPortFile(event).getName()).isEqualTo("portfile-management");
	}

	@Test
	void unrelatedEventsAreIgnored() {
		File file = new File(this.tempDir, "port.file");
		NetworkServerPortFileWriter listener = new NetworkServerPortFileWriter(file);

		ApplicationEvent unrelatedEvent = new ApplicationEvent(this) {
		};

		listener.onApplicationEvent(unrelatedEvent);

		assertThat(file).doesNotExist();
	}

	@Test
	void existingFileIsOverwritten() throws Exception {
		File file = new File(this.tempDir, "port.file");
		FileCopyUtils.copy("9999".getBytes(), file);

		NetworkServerPortFileWriter listener = new NetworkServerPortFileWriter(file);
		listener.onApplicationEvent(new TestPortBoundEvent(this, 8080, ""));

		assertThat(contentOf(file)).isEqualTo("8080");
	}

	@Test
	void namespaceCanBeNullUsesDefaultFile() {
		File file = new File(this.tempDir, "port.file");
		NetworkServerPortFileWriter listener = new NetworkServerPortFileWriter(file);

		listener.onApplicationEvent(new TestPortBoundEvent(this, 8080, null));
		assertThat(contentOf(file)).isEqualTo("8080");
	}

	@Test
	void emptyNamespaceUsesDefaultFile() {
		File file = new File(this.tempDir, "port.file");
		NetworkServerPortFileWriter listener = new NetworkServerPortFileWriter(file);

		listener.onApplicationEvent(new TestPortBoundEvent(this, 8080, ""));
		assertThat(contentOf(file)).isEqualTo("8080");
	}

	private Set<String> collectFileNames(File directory) {
		Set<String> names = new HashSet<>();
		if (directory.isDirectory()) {
			for (File file : Objects.requireNonNull(directory.listFiles())) {
				names.add(file.getName());
			}
		}
		return names;
	}

	@Test
	void systemPropertyOverrideWithNamespaceAppendsSuffixCorrectly() {
		File customFile = new File(this.tempDir, "custom.file");
		System.setProperty("PORTFILE", customFile.getAbsolutePath());

		NetworkServerPortFileWriter listener = new NetworkServerPortFileWriter();
		listener.onApplicationEvent(new TestPortBoundEvent(this, 9090, "management"));

		assertThat(customFile).doesNotExist();

		File expectedFile = new File(this.tempDir, "custom-management.file");
		assertThat(contentOf(expectedFile)).isEqualTo("9090");
	}

	@Test
	void createPortFileWithStringFilenameConstructor() {
		File file = new File(this.tempDir, "string-constructor.file");
		NetworkServerPortFileWriter listener = new NetworkServerPortFileWriter(file.getAbsolutePath());
		listener.onApplicationEvent(new TestPortBoundEvent(this, 8080, ""));
		assertThat(contentOf(file)).isEqualTo("8080");
	}

	/**
	 * Private test event to simulate any server broadcasting a bound port.
	 */
	private static class TestPortBoundEvent extends ApplicationEvent implements PortBound {

		private final int port;

		private final @Nullable String namespace;

		TestPortBoundEvent(Object source, int port, @Nullable String namespace) {
			super(source);
			this.port = port;
			this.namespace = namespace;
		}

		@Override
		public int getPort() {
			return this.port;
		}

		@Override
		public @Nullable String getNamespace() {
			return this.namespace;
		}

	}

}
