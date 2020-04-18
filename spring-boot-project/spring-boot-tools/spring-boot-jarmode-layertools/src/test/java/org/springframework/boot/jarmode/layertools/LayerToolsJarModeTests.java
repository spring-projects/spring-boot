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
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link LayerToolsJarMode}.
 *
 * @author Phillip Webb
 */
class LayerToolsJarModeTests {

	private static final String[] NO_ARGS = {};

	private TestPrintStream out;

	private PrintStream systemOut;

	@TempDir
	File temp;

	@BeforeEach
	void setup() throws Exception {
		Context context = mock(Context.class);
		given(context.getJarFile()).willReturn(createJarFile("test.jar"));
		this.out = new TestPrintStream(this);
		this.systemOut = System.out;
		System.setOut(this.out);
		LayerToolsJarMode.Runner.contextOverride = context;
	}

	@AfterEach
	void restore() {
		System.setOut(this.systemOut);
		LayerToolsJarMode.Runner.contextOverride = null;
	}

	@Test
	void mainWithNoParametersShowsHelp() {
		new LayerToolsJarMode().run("layertools", NO_ARGS);
		assertThat(this.out).hasSameContentAsResource("help-output.txt");
	}

	@Test
	void mainWithArgRunsCommand() {
		new LayerToolsJarMode().run("layertools", new String[] { "list" });
		assertThat(this.out).hasSameContentAsResource("list-output.txt");
	}

	private File createJarFile(String name) throws IOException {
		File file = new File(this.temp, name);
		try (ZipOutputStream jarOutputStream = new ZipOutputStream(new FileOutputStream(file))) {
			JarEntry indexEntry = new JarEntry("BOOT-INF/layers.idx");
			jarOutputStream.putNextEntry(indexEntry);
			Writer writer = new OutputStreamWriter(jarOutputStream, StandardCharsets.UTF_8);
			writer.write("- \"0001\":\n");
			writer.write("  - \"BOOT-INF/lib/a.jar\"\n");
			writer.write("  - \"BOOT-INF/lib/b.jar\"\n");
			writer.write("- \"0002\":\n");
			writer.write("  - \"0002 BOOT-INF/lib/c.jar\"\n");
			writer.write("- \"0003\":\n");
			writer.write("  - \"BOOT-INF/lib/d.jar\"\n");
			writer.flush();
		}
		return file;
	}

}
