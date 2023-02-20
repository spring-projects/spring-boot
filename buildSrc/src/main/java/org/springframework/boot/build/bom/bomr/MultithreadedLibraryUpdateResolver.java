/*
 * Copyright 2012-2023 the original author or authors.
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.UpgradePolicy;

/**
 * Uses multiple threads to find library updates.
 *
 * @author Moritz Halbritter
 */
class MultithreadedLibraryUpdateResolver extends StandardLibraryUpdateResolver {

	private static final Logger LOGGER = LoggerFactory.getLogger(MultithreadedLibraryUpdateResolver.class);

	private final int threads;

	MultithreadedLibraryUpdateResolver(VersionResolver versionResolver, UpgradePolicy upgradePolicy, int threads) {
		super(versionResolver, upgradePolicy);
		this.threads = threads;
	}

	@Override
	public List<LibraryWithVersionOptions> findLibraryUpdates(Collection<Library> librariesToUpgrade,
			Map<String, Library> librariesByName) {
		LOGGER.info("Looking for updates using {} threads", this.threads);
		ExecutorService executorService = Executors.newFixedThreadPool(this.threads);
		try {
			List<Future<LibraryWithVersionOptions>> jobs = new ArrayList<>();
			for (Library library : librariesToUpgrade) {
				if (isLibraryExcluded(library)) {
					continue;
				}
				jobs.add(executorService.submit(() -> {
					LOGGER.info("Looking for updates for {}", library.getName());
					long start = System.nanoTime();
					List<VersionOption> versionOptions = getVersionOptions(library, librariesByName);
					LOGGER.info("Found {} updates for {}, took {}", versionOptions.size(), library.getName(),
							Duration.ofNanos(System.nanoTime() - start));
					return new LibraryWithVersionOptions(library, versionOptions);
				}));
			}
			List<LibraryWithVersionOptions> result = new ArrayList<>();
			for (Future<LibraryWithVersionOptions> job : jobs) {
				try {
					result.add(job.get());
				}
				catch (InterruptedException | ExecutionException ex) {
					throw new RuntimeException(ex);
				}
			}
			return result;
		}
		finally {
			executorService.shutdownNow();
		}
	}

}
