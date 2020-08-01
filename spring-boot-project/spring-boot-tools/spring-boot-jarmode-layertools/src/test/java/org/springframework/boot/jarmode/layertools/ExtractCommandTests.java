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

package org.springframework.boot.jarmode.layertools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link ExtractCommand}.
 *
 * @author Phillip Webb
 */
class ExtractCommandTests {

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
		MockitoAnnotations.initMocks(this);
		this.jarFile = createJarFile("test.jar");
		this.extract = new File(this.temp, "extract");
		this.extract.mkdir();
		given(this.context.getJarFile()).willReturn(this.jarFile);
		given(this.context.getWorkingDir()).willReturn(this.extract);
		this.command = new ExtractCommand(this.context, this.layers);
	}

	@Test
	void runExtractsLayers() throws Exception {
		this.command.run(Collections.emptyMap(), Collections.emptyList());
		assertThat(this.extract.list()).containsOnly("a", "b", "c", "d");
		assertThat(new File(this.extract, "a/a/a.jar")).exists();
		assertThat(new File(this.extract, "b/b/b.jar")).exists();
		assertThat(new File(this.extract, "c/c/c.jar")).exists();
		assertThat(new File(this.extract, "d")).isDirectory();
	}

	@Test
	void runWhenHasDestinationOptionExtractsLayers() {
		File out = new File(this.extract, "out");
		this.command.run(Collections.singletonMap(ExtractCommand.DESTINATION_OPTION, out.getAbsolutePath()),
				Collections.emptyList());
		assertThat(this.extract.list()).containsOnly("out");
		assertThat(new File(this.extract, "out/a/a/a.jar")).exists();
		assertThat(new File(this.extract, "out/b/b/b.jar")).exists();
		assertThat(new File(this.extract, "out/c/c/c.jar")).exists();
	}

	@Test
	void runWhenHasLayerParamsExtractsLimitedLayers() {
		this.command.run(Collections.emptyMap(), Arrays.asList("a", "c"));
		assertThat(this.extract.list()).containsOnly("a", "c");
		assertThat(new File(this.extract, "a/a/a.jar")).exists();
		assertThat(new File(this.extract, "c/c/c.jar")).exists();
	}

	private File createJarFile(String name) throws IOException {
		File file = new File(this.temp, name);
		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(file))) {
			out.putNextEntry(new ZipEntry("a/"));
			out.closeEntry();
			out.putNextEntry(new ZipEntry("a/a.jar"));
			out.closeEntry();
			out.putNextEntry(new ZipEntry("b/"));
			out.closeEntry();
			out.putNextEntry(new ZipEntry("b/b.jar"));
			out.closeEntry();
			out.putNextEntry(new ZipEntry("c/"));
			out.closeEntry();
			out.putNextEntry(new ZipEntry("c/c.jar"));
			out.closeEntry();
			out.putNextEntry(new ZipEntry("d/"));
			out.closeEntry();
		}
		return file;
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
