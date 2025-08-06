/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.undertow.autoconfigure;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import io.undertow.UndertowOptions;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Undertow server properties.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Ivan Sopov
 * @author Marcos Barbero
 * @author Eddú Meléndez
 * @author Quinten De Swaef
 * @author Venil Noronha
 * @author Aurélien Leboulanger
 * @author Brian Clozel
 * @author Olivier Lamy
 * @author Chentao Qu
 * @author Artsiom Yudovin
 * @author Andrew McGhie
 * @author Rafiullah Hamedy
 * @author Dirk Deyne
 * @author HaiTao Zhang
 * @author Victor Mandujano
 * @author Chris Bono
 * @author Parviz Rozikov
 * @author Florian Storz
 * @author Michael Weidmann
 * @author Lasse Wulff
 * @since 4.0.0
 */
@ConfigurationProperties("server.undertow")
public class UndertowServerProperties {

	/**
	 * Maximum size of the HTTP post content. When the value is -1, the default, the size
	 * is unlimited.
	 */
	private DataSize maxHttpPostSize = DataSize.ofBytes(-1);

	/**
	 * Size of each buffer. The default is derived from the maximum amount of memory that
	 * is available to the JVM.
	 */
	private @Nullable DataSize bufferSize;

	/**
	 * Whether to allocate buffers outside the Java heap. The default is derived from the
	 * maximum amount of memory that is available to the JVM.
	 */
	private @Nullable Boolean directBuffers;

	/**
	 * Whether servlet filters should be initialized on startup.
	 */
	private boolean eagerFilterInit = true;

	/**
	 * Maximum number of query or path parameters that are allowed. This limit exists to
	 * prevent hash collision based DOS attacks.
	 */
	private int maxParameters = UndertowOptions.DEFAULT_MAX_PARAMETERS;

	/**
	 * Maximum number of headers that are allowed. This limit exists to prevent hash
	 * collision based DOS attacks.
	 */
	private int maxHeaders = UndertowOptions.DEFAULT_MAX_HEADERS;

	/**
	 * Maximum number of cookies that are allowed. This limit exists to prevent hash
	 * collision based DOS attacks.
	 */
	private int maxCookies = 200;

	/**
	 * Whether encoded slash characters (%2F) should be decoded. Decoding can cause
	 * security problems if a front-end proxy does not perform the same decoding. Only
	 * enable this if you have a legacy application that requires it. When set,
	 * server.undertow.allow-encoded-slash has no effect.
	 */
	private @Nullable Boolean decodeSlash;

	/**
	 * Whether the URL should be decoded. When disabled, percent-encoded characters in the
	 * URL will be left as-is.
	 */
	private boolean decodeUrl = true;

	/**
	 * Charset used to decode URLs.
	 */
	private Charset urlCharset = StandardCharsets.UTF_8;

	/**
	 * Whether the 'Connection: keep-alive' header should be added to all responses, even
	 * if not required by the HTTP specification.
	 */
	private boolean alwaysSetKeepAlive = true;

	/**
	 * Amount of time a connection can sit idle without processing a request, before it is
	 * closed by the server.
	 */
	private @Nullable Duration noRequestTimeout;

	/**
	 * Whether to preserve the path of a request when it is forwarded.
	 */
	private boolean preservePathOnForward = false;

	private final Accesslog accesslog = new Accesslog();

	/**
	 * Thread related configuration.
	 */
	private final Threads threads = new Threads();

	private final Options options = new Options();

	public DataSize getMaxHttpPostSize() {
		return this.maxHttpPostSize;
	}

	public void setMaxHttpPostSize(DataSize maxHttpPostSize) {
		this.maxHttpPostSize = maxHttpPostSize;
	}

	public @Nullable DataSize getBufferSize() {
		return this.bufferSize;
	}

	public void setBufferSize(@Nullable DataSize bufferSize) {
		this.bufferSize = bufferSize;
	}

	public @Nullable Boolean getDirectBuffers() {
		return this.directBuffers;
	}

	public void setDirectBuffers(@Nullable Boolean directBuffers) {
		this.directBuffers = directBuffers;
	}

	public boolean isEagerFilterInit() {
		return this.eagerFilterInit;
	}

	public void setEagerFilterInit(boolean eagerFilterInit) {
		this.eagerFilterInit = eagerFilterInit;
	}

	public int getMaxParameters() {
		return this.maxParameters;
	}

	public void setMaxParameters(Integer maxParameters) {
		this.maxParameters = maxParameters;
	}

	public int getMaxHeaders() {
		return this.maxHeaders;
	}

	public void setMaxHeaders(int maxHeaders) {
		this.maxHeaders = maxHeaders;
	}

	public Integer getMaxCookies() {
		return this.maxCookies;
	}

	public void setMaxCookies(Integer maxCookies) {
		this.maxCookies = maxCookies;
	}

	public @Nullable Boolean getDecodeSlash() {
		return this.decodeSlash;
	}

	public void setDecodeSlash(@Nullable Boolean decodeSlash) {
		this.decodeSlash = decodeSlash;
	}

	public boolean isDecodeUrl() {
		return this.decodeUrl;
	}

	public void setDecodeUrl(Boolean decodeUrl) {
		this.decodeUrl = decodeUrl;
	}

	public Charset getUrlCharset() {
		return this.urlCharset;
	}

	public void setUrlCharset(Charset urlCharset) {
		this.urlCharset = urlCharset;
	}

	public boolean isAlwaysSetKeepAlive() {
		return this.alwaysSetKeepAlive;
	}

	public void setAlwaysSetKeepAlive(boolean alwaysSetKeepAlive) {
		this.alwaysSetKeepAlive = alwaysSetKeepAlive;
	}

	public @Nullable Duration getNoRequestTimeout() {
		return this.noRequestTimeout;
	}

	public void setNoRequestTimeout(@Nullable Duration noRequestTimeout) {
		this.noRequestTimeout = noRequestTimeout;
	}

	public boolean isPreservePathOnForward() {
		return this.preservePathOnForward;
	}

	public void setPreservePathOnForward(boolean preservePathOnForward) {
		this.preservePathOnForward = preservePathOnForward;
	}

	public UndertowServerProperties.Accesslog getAccesslog() {
		return this.accesslog;
	}

	public UndertowServerProperties.Threads getThreads() {
		return this.threads;
	}

	public UndertowServerProperties.Options getOptions() {
		return this.options;
	}

	/**
	 * Undertow access log properties.
	 */
	public static class Accesslog {

		/**
		 * Whether to enable the access log.
		 */
		private boolean enabled = false;

		/**
		 * Format pattern for access logs.
		 */
		private String pattern = "common";

		/**
		 * Log file name prefix.
		 */
		protected String prefix = "access_log.";

		/**
		 * Log file name suffix.
		 */
		private String suffix = "log";

		/**
		 * Undertow access log directory.
		 */
		private File dir = new File("logs");

		/**
		 * Whether to enable access log rotation.
		 */
		private boolean rotate = true;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public String getPattern() {
			return this.pattern;
		}

		public void setPattern(String pattern) {
			this.pattern = pattern;
		}

		public String getPrefix() {
			return this.prefix;
		}

		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		public String getSuffix() {
			return this.suffix;
		}

		public void setSuffix(String suffix) {
			this.suffix = suffix;
		}

		public File getDir() {
			return this.dir;
		}

		public void setDir(File dir) {
			this.dir = dir;
		}

		public boolean isRotate() {
			return this.rotate;
		}

		public void setRotate(boolean rotate) {
			this.rotate = rotate;
		}

	}

	/**
	 * Undertow thread properties.
	 */
	public static class Threads {

		/**
		 * Number of I/O threads to create for the worker. The default is derived from the
		 * number of available processors.
		 */
		private @Nullable Integer io;

		/**
		 * Number of worker threads. The default is 8 times the number of I/O threads.
		 */
		private @Nullable Integer worker;

		public @Nullable Integer getIo() {
			return this.io;
		}

		public void setIo(@Nullable Integer io) {
			this.io = io;
		}

		public @Nullable Integer getWorker() {
			return this.worker;
		}

		public void setWorker(@Nullable Integer worker) {
			this.worker = worker;
		}

	}

	public static class Options {

		/**
		 * Socket options as defined in org.xnio.Options.
		 */
		private final Map<String, String> socket = new LinkedHashMap<>();

		/**
		 * Server options as defined in io.undertow.UndertowOptions.
		 */
		private final Map<String, String> server = new LinkedHashMap<>();

		public Map<String, String> getServer() {
			return this.server;
		}

		public Map<String, String> getSocket() {
			return this.socket;
		}

	}

}
