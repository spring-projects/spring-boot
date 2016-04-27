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
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * {@link MvcEndpoint} to expose heap dumps.
 *
 * @author Lari Hotari
 */
@ConfigurationProperties("endpoints.heapdump")
@HypermediaDisabled
public class HeapdumpMvcEndpoint extends AbstractMvcEndpoint implements MvcEndpoint {
	private static final Log logger = LogFactory.getLog(HeapdumpMvcEndpoint.class);
	private final Heapdumper heapdumper = new Heapdumper();
	String fileNameBase = "heapdump";
	private final ResponseEntity<Map<String, String>> HEAPDUMPER_NOT_AVAILABLE_RESPONSE = new ResponseEntity<Map<String, String>>(
			Collections.singletonMap("message",
					"Heapdumping is not supported in this environment."),
			HttpStatus.NOT_FOUND);
	private static final ResponseEntity<Map<String, String>> HEAPDUMP_ALREADY_IN_PROGRESS = new ResponseEntity<Map<String, String>>(
			Collections.singletonMap("message",
					"Only a single heapdump can be requested at a time."),
			HttpStatus.TOO_MANY_REQUESTS);
	private final Lock heapDumpLock = new ReentrantLock();

	public HeapdumpMvcEndpoint() {
		setPath("/heapdump");
	}

	@RequestMapping(path = "", method = RequestMethod.GET)
	public ResponseEntity<Map<String, String>> dump(HttpServletResponse response)
			throws IOException {
		return dumpHeap(response, false);
	}

	@RequestMapping(path = "/live", method = RequestMethod.GET)
	public ResponseEntity<Map<String, String>> dumpLive(HttpServletResponse response)
			throws IOException {
		return dumpHeap(response, true);
	}

	private ResponseEntity<Map<String, String>> dumpHeap(HttpServletResponse response,
			boolean live) throws IOException {
		if (!isEnabled()) {
			return MvcEndpoint.DISABLED_RESPONSE;
		}
		if (!heapdumper.isAvailable()) {
			return HEAPDUMPER_NOT_AVAILABLE_RESPONSE;
		}
		if (heapDumpLock.tryLock()) {
			try {
				doDumpHeap(response, live);
			}
			finally {
				heapDumpLock.unlock();
			}
		}
		else {
			return HEAPDUMP_ALREADY_IN_PROGRESS;
		}
		return null;
	}

	private void doDumpHeap(HttpServletResponse response, boolean live)
			throws IOException {
		final File dumpFile = File.createTempFile(createFileName(live), ".hprof");
		// file must not exist before creating heap dump
		dumpFile.delete();
		try {
			heapdumper.dumpHeap(dumpFile, live);
			streamFileAndGzipToResponse(response, dumpFile);
		}
		finally {
			dumpFile.delete();
		}
	}

	private void streamFileAndGzipToResponse(HttpServletResponse response, File dumpFile)
			throws IOException {
		response.setContentType("application/octet-stream");
		String fileName = dumpFile.getName() + ".gz";
		response.setHeader("Content-Disposition",
				"attachment; filename=\"" + fileName + "\"");

		OutputStream outputStream = response.getOutputStream();
		FileInputStream input = null;
		GZIPOutputStream output = null;
		try {
			input = new FileInputStream(dumpFile);
			output = new GZIPOutputStream(outputStream);
			StreamUtils.copy(input, output);
		}
		finally {
			try {
				if (input != null) {
					input.close();
				}
			}
			catch (IOException ioe) {
				// ignore
			}
			try {
				if (output != null) {
					output.close();
				}
			}
			catch (IOException ioe) {
				// ignore
			}
		}
	}

	private String createFileName(boolean live) {
		return fileNameBase + new SimpleDateFormat("yyyy-MM-dd-HH-mm").format(new Date())
				+ "-" + (live ? "live-" : "");
	}

	public void setFileNameBase(String fileNameBase) {
		this.fileNameBase = fileNameBase;
	}

	/**
	 * Uses com.sun.management.HotSpotDiagnosticMXBean available on Oracle and OpenJDK to
	 * dump the heap to a file.
	 */
	static class Heapdumper {
		Object hotSpotDiagnosticMXBean;

		Method dumpHeapMethod;

		Heapdumper() {
			try {
				Class<?> hotSpotDiagnosticMXBeanClass = getClass().getClassLoader()
						.loadClass("com.sun.management.HotSpotDiagnosticMXBean");
				Method getPlatformMXBeanMethod = ReflectionUtils.findMethod(
						ManagementFactory.class, "getPlatformMXBean", Class.class);
				hotSpotDiagnosticMXBean = ReflectionUtils.invokeMethod(
						getPlatformMXBeanMethod, null, hotSpotDiagnosticMXBeanClass);
				dumpHeapMethod = ReflectionUtils.findMethod(hotSpotDiagnosticMXBeanClass,
						"dumpHeap", String.class, boolean.class);
			}
			catch (Exception e) {
				logger.warn(
						"Unable to locate com.sun.management.HotSpotDiagnosticMXBean.",
						e);
				hotSpotDiagnosticMXBean = null;
			}
		}

		boolean isAvailable() {
			return hotSpotDiagnosticMXBean != null;
		}

		void dumpHeap(File file, boolean live) {
			ReflectionUtils.invokeMethod(dumpHeapMethod, hotSpotDiagnosticMXBean,
					file.getAbsolutePath(), live);
		}

	}
}
