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

package org.springframework.boot.web.embedded.undertow;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.accesslog.DefaultAccessLogReceiver;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import org.springframework.util.Assert;

/**
 * An {@link HttpHandlerFactory} for an {@link AccessLogHandler}.
 *
 * @author Andy Wilkinson
 */
class AccessLogHttpHandlerFactory implements HttpHandlerFactory {

	private final File directory;

	private final String pattern;

	private final String prefix;

	private final String suffix;

	private final boolean rotate;

	/**
     * Constructs a new AccessLogHttpHandlerFactory with the specified parameters.
     *
     * @param directory the directory where the access log files will be stored
     * @param pattern the pattern used to format the access log entries
     * @param prefix the prefix to be added to the access log file names
     * @param suffix the suffix to be added to the access log file names
     * @param rotate true if the access log files should be rotated, false otherwise
     */
    AccessLogHttpHandlerFactory(File directory, String pattern, String prefix, String suffix, boolean rotate) {
		this.directory = directory;
		this.pattern = pattern;
		this.prefix = prefix;
		this.suffix = suffix;
		this.rotate = rotate;
	}

	/**
     * Returns an HttpHandler that logs access information to a file.
     * 
     * @param next the next HttpHandler in the chain
     * @return the HttpHandler that logs access information
     * @throws IllegalStateException if failed to create the AccessLogHandler
     */
    @Override
	public HttpHandler getHandler(HttpHandler next) {
		try {
			createAccessLogDirectoryIfNecessary();
			XnioWorker worker = createWorker();
			String baseName = (this.prefix != null) ? this.prefix : "access_log.";
			String formatString = (this.pattern != null) ? this.pattern : "common";
			return new ClosableAccessLogHandler(next, worker,
					new DefaultAccessLogReceiver(worker, this.directory, baseName, this.suffix, this.rotate),
					formatString);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to create AccessLogHandler", ex);
		}
	}

	/**
     * Creates the access log directory if it does not exist.
     * 
     * @throws IllegalStateException if the access log directory is not set or if it fails to create the directory
     */
    private void createAccessLogDirectoryIfNecessary() {
		Assert.state(this.directory != null, "Access log directory is not set");
		if (!this.directory.isDirectory() && !this.directory.mkdirs()) {
			throw new IllegalStateException("Failed to create access log directory '" + this.directory + "'");
		}
	}

	/**
     * Creates a new XnioWorker instance.
     *
     * @return the created XnioWorker instance
     * @throws IOException if an I/O error occurs while creating the XnioWorker
     */
    private XnioWorker createWorker() throws IOException {
		Xnio xnio = Xnio.getInstance(Undertow.class.getClassLoader());
		return xnio.createWorker(OptionMap.builder().set(Options.THREAD_DAEMON, true).getMap());
	}

	/**
	 * {@link Closeable} variant of {@link AccessLogHandler}.
	 */
	private static class ClosableAccessLogHandler extends AccessLogHandler implements Closeable {

		private final DefaultAccessLogReceiver accessLogReceiver;

		private final XnioWorker worker;

		/**
         * Constructs a new ClosableAccessLogHandler with the specified parameters.
         *
         * @param next the next HttpHandler in the chain
         * @param worker the XnioWorker used for logging
         * @param accessLogReceiver the DefaultAccessLogReceiver used for logging
         * @param formatString the format string for the log entries
         */
        ClosableAccessLogHandler(HttpHandler next, XnioWorker worker, DefaultAccessLogReceiver accessLogReceiver,
				String formatString) {
			super(next, accessLogReceiver, formatString, Undertow.class.getClassLoader());
			this.worker = worker;
			this.accessLogReceiver = accessLogReceiver;
		}

		/**
         * Closes the ClosableAccessLogHandler by closing the accessLogReceiver and shutting down the worker thread.
         * 
         * @throws IOException if an I/O error occurs while closing the accessLogReceiver
         * @throws RuntimeException if an exception occurs while closing the accessLogReceiver
         */
        @Override
		public void close() throws IOException {
			try {
				this.accessLogReceiver.close();
				this.worker.shutdown();
				this.worker.awaitTermination(30, TimeUnit.SECONDS);
			}
			catch (IOException ex) {
				throw new RuntimeException(ex);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}

	}

}
