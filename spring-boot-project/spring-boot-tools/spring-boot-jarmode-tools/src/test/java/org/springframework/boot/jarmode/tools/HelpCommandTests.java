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

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link HelpCommand}.
 *
 * @author Phillip Webb
 */
class HelpCommandTests {

	private HelpCommand command;

	private TestPrintStream out;

	@TempDir
	Path temp;

	@BeforeEach
	void setup() {
		Context context = Mockito.mock(Context.class);
		given(context.getArchiveFile()).willReturn(this.temp.resolve("test.jar").toFile());
		this.command = new HelpCommand(context, List.of(new TestCommand()), "tools");
		this.out = new TestPrintStream(this);
	}

	@Test
	void shouldPrintAllCommands() {
		this.command.run(this.out, Collections.emptyList());
		assertThat(this.out).hasSameContentAsResource("help-output.txt");
	}

	@Test
	void shouldPrintCommandSpecificHelp() {
		this.command.run(this.out, List.of("test"));
		System.out.println(this.out);
		assertThat(this.out).hasSameContentAsResource("help-test-output.txt");
	}

}
