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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link ListCommand}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 */
@ExtendWith(MockitoExtension.class)
class ListCommandTests {

	@TempDir
	File temp;

	@Mock
	private Context context;

	private File jarFile;

	private ListCommand command;

	private TestPrintStream out;

	@BeforeEach
	void setup() throws Exception {
		this.jarFile = createJarFile("test.jar");
		given(this.context.getJarFile()).willReturn(this.jarFile);
		this.command = new ListCommand(this.context);
		this.out = new TestPrintStream(this);
	}

	@Test
	void listLayersShouldListLayers() {
		Layers layers = IndexedLayers.get(this.context);
		this.command.printLayers(layers, this.out);
		assertThat(this.out).hasSameContentAsResource("list-output.txt");
	}

	private File createJarFile(String name) throws IOException {
		File file = new File(this.temp, name);
		try (ZipOutputStream jarOutputStream = new ZipOutputStream(new FileOutputStream(file))) {
			writeLayersIndex(jarOutputStream);
			String entryPrefix = "BOOT-INF/lib/";
			jarOutputStream.putNextEntry(new ZipEntry(entryPrefix + "a/"));
			jarOutputStream.closeEntry();
			jarOutputStream.putNextEntry(new ZipEntry(entryPrefix + "a/a.jar"));
			jarOutputStream.closeEntry();
			jarOutputStream.putNextEntry(new ZipEntry(entryPrefix + "b/"));
			jarOutputStream.closeEntry();
			jarOutputStream.putNextEntry(new ZipEntry(entryPrefix + "b/b.jar"));
			jarOutputStream.closeEntry();
			jarOutputStream.putNextEntry(new ZipEntry(entryPrefix + "c/"));
			jarOutputStream.closeEntry();
			jarOutputStream.putNextEntry(new ZipEntry(entryPrefix + "c/c.jar"));
			jarOutputStream.closeEntry();
			jarOutputStream.putNextEntry(new ZipEntry(entryPrefix + "d/"));
			jarOutputStream.closeEntry();
		}
		return file;
	}

	private void writeLayersIndex(ZipOutputStream out) throws IOException {
		JarEntry indexEntry = new JarEntry("BOOT-INF/layers.idx");
		out.putNextEntry(indexEntry);
		Writer writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
		writer.write("- \"0001\":\n");
		writer.write("  - \"BOOT-INF/lib/a.jar\"\n");
		writer.write("  - \"BOOT-INF/lib/b.jar\"\n");
		writer.write("- \"0002\":\n");
		writer.write("  - \"BOOT-INF/lib/c.jar\"\n");
		writer.write("- \"0003\":\n");
		writer.write("  - \"BOOT-INF/lib/d.jar\"\n");
		writer.flush();
	}

}
