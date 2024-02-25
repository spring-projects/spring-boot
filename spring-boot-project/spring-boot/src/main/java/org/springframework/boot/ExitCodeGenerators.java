/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;

/**
 * Maintains an ordered collection of {@link ExitCodeGenerator} instances and allows the
 * final exit code to be calculated. Generators are ordered by {@link Order @Order} and
 * {@link Ordered}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author GenKui Du
 * @see #getExitCode()
 * @see ExitCodeGenerator
 */
class ExitCodeGenerators implements Iterable<ExitCodeGenerator> {

	private final List<ExitCodeGenerator> generators = new ArrayList<>();

	/**
     * Adds multiple ExitCodeExceptionMappers to handle a specific Throwable exception.
     * 
     * @param exception the Throwable exception to be handled
     * @param mappers   the ExitCodeExceptionMappers to be added
     * @throws IllegalArgumentException if exception or mappers is null
     */
    void addAll(Throwable exception, ExitCodeExceptionMapper... mappers) {
		Assert.notNull(exception, "Exception must not be null");
		Assert.notNull(mappers, "Mappers must not be null");
		addAll(exception, Arrays.asList(mappers));
	}

	/**
     * Adds the given exception to all the provided mappers.
     * 
     * @param exception the exception to be added
     * @param mappers   the collection of mappers to add the exception to
     * @throws IllegalArgumentException if the exception or mappers are null
     */
    void addAll(Throwable exception, Iterable<? extends ExitCodeExceptionMapper> mappers) {
		Assert.notNull(exception, "Exception must not be null");
		Assert.notNull(mappers, "Mappers must not be null");
		for (ExitCodeExceptionMapper mapper : mappers) {
			add(exception, mapper);
		}
	}

	/**
     * Adds a new exit code generator based on the provided exception and mapper.
     * 
     * @param exception the exception to be mapped to an exit code
     * @param mapper the mapper used to map the exception to an exit code
     * @throws IllegalArgumentException if either the exception or the mapper is null
     */
    void add(Throwable exception, ExitCodeExceptionMapper mapper) {
		Assert.notNull(exception, "Exception must not be null");
		Assert.notNull(mapper, "Mapper must not be null");
		add(new MappedExitCodeGenerator(exception, mapper));
	}

	/**
     * Adds all the given ExitCodeGenerator objects to the list of generators.
     * 
     * @param generators the ExitCodeGenerator objects to be added
     * @throws IllegalArgumentException if the generators parameter is null
     */
    void addAll(ExitCodeGenerator... generators) {
		Assert.notNull(generators, "Generators must not be null");
		addAll(Arrays.asList(generators));
	}

	/**
     * Adds all the exit code generators from the given iterable to the list of generators.
     * 
     * @param generators the iterable containing the exit code generators to be added (must not be null)
     * @throws IllegalArgumentException if the generators parameter is null
     */
    void addAll(Iterable<? extends ExitCodeGenerator> generators) {
		Assert.notNull(generators, "Generators must not be null");
		for (ExitCodeGenerator generator : generators) {
			add(generator);
		}
	}

	/**
     * Adds an ExitCodeGenerator to the list of generators.
     * 
     * @param generator the ExitCodeGenerator to be added (must not be null)
     * @throws IllegalArgumentException if the generator is null
     */
    void add(ExitCodeGenerator generator) {
		Assert.notNull(generator, "Generator must not be null");
		this.generators.add(generator);
		AnnotationAwareOrderComparator.sort(this.generators);
	}

	/**
     * Returns an iterator over the elements in this ExitCodeGenerators collection.
     *
     * @return an iterator over the elements in this collection
     */
    @Override
	public Iterator<ExitCodeGenerator> iterator() {
		return this.generators.iterator();
	}

	/**
	 * Get the final exit code that should be returned. The final exit code is the first
	 * non-zero exit code that is {@link ExitCodeGenerator#getExitCode generated}.
	 * @return the final exit code.
	 */
	int getExitCode() {
		int exitCode = 0;
		for (ExitCodeGenerator generator : this.generators) {
			try {
				int value = generator.getExitCode();
				if (value != 0) {
					exitCode = value;
					break;
				}
			}
			catch (Exception ex) {
				exitCode = 1;
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

		/**
         * Constructs a new MappedExitCodeGenerator with the specified exception and mapper.
         * 
         * @param exception the Throwable exception to be mapped
         * @param mapper the ExitCodeExceptionMapper used to map the exception to an exit code
         */
        MappedExitCodeGenerator(Throwable exception, ExitCodeExceptionMapper mapper) {
			this.exception = exception;
			this.mapper = mapper;
		}

		/**
         * Returns the exit code based on the exception thrown.
         * 
         * @return the exit code
         */
        @Override
		public int getExitCode() {
			return this.mapper.getExitCode(this.exception);
		}

	}

}
