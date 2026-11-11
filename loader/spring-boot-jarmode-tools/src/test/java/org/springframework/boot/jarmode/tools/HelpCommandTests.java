/*
 * Copyright 2012-present the original author or authors.
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
