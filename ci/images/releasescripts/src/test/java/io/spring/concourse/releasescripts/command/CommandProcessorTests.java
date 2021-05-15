/*
 * Copyright 2012-2019 the original author or authors.
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

package io.spring.concourse.releasescripts.command;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link CommandProcessor}.
 *
 * @author Madhura Bhave
 */
class CommandProcessorTests {

	private static final String[] NO_ARGS = {};

	@Test
	void runWhenNoArgumentThrowsException() {
		CommandProcessor processor = new CommandProcessor(Collections.singletonList(mock(Command.class)));
		assertThatIllegalStateException().isThrownBy(() -> processor.run(new DefaultApplicationArguments(NO_ARGS)))
				.withMessage("No command argument specified");
	}

	@Test
	void runWhenUnknownCommandThrowsException() {
		Command fooCommand = mock(Command.class);
		given(fooCommand.getName()).willReturn("foo");
		CommandProcessor processor = new CommandProcessor(Collections.singletonList(fooCommand));
		DefaultApplicationArguments args = new DefaultApplicationArguments(new String[] { "bar", "go" });
		assertThatIllegalStateException().isThrownBy(() -> processor.run(args)).withMessage("Unknown command 'bar'");
	}

	@Test
	void runDelegatesToCommand() throws Exception {
		Command fooCommand = mock(Command.class);
		given(fooCommand.getName()).willReturn("foo");
		Command barCommand = mock(Command.class);
		given(barCommand.getName()).willReturn("bar");
		CommandProcessor processor = new CommandProcessor(Arrays.asList(fooCommand, barCommand));
		DefaultApplicationArguments args = new DefaultApplicationArguments(new String[] { "bar", "go" });
		processor.run(args);
		verify(fooCommand, never()).run(any());
		verify(barCommand).run(args);
	}

}