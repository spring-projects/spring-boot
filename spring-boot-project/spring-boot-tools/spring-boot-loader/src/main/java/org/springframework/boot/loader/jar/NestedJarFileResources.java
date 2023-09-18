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

package org.springframework.boot.loader.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.zip.Inflater;

import org.springframework.boot.loader.ref.Cleaner;
import org.springframework.boot.loader.zip.ZipContent;

/**
 * Resources created managed and cleaned by a {@link NestedJarFile} instance and suitable
 * for registration with a {@link Cleaner}.
 *
 * @author Phillip Webb
 */
class NestedJarFileResources implements Runnable {

	private static final int INFLATER_CACHE_LIMIT = 20;

	private ZipContent zipContent;

	private final Set<InputStream> inputStreams = Collections.newSetFromMap(new WeakHashMap<>());

	private Deque<Inflater> inflaterCache = new ArrayDeque<>();

	/**
	 * Create a new {@link NestedJarFileResources} instance.
	 * @param file the source zip file
	 * @param nestedEntryName the nested entry or {@code null}
	 * @throws IOException on I/O error
	 */
	NestedJarFileResources(File file, String nestedEntryName) throws IOException {
		this.zipContent = ZipContent.open(file.toPath(), nestedEntryName);
	}

	/**
	 * Return the underling {@link ZipContent}.
	 * @return the zip content
	 */
	ZipContent zipContent() {
		return this.zipContent;
	}

	/**
	 * Add a managed input stream resource.
	 * @param inputStream the input stream
	 */
	void addInputStream(InputStream inputStream) {
		synchronized (this.inputStreams) {
			this.inputStreams.add(inputStream);
		}
	}

	/**
	 * Remove a managed input stream resource.
	 * @param inputStream the input stream
	 */
	void removeInputStream(InputStream inputStream) {
		synchronized (this.inputStreams) {
			this.inputStreams.remove(inputStream);
		}
	}

	/**
	 * Create a {@link Runnable} action to cleanup the given inflater.
	 * @param inflater the inflater to cleanup
	 * @return the cleanup action
	 */
	Runnable createInflatorCleanupAction(Inflater inflater) {
		return () -> endOrCacheInflater(inflater);
	}

	/**
	 * Get previously used {@link Inflater} from the cache, or create a new one.
	 * @return a usable {@link Inflater}
	 */
	Inflater getOrCreateInflater() {
		Deque<Inflater> inflaterCache = this.inflaterCache;
		if (inflaterCache != null) {
			synchronized (inflaterCache) {
				Inflater inflater = this.inflaterCache.poll();
				if (inflater != null) {
					return inflater;
				}
			}
		}
		return new Inflater(true);
	}

	/**
	 * Either release the given {@link Inflater} by calling {@link Inflater#end()} or add
	 * it to the cache for later reuse.
	 * @param inflater the inflater to end or cache
	 */
	private void endOrCacheInflater(Inflater inflater) {
		Deque<Inflater> inflaterCache = this.inflaterCache;
		if (inflaterCache != null) {
			synchronized (inflaterCache) {
				if (this.inflaterCache == inflaterCache && inflaterCache.size() < INFLATER_CACHE_LIMIT) {
					inflater.reset();
					this.inflaterCache.add(inflater);
					return;
				}
			}
		}
		inflater.end();
	}

	/**
	 * Called by the {@link Cleaner} to free resources.
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		releaseAll();
	}

	private void releaseAll() {
		IOException exceptionChain = null;
		exceptionChain = releaseInflators(exceptionChain);
		exceptionChain = releaseInputStreams(exceptionChain);
		exceptionChain = releaseZipContent(exceptionChain);
		if (exceptionChain != null) {
			throw new UncheckedIOException(exceptionChain);
		}
	}

	private IOException releaseInflators(IOException exceptionChain) {
		Deque<Inflater> inflaterCache = this.inflaterCache;
		if (inflaterCache != null) {
			try {
				synchronized (inflaterCache) {
					inflaterCache.forEach(Inflater::end);
				}
			}
			finally {
				this.inflaterCache = null;
			}
		}
		return exceptionChain;
	}

	private IOException releaseInputStreams(IOException exceptionChain) {
		synchronized (this.inputStreams) {
			for (InputStream inputStream : List.copyOf(this.inputStreams)) {
				try {
					inputStream.close();
				}
				catch (IOException ex) {
					exceptionChain = addToExceptionChain(exceptionChain, ex);
				}
			}
			this.inputStreams.clear();
		}
		return exceptionChain;
	}

	private IOException releaseZipContent(IOException exceptionChain) {
		ZipContent zipContent = this.zipContent;
		if (zipContent != null) {
			try {
				zipContent.close();
			}
			catch (IOException ex) {
				exceptionChain = addToExceptionChain(exceptionChain, ex);
			}
			finally {
				this.zipContent = null;
			}
		}
		return exceptionChain;
	}

	private IOException addToExceptionChain(IOException exceptionChain, IOException ex) {
		if (exceptionChain != null) {
			exceptionChain.addSuppressed(ex);
			return exceptionChain;
		}
		return ex;
	}

}
