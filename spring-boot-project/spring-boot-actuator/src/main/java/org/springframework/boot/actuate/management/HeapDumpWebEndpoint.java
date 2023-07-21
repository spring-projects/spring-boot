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

package org.springframework.boot.actuate.management;

import java.io.Closeable;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.PlatformManagedObject;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.web.WebEndpointResponse;
import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Web {@link Endpoint @Endpoint} to expose heap dumps.
 *
 * @author Lari Hotari
 * @author Phillip Webb
 * @author Raja Kolli
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@WebEndpoint(id = "heapdump")
public class HeapDumpWebEndpoint {

	private final long timeout;

	private final Lock lock = new ReentrantLock();

	private HeapDumper heapDumper;

	public HeapDumpWebEndpoint() {
		this(TimeUnit.SECONDS.toMillis(10));
	}

	protected HeapDumpWebEndpoint(long timeout) {
		this.timeout = timeout;
	}

	@ReadOperation
	public WebEndpointResponse<Resource> heapDump(@Nullable Boolean live) {
		try {
			if (this.lock.tryLock(this.timeout, TimeUnit.MILLISECONDS)) {
				try {
					return new WebEndpointResponse<>(dumpHeap(live));
				}
				finally {
					this.lock.unlock();
				}
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		catch (IOException ex) {
			return new WebEndpointResponse<>(WebEndpointResponse.STATUS_INTERNAL_SERVER_ERROR);
		}
		catch (HeapDumperUnavailableException ex) {
			return new WebEndpointResponse<>(WebEndpointResponse.STATUS_SERVICE_UNAVAILABLE);
		}
		return new WebEndpointResponse<>(WebEndpointResponse.STATUS_TOO_MANY_REQUESTS);
	}

	private Resource dumpHeap(Boolean live) throws IOException, InterruptedException {
		if (this.heapDumper == null) {
			this.heapDumper = createHeapDumper();
		}
		File file = this.heapDumper.dumpHeap(live);
		return new TemporaryFileSystemResource(file);
	}

	/**
	 * Factory method used to create the {@link HeapDumper}.
	 * @return the heap dumper to use
	 * @throws HeapDumperUnavailableException if the heap dumper cannot be created
	 */
	protected HeapDumper createHeapDumper() throws HeapDumperUnavailableException {
		try {
			return new HotSpotDiagnosticMXBeanHeapDumper();
		}
		catch (HeapDumperUnavailableException ex) {
			return new OpenJ9DiagnosticsMXBeanHeapDumper();
		}
	}

	/**
	 * Strategy interface used to dump the heap to a file.
	 */
	@FunctionalInterface
	protected interface HeapDumper {

		/**
		 * Dump the current heap to a file.
		 * @param live if only <em>live</em> objects (i.e. objects that are reachable from
		 * others) should be dumped. May be {@code null} to use a JVM-specific default.
		 * @return the file containing the heap dump
		 * @throws IOException on IO error
		 * @throws InterruptedException on thread interruption
		 * @throws IllegalArgumentException if live is non-null and is not supported by
		 * the JVM
		 * @since 3.0.0
		 */
		File dumpHeap(Boolean live) throws IOException, InterruptedException;

	}

	/**
	 * {@link HeapDumper} that uses {@code com.sun.management.HotSpotDiagnosticMXBean},
	 * available on Oracle and OpenJDK, to dump the heap to a file.
	 */
	protected static class HotSpotDiagnosticMXBeanHeapDumper implements HeapDumper {

		private final Object diagnosticMXBean;

		private final Method dumpHeapMethod;

		@SuppressWarnings("unchecked")
		protected HotSpotDiagnosticMXBeanHeapDumper() {
			try {
				Class<?> diagnosticMXBeanClass = ClassUtils
					.resolveClassName("com.sun.management.HotSpotDiagnosticMXBean", null);
				this.diagnosticMXBean = ManagementFactory
					.getPlatformMXBean((Class<PlatformManagedObject>) diagnosticMXBeanClass);
				this.dumpHeapMethod = ReflectionUtils.findMethod(diagnosticMXBeanClass, "dumpHeap", String.class,
						Boolean.TYPE);
			}
			catch (Throwable ex) {
				throw new HeapDumperUnavailableException("Unable to locate HotSpotDiagnosticMXBean", ex);
			}
		}

		@Override
		public File dumpHeap(Boolean live) throws IOException {
			File file = createTempFile();
			ReflectionUtils.invokeMethod(this.dumpHeapMethod, this.diagnosticMXBean, file.getAbsolutePath(),
					(live != null) ? live : true);
			return file;
		}

		private File createTempFile() throws IOException {
			String date = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm").format(LocalDateTime.now());
			File file = File.createTempFile("heap-" + date, ".hprof");
			file.delete();
			return file;
		}

	}

	/**
	 * {@link HeapDumper} that uses
	 * {@code openj9.lang.management.OpenJ9DiagnosticsMXBean}, available on OpenJ9, to
	 * dump the heap to a file.
	 */
	private static final class OpenJ9DiagnosticsMXBeanHeapDumper implements HeapDumper {

		private final Object diagnosticMXBean;

		private final Method dumpHeapMethod;

		@SuppressWarnings("unchecked")
		private OpenJ9DiagnosticsMXBeanHeapDumper() {
			try {
				Class<?> mxBeanClass = ClassUtils.resolveClassName("openj9.lang.management.OpenJ9DiagnosticsMXBean",
						null);
				this.diagnosticMXBean = ManagementFactory.getPlatformMXBean((Class<PlatformManagedObject>) mxBeanClass);
				this.dumpHeapMethod = ReflectionUtils.findMethod(mxBeanClass, "triggerDumpToFile", String.class,
						String.class);
			}
			catch (Throwable ex) {
				throw new HeapDumperUnavailableException("Unable to locate OpenJ9DiagnosticsMXBean", ex);
			}
		}

		@Override
		public File dumpHeap(Boolean live) throws IOException, InterruptedException {
			Assert.isNull(live, "OpenJ9DiagnosticsMXBean does not support live parameter when dumping the heap");
			return new File(
					(String) ReflectionUtils.invokeMethod(this.dumpHeapMethod, this.diagnosticMXBean, "heap", null));
		}

	}

	/**
	 * Exception to be thrown if the {@link HeapDumper} cannot be created.
	 */
	protected static class HeapDumperUnavailableException extends RuntimeException {

		public HeapDumperUnavailableException(String message, Throwable cause) {
			super(message, cause);
		}

	}

	private static final class TemporaryFileSystemResource extends FileSystemResource {

		private final Log logger = LogFactory.getLog(getClass());

		private TemporaryFileSystemResource(File file) {
			super(file);
		}

		@Override
		public ReadableByteChannel readableChannel() throws IOException {
			ReadableByteChannel readableChannel = super.readableChannel();
			return new ReadableByteChannel() {

				@Override
				public boolean isOpen() {
					return readableChannel.isOpen();
				}

				@Override
				public void close() throws IOException {
					closeThenDeleteFile(readableChannel);
				}

				@Override
				public int read(ByteBuffer dst) throws IOException {
					return readableChannel.read(dst);
				}

			};
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new FilterInputStream(super.getInputStream()) {

				@Override
				public void close() throws IOException {
					closeThenDeleteFile(this.in);
				}

			};
		}

		private void closeThenDeleteFile(Closeable closeable) throws IOException {
			try {
				closeable.close();
			}
			finally {
				deleteFile();
			}
		}

		private void deleteFile() {
			try {
				Files.delete(getFile().toPath());
			}
			catch (IOException ex) {
				TemporaryFileSystemResource.this.logger
					.warn("Failed to delete temporary heap dump file '" + getFile() + "'", ex);
			}
		}

		@Override
		public boolean isFile() {
			// Prevent zero-copy so we can delete the file on close
			return false;
		}

	}

}
