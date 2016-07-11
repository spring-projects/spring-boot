/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.PlatformManagedObject;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.UsesJava7;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * {@link MvcEndpoint} to expose heap dumps.
 *
 * @author Lari Hotari
 * @author Phillip Webb
 * @since 1.4.0
 */
@ConfigurationProperties("endpoints.heapdump")
@HypermediaDisabled
public class HeapdumpMvcEndpoint extends AbstractMvcEndpoint implements MvcEndpoint {

	private final long timeout;

	private final Lock lock = new ReentrantLock();

	private HeapDumper heapDumper;

	public HeapdumpMvcEndpoint() {
		this(TimeUnit.SECONDS.toMillis(10));
	}

	protected HeapdumpMvcEndpoint(long timeout) {
		super("/heapdump", true);
		this.timeout = timeout;
	}

	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public void invoke(@RequestParam(defaultValue = "true") boolean live,
			HttpServletRequest request, HttpServletResponse response)
					throws IOException, ServletException {
		if (!isEnabled()) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return;
		}
		try {
			if (this.lock.tryLock(this.timeout, TimeUnit.MILLISECONDS)) {
				try {
					dumpHeap(live, request, response);
					return;
				}
				finally {
					this.lock.unlock();
				}
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
	}

	private void dumpHeap(boolean live, HttpServletRequest request,
			HttpServletResponse response)
					throws IOException, ServletException, InterruptedException {
		if (this.heapDumper == null) {
			this.heapDumper = createHeapDumper();
		}
		File file = createTempFile(live);
		try {
			this.heapDumper.dumpHeap(file, live);
			handle(file, request, response);
		}
		finally {
			file.delete();
		}
	}

	private File createTempFile(boolean live) throws IOException {
		String date = new SimpleDateFormat("yyyy-MM-dd-HH-mm").format(new Date());
		File file = File.createTempFile("heapdump" + date + (live ? "-live" : ""),
				".hprof");
		file.delete();
		return file;
	}

	/**
	 * Factory method used to create the {@link HeapDumper}.
	 * @return the heap dumper to use
	 * @throws HeapDumperUnavailableException if the heap dumper cannot be created
	 */
	protected HeapDumper createHeapDumper() throws HeapDumperUnavailableException {
		return new HotSpotDiagnosticMXBeanHeapDumper();
	}

	/**
	 * Handle the heap dump file and respond. By default this method will return the
	 * response as a GZip stream.
	 * @param heapDumpFile the generated dump file
	 * @param request the HTTP request
	 * @param response the HTTP response
	 * @throws ServletException on servlet error
	 * @throws IOException on IO error
	 */
	protected void handle(File heapDumpFile, HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("application/octet-stream");
		response.setHeader("Content-Disposition",
				"attachment; filename=\"" + (heapDumpFile.getName() + ".gz") + "\"");
		try {
			InputStream in = new FileInputStream(heapDumpFile);
			try {
				GZIPOutputStream out = new GZIPOutputStream(response.getOutputStream());
				StreamUtils.copy(in, out);
				out.finish();
			}
			catch (NullPointerException ex) {
			}
			finally {
				try {
					in.close();
				}
				catch (Throwable ex) {
				}
			}
		}
		catch (FileNotFoundException ex) {
		}
	}

	/**
	 * Strategy interface used to dump the heap to a file.
	 */
	protected interface HeapDumper {

		/**
		 * Dump the current heap to the specified file.
		 * @param file the file to dump the heap to
		 * @param live if only <em>live</em> objects (i.e. objects that are reachable from
		 * others) should be dumped
		 * @throws IOException on IO error
		 * @throws InterruptedException on thread interruption
		 */
		void dumpHeap(File file, boolean live) throws IOException, InterruptedException;

	}

	/**
	 * {@link HeapDumper} that uses {@code com.sun.management.HotSpotDiagnosticMXBean}
	 * available on Oracle and OpenJDK to dump the heap to a file.
	 */
	@UsesJava7
	protected static class HotSpotDiagnosticMXBeanHeapDumper implements HeapDumper {

		private Object diagnosticMXBean;

		private Method dumpHeapMethod;

		@SuppressWarnings("unchecked")
		protected HotSpotDiagnosticMXBeanHeapDumper() {
			try {
				Class<?> diagnosticMXBeanClass = ClassUtils.resolveClassName(
						"com.sun.management.HotSpotDiagnosticMXBean", null);
				this.diagnosticMXBean = ManagementFactory.getPlatformMXBean(
						(Class<PlatformManagedObject>) diagnosticMXBeanClass);
				this.dumpHeapMethod = ReflectionUtils.findMethod(diagnosticMXBeanClass,
						"dumpHeap", String.class, Boolean.TYPE);
			}
			catch (Throwable ex) {
				throw new HeapDumperUnavailableException(
						"Unable to locate HotSpotDiagnosticMXBean", ex);
			}
		}

		@Override
		public void dumpHeap(File file, boolean live) {
			ReflectionUtils.invokeMethod(this.dumpHeapMethod, this.diagnosticMXBean,
					file.getAbsolutePath(), live);
		}

	}

	/**
	 * Exception to be thrown if the {@link HeapDumper} cannot be created.
	 */
	@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
	protected static class HeapDumperUnavailableException extends RuntimeException {

		public HeapDumperUnavailableException(String message, Throwable cause) {
			super(message, cause);
		}

	}

}
