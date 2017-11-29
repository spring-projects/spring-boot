/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot;

import java.io.IOException;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ExitCodeGenerators}.
 *
 * @author Phillip Webb
 */
public class ExitCodeGeneratorsTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void addAllWhenGeneratorsIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Generators must not be null");
		List<ExitCodeGenerator> generators = null;
		new ExitCodeGenerators().addAll(generators);
	}

	@Test
	public void addWhenGeneratorIsNullShouldThrowException() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Generator must not be null");
		new ExitCodeGenerators().add(null);
	}

	@Test
	public void getExitCodeWhenNoGeneratorsShouldReturnZero() throws Exception {
		assertThat(new ExitCodeGenerators().getExitCode()).isEqualTo(0);
	}

	@Test
	public void getExitCodeWhenGeneratorThrowsShouldReturnOne() throws Exception {
		ExitCodeGenerator generator = mock(ExitCodeGenerator.class);
		given(generator.getExitCode()).willThrow(new IllegalStateException());
		ExitCodeGenerators generators = new ExitCodeGenerators();
		generators.add(generator);
		assertThat(generators.getExitCode()).isEqualTo(1);
	}

	@Test
	public void getExitCodeWhenAllNegativeShouldReturnLowestValue() throws Exception {
		ExitCodeGenerators generators = new ExitCodeGenerators();
		generators.add(mockGenerator(-1));
		generators.add(mockGenerator(-3));
		generators.add(mockGenerator(-2));
		assertThat(generators.getExitCode()).isEqualTo(-3);
	}

	@Test
	public void getExitCodeWhenAllPositiveShouldReturnHighestValue() throws Exception {
		ExitCodeGenerators generators = new ExitCodeGenerators();
		generators.add(mockGenerator(1));
		generators.add(mockGenerator(3));
		generators.add(mockGenerator(2));
		assertThat(generators.getExitCode()).isEqualTo(3);
	}

	@Test
	public void getExitCodeWhenUsingExitCodeExceptionMapperShouldCallMapper()
			throws Exception {
		ExitCodeGenerators generators = new ExitCodeGenerators();
		Exception e = new IOException();
		generators.add(e, mockMapper(IllegalStateException.class, 1));
		generators.add(e, mockMapper(IOException.class, 2));
		generators.add(e, mockMapper(UnsupportedOperationException.class, 3));
		assertThat(generators.getExitCode()).isEqualTo(2);
	}

	private ExitCodeGenerator mockGenerator(int exitCode) {
		ExitCodeGenerator generator = mock(ExitCodeGenerator.class);
		given(generator.getExitCode()).willReturn(exitCode);
		return generator;
	}

	private ExitCodeExceptionMapper mockMapper(final Class<?> exceptionType,
			final int exitCode) {
		return (exception) -> {
			if (exceptionType.isInstance(exception)) {
				return exitCode;
			}
			return 0;
		};
	}

}
