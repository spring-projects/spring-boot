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

package org.springframework.boot.jarmode.tools;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ToolsJarMode}.
 *
 * @author Moritz Halbritter
 */
class ToolsJarModeTests extends AbstractJarModeTests {

	private ToolsJarMode mode;

	private TestPrintStream out;

	@BeforeEach
	void setUp() throws IOException {
		this.out = new TestPrintStream(this);
		Context context = new Context(createArchive(), this.tempDir);
		this.mode = new ToolsJarMode(context, this.out);
	}

	@Test
	void shouldAcceptToolsMode() {
		assertThat(this.mode.accepts("tools")).isTrue();
		assertThat(this.mode.accepts("something-else")).isFalse();
	}

	@Test
	void noParametersShowsHelp() {
		run();
		assertThat(this.out).hasSameContentAsResource("tools-help-output.txt");
	}

	@Test
	void helpForExtract() {
		run("help", "extract");
		assertThat(this.out).hasSameContentAsResource("tools-help-extract-output.txt");
	}

	@Test
	void helpForListLayers() {
		run("help", "list-layers");
		assertThat(this.out).hasSameContentAsResource("tools-help-list-layers-output.txt");
	}

	@Test
	void helpForHelp() {
		run("help", "help");
		assertThat(this.out).hasSameContentAsResource("tools-help-help-output.txt");
	}

	@Test
	void helpForUnknownCommand() {
		run("help", "unknown-command");
		assertThat(this.out).hasSameContentAsResource("tools-help-unknown-command-output.txt");
	}

	@Test
	void unknownCommandShowsErrorAndHelp() {
		run("something-invalid");
		assertThat(this.out).hasSameContentAsResource("tools-error-command-unknown-output.txt");
	}

	@Test
	void unknownOptionShowsErrorAndCommandHelp() {
		run("extract", "--something-invalid");
		assertThat(this.out).hasSameContentAsResource("tools-error-option-unknown-output.txt");
	}

	@Test
	void optionMissingRequiredValueShowsErrorAndCommandHelp() {
		run("extract", "--destination");
		assertThat(this.out).hasSameContentAsResource("tools-error-option-missing-value-output.txt");
	}

	private void run(String... args) {
		this.mode.run("tools", args);
	}

}
