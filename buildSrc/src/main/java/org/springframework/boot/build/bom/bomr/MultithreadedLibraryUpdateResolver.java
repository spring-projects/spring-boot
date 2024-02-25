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

package org.springframework.boot.build.bom.bomr;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.build.bom.Library;

/**
 * {@link LibraryUpdateResolver} decorator that uses multiple threads to find library
 * updates.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 */
class MultithreadedLibraryUpdateResolver implements LibraryUpdateResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(MultithreadedLibraryUpdateResolver.class);

	private final int threads;

	private final LibraryUpdateResolver delegate;

	/**
	 * Initializes a new instance of the MultithreadedLibraryUpdateResolver class with the
	 * specified number of threads and delegate.
	 * @param threads The number of threads to be used for resolving library updates.
	 * @param delegate The delegate responsible for resolving library updates.
	 */
	MultithreadedLibraryUpdateResolver(int threads, LibraryUpdateResolver delegate) {
		this.threads = threads;
		this.delegate = delegate;
	}

	/**
	 * Finds library updates for a collection of libraries to upgrade.
	 * @param librariesToUpgrade the collection of libraries to upgrade
	 * @param librariesByName the map of libraries by name
	 * @return a list of LibraryWithVersionOptions objects representing the library
	 * updates
	 */
	@Override
	public List<LibraryWithVersionOptions> findLibraryUpdates(Collection<Library> librariesToUpgrade,
			Map<String, Library> librariesByName) {
		LOGGER.info("Looking for updates using {} threads", this.threads);
		ExecutorService executorService = Executors.newFixedThreadPool(this.threads);
		try {
			return librariesToUpgrade.stream().map((library) -> {
				if (library.getVersionAlignment() == null) {
					return executorService.submit(() -> this.delegate
						.findLibraryUpdates(Collections.singletonList(library), librariesByName));
				}
				else {
					return CompletableFuture.completedFuture(
							this.delegate.findLibraryUpdates(Collections.singletonList(library), librariesByName));
				}
			}).flatMap(this::getResult).toList();
		}
		finally {
			executorService.shutdownNow();
		}
	}

	/**
	 * Retrieves the result of a job that returns a list of LibraryWithVersionOptions
	 * objects.
	 * @param job the Future representing the job that returns the list of
	 * LibraryWithVersionOptions objects
	 * @return a Stream of LibraryWithVersionOptions objects
	 * @throws RuntimeException if the job is interrupted or encounters an execution
	 * exception
	 */
	private Stream<LibraryWithVersionOptions> getResult(Future<List<LibraryWithVersionOptions>> job) {
		try {
			return job.get().stream();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(ex);
		}
		catch (ExecutionException ex) {
			throw new RuntimeException(ex);
		}
	}

}
