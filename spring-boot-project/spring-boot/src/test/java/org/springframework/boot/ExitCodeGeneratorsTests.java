/*
 * Copyright 2012-2018 the original author or authors.
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

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ExitCodeGenerators}.
 *
 * @author Phillip Webb
 */
public class ExitCodeGeneratorsTests {

	@Test
	public void addAllWhenGeneratorsIsNullShouldThrowException() {
		assertThatIllegalArgumentException().isThrownBy(() -> {
			List<ExitCodeGenerator> generators = null;
			new ExitCodeGenerators().addAll(generators);
		}).withMessageContaining("Generators must not be null");
	}

	@Test
	public void addWhenGeneratorIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ExitCodeGenerators().add(null))
				.withMessageContaining("Generator must not be null");
	}

	@Test
	public void getExitCodeWhenNoGeneratorsShouldReturnZero() {
		assertThat(new ExitCodeGenerators().getExitCode()).isEqualTo(0);
	}

	@Test
	public void getExitCodeWhenGeneratorThrowsShouldReturnOne() {
		ExitCodeGenerator generator = mock(ExitCodeGenerator.class);
		given(generator.getExitCode()).willThrow(new IllegalStateException());
		ExitCodeGenerators generators = new ExitCodeGenerators();
		generators.add(generator);
		assertThat(generators.getExitCode()).isEqualTo(1);
	}

	@Test
	public void getExitCodeWhenAllNegativeShouldReturnLowestValue() {
		ExitCodeGenerators generators = new ExitCodeGenerators();
		generators.add(mockGenerator(-1));
		generators.add(mockGenerator(-3));
		generators.add(mockGenerator(-2));
		assertThat(generators.getExitCode()).isEqualTo(-3);
	}

	@Test
	public void getExitCodeWhenAllPositiveShouldReturnHighestValue() {
		ExitCodeGenerators generators = new ExitCodeGenerators();
		generators.add(mockGenerator(1));
		generators.add(mockGenerator(3));
		generators.add(mockGenerator(2));
		assertThat(generators.getExitCode()).isEqualTo(3);
	}

	@Test
	public void getExitCodeWhenUsingExitCodeExceptionMapperShouldCallMapper() {
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

	private ExitCodeExceptionMapper mockMapper(Class<?> exceptionType, int exitCode) {
		return (exception) -> {
			if (exceptionType.isInstance(exception)) {
				return exitCode;
			}
			return 0;
		};
	}

}
