/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.jarmode.layertools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link ExtractCommand}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
@ExtendWith(MockitoExtension.class)
class ExtractCommandTests {

	private static final FileTime CREATION_TIME = FileTime.from(Instant.now().minus(3, ChronoUnit.DAYS));

	private static final FileTime LAST_MODIFIED_TIME = FileTime.from(Instant.now().minus(2, ChronoUnit.DAYS));

	private static final FileTime LAST_ACCESS_TIME = FileTime.from(Instant.now().minus(1, ChronoUnit.DAYS));

	@TempDir
	File temp;

	@Mock
	private Context context;

	private File jarFile;

	private File extract;

	private Layers layers = new TestLayers();

	private ExtractCommand command;

	@BeforeEach
	void setup() throws Exception {
		this.jarFile = createJarFile("test.jar");
		this.extract = new File(this.temp, "extract");
		this.extract.mkdir();
		this.command = new ExtractCommand(this.context, this.layers);
	}

	@Test
	void runExtractsLayers() {
		given(this.context.getArchiveFile()).willReturn(this.jarFile);
		given(this.context.getWorkingDir()).willReturn(this.extract);
		this.command.run(Collections.emptyMap(), Collections.emptyList());
		assertThat(this.extract.list()).containsOnly("a", "b", "c", "d");
		assertThat(new File(this.extract, "a/a/a.jar")).exists().satisfies(this::timeAttributes);
		assertThat(new File(this.extract, "b/b/b.jar")).exists().satisfies(this::timeAttributes);
		assertThat(new File(this.extract, "c/c/c.jar")).exists().satisfies(this::timeAttributes);
		assertThat(new File(this.extract, "d")).isDirectory();
		assertThat(new File(this.extract.getParentFile(), "e.jar")).doesNotExist();
	}

	private void timeAttributes(File file) {
		try {
			BasicFileAttributes basicAttributes = Files
					.getFileAttributeView(file.toPath(), BasicFileAttributeView.class, new LinkOption[0])
					.readAttributes();
			assertThat(basicAttributes.lastModifiedTime().to(TimeUnit.SECONDS))
					.isEqualTo(LAST_MODIFIED_TIME.to(TimeUnit.SECONDS));
			assertThat(basicAttributes.creationTime().to(TimeUnit.SECONDS)).satisfiesAnyOf(
					(creationTime) -> assertThat(creationTime).isEqualTo(CREATION_TIME.to(TimeUnit.SECONDS)),
					// On macOS (at least) the creation time is the last modified time
					(creationTime) -> assertThat(creationTime).isEqualTo(LAST_MODIFIED_TIME.to(TimeUnit.SECONDS)));
			assertThat(basicAttributes.lastAccessTime().to(TimeUnit.SECONDS))
					.isEqualTo(LAST_ACCESS_TIME.to(TimeUnit.SECONDS));
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Test
	void runWhenHasDestinationOptionExtractsLayers() {
		given(this.context.getArchiveFile()).willReturn(this.jarFile);
		File out = new File(this.extract, "out");
		this.command.run(Collections.singletonMap(ExtractCommand.DESTINATION_OPTION, out.getAbsolutePath()),
				Collections.emptyList());
		assertThat(this.extract.list()).containsOnly("out");
		assertThat(new File(this.extract, "out/a/a/a.jar")).exists().satisfies(this::timeAttributes);
		assertThat(new File(this.extract, "out/b/b/b.jar")).exists().satisfies(this::timeAttributes);
		assertThat(new File(this.extract, "out/c/c/c.jar")).exists().satisfies(this::timeAttributes);
	}

	@Test
	void runWhenHasLayerParamsExtractsLimitedLayers() {
		given(this.context.getArchiveFile()).willReturn(this.jarFile);
		given(this.context.getWorkingDir()).willReturn(this.extract);
		this.command.run(Collections.emptyMap(), Arrays.asList("a", "c"));
		assertThat(this.extract.list()).containsOnly("a", "c");
		assertThat(new File(this.extract, "a/a/a.jar")).exists().satisfies(this::timeAttributes);
		assertThat(new File(this.extract, "c/c/c.jar")).exists().satisfies(this::timeAttributes);
		assertThat(new File(this.extract.getParentFile(), "e.jar")).doesNotExist();
	}

	@Test
	void runWithJarFileContainingNoEntriesFails() throws IOException {
		File file = new File(this.temp, "empty.jar");
		try (FileWriter writer = new FileWriter(file)) {
			writer.write("text");
		}
		given(this.context.getArchiveFile()).willReturn(file);
		given(this.context.getWorkingDir()).willReturn(this.extract);
		assertThatIllegalStateException()
				.isThrownBy(() -> this.command.run(Collections.emptyMap(), Collections.emptyList()))
				.withMessageContaining("not compatible with layertools");
	}

	@Test
	void runWithJarFileThatWouldWriteEntriesOutsideDestinationFails() throws Exception {
		this.jarFile = createJarFile("test.jar", (out) -> {
			try {
				out.putNextEntry(new ZipEntry("e/../../e.jar"));
				out.closeEntry();
			}
			catch (IOException ex) {
				throw new IllegalStateException(ex);
			}
		});
		given(this.context.getArchiveFile()).willReturn(this.jarFile);
		assertThatIllegalStateException()
				.isThrownBy(() -> this.command.run(Collections.emptyMap(), Collections.emptyList()))
				.withMessageContaining("Entry 'e/../../e.jar' would be written");
	}

	private File createJarFile(String name) throws Exception {
		return createJarFile(name, (out) -> {
		});
	}

	private File createJarFile(String name, Consumer<ZipOutputStream> streamHandler) throws Exception {
		File file = new File(this.temp, name);
		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
			out.putNextEntry(entry("a/"));
			out.closeEntry();
			out.putNextEntry(entry("a/a.jar"));
			out.closeEntry();
			out.putNextEntry(entry("b/"));
			out.closeEntry();
			out.putNextEntry(entry("b/b.jar"));
			out.closeEntry();
			out.putNextEntry(entry("c/"));
			out.closeEntry();
			out.putNextEntry(entry("c/c.jar"));
			out.closeEntry();
			out.putNextEntry(entry("d/"));
			out.closeEntry();
			out.putNextEntry(entry("META-INF/MANIFEST.MF"));
			out.write(getFile("test-manifest.MF").getBytes());
			out.closeEntry();
			streamHandler.accept(out);
		}
		return file;
	}

	private ZipEntry entry(String path) {
		ZipEntry entry = new ZipEntry(path);
		entry.setCreationTime(CREATION_TIME);
		entry.setLastModifiedTime(LAST_MODIFIED_TIME);
		entry.setLastAccessTime(LAST_ACCESS_TIME);
		return entry;
	}

	private String getFile(String fileName) throws Exception {
		ClassPathResource resource = new ClassPathResource(fileName, getClass());
		InputStreamReader reader = new InputStreamReader(resource.getInputStream());
		return FileCopyUtils.copyToString(reader);
	}

	private static class TestLayers implements Layers {

		@Override
		public Iterator<String> iterator() {
			return Arrays.asList("a", "b", "c", "d").iterator();
		}

		@Override
		public String getLayer(ZipEntry entry) {
			if (entry.getName().startsWith("a")) {
				return "a";
			}
			if (entry.getName().startsWith("b")) {
				return "b";
			}
			return "c";
		}

	}

}
