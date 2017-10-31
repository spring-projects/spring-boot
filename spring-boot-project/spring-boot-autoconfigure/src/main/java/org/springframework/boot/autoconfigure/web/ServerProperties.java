/*
 * Copyright 2012-2017 the original author or authors.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.servlet.server.Jsp;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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
 */
@ConfigurationProperties(prefix = "server", ignoreUnknownFields = true)
public class ServerProperties {

	/**
	 * Server HTTP port.
	 */
	private Integer port;

	/**
	 * Network address to which the server should bind to.
	 */
	private InetAddress address;

	/**
	 * Display name of the application.
	 */
	private String displayName = "application";

	@NestedConfigurationProperty
	private ErrorProperties error = new ErrorProperties();

	/**
	 * If X-Forwarded-* headers should be applied to the HttpRequest.
	 */
	private Boolean useForwardHeaders;

	/**
	 * Value to use for the Server response header (no header is sent if empty).
	 */
	private String serverHeader;

	/**
	 * Maximum size in bytes of the HTTP message header.
	 */
	private int maxHttpHeaderSize = 0; // bytes

	/**
	 * Time in milliseconds that connectors will wait for another HTTP request before
	 * closing the connection. When not set, the connector's server-specific default will
	 * be used. Use a value of -1 to indicate no (i.e. infinite) timeout.
	 */
	private Integer connectionTimeout;

	private Session session = new Session();

	@NestedConfigurationProperty
	private Ssl ssl;

	@NestedConfigurationProperty
	private Compression compression = new Compression();

	@NestedConfigurationProperty
	private Http2 http2 = new Http2();

	private Servlet servlet = new Servlet();

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

	public String getDisplayName() {
		return this.displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
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

	public int getMaxHttpHeaderSize() {
		return this.maxHttpHeaderSize;
	}

	public void setMaxHttpHeaderSize(int maxHttpHeaderSize) {
		this.maxHttpHeaderSize = maxHttpHeaderSize;
	}

	public Integer getConnectionTimeout() {
		return this.connectionTimeout;
	}

	public void setConnectionTimeout(Integer connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public ErrorProperties getError() {
		return this.error;
	}

	public Session getSession() {
		return this.session;
	}

	public void setSession(Session session) {
		this.session = session;
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

	public void setServlet(Servlet servlet) {
		this.servlet = servlet;
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
	public class Servlet {

		/**
		 * Servlet context init parameters.
		 */
		private final Map<String, String> contextParameters = new HashMap<>();

		/**
		 * Context path of the application.
		 */
		private String contextPath;

		/**
		 * Path of the main dispatcher servlet.
		 */
		private String path = "/";

		@NestedConfigurationProperty
		private Jsp jsp = new Jsp();

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

		public String getPath() {
			return this.path;
		}

		public void setPath(String path) {
			Assert.notNull(path, "Path must not be null");
			this.path = path;
		}

		public Map<String, String> getContextParameters() {
			return this.contextParameters;
		}

		public Jsp getJsp() {
			return this.jsp;
		}

		public void setJsp(Jsp jsp) {
			this.jsp = jsp;
		}

		public String getServletMapping() {
			if (this.path.equals("") || this.path.equals("/")) {
				return "/";
			}
			if (this.path.contains("*")) {
				return this.path;
			}
			if (this.path.endsWith("/")) {
				return this.path + "*";
			}
			return this.path + "/*";
		}

		public String getPath(String path) {
			String prefix = getServletPrefix();
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			return prefix + path;
		}

		public String getServletPrefix() {
			String result = this.path;
			if (result.contains("*")) {
				result = result.substring(0, result.indexOf("*"));
			}
			if (result.endsWith("/")) {
				result = result.substring(0, result.length() - 1);
			}
			return result;
		}

		public String[] getPathsArray(Collection<String> paths) {
			String[] result = new String[paths.size()];
			int i = 0;
			for (String path : paths) {
				result[i++] = getPath(path);
			}
			return result;
		}

		public String[] getPathsArray(String[] paths) {
			String[] result = new String[paths.length];
			int i = 0;
			for (String path : paths) {
				result[i++] = getPath(path);
			}
			return result;
		}

	}

	/**
	 * Session properties.
	 */
	public static class Session {

		/**
		 * Session timeout in seconds.
		 */
		private Integer timeout;

		/**
		 * Session tracking modes (one or more of the following: "cookie", "url", "ssl").
		 */
		private Set<SessionTrackingMode> trackingModes;

		/**
		 * Persist session data between restarts.
		 */
		private boolean persistent;

		/**
		 * Directory used to store session data.
		 */
		private File storeDir;

		private Cookie cookie = new Cookie();

		public Cookie getCookie() {
			return this.cookie;
		}

		public Integer getTimeout() {
			return this.timeout;
		}

		public void setTimeout(Integer sessionTimeout) {
			this.timeout = sessionTimeout;
		}

		public Set<SessionTrackingMode> getTrackingModes() {
			return this.trackingModes;
		}

		public void setTrackingModes(Set<SessionTrackingMode> trackingModes) {
			this.trackingModes = trackingModes;
		}

		public boolean isPersistent() {
			return this.persistent;
		}

		public void setPersistent(boolean persistent) {
			this.persistent = persistent;
		}

		public File getStoreDir() {
			return this.storeDir;
		}

		public void setStoreDir(File storeDir) {
			this.storeDir = storeDir;
		}

		/**
		 * Cookie properties.
		 */
		public static class Cookie {

			/**
			 * Session cookie name.
			 */
			private String name;

			/**
			 * Domain for the session cookie.
			 */
			private String domain;

			/**
			 * Path of the session cookie.
			 */
			private String path;

			/**
			 * Comment for the session cookie.
			 */
			private String comment;

			/**
			 * "HttpOnly" flag for the session cookie.
			 */
			private Boolean httpOnly;

			/**
			 * "Secure" flag for the session cookie.
			 */
			private Boolean secure;

			/**
			 * Maximum age of the session cookie in seconds.
			 */
			private Integer maxAge;

			public String getName() {
				return this.name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public String getDomain() {
				return this.domain;
			}

			public void setDomain(String domain) {
				this.domain = domain;
			}

			public String getPath() {
				return this.path;
			}

			public void setPath(String path) {
				this.path = path;
			}

			public String getComment() {
				return this.comment;
			}

			public void setComment(String comment) {
				this.comment = comment;
			}

			public Boolean getHttpOnly() {
				return this.httpOnly;
			}

			public void setHttpOnly(Boolean httpOnly) {
				this.httpOnly = httpOnly;
			}

			public Boolean getSecure() {
				return this.secure;
			}

			public void setSecure(Boolean secure) {
				this.secure = secure;
			}

			public Integer getMaxAge() {
				return this.maxAge;
			}

			public void setMaxAge(Integer maxAge) {
				this.maxAge = maxAge;
			}

		}

		/**
		 * Available session tracking modes (mirrors
		 * {@link javax.servlet.SessionTrackingMode}.
		 */
		public enum SessionTrackingMode {

			/**
			 * Send a cookie in response to the client's first request.
			 */
			COOKIE,

			/**
			 * Rewrite the URL to append a session ID.
			 */
			URL,

			/**
			 * Use SSL build-in mechanism to track the session.
			 */
			SSL

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
				+ "172\\.3[0-1]{1}\\.\\d{1,3}\\.\\d{1,3}";

		/**
		 * Header that holds the incoming protocol, usually named "X-Forwarded-Proto".
		 */
		private String protocolHeader;

		/**
		 * Value of the protocol header that indicates that the incoming request uses SSL.
		 */
		private String protocolHeaderHttpsValue = "https";

		/**
		 * Name of the HTTP header used to override the original port value.
		 */
		private String portHeader = "X-Forwarded-Port";

		/**
		 * Name of the http header from which the remote ip is extracted..
		 */
		private String remoteIpHeader;

		/**
		 * Tomcat base directory. If not specified a temporary directory will be used.
		 */
		private File basedir;

		/**
		 * Delay in seconds between the invocation of backgroundProcess methods.
		 */
		private int backgroundProcessorDelay = 30; // seconds

		/**
		 * Maximum amount of worker threads.
		 */
		private int maxThreads = 0; // Number of threads in protocol handler

		/**
		 * Minimum amount of worker threads.
		 */
		private int minSpareThreads = 0; // Minimum spare threads in protocol handler

		/**
		 * Maximum size in bytes of the HTTP post content.
		 */
		private int maxHttpPostSize = 0; // bytes

		/**
		 * Maximum size in bytes of the HTTP message header.
		 */
		private int maxHttpHeaderSize = 0; // bytes

		/**
		 * Whether requests to the context root should be redirected by appending a / to
		 * the path.
		 */
		private Boolean redirectContextRoot;

		/**
		 * Character encoding to use to decode the URI.
		 */
		private Charset uriEncoding;

		/**
		 * Maximum number of connections that the server will accept and process at any
		 * given time. Once the limit has been reached, the operating system may still
		 * accept connections based on the "acceptCount" property.
		 */
		private int maxConnections = 0;

		/**
		 * Maximum queue length for incoming connection requests when all possible request
		 * processing threads are in use.
		 */
		private int acceptCount = 0;

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

		public int getMaxHttpPostSize() {
			return this.maxHttpPostSize;
		}

		public void setMaxHttpPostSize(int maxHttpPostSize) {
			this.maxHttpPostSize = maxHttpPostSize;
		}

		public Accesslog getAccesslog() {
			return this.accesslog;
		}

		public int getBackgroundProcessorDelay() {
			return this.backgroundProcessorDelay;
		}

		public void setBackgroundProcessorDelay(int backgroundProcessorDelay) {
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

		public int getMaxHttpHeaderSize() {
			return this.maxHttpHeaderSize;
		}

		public void setMaxHttpHeaderSize(int maxHttpHeaderSize) {
			this.maxHttpHeaderSize = maxHttpHeaderSize;
		}

		public int getAcceptCount() {
			return this.acceptCount;
		}

		public void setAcceptCount(int acceptCount) {
			this.acceptCount = acceptCount;
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
			 * Directory in which log files are created. Can be relative to the tomcat
			 * base dir or absolute.
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
			 * Enable access log rotation.
			 */
			private boolean rotate = true;

			/**
			 * Defer inclusion of the date stamp in the file name until rotate time.
			 */
			private boolean renameOnRotate;

			/**
			 * Date format to place in log file name.
			 */
			private String fileDateFormat = ".yyyy-MM-dd";

			/**
			 * Set request attributes for IP address, Hostname, protocol and port used for
			 * the request.
			 */
			private boolean requestAttributesEnabled;

			/**
			 * Buffer output such that it is only flushed periodically.
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
			 * Time-to-live in milliseconds of the static resource cache.
			 */
			private Long cacheTtl;

			public Long getCacheTtl() {
				return this.cacheTtl;
			}

			public void setCacheTtl(Long cacheTtl) {
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
		 * Maximum size in bytes of the HTTP post or put content.
		 */
		private int maxHttpPostSize = 0; // bytes

		/**
		 * Number of acceptor threads to use.
		 */
		private Integer acceptors;

		/**
		 * Number of selector threads to use.
		 */
		private Integer selectors;

		public Accesslog getAccesslog() {
			return this.accesslog;
		}

		public int getMaxHttpPostSize() {
			return this.maxHttpPostSize;
		}

		public void setMaxHttpPostSize(int maxHttpPostSize) {
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
			 * Log filename. If not specified, logs will be redirected to "System.err".
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
		 * Maximum size in bytes of the HTTP post content.
		 */
		private long maxHttpPostSize = 0; // bytes

		/**
		 * Size of each buffer in bytes.
		 */
		private Integer bufferSize;

		/**
		 * Number of I/O threads to create for the worker.
		 */
		private Integer ioThreads;

		/**
		 * Number of worker threads.
		 */
		private Integer workerThreads;

		/**
		 * Allocate buffers outside the Java heap.
		 */
		private Boolean directBuffers;

		/**
		 * Whether servlet filters should be initialized on startup.
		 */
		private boolean eagerFilterInit = true;

		private final Accesslog accesslog = new Accesslog();

		public long getMaxHttpPostSize() {
			return this.maxHttpPostSize;
		}

		public void setMaxHttpPostSize(long maxHttpPostSize) {
			this.maxHttpPostSize = maxHttpPostSize;
		}

		public Integer getBufferSize() {
			return this.bufferSize;
		}

		public void setBufferSize(Integer bufferSize) {
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
			 * Enable access log.
			 */
			private Boolean enabled;

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
			 * Enable access log rotation.
			 */
			private boolean rotate = true;

			public Boolean getEnabled() {
				return this.enabled;
			}

			public void setEnabled(Boolean enabled) {
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
