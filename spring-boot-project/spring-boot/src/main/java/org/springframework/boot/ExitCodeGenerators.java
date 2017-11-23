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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.springframework.lang.NonNull;

/**
 * Maintains a collection of {@link ExitCodeGenerator} instances and allows the final exit
 * code to be calculated.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @see #getExitCode()
 * @see ExitCodeGenerator
 */
class ExitCodeGenerators implements Iterable<ExitCodeGenerator> {

	private List<ExitCodeGenerator> generators = new ArrayList<>();

	public void addAll(@NonNull Throwable exception,
			@NonNull ExitCodeExceptionMapper... mappers) {
		addAll(exception, Arrays.asList(mappers));
	}

	public void addAll(@NonNull Throwable exception,
			@NonNull Iterable<? extends ExitCodeExceptionMapper> mappers) {
		for (ExitCodeExceptionMapper mapper : mappers) {
			add(exception, mapper);
		}
	}

	public void add(@NonNull Throwable exception,
			@NonNull ExitCodeExceptionMapper mapper) {
		add(new MappedExitCodeGenerator(exception, mapper));
	}

	public void addAll(@NonNull ExitCodeGenerator... generators) {
		addAll(Arrays.asList(generators));
	}

	public void addAll(@NonNull Iterable<? extends ExitCodeGenerator> generators) {
		for (ExitCodeGenerator generator : generators) {
			add(generator);
		}
	}

	public void add(@NonNull ExitCodeGenerator generator) {
		this.generators.add(generator);
	}

	@Override
	public Iterator<ExitCodeGenerator> iterator() {
		return this.generators.iterator();
	}

	/**
	 * Get the final exit code that should be returned based on all contained generators.
	 * @return the final exit code.
	 */
	public int getExitCode() {
		int exitCode = 0;
		for (ExitCodeGenerator generator : this.generators) {
			try {
				int value = generator.getExitCode();
				if (value > 0 && value > exitCode || value < 0 && value < exitCode) {
					exitCode = value;
				}
			}
			catch (Exception ex) {
				exitCode = (exitCode == 0 ? 1 : exitCode);
				ex.printStackTrace();
			}
		}
		return exitCode;
	}

	/**
	 * Adapts an {@link ExitCodeExceptionMapper} to an {@link ExitCodeGenerator}.
	 */
	private static class MappedExitCodeGenerator implements ExitCodeGenerator {

		private final Throwable exception;

		private final ExitCodeExceptionMapper mapper;

		MappedExitCodeGenerator(Throwable exception, ExitCodeExceptionMapper mapper) {
			this.exception = exception;
			this.mapper = mapper;
		}

		@Override
		public int getExitCode() {
			return this.mapper.getExitCode(this.exception);
		}

	}

}
