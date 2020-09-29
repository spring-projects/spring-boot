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

package org.springframework.boot.autoconfigure.web;

import java.io.File;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.undertow.UndertowOptions;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.servlet.server.Encoding;
import org.springframework.boot.web.servlet.server.Jsp;
import org.springframework.boot.web.servlet.server.Session;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

/**
 * {@link ConfigurationProperties @ConfigurationProperties} for a web server (e.g. port
 * and path settings).
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
 * @since 1.0.0
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
	 * Strategy for handling X-Forwarded-* headers.
	 */
	private ForwardHeadersStrategy forwardHeadersStrategy;

	/**
	 * Value to use for the Server response header (if empty, no header is sent).
	 */
	private String serverHeader;

	/**
	 * Maximum size of the HTTP message header.
	 */
	private DataSize maxHttpHeaderSize = DataSize.ofKilobytes(8);

	/**
	 * Type of shutdown that the server will support.
	 */
	private Shutdown shutdown = Shutdown.IMMEDIATE;

	@NestedConfigurationProperty
	private Ssl ssl;

	@NestedConfigurationProperty
	private final Compression compression = new Compression();

	@NestedConfigurationProperty
	private final Http2 http2 = new Http2();

	private final Servlet servlet = new Servlet();

	private final Tomcat tomcat = new Tomcat();

	private final Jetty jetty = new Jetty();

	private final Netty netty = new Netty();

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

	public Shutdown getShutdown() {
		return this.shutdown;
	}

	public void setShutdown(Shutdown shutdown) {
		this.shutdown = shutdown;
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

	public Netty getNetty() {
		return this.netty;
	}

	public Undertow getUndertow() {
		return this.undertow;
	}

	public ForwardHeadersStrategy getForwardHeadersStrategy() {
		return this.forwardHeadersStrategy;
	}

	public void setForwardHeadersStrategy(ForwardHeadersStrategy forwardHeadersStrategy) {
		this.forwardHeadersStrategy = forwardHeadersStrategy;
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

		/**
		 * Whether to register the default Servlet with the container.
		 */
		private boolean registerDefaultServlet = false;

		@NestedConfigurationProperty
		private final Encoding encoding = new Encoding();

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
			String candidate = StringUtils.trimWhitespace(contextPath);
			if (StringUtils.hasText(candidate) && candidate.endsWith("/")) {
				return candidate.substring(0, candidate.length() - 1);
			}
			return candidate;
		}

		public String getApplicationDisplayName() {
			return this.applicationDisplayName;
		}

		public void setApplicationDisplayName(String displayName) {
			this.applicationDisplayName = displayName;
		}

		public boolean isRegisterDefaultServlet() {
			return this.registerDefaultServlet;
		}

		public void setRegisterDefaultServlet(boolean registerDefaultServlet) {
			this.registerDefaultServlet = registerDefaultServlet;
		}

		public Map<String, String> getContextParameters() {
			return this.contextParameters;
		}

		public Encoding getEncoding() {
			return this.encoding;
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
		 * Thread related configuration.
		 */
		private final Threads threads = new Threads();

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
		 * Maximum size of the form content in any HTTP post request.
		 */
		private DataSize maxHttpFormPostSize = DataSize.ofMegabytes(2);

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
		private boolean useRelativeRedirects;

		/**
		 * Character encoding to use to decode the URI.
		 */
		private Charset uriEncoding = StandardCharsets.UTF_8;

		/**
		 * Maximum number of connections that the server accepts and processes at any
		 * given time. Once the limit has been reached, the operating system may still
		 * accept connections based on the "acceptCount" property.
		 */
		private int maxConnections = 8192;

		/**
		 * Maximum queue length for incoming connection requests when all possible request
		 * processing threads are in use.
		 */
		private int acceptCount = 100;

		/**
		 * Maximum number of idle processors that will be retained in the cache and reused
		 * with a subsequent request. When set to -1 the cache will be unlimited with a
		 * theoretical maximum size equal to the maximum number of connections.
		 */
		private int processorCache = 200;

		/**
		 * Comma-separated list of additional patterns that match jars to ignore for TLD
		 * scanning. The special '?' and '*' characters can be used in the pattern to
		 * match one and only one character and zero or more characters respectively.
		 */
		private List<String> additionalTldSkipPatterns = new ArrayList<>();

		/**
		 * Comma-separated list of additional unencoded characters that should be allowed
		 * in URI paths. Only "< > [ \ ] ^ ` { | }" are allowed.
		 */
		private List<Character> relaxedPathChars = new ArrayList<>();

		/**
		 * Comma-separated list of additional unencoded characters that should be allowed
		 * in URI query strings. Only "< > [ \ ] ^ ` { | }" are allowed.
		 */
		private List<Character> relaxedQueryChars = new ArrayList<>();

		/**
		 * Amount of time the connector will wait, after accepting a connection, for the
		 * request URI line to be presented.
		 */
		private Duration connectionTimeout;

		/**
		 * Static resource configuration.
		 */
		private final Resource resource = new Resource();

		/**
		 * Modeler MBean Registry configuration.
		 */
		private final Mbeanregistry mbeanregistry = new Mbeanregistry();

		/**
		 * Remote Ip Valve configuration.
		 */
		private final Remoteip remoteip = new Remoteip();

		@Deprecated
		@DeprecatedConfigurationProperty(replacement = "server.tomcat.threads.max")
		public int getMaxThreads() {
			return getThreads().getMax();
		}

		@Deprecated
		public void setMaxThreads(int maxThreads) {
			getThreads().setMax(maxThreads);
		}

		@Deprecated
		@DeprecatedConfigurationProperty(replacement = "server.tomcat.threads.min-spare")
		public int getMinSpareThreads() {
			return getThreads().getMinSpare();
		}

		@Deprecated
		public void setMinSpareThreads(int minSpareThreads) {
			getThreads().setMinSpare(minSpareThreads);
		}

		public DataSize getMaxHttpFormPostSize() {
			return this.maxHttpFormPostSize;
		}

		public void setMaxHttpFormPostSize(DataSize maxHttpFormPostSize) {
			this.maxHttpFormPostSize = maxHttpFormPostSize;
		}

		public Accesslog getAccesslog() {
			return this.accesslog;
		}

		public Threads getThreads() {
			return this.threads;
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

		@DeprecatedConfigurationProperty(replacement = "server.tomcat.remoteip.internal-proxies")
		@Deprecated
		public String getInternalProxies() {
			return this.remoteip.getInternalProxies();
		}

		@Deprecated
		public void setInternalProxies(String internalProxies) {
			this.remoteip.setInternalProxies(internalProxies);
		}

		@DeprecatedConfigurationProperty(replacement = "server.tomcat.remoteip.protocol-header")
		@Deprecated
		public String getProtocolHeader() {
			return this.remoteip.getProtocolHeader();
		}

		@Deprecated
		public void setProtocolHeader(String protocolHeader) {
			this.remoteip.setProtocolHeader(protocolHeader);
		}

		@DeprecatedConfigurationProperty(replacement = "server.tomcat.remoteip.protocol-header-https-value")
		@Deprecated
		public String getProtocolHeaderHttpsValue() {
			return this.remoteip.getProtocolHeaderHttpsValue();
		}

		@Deprecated
		public void setProtocolHeaderHttpsValue(String protocolHeaderHttpsValue) {
			this.remoteip.setProtocolHeaderHttpsValue(protocolHeaderHttpsValue);
		}

		@DeprecatedConfigurationProperty(replacement = "server.tomcat.remoteip.host-header")
		@Deprecated
		public String getHostHeader() {
			return this.remoteip.getHostHeader();
		}

		@Deprecated
		public void setHostHeader(String hostHeader) {
			this.remoteip.setHostHeader(hostHeader);
		}

		@DeprecatedConfigurationProperty(replacement = "server.tomcat.remote.port-header")
		@Deprecated
		public String getPortHeader() {
			return this.remoteip.getPortHeader();
		}

		@Deprecated
		public void setPortHeader(String portHeader) {
			this.remoteip.setPortHeader(portHeader);
		}

		@DeprecatedConfigurationProperty(replacement = "server.tomcat.remoteip.remote-ip-header")
		@Deprecated
		public String getRemoteIpHeader() {
			return this.remoteip.getRemoteIpHeader();
		}

		@Deprecated
		public void setRemoteIpHeader(String remoteIpHeader) {
			this.remoteip.setRemoteIpHeader(remoteIpHeader);
		}

		public Boolean getRedirectContextRoot() {
			return this.redirectContextRoot;
		}

		public void setRedirectContextRoot(Boolean redirectContextRoot) {
			this.redirectContextRoot = redirectContextRoot;
		}

		@Deprecated
		public Boolean getUseRelativeRedirects() {
			return this.useRelativeRedirects;
		}

		public boolean isUseRelativeRedirects() {
			return this.useRelativeRedirects;
		}

		public void setUseRelativeRedirects(boolean useRelativeRedirects) {
			this.useRelativeRedirects = useRelativeRedirects;
		}

		@Deprecated
		public void setUseRelativeRedirects(Boolean useRelativeRedirects) {
			this.useRelativeRedirects = (useRelativeRedirects != null) ? useRelativeRedirects : false;
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

		public List<Character> getRelaxedPathChars() {
			return this.relaxedPathChars;
		}

		public void setRelaxedPathChars(List<Character> relaxedPathChars) {
			this.relaxedPathChars = relaxedPathChars;
		}

		public List<Character> getRelaxedQueryChars() {
			return this.relaxedQueryChars;
		}

		public void setRelaxedQueryChars(List<Character> relaxedQueryChars) {
			this.relaxedQueryChars = relaxedQueryChars;
		}

		public Duration getConnectionTimeout() {
			return this.connectionTimeout;
		}

		public void setConnectionTimeout(Duration connectionTimeout) {
			this.connectionTimeout = connectionTimeout;
		}

		public Resource getResource() {
			return this.resource;
		}

		public Mbeanregistry getMbeanregistry() {
			return this.mbeanregistry;
		}

		public Remoteip getRemoteip() {
			return this.remoteip;
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
			 * Whether logging of the request will only be enabled if
			 * "ServletRequest.getAttribute(conditionIf)" does not yield null.
			 */
			private String conditionIf;

			/**
			 * Whether logging of the request will only be enabled if
			 * "ServletRequest.getAttribute(conditionUnless)" yield null.
			 */
			private String conditionUnless;

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
			 * Character set used by the log file. Default to the system default character
			 * set.
			 */
			private String encoding;

			/**
			 * Locale used to format timestamps in log entries and in log file name
			 * suffix. Default to the default locale of the Java process.
			 */
			private String locale;

			/**
			 * Whether to check for log file existence so it can be recreated it if an
			 * external process has renamed it.
			 */
			private boolean checkExists = false;

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
			 * Number of days to retain the access log files before they are removed.
			 */
			private int maxDays = -1;

			/**
			 * Date format to place in the log file name.
			 */
			private String fileDateFormat = ".yyyy-MM-dd";

			/**
			 * Whether to use IPv6 canonical representation format as defined by RFC 5952.
			 */
			private boolean ipv6Canonical = false;

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

			public String getConditionIf() {
				return this.conditionIf;
			}

			public void setConditionIf(String conditionIf) {
				this.conditionIf = conditionIf;
			}

			public String getConditionUnless() {
				return this.conditionUnless;
			}

			public void setConditionUnless(String conditionUnless) {
				this.conditionUnless = conditionUnless;
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

			public String getEncoding() {
				return this.encoding;
			}

			public void setEncoding(String encoding) {
				this.encoding = encoding;
			}

			public String getLocale() {
				return this.locale;
			}

			public void setLocale(String locale) {
				this.locale = locale;
			}

			public boolean isCheckExists() {
				return this.checkExists;
			}

			public void setCheckExists(boolean checkExists) {
				this.checkExists = checkExists;
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

			public int getMaxDays() {
				return this.maxDays;
			}

			public void setMaxDays(int maxDays) {
				this.maxDays = maxDays;
			}

			public String getFileDateFormat() {
				return this.fileDateFormat;
			}

			public void setFileDateFormat(String fileDateFormat) {
				this.fileDateFormat = fileDateFormat;
			}

			public boolean isIpv6Canonical() {
				return this.ipv6Canonical;
			}

			public void setIpv6Canonical(boolean ipv6Canonical) {
				this.ipv6Canonical = ipv6Canonical;
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
		 * Tomcat thread properties.
		 */
		public static class Threads {

			/**
			 * Maximum amount of worker threads.
			 */
			private int max = 200;

			/**
			 * Minimum amount of worker threads.
			 */
			private int minSpare = 10;

			public int getMax() {
				return this.max;
			}

			public void setMax(int max) {
				this.max = max;
			}

			public int getMinSpare() {
				return this.minSpare;
			}

			public void setMinSpare(int minSpare) {
				this.minSpare = minSpare;
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

		public static class Mbeanregistry {

			/**
			 * Whether Tomcat's MBean Registry should be enabled.
			 */
			private boolean enabled;

			public boolean isEnabled() {
				return this.enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

		}

		public static class Remoteip {

			/**
			 * Regular expression that matches proxies that are to be trusted.
			 */
			private String internalProxies = "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|" // 10/8
					+ "192\\.168\\.\\d{1,3}\\.\\d{1,3}|" // 192.168/16
					+ "169\\.254\\.\\d{1,3}\\.\\d{1,3}|" // 169.254/16
					+ "127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|" // 127/8
					+ "172\\.1[6-9]{1}\\.\\d{1,3}\\.\\d{1,3}|" // 172.16/12
					+ "172\\.2[0-9]{1}\\.\\d{1,3}\\.\\d{1,3}|172\\.3[0-1]{1}\\.\\d{1,3}\\.\\d{1,3}|" //
					+ "0:0:0:0:0:0:0:1|::1";

			/**
			 * Header that holds the incoming protocol, usually named "X-Forwarded-Proto".
			 */
			private String protocolHeader;

			/**
			 * Value of the protocol header indicating whether the incoming request uses
			 * SSL.
			 */
			private String protocolHeaderHttpsValue = "https";

			/**
			 * Name of the HTTP header from which the remote host is extracted.
			 */
			private String hostHeader = "X-Forwarded-Host";

			/**
			 * Name of the HTTP header used to override the original port value.
			 */
			private String portHeader = "X-Forwarded-Port";

			/**
			 * Name of the HTTP header from which the remote IP is extracted. For
			 * instance, `X-FORWARDED-FOR`.
			 */
			private String remoteIpHeader;

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

			public String getHostHeader() {
				return this.hostHeader;
			}

			public void setHostHeader(String hostHeader) {
				this.hostHeader = hostHeader;
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

			public String getRemoteIpHeader() {
				return this.remoteIpHeader;
			}

			public void setRemoteIpHeader(String remoteIpHeader) {
				this.remoteIpHeader = remoteIpHeader;
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
		 * Thread related configuration.
		 */
		private final Threads threads = new Threads();

		/**
		 * Maximum size of the form content in any HTTP post request.
		 */
		private DataSize maxHttpFormPostSize = DataSize.ofBytes(200000);

		/**
		 * Time that the connection can be idle before it is closed.
		 */
		private Duration connectionIdleTimeout;

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

		@Deprecated
		@DeprecatedConfigurationProperty(replacement = "server.jetty.threads.acceptors")
		public Integer getAcceptors() {
			return getThreads().getAcceptors();
		}

		@Deprecated
		public void setAcceptors(Integer acceptors) {
			getThreads().setAcceptors(acceptors);
		}

		@Deprecated
		@DeprecatedConfigurationProperty(replacement = "server.jetty.threads.selectors")
		public Integer getSelectors() {
			return getThreads().getSelectors();
		}

		@Deprecated
		public void setSelectors(Integer selectors) {
			getThreads().setSelectors(selectors);
		}

		@Deprecated
		@DeprecatedConfigurationProperty(replacement = "server.jetty.threads.min")
		public Integer getMinThreads() {
			return getThreads().getMin();
		}

		@Deprecated
		public void setMinThreads(Integer minThreads) {
			getThreads().setMin(minThreads);
		}

		@Deprecated
		@DeprecatedConfigurationProperty(replacement = "server.jetty.threads.max")
		public Integer getMaxThreads() {
			return getThreads().getMax();
		}

		@Deprecated
		public void setMaxThreads(Integer maxThreads) {
			getThreads().setMax(maxThreads);
		}

		@Deprecated
		@DeprecatedConfigurationProperty(replacement = "server.jetty.threads.max-queue-capacity")
		public Integer getMaxQueueCapacity() {
			return getThreads().getMaxQueueCapacity();
		}

		@Deprecated
		public void setMaxQueueCapacity(Integer maxQueueCapacity) {
			getThreads().setMaxQueueCapacity(maxQueueCapacity);
		}

		@Deprecated
		@DeprecatedConfigurationProperty(replacement = "server.jetty.threads.idle-timeout")
		public Duration getThreadIdleTimeout() {
			return getThreads().getIdleTimeout();
		}

		@Deprecated
		public void setThreadIdleTimeout(Duration threadIdleTimeout) {
			getThreads().setIdleTimeout(threadIdleTimeout);
		}

		public Duration getConnectionIdleTimeout() {
			return this.connectionIdleTimeout;
		}

		public void setConnectionIdleTimeout(Duration connectionIdleTimeout) {
			this.connectionIdleTimeout = connectionIdleTimeout;
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
			private FORMAT format = FORMAT.NCSA;

			/**
			 * Custom log format, see org.eclipse.jetty.server.CustomRequestLog. If
			 * defined, overrides the "format" configuration key.
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

			public FORMAT getFormat() {
				return this.format;
			}

			public void setFormat(FORMAT format) {
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
			public enum FORMAT {

				/**
				 * NCSA format, as defined in CustomRequestLog#NCSA_FORMAT.
				 */
				NCSA,

				/**
				 * Extended NCSA format, as defined in
				 * CustomRequestLog#EXTENDED_NCSA_FORMAT.
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
			 * Maximum number of threads.
			 */
			private Integer max = 200;

			/**
			 * Minimum number of threads.
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

	/**
	 * Netty properties.
	 */
	public static class Netty {

		/**
		 * Connection timeout of the Netty channel.
		 */
		private Duration connectionTimeout;

		/**
		 * Maximum content length of an H2C upgrade request.
		 */
		private DataSize h2cMaxContentLength = DataSize.ofBytes(0);

		/**
		 * Initial buffer size for HTTP request decoding.
		 */
		private DataSize initialBufferSize = DataSize.ofBytes(128);

		/**
		 * Maximum chunk size that can be decoded for an HTTP request.
		 */
		private DataSize maxChunkSize = DataSize.ofKilobytes(8);

		/**
		 * Maximum length that can be decoded for an HTTP request's initial line.
		 */
		private DataSize maxInitialLineLength = DataSize.ofKilobytes(4);

		/**
		 * Whether to validate headers when decoding requests.
		 */
		private boolean validateHeaders = true;

		public Duration getConnectionTimeout() {
			return this.connectionTimeout;
		}

		public void setConnectionTimeout(Duration connectionTimeout) {
			this.connectionTimeout = connectionTimeout;
		}

		public DataSize getH2cMaxContentLength() {
			return this.h2cMaxContentLength;
		}

		public void setH2cMaxContentLength(DataSize h2cMaxContentLength) {
			this.h2cMaxContentLength = h2cMaxContentLength;
		}

		public DataSize getInitialBufferSize() {
			return this.initialBufferSize;
		}

		public void setInitialBufferSize(DataSize initialBufferSize) {
			this.initialBufferSize = initialBufferSize;
		}

		public DataSize getMaxChunkSize() {
			return this.maxChunkSize;
		}

		public void setMaxChunkSize(DataSize maxChunkSize) {
			this.maxChunkSize = maxChunkSize;
		}

		public DataSize getMaxInitialLineLength() {
			return this.maxInitialLineLength;
		}

		public void setMaxInitialLineLength(DataSize maxInitialLineLength) {
			this.maxInitialLineLength = maxInitialLineLength;
		}

		public boolean isValidateHeaders() {
			return this.validateHeaders;
		}

		public void setValidateHeaders(boolean validateHeaders) {
			this.validateHeaders = validateHeaders;
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
		 * Whether to allocate buffers outside the Java heap. The default is derived from
		 * the maximum amount of memory that is available to the JVM.
		 */
		private Boolean directBuffers;

		/**
		 * Whether servlet filters should be initialized on startup.
		 */
		private boolean eagerFilterInit = true;

		/**
		 * Maximum number of query or path parameters that are allowed. This limit exists
		 * to prevent hash collision based DOS attacks.
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
		 * Whether the server should decode percent encoded slash characters. Enabling
		 * encoded slashes can have security implications due to different servers
		 * interpreting the slash differently. Only enable this if you have a legacy
		 * application that requires it.
		 */
		private boolean allowEncodedSlash = false;

		/**
		 * Whether the URL should be decoded. When disabled, percent-encoded characters in
		 * the URL will be left as-is.
		 */
		private boolean decodeUrl = true;

		/**
		 * Charset used to decode URLs.
		 */
		private Charset urlCharset = StandardCharsets.UTF_8;

		/**
		 * Whether the 'Connection: keep-alive' header should be added to all responses,
		 * even if not required by the HTTP specification.
		 */
		private boolean alwaysSetKeepAlive = true;

		/**
		 * Amount of time a connection can sit idle without processing a request, before
		 * it is closed by the server.
		 */
		private Duration noRequestTimeout;

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

		public DataSize getBufferSize() {
			return this.bufferSize;
		}

		public void setBufferSize(DataSize bufferSize) {
			this.bufferSize = bufferSize;
		}

		@Deprecated
		@DeprecatedConfigurationProperty(replacement = "server.undertow.threads.io")
		public Integer getIoThreads() {
			return getThreads().getIo();
		}

		@Deprecated
		public void setIoThreads(Integer ioThreads) {
			getThreads().setIo(ioThreads);
		}

		@Deprecated
		@DeprecatedConfigurationProperty(replacement = "server.undertow.threads.worker")
		public Integer getWorkerThreads() {
			return getThreads().getWorker();
		}

		@Deprecated
		public void setWorkerThreads(Integer workerThreads) {
			getThreads().setWorker(workerThreads);
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

		public boolean isAllowEncodedSlash() {
			return this.allowEncodedSlash;
		}

		public void setAllowEncodedSlash(boolean allowEncodedSlash) {
			this.allowEncodedSlash = allowEncodedSlash;
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

		public Duration getNoRequestTimeout() {
			return this.noRequestTimeout;
		}

		public void setNoRequestTimeout(Duration noRequestTimeout) {
			this.noRequestTimeout = noRequestTimeout;
		}

		public Accesslog getAccesslog() {
			return this.accesslog;
		}

		public Threads getThreads() {
			return this.threads;
		}

		public Options getOptions() {
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
			 * Number of I/O threads to create for the worker. The default is derived from
			 * the number of available processors.
			 */
			private Integer io;

			/**
			 * Number of worker threads. The default is 8 times the number of I/O threads.
			 */
			private Integer worker;

			public Integer getIo() {
				return this.io;
			}

			public void setIo(Integer io) {
				this.io = io;
			}

			public Integer getWorker() {
				return this.worker;
			}

			public void setWorker(Integer worker) {
				this.worker = worker;
			}

		}

		public static class Options {

			private Map<String, String> socket = new LinkedHashMap<>();

			private Map<String, String> server = new LinkedHashMap<>();

			public Map<String, String> getServer() {
				return this.server;
			}

			public Map<String, String> getSocket() {
				return this.socket;
			}

		}

	}

	/**
	 * Strategies for supporting forward headers.
	 */
	public enum ForwardHeadersStrategy {

		/**
		 * Use the underlying container's native support for forwarded headers.
		 */
		NATIVE,

		/**
		 * Use Spring's support for handling forwarded headers.
		 */
		FRAMEWORK,

		/**
		 * Ignore X-Forwarded-* headers.
		 */
		NONE

	}

}
