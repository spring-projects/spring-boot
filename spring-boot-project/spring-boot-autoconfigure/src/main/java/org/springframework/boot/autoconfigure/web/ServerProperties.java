/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import java.io.File;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.servlet.server.Jsp;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

/**
 * {@link ConfigurationProperties} for a web server (e.g. port and path settings).
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
 */
@ConfigurationProperties(prefix = "server", ignoreUnknownFields = true)
public class ServerProperties {

	/**
	 * Server HTTP port.
	 */
	private Integer port;

	/**
	 * Network address to which the server should bind.
	 */
	private InetAddress address;

	@NestedConfigurationProperty
	private final ErrorProperties error = new ErrorProperties();

	/**
	 * Whether X-Forwarded-* headers should be applied to the HttpRequest.
	 */
	private Boolean useForwardHeaders;

	/**
	 * Value to use for the Server response header (if empty, no header is sent).
	 */
	private String serverHeader;

	/**
	 * Maximum size of the HTTP message header.
	 */
	private DataSize maxHttpHeaderSize = DataSize.ofKilobytes(8);

	/**
	 * Time that connectors wait for another HTTP request before closing the connection.
	 * When not set, the connector's container-specific default is used. Use a value of -1
	 * to indicate no (that is, an infinite) timeout.
	 */
	private Duration connectionTimeout;

	@NestedConfigurationProperty
	private Ssl ssl;

	@NestedConfigurationProperty
	private final Compression compression = new Compression();

	@NestedConfigurationProperty
	private final Http2 http2 = new Http2();

	private final Servlet servlet = new Servlet();

	private final Tomcat tomcat = new Tomcat();

	private final Jetty jetty = new Jetty();

	private final Undertow undertow = new Undertow();

	public Integer getPort() {
		return this.port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public InetAddress getAddress() {
		return this.address;
	}

	public void setAddress(InetAddress address) {
		this.address = address;
	}

	public Boolean isUseForwardHeaders() {
		return this.useForwardHeaders;
	}

	public void setUseForwardHeaders(Boolean useForwardHeaders) {
		this.useForwardHeaders = useForwardHeaders;
	}

	public String getServerHeader() {
		return this.serverHeader;
	}

	public void setServerHeader(String serverHeader) {
		this.serverHeader = serverHeader;
	}

	public DataSize getMaxHttpHeaderSize() {
		return this.maxHttpHeaderSize;
	}

	public void setMaxHttpHeaderSize(DataSize maxHttpHeaderSize) {
		this.maxHttpHeaderSize = maxHttpHeaderSize;
	}

	public Duration getConnectionTimeout() {
		return this.connectionTimeout;
	}

	public void setConnectionTimeout(Duration connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public ErrorProperties getError() {
		return this.error;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public void setSsl(Ssl ssl) {
		this.ssl = ssl;
	}

	public Compression getCompression() {
		return this.compression;
	}

	public Http2 getHttp2() {
		return this.http2;
	}

	public Servlet getServlet() {
		return this.servlet;
	}

	public Tomcat getTomcat() {
		return this.tomcat;
	}

	public Jetty getJetty() {
		return this.jetty;
	}

	public Undertow getUndertow() {
		return this.undertow;
	}

	/**
	 * Servlet properties.
	 */
	public static class Servlet {

		/**
		 * Servlet context init parameters.
		 */
		private final Map<String, String> contextParameters = new HashMap<>();

		/**
		 * Context path of the application.
		 */
		private String contextPath;

		/**
		 * Display name of the application.
		 */
		private String applicationDisplayName = "application";

		@NestedConfigurationProperty
		private final Jsp jsp = new Jsp();

		@NestedConfigurationProperty
		private final Session session = new Session();

		public String getContextPath() {
			return this.contextPath;
		}

		public void setContextPath(String contextPath) {
			this.contextPath = cleanContextPath(contextPath);
		}

		private String cleanContextPath(String contextPath) {
			if (StringUtils.hasText(contextPath) && contextPath.endsWith("/")) {
				return contextPath.substring(0, contextPath.length() - 1);
			}
			return contextPath;
		}

		public String getApplicationDisplayName() {
			return this.applicationDisplayName;
		}

		public void setApplicationDisplayName(String displayName) {
			this.applicationDisplayName = displayName;
		}

		public Map<String, String> getContextParameters() {
			return this.contextParameters;
		}

		public Jsp getJsp() {
			return this.jsp;
		}

		public Session getSession() {
			return this.session;
		}

	}

	/**
	 * Tomcat properties.
	 */
	public static class Tomcat {

		/**
		 * Access log configuration.
		 */
		private final Accesslog accesslog = new Accesslog();

		/**
		 * Regular expression that matches proxies that are to be trusted.
		 */
		private String internalProxies = "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|" // 10/8
				+ "192\\.168\\.\\d{1,3}\\.\\d{1,3}|" // 192.168/16
				+ "169\\.254\\.\\d{1,3}\\.\\d{1,3}|" // 169.254/16
				+ "127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|" // 127/8
				+ "172\\.1[6-9]{1}\\.\\d{1,3}\\.\\d{1,3}|" // 172.16/12
				+ "172\\.2[0-9]{1}\\.\\d{1,3}\\.\\d{1,3}|"
				+ "172\\.3[0-1]{1}\\.\\d{1,3}\\.\\d{1,3}|" //
				+ "0:0:0:0:0:0:0:1|::1";

		/**
		 * Header that holds the incoming protocol, usually named "X-Forwarded-Proto".
		 */
		private String protocolHeader;

		/**
		 * Value of the protocol header indicating whether the incoming request uses SSL.
		 */
		private String protocolHeaderHttpsValue = "https";

		/**
		 * Name of the HTTP header used to override the original port value.
		 */
		private String portHeader = "X-Forwarded-Port";

		/**
		 * Name of the HTTP header from which the remote IP is extracted. For instance,
		 * `X-FORWARDED-FOR`.
		 */
		private String remoteIpHeader;

		/**
		 * Tomcat base directory. If not specified, a temporary directory is used.
		 */
		private File basedir;

		/**
		 * Delay between the invocation of backgroundProcess methods. If a duration suffix
		 * is not specified, seconds will be used.
		 */
		@DurationUnit(ChronoUnit.SECONDS)
		private Duration backgroundProcessorDelay = Duration.ofSeconds(10);

		/**
		 * Maximum amount of worker threads.
		 */
		private int maxThreads = 200;

		/**
		 * Minimum amount of worker threads.
		 */
		private int minSpareThreads = 10;

		/**
		 * Maximum size of the HTTP post content.
		 */
		private DataSize maxHttpPostSize = DataSize.ofMegabytes(2);

		/**
		 * Maximum size of the HTTP message header.
		 */
		private DataSize maxHttpHeaderSize = DataSize.ofBytes(0);

		/**
		 * Maximum amount of request body to swallow.
		 */
		private DataSize maxSwallowSize = DataSize.ofMegabytes(2);

		/**
		 * Whether requests to the context root should be redirected by appending a / to
		 * the path.
		 */
		private Boolean redirectContextRoot = true;

		/**
		 * Whether HTTP 1.1 and later location headers generated by a call to sendRedirect
		 * will use relative or absolute redirects.
		 */
		private Boolean useRelativeRedirects;

		/**
		 * Character encoding to use to decode the URI.
		 */
		private Charset uriEncoding = StandardCharsets.UTF_8;

		/**
		 * Maximum number of connections that the server accepts and processes at any
		 * given time. Once the limit has been reached, the operating system may still
		 * accept connections based on the "acceptCount" property.
		 */
		private int maxConnections = 10000;

		/**
		 * Maximum queue length for incoming connection requests when all possible request
		 * processing threads are in use.
		 */
		private int acceptCount = 100;

		/**
		 * Maximum number of idle processors that will be retained in the cache and reused
		 * with a subsequent request.
		 */
		private int processorCache = 200;

		/**
		 * Comma-separated list of additional patterns that match jars to ignore for TLD
		 * scanning. The special '?' and '*' characters can be used in the pattern to
		 * match one and only one character and zero or more characters respectively.
		 */
		private List<String> additionalTldSkipPatterns = new ArrayList<>();

		/**
		 * Static resource configuration.
		 */
		private final Resource resource = new Resource();

		public int getMaxThreads() {
			return this.maxThreads;
		}

		public void setMaxThreads(int maxThreads) {
			this.maxThreads = maxThreads;
		}

		public int getMinSpareThreads() {
			return this.minSpareThreads;
		}

		public void setMinSpareThreads(int minSpareThreads) {
			this.minSpareThreads = minSpareThreads;
		}

		public DataSize getMaxHttpPostSize() {
			return this.maxHttpPostSize;
		}

		public void setMaxHttpPostSize(DataSize maxHttpPostSize) {
			this.maxHttpPostSize = maxHttpPostSize;
		}

		public Accesslog getAccesslog() {
			return this.accesslog;
		}

		public Duration getBackgroundProcessorDelay() {
			return this.backgroundProcessorDelay;
		}

		public void setBackgroundProcessorDelay(Duration backgroundProcessorDelay) {
			this.backgroundProcessorDelay = backgroundProcessorDelay;
		}

		public File getBasedir() {
			return this.basedir;
		}

		public void setBasedir(File basedir) {
			this.basedir = basedir;
		}

		public String getInternalProxies() {
			return this.internalProxies;
		}

		public void setInternalProxies(String internalProxies) {
			this.internalProxies = internalProxies;
		}

		public String getProtocolHeader() {
			return this.protocolHeader;
		}

		public void setProtocolHeader(String protocolHeader) {
			this.protocolHeader = protocolHeader;
		}

		public String getProtocolHeaderHttpsValue() {
			return this.protocolHeaderHttpsValue;
		}

		public void setProtocolHeaderHttpsValue(String protocolHeaderHttpsValue) {
			this.protocolHeaderHttpsValue = protocolHeaderHttpsValue;
		}

		public String getPortHeader() {
			return this.portHeader;
		}

		public void setPortHeader(String portHeader) {
			this.portHeader = portHeader;
		}

		public Boolean getRedirectContextRoot() {
			return this.redirectContextRoot;
		}

		public void setRedirectContextRoot(Boolean redirectContextRoot) {
			this.redirectContextRoot = redirectContextRoot;
		}

		public Boolean getUseRelativeRedirects() {
			return this.useRelativeRedirects;
		}

		public void setUseRelativeRedirects(Boolean useRelativeRedirects) {
			this.useRelativeRedirects = useRelativeRedirects;
		}

		public String getRemoteIpHeader() {
			return this.remoteIpHeader;
		}

		public void setRemoteIpHeader(String remoteIpHeader) {
			this.remoteIpHeader = remoteIpHeader;
		}

		public Charset getUriEncoding() {
			return this.uriEncoding;
		}

		public void setUriEncoding(Charset uriEncoding) {
			this.uriEncoding = uriEncoding;
		}

		public int getMaxConnections() {
			return this.maxConnections;
		}

		public void setMaxConnections(int maxConnections) {
			this.maxConnections = maxConnections;
		}

		@Deprecated
		@DeprecatedConfigurationProperty(replacement = "server.max-http-header-size")
		public DataSize getMaxHttpHeaderSize() {
			return this.maxHttpHeaderSize;
		}

		@Deprecated
		public void setMaxHttpHeaderSize(DataSize maxHttpHeaderSize) {
			this.maxHttpHeaderSize = maxHttpHeaderSize;
		}

		public DataSize getMaxSwallowSize() {
			return this.maxSwallowSize;
		}

		public void setMaxSwallowSize(DataSize maxSwallowSize) {
			this.maxSwallowSize = maxSwallowSize;
		}

		public int getAcceptCount() {
			return this.acceptCount;
		}

		public void setAcceptCount(int acceptCount) {
			this.acceptCount = acceptCount;
		}

		public int getProcessorCache() {
			return this.processorCache;
		}

		public void setProcessorCache(int processorCache) {
			this.processorCache = processorCache;
		}

		public List<String> getAdditionalTldSkipPatterns() {
			return this.additionalTldSkipPatterns;
		}

		public void setAdditionalTldSkipPatterns(List<String> additionalTldSkipPatterns) {
			this.additionalTldSkipPatterns = additionalTldSkipPatterns;
		}

		public Resource getResource() {
			return this.resource;
		}

		/**
		 * Tomcat access log properties.
		 */
		public static class Accesslog {

			/**
			 * Enable access log.
			 */
			private boolean enabled = false;

			/**
			 * Format pattern for access logs.
			 */
			private String pattern = "common";

			/**
			 * Directory in which log files are created. Can be absolute or relative to
			 * the Tomcat base dir.
			 */
			private String directory = "logs";

			/**
			 * Log file name prefix.
			 */
			protected String prefix = "access_log";

			/**
			 * Log file name suffix.
			 */
			private String suffix = ".log";

			/**
			 * Whether to enable access log rotation.
			 */
			private boolean rotate = true;

			/**
			 * Whether to defer inclusion of the date stamp in the file name until rotate
			 * time.
			 */
			private boolean renameOnRotate = false;

			/**
			 * Date format to place in the log file name.
			 */
			private String fileDateFormat = ".yyyy-MM-dd";

			/**
			 * Set request attributes for the IP address, Hostname, protocol, and port
			 * used for the request.
			 */
			private boolean requestAttributesEnabled = false;

			/**
			 * Whether to buffer output such that it is flushed only periodically.
			 */
			private boolean buffered = true;

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

			public String getDirectory() {
				return this.directory;
			}

			public void setDirectory(String directory) {
				this.directory = directory;
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

			public boolean isRotate() {
				return this.rotate;
			}

			public void setRotate(boolean rotate) {
				this.rotate = rotate;
			}

			public boolean isRenameOnRotate() {
				return this.renameOnRotate;
			}

			public void setRenameOnRotate(boolean renameOnRotate) {
				this.renameOnRotate = renameOnRotate;
			}

			public String getFileDateFormat() {
				return this.fileDateFormat;
			}

			public void setFileDateFormat(String fileDateFormat) {
				this.fileDateFormat = fileDateFormat;
			}

			public boolean isRequestAttributesEnabled() {
				return this.requestAttributesEnabled;
			}

			public void setRequestAttributesEnabled(boolean requestAttributesEnabled) {
				this.requestAttributesEnabled = requestAttributesEnabled;
			}

			public boolean isBuffered() {
				return this.buffered;
			}

			public void setBuffered(boolean buffered) {
				this.buffered = buffered;
			}

		}

		/**
		 * Tomcat static resource properties.
		 */
		public static class Resource {

			/**
			 * Whether static resource caching is permitted for this web application.
			 */
			private boolean allowCaching = true;

			/**
			 * Time-to-live of the static resource cache.
			 */
			private Duration cacheTtl;

			public boolean isAllowCaching() {
				return this.allowCaching;
			}

			public void setAllowCaching(boolean allowCaching) {
				this.allowCaching = allowCaching;
			}

			public Duration getCacheTtl() {
				return this.cacheTtl;
			}

			public void setCacheTtl(Duration cacheTtl) {
				this.cacheTtl = cacheTtl;
			}

		}

	}

	/**
	 * Jetty properties.
	 */
	public static class Jetty {

		/**
		 * Access log configuration.
		 */
		private final Accesslog accesslog = new Accesslog();

		/**
		 * Maximum size of the HTTP post or put content.
		 */
		private DataSize maxHttpPostSize = DataSize.ofBytes(200000);

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

		public Accesslog getAccesslog() {
			return this.accesslog;
		}

		public DataSize getMaxHttpPostSize() {
			return this.maxHttpPostSize;
		}

		public void setMaxHttpPostSize(DataSize maxHttpPostSize) {
			this.maxHttpPostSize = maxHttpPostSize;
		}

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

		/**
		 * Jetty access log properties.
		 */
		public static class Accesslog {

			/**
			 * Enable access log.
			 */
			private boolean enabled = false;

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
			 * Enable extended NCSA format.
			 */
			private boolean extendedFormat;

			/**
			 * Timestamp format of the request log.
			 */
			private String dateFormat = "dd/MMM/yyyy:HH:mm:ss Z";

			/**
			 * Locale of the request log.
			 */
			private Locale locale;

			/**
			 * Timezone of the request log.
			 */
			private TimeZone timeZone = TimeZone.getTimeZone("GMT");

			/**
			 * Enable logging of the request cookies.
			 */
			private boolean logCookies;

			/**
			 * Enable logging of the request hostname.
			 */
			private boolean logServer;

			/**
			 * Enable logging of request processing time.
			 */
			private boolean logLatency;

			public boolean isEnabled() {
				return this.enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
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

			public boolean isExtendedFormat() {
				return this.extendedFormat;
			}

			public void setExtendedFormat(boolean extendedFormat) {
				this.extendedFormat = extendedFormat;
			}

			public String getDateFormat() {
				return this.dateFormat;
			}

			public void setDateFormat(String dateFormat) {
				this.dateFormat = dateFormat;
			}

			public Locale getLocale() {
				return this.locale;
			}

			public void setLocale(Locale locale) {
				this.locale = locale;
			}

			public TimeZone getTimeZone() {
				return this.timeZone;
			}

			public void setTimeZone(TimeZone timeZone) {
				this.timeZone = timeZone;
			}

			public boolean isLogCookies() {
				return this.logCookies;
			}

			public void setLogCookies(boolean logCookies) {
				this.logCookies = logCookies;
			}

			public boolean isLogServer() {
				return this.logServer;
			}

			public void setLogServer(boolean logServer) {
				this.logServer = logServer;
			}

			public boolean isLogLatency() {
				return this.logLatency;
			}

			public void setLogLatency(boolean logLatency) {
				this.logLatency = logLatency;
			}

		}

	}

	/**
	 * Undertow properties.
	 */
	public static class Undertow {

		/**
		 * Maximum size of the HTTP post content. When the value is -1, the default, the
		 * size is unlimited.
		 */
		private DataSize maxHttpPostSize = DataSize.ofBytes(-1);

		/**
		 * Size of each buffer. The default is derived from the maximum amount of memory
		 * that is available to the JVM.
		 */
		private DataSize bufferSize;

		/**
		 * Number of I/O threads to create for the worker. The default is derived from the
		 * number of available processors.
		 */
		private Integer ioThreads;

		/**
		 * Number of worker threads. The default is 8 times the number of I/O threads.
		 */
		private Integer workerThreads;

		/**
		 * Whether to allocate buffers outside the Java heap. The default is derived from
		 * the maximum amount of memory that is available to the JVM.
		 */
		private Boolean directBuffers;

		/**
		 * Whether servlet filters should be initialized on startup.
		 */
		private boolean eagerFilterInit = true;

		private final Accesslog accesslog = new Accesslog();

		public DataSize getMaxHttpPostSize() {
			return this.maxHttpPostSize;
		}

		public void setMaxHttpPostSize(DataSize maxHttpPostSize) {
			this.maxHttpPostSize = maxHttpPostSize;
		}

		public DataSize getBufferSize() {
			return this.bufferSize;
		}

		public void setBufferSize(DataSize bufferSize) {
			this.bufferSize = bufferSize;
		}

		public Integer getIoThreads() {
			return this.ioThreads;
		}

		public void setIoThreads(Integer ioThreads) {
			this.ioThreads = ioThreads;
		}

		public Integer getWorkerThreads() {
			return this.workerThreads;
		}

		public void setWorkerThreads(Integer workerThreads) {
			this.workerThreads = workerThreads;
		}

		public Boolean getDirectBuffers() {
			return this.directBuffers;
		}

		public void setDirectBuffers(Boolean directBuffers) {
			this.directBuffers = directBuffers;
		}

		public boolean isEagerFilterInit() {
			return this.eagerFilterInit;
		}

		public void setEagerFilterInit(boolean eagerFilterInit) {
			this.eagerFilterInit = eagerFilterInit;
		}

		public Accesslog getAccesslog() {
			return this.accesslog;
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

	}

}
