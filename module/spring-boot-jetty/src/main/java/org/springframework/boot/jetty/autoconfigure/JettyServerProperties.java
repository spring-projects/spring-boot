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

package org.springframework.boot.jetty.autoconfigure;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Jetty server properties.
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
@ConfigurationProperties("server.jetty")
public class JettyServerProperties {

	/**
	 * Maximum size of the form content in any HTTP post request.
	 */
	private DataSize maxHttpFormPostSize = DataSize.ofBytes(200000);

	/**
	 * Maximum number of form keys.
	 */
	private int maxFormKeys = 1000;

	/**
	 * Time that the connection can be idle before it is closed.
	 */
	private Duration connectionIdleTimeout;

	/**
	 * Maximum size of the HTTP response header.
	 */
	private DataSize maxHttpResponseHeaderSize = DataSize.ofKilobytes(8);

	/**
	 * Maximum number of connections that the server accepts and processes at any given
	 * time.
	 */
	private int maxConnections = -1;

	/**
	 * Access log configuration.
	 */
	private final Accesslog accesslog = new Accesslog();

	/**
	 * Thread related configuration.
	 */
	private final Threads threads = new Threads();

	public Accesslog getAccesslog() {
		return this.accesslog;
	}

	public Threads getThreads() {
		return this.threads;
	}

	public DataSize getMaxHttpFormPostSize() {
		return this.maxHttpFormPostSize;
	}

	public void setMaxHttpFormPostSize(DataSize maxHttpFormPostSize) {
		this.maxHttpFormPostSize = maxHttpFormPostSize;
	}

	public int getMaxFormKeys() {
		return this.maxFormKeys;
	}

	public void setMaxFormKeys(int maxFormKeys) {
		this.maxFormKeys = maxFormKeys;
	}

	public Duration getConnectionIdleTimeout() {
		return this.connectionIdleTimeout;
	}

	public void setConnectionIdleTimeout(Duration connectionIdleTimeout) {
		this.connectionIdleTimeout = connectionIdleTimeout;
	}

	public DataSize getMaxHttpResponseHeaderSize() {
		return this.maxHttpResponseHeaderSize;
	}

	public void setMaxHttpResponseHeaderSize(DataSize maxHttpResponseHeaderSize) {
		this.maxHttpResponseHeaderSize = maxHttpResponseHeaderSize;
	}

	public int getMaxConnections() {
		return this.maxConnections;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	/**
	 * Jetty access log properties.
	 */
	public static class Accesslog {

		/**
		 * Enable access log.
		 */
		private boolean enabled = false;

		/**
		 * Log format.
		 */
		private Accesslog.Format format = Format.NCSA;

		/**
		 * Custom log format, see org.eclipse.jetty.server.CustomRequestLog. If defined,
		 * overrides the "format" configuration key.
		 */
		private String customFormat;

		/**
		 * Log filename. If not specified, logs redirect to "System.err".
		 */
		private String filename;

		/**
		 * Date format to place in log file name.
		 */
		private String fileDateFormat;

		/**
		 * Number of days before rotated log files are deleted.
		 */
		private int retentionPeriod = 31; // no days

		/**
		 * Append to log.
		 */
		private boolean append;

		/**
		 * Request paths that should not be logged.
		 */
		private List<String> ignorePaths;

		public boolean isEnabled() {
			return this.enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public Accesslog.Format getFormat() {
			return this.format;
		}

		public void setFormat(Accesslog.Format format) {
			this.format = format;
		}

		public String getCustomFormat() {
			return this.customFormat;
		}

		public void setCustomFormat(String customFormat) {
			this.customFormat = customFormat;
		}

		public String getFilename() {
			return this.filename;
		}

		public void setFilename(String filename) {
			this.filename = filename;
		}

		public String getFileDateFormat() {
			return this.fileDateFormat;
		}

		public void setFileDateFormat(String fileDateFormat) {
			this.fileDateFormat = fileDateFormat;
		}

		public int getRetentionPeriod() {
			return this.retentionPeriod;
		}

		public void setRetentionPeriod(int retentionPeriod) {
			this.retentionPeriod = retentionPeriod;
		}

		public boolean isAppend() {
			return this.append;
		}

		public void setAppend(boolean append) {
			this.append = append;
		}

		public List<String> getIgnorePaths() {
			return this.ignorePaths;
		}

		public void setIgnorePaths(List<String> ignorePaths) {
			this.ignorePaths = ignorePaths;
		}

		/**
		 * Log format for Jetty access logs.
		 */
		public enum Format {

			/**
			 * NCSA format, as defined in CustomRequestLog#NCSA_FORMAT.
			 */
			NCSA,

			/**
			 * Extended NCSA format, as defined in CustomRequestLog#EXTENDED_NCSA_FORMAT.
			 */
			EXTENDED_NCSA

		}

	}

	/**
	 * Jetty thread properties.
	 */
	public static class Threads {

		/**
		 * Number of acceptor threads to use. When the value is -1, the default, the
		 * number of acceptors is derived from the operating environment.
		 */
		private Integer acceptors = -1;

		/**
		 * Number of selector threads to use. When the value is -1, the default, the
		 * number of selectors is derived from the operating environment.
		 */
		private Integer selectors = -1;

		/**
		 * Maximum number of threads. Doesn't have an effect if virtual threads are
		 * enabled.
		 */
		private Integer max = 200;

		/**
		 * Minimum number of threads. Doesn't have an effect if virtual threads are
		 * enabled.
		 */
		private Integer min = 8;

		/**
		 * Maximum capacity of the thread pool's backing queue. A default is computed
		 * based on the threading configuration.
		 */
		private Integer maxQueueCapacity;

		/**
		 * Maximum thread idle time.
		 */
		private Duration idleTimeout = Duration.ofMillis(60000);

		public Integer getAcceptors() {
			return this.acceptors;
		}

		public void setAcceptors(Integer acceptors) {
			this.acceptors = acceptors;
		}

		public Integer getSelectors() {
			return this.selectors;
		}

		public void setSelectors(Integer selectors) {
			this.selectors = selectors;
		}

		public void setMin(Integer min) {
			this.min = min;
		}

		public Integer getMin() {
			return this.min;
		}

		public void setMax(Integer max) {
			this.max = max;
		}

		public Integer getMax() {
			return this.max;
		}

		public Integer getMaxQueueCapacity() {
			return this.maxQueueCapacity;
		}

		public void setMaxQueueCapacity(Integer maxQueueCapacity) {
			this.maxQueueCapacity = maxQueueCapacity;
		}

		public void setIdleTimeout(Duration idleTimeout) {
			this.idleTimeout = idleTimeout;
		}

		public Duration getIdleTimeout() {
			return this.idleTimeout;
		}

	}

}
