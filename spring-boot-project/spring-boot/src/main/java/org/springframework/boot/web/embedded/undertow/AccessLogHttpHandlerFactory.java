/*
 * Copyright 2012-2020 the original author or authors.
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
 * A {@link HttpHandlerFactory} for an {@link AccessLogHandler}.
 *
 * @author Andy Wilkinson
 */
class AccessLogHttpHandlerFactory implements HttpHandlerFactory {

	private final File directory;

	private final String pattern;

	private final String prefix;

	private final String suffix;

	private final boolean rotate;

	AccessLogHttpHandlerFactory(File directory, String pattern, String prefix, String suffix, boolean rotate) {
		this.directory = directory;
		this.pattern = pattern;
		this.prefix = prefix;
		this.suffix = suffix;
		this.rotate = rotate;
	}

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

	private void createAccessLogDirectoryIfNecessary() {
		Assert.state(this.directory != null, "Access log directory is not set");
		if (!this.directory.isDirectory() && !this.directory.mkdirs()) {
			throw new IllegalStateException("Failed to create access log directory '" + this.directory + "'");
		}
	}

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

		ClosableAccessLogHandler(HttpHandler next, XnioWorker worker, DefaultAccessLogReceiver accessLogReceiver,
				String formatString) {
			super(next, accessLogReceiver, formatString, Undertow.class.getClassLoader());
			this.worker = worker;
			this.accessLogReceiver = accessLogReceiver;
		}

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
