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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HelpCommand}.
 *
 * @author Phillip Webb
 */
class HelpCommandTests {

	private HelpCommand command;

	private TestPrintStream out;

	@TempDir
	File temp;

	@BeforeEach
	void setup() throws Exception {
		Context context = mock(Context.class);
		given(context.getArchiveFile()).willReturn(createJarFile("test.jar"));
		this.command = new HelpCommand(context, LayerToolsJarMode.Runner.getCommands(context));
		this.out = new TestPrintStream(this);
	}

	@Test
	void runWhenHasNoParametersPrintsUsage() {
		this.command.run(this.out, Collections.emptyMap(), Collections.emptyList());
		assertThat(this.out).hasSameContentAsResource("help-output.txt");
	}

	@Test
	void runWhenHasNoCommandParameterPrintsUsage() {
		this.command.run(this.out, Collections.emptyMap(), Arrays.asList("extract"));
		System.out.println(this.out);
		assertThat(this.out).hasSameContentAsResource("help-extract-output.txt");
	}

	private File createJarFile(String name) throws Exception {
		File file = new File(this.temp, name);
		try (ZipOutputStream jarOutputStream = new ZipOutputStream(new FileOutputStream(file))) {
			jarOutputStream.putNextEntry(new JarEntry("META-INF/MANIFEST.MF"));
			jarOutputStream.write(getFile("test-manifest.MF").getBytes());
			jarOutputStream.closeEntry();
			JarEntry indexEntry = new JarEntry("BOOT-INF/layers.idx");
			jarOutputStream.putNextEntry(indexEntry);
			Writer writer = new OutputStreamWriter(jarOutputStream, StandardCharsets.UTF_8);
			writer.write("- \"0001\":\n");
			writer.write("  - \"BOOT-INF/lib/a.jar\"\n");
			writer.write("  - \"BOOT-INF/lib/b.jar\"\n");
			writer.write("- \"0002\":\n");
			writer.write("  - \"BOOT-INF/lib/c.jar\"\n");
			writer.write("- \"0003\":\n");
			writer.write("  - \"BOOT-INF/lib/d.jar\"\n");
			writer.flush();
		}
		return file;
	}

	private String getFile(String fileName) throws Exception {
		ClassPathResource resource = new ClassPathResource(fileName, getClass());
		InputStreamReader reader = new InputStreamReader(resource.getInputStream());
		return FileCopyUtils.copyToString(reader);
	}

}
