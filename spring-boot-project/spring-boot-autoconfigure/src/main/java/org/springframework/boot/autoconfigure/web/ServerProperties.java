/*
 * Copyright 2012-2024 the original author or authors.
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
import org.springframework.boot.web.server.Cookie;
import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.MimeMappings;
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
 * @author Parviz Rozikov
 * @author Florian Storz
 * @author Michael Weidmann
 * @author Lasse Wulff
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
	 * Maximum size of the HTTP request header.
	 */
	private DataSize maxHttpRequestHeaderSize = DataSize.ofKilobytes(8);

	/**
	 * Type of shutdown that the server will support.
	 */
	private Shutdown shutdown = Shutdown.IMMEDIATE;

	@NestedConfigurationProperty
	private Ssl ssl;

	@NestedConfigurationProperty
	private final Compression compression = new Compression();

	/**
	 * Custom MIME mappings in addition to the default MIME mappings.
	 */
	private final MimeMappings mimeMappings = MimeMappings.lazyCopy(MimeMappings.DEFAULT);

	@NestedConfigurationProperty
	private final Http2 http2 = new Http2();

	private final Servlet servlet = new Servlet();

	private final Reactive reactive = new Reactive();

	private final Tomcat tomcat = new Tomcat();

	private final Jetty jetty = new Jetty();

	private final Netty netty = new Netty();

	private final Undertow undertow = new Undertow();

	/**
     * Returns the port number of the server.
     *
     * @return the port number
     */
    public Integer getPort() {
		return this.port;
	}

	/**
     * Sets the port number for the server.
     * 
     * @param port the port number to be set
     */
    public void setPort(Integer port) {
		this.port = port;
	}

	/**
     * Returns the InetAddress object representing the address of the server.
     *
     * @return the InetAddress object representing the address of the server
     */
    public InetAddress getAddress() {
		return this.address;
	}

	/**
     * Sets the address of the server.
     * 
     * @param address the InetAddress object representing the server address
     */
    public void setAddress(InetAddress address) {
		this.address = address;
	}

	/**
     * Returns the server header.
     *
     * @return the server header
     */
    public String getServerHeader() {
		return this.serverHeader;
	}

	/**
     * Sets the server header value.
     * 
     * @param serverHeader the server header value to be set
     */
    public void setServerHeader(String serverHeader) {
		this.serverHeader = serverHeader;
	}

	/**
     * Returns the maximum size of an HTTP request header.
     *
     * @return the maximum size of an HTTP request header
     */
    public DataSize getMaxHttpRequestHeaderSize() {
		return this.maxHttpRequestHeaderSize;
	}

	/**
     * Sets the maximum size of the HTTP request header.
     * 
     * @param maxHttpRequestHeaderSize the maximum size of the HTTP request header
     */
    public void setMaxHttpRequestHeaderSize(DataSize maxHttpRequestHeaderSize) {
		this.maxHttpRequestHeaderSize = maxHttpRequestHeaderSize;
	}

	/**
     * Returns the shutdown object.
     * 
     * @return the shutdown object
     */
    public Shutdown getShutdown() {
		return this.shutdown;
	}

	/**
     * Sets the shutdown object for the ServerProperties class.
     * 
     * @param shutdown the shutdown object to be set
     */
    public void setShutdown(Shutdown shutdown) {
		this.shutdown = shutdown;
	}

	/**
     * Returns the error properties of the server.
     *
     * @return the error properties of the server
     */
    public ErrorProperties getError() {
		return this.error;
	}

	/**
     * Returns the SSL object associated with this ServerProperties instance.
     *
     * @return the SSL object
     */
    public Ssl getSsl() {
		return this.ssl;
	}

	/**
     * Sets the SSL configuration for the server.
     * 
     * @param ssl the SSL configuration to set
     */
    public void setSsl(Ssl ssl) {
		this.ssl = ssl;
	}

	/**
     * Returns the compression type used by the server.
     * 
     * @return the compression type
     */
    public Compression getCompression() {
		return this.compression;
	}

	/**
     * Returns the MimeMappings object associated with this ServerProperties instance.
     * 
     * @return the MimeMappings object
     */
    public MimeMappings getMimeMappings() {
		return this.mimeMappings;
	}

	/**
     * Sets the custom MIME mappings for the server.
     * 
     * @param customMappings a map containing the custom MIME mappings to be set
     */
    public void setMimeMappings(Map<String, String> customMappings) {
		customMappings.forEach(this.mimeMappings::add);
	}

	/**
     * Returns the Http2 object associated with this ServerProperties instance.
     *
     * @return the Http2 object
     */
    public Http2 getHttp2() {
		return this.http2;
	}

	/**
     * Returns the servlet associated with this ServerProperties object.
     *
     * @return the servlet associated with this ServerProperties object
     */
    public Servlet getServlet() {
		return this.servlet;
	}

	/**
     * Returns the Reactive object associated with this ServerProperties instance.
     *
     * @return the Reactive object associated with this ServerProperties instance
     */
    public Reactive getReactive() {
		return this.reactive;
	}

	/**
     * Returns the Tomcat instance associated with this ServerProperties object.
     *
     * @return the Tomcat instance
     */
    public Tomcat getTomcat() {
		return this.tomcat;
	}

	/**
     * Returns the Jetty instance associated with this ServerProperties object.
     *
     * @return the Jetty instance
     */
    public Jetty getJetty() {
		return this.jetty;
	}

	/**
     * Returns the Netty object associated with this ServerProperties instance.
     *
     * @return the Netty object
     */
    public Netty getNetty() {
		return this.netty;
	}

	/**
     * Returns the Undertow instance associated with this ServerProperties object.
     *
     * @return the Undertow instance
     */
    public Undertow getUndertow() {
		return this.undertow;
	}

	/**
     * Returns the forward headers strategy used by the server.
     *
     * @return the forward headers strategy
     */
    public ForwardHeadersStrategy getForwardHeadersStrategy() {
		return this.forwardHeadersStrategy;
	}

	/**
     * Sets the forward headers strategy for the server.
     * 
     * @param forwardHeadersStrategy the forward headers strategy to be set
     */
    public void setForwardHeadersStrategy(ForwardHeadersStrategy forwardHeadersStrategy) {
		this.forwardHeadersStrategy = forwardHeadersStrategy;
	}

	/**
	 * Servlet server properties.
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

		/**
         * Returns the context path of the servlet.
         *
         * @return the context path of the servlet
         */
        public String getContextPath() {
			return this.contextPath;
		}

		/**
         * Sets the context path for the servlet.
         * 
         * @param contextPath the context path to be set
         */
        public void setContextPath(String contextPath) {
			this.contextPath = cleanContextPath(contextPath);
		}

		/**
         * Cleans the given context path by removing any leading or trailing whitespace and removing any trailing slash.
         * 
         * @param contextPath the context path to be cleaned
         * @return the cleaned context path, or null if the input is null or empty
         */
        private String cleanContextPath(String contextPath) {
			String candidate = null;
			if (StringUtils.hasLength(contextPath)) {
				candidate = contextPath.strip();
			}
			if (StringUtils.hasText(candidate) && candidate.endsWith("/")) {
				return candidate.substring(0, candidate.length() - 1);
			}
			return candidate;
		}

		/**
         * Returns the display name of the application.
         *
         * @return the display name of the application
         */
        public String getApplicationDisplayName() {
			return this.applicationDisplayName;
		}

		/**
         * Sets the display name of the application.
         * 
         * @param displayName the display name to be set
         */
        public void setApplicationDisplayName(String displayName) {
			this.applicationDisplayName = displayName;
		}

		/**
         * Returns a boolean value indicating whether the default servlet is registered.
         * 
         * @return true if the default servlet is registered, false otherwise
         */
        public boolean isRegisterDefaultServlet() {
			return this.registerDefaultServlet;
		}

		/**
         * Sets whether to register the default servlet.
         * 
         * @param registerDefaultServlet true to register the default servlet, false otherwise
         */
        public void setRegisterDefaultServlet(boolean registerDefaultServlet) {
			this.registerDefaultServlet = registerDefaultServlet;
		}

		/**
         * Returns the context parameters of the servlet.
         * 
         * @return a Map containing the context parameters as key-value pairs
         */
        public Map<String, String> getContextParameters() {
			return this.contextParameters;
		}

		/**
         * Returns the encoding used for the request and response bodies.
         * 
         * @return the encoding used for the request and response bodies
         */
        public Encoding getEncoding() {
			return this.encoding;
		}

		/**
         * Returns the Jsp object associated with this Servlet.
         *
         * @return the Jsp object associated with this Servlet
         */
        public Jsp getJsp() {
			return this.jsp;
		}

		/**
         * Returns the current session associated with this servlet.
         * 
         * @return the current session associated with this servlet
         */
        public Session getSession() {
			return this.session;
		}

	}

	/**
	 * Reactive server properties.
	 */
	public static class Reactive {

		private final Session session = new Session();

		/**
         * Returns the session associated with this Reactive object.
         *
         * @return the session associated with this Reactive object
         */
        public Session getSession() {
			return this.session;
		}

		/**
         * Session class.
         */
        public static class Session {

			/**
			 * Session timeout. If a duration suffix is not specified, seconds will be
			 * used.
			 */
			@DurationUnit(ChronoUnit.SECONDS)
			private Duration timeout = Duration.ofMinutes(30);

			/**
			 * The maximum number of sessions that can be stored.
			 */
			private int maxSessions = 10000;

			@NestedConfigurationProperty
			private final Cookie cookie = new Cookie();

			/**
             * Returns the timeout duration for the session.
             *
             * @return the timeout duration for the session
             */
            public Duration getTimeout() {
				return this.timeout;
			}

			/**
             * Sets the timeout for the session.
             * 
             * @param timeout the duration of the timeout
             */
            public void setTimeout(Duration timeout) {
				this.timeout = timeout;
			}

			/**
             * Returns the maximum number of sessions allowed.
             *
             * @return the maximum number of sessions
             */
            public int getMaxSessions() {
				return this.maxSessions;
			}

			/**
             * Sets the maximum number of sessions allowed.
             * 
             * @param maxSessions the maximum number of sessions to be set
             */
            public void setMaxSessions(int maxSessions) {
				this.maxSessions = maxSessions;
			}

			/**
             * Returns the cookie associated with this session.
             *
             * @return the cookie associated with this session
             */
            public Cookie getCookie() {
				return this.cookie;
			}

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
		 * the path. When using SSL terminated at a proxy, this property should be set to
		 * false.
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
		 * Time to wait for another HTTP request before the connection is closed. When not
		 * set the connectionTimeout is used. When set to -1 there will be no timeout.
		 */
		private Duration keepAliveTimeout;

		/**
		 * Maximum number of HTTP requests that can be pipelined before the connection is
		 * closed. When set to 0 or 1, keep-alive and pipelining are disabled. When set to
		 * -1, an unlimited number of pipelined or keep-alive requests are allowed.
		 */
		private int maxKeepAliveRequests = 100;

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
		 * Whether to reject requests with illegal header names or values.
		 */
		@Deprecated(since = "2.7.12", forRemoval = true) // Remove in 3.3
		private boolean rejectIllegalHeader = true;

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

		/**
		 * Maximum size of the HTTP response header.
		 */
		private DataSize maxHttpResponseHeaderSize = DataSize.ofKilobytes(8);

		/**
         * Returns the maximum size of an HTTP form post.
         * 
         * @return the maximum size of an HTTP form post
         */
        public DataSize getMaxHttpFormPostSize() {
			return this.maxHttpFormPostSize;
		}

		/**
         * Sets the maximum size of the HTTP form post data that will be accepted by this Tomcat instance.
         * 
         * @param maxHttpFormPostSize the maximum size of the HTTP form post data
         */
        public void setMaxHttpFormPostSize(DataSize maxHttpFormPostSize) {
			this.maxHttpFormPostSize = maxHttpFormPostSize;
		}

		/**
         * Returns the access log associated with this Tomcat instance.
         *
         * @return the access log
         */
        public Accesslog getAccesslog() {
			return this.accesslog;
		}

		/**
         * Returns the Threads object associated with this Tomcat instance.
         *
         * @return the Threads object
         */
        public Threads getThreads() {
			return this.threads;
		}

		/**
         * Returns the background processor delay.
         * 
         * @return the background processor delay
         */
        public Duration getBackgroundProcessorDelay() {
			return this.backgroundProcessorDelay;
		}

		/**
         * Sets the delay for the background processor.
         * 
         * @param backgroundProcessorDelay the delay for the background processor
         */
        public void setBackgroundProcessorDelay(Duration backgroundProcessorDelay) {
			this.backgroundProcessorDelay = backgroundProcessorDelay;
		}

		/**
         * Returns the base directory.
         * 
         * @return the base directory
         */
        public File getBasedir() {
			return this.basedir;
		}

		/**
         * Sets the base directory for the Tomcat instance.
         * 
         * @param basedir the base directory to be set
         */
        public void setBasedir(File basedir) {
			this.basedir = basedir;
		}

		/**
         * Returns the value of the redirectContextRoot property.
         * 
         * @return true if the redirectContextRoot is enabled, false otherwise
         */
        public Boolean getRedirectContextRoot() {
			return this.redirectContextRoot;
		}

		/**
         * Sets the flag indicating whether to redirect the context root.
         * 
         * @param redirectContextRoot the flag indicating whether to redirect the context root
         */
        public void setRedirectContextRoot(Boolean redirectContextRoot) {
			this.redirectContextRoot = redirectContextRoot;
		}

		/**
         * Returns the value indicating whether relative redirects are used.
         * 
         * @return {@code true} if relative redirects are used, {@code false} otherwise.
         */
        public boolean isUseRelativeRedirects() {
			return this.useRelativeRedirects;
		}

		/**
         * Sets whether to use relative redirects.
         * 
         * @param useRelativeRedirects true to use relative redirects, false otherwise
         */
        public void setUseRelativeRedirects(boolean useRelativeRedirects) {
			this.useRelativeRedirects = useRelativeRedirects;
		}

		/**
         * Returns the character encoding used for decoding URIs.
         * 
         * @return the character encoding used for decoding URIs
         */
        public Charset getUriEncoding() {
			return this.uriEncoding;
		}

		/**
         * Sets the encoding to be used for URI decoding.
         * 
         * @param uriEncoding the encoding to be used for URI decoding
         */
        public void setUriEncoding(Charset uriEncoding) {
			this.uriEncoding = uriEncoding;
		}

		/**
         * Returns the maximum number of connections allowed.
         *
         * @return the maximum number of connections
         */
        public int getMaxConnections() {
			return this.maxConnections;
		}

		/**
         * Sets the maximum number of connections allowed for the Tomcat server.
         * 
         * @param maxConnections the maximum number of connections to be set
         */
        public void setMaxConnections(int maxConnections) {
			this.maxConnections = maxConnections;
		}

		/**
         * Returns the maximum swallow size.
         *
         * @return the maximum swallow size
         */
        public DataSize getMaxSwallowSize() {
			return this.maxSwallowSize;
		}

		/**
         * Sets the maximum swallow size for the Tomcat instance.
         * 
         * @param maxSwallowSize the maximum swallow size to be set
         */
        public void setMaxSwallowSize(DataSize maxSwallowSize) {
			this.maxSwallowSize = maxSwallowSize;
		}

		/**
         * Returns the number of accepted connections.
         *
         * @return the number of accepted connections
         */
        public int getAcceptCount() {
			return this.acceptCount;
		}

		/**
         * Sets the maximum number of requests that can be queued when all request processing threads are busy.
         * 
         * @param acceptCount the maximum number of requests to be queued
         */
        public void setAcceptCount(int acceptCount) {
			this.acceptCount = acceptCount;
		}

		/**
         * Returns the processor cache of the Tomcat.
         *
         * @return the processor cache of the Tomcat
         */
        public int getProcessorCache() {
			return this.processorCache;
		}

		/**
         * Sets the processor cache size for the Tomcat class.
         * 
         * @param processorCache the size of the processor cache to be set
         */
        public void setProcessorCache(int processorCache) {
			this.processorCache = processorCache;
		}

		/**
         * Returns the keep-alive timeout duration.
         *
         * @return the keep-alive timeout duration
         */
        public Duration getKeepAliveTimeout() {
			return this.keepAliveTimeout;
		}

		/**
         * Sets the keep-alive timeout for the Tomcat server.
         * 
         * @param keepAliveTimeout the duration of the keep-alive timeout
         */
        public void setKeepAliveTimeout(Duration keepAliveTimeout) {
			this.keepAliveTimeout = keepAliveTimeout;
		}

		/**
         * Returns the maximum number of keep-alive requests allowed.
         *
         * @return the maximum number of keep-alive requests
         */
        public int getMaxKeepAliveRequests() {
			return this.maxKeepAliveRequests;
		}

		/**
         * Sets the maximum number of keep-alive requests allowed for this Tomcat instance.
         * 
         * @param maxKeepAliveRequests the maximum number of keep-alive requests
         */
        public void setMaxKeepAliveRequests(int maxKeepAliveRequests) {
			this.maxKeepAliveRequests = maxKeepAliveRequests;
		}

		/**
         * Returns the list of additional top-level domain (TLD) skip patterns.
         * 
         * @return the list of additional TLD skip patterns
         */
        public List<String> getAdditionalTldSkipPatterns() {
			return this.additionalTldSkipPatterns;
		}

		/**
         * Sets the additional top-level domain skip patterns.
         * 
         * @param additionalTldSkipPatterns the list of additional top-level domain skip patterns to be set
         */
        public void setAdditionalTldSkipPatterns(List<String> additionalTldSkipPatterns) {
			this.additionalTldSkipPatterns = additionalTldSkipPatterns;
		}

		/**
         * Returns the list of relaxed path characters.
         * 
         * @return the list of relaxed path characters
         */
        public List<Character> getRelaxedPathChars() {
			return this.relaxedPathChars;
		}

		/**
         * Sets the list of relaxed path characters.
         * 
         * @param relaxedPathChars the list of characters to be considered as relaxed path characters
         */
        public void setRelaxedPathChars(List<Character> relaxedPathChars) {
			this.relaxedPathChars = relaxedPathChars;
		}

		/**
         * Returns the list of relaxed query characters.
         * 
         * @return the list of relaxed query characters
         */
        public List<Character> getRelaxedQueryChars() {
			return this.relaxedQueryChars;
		}

		/**
         * Sets the list of relaxed query characters.
         * 
         * @param relaxedQueryChars the list of characters to be considered as relaxed query characters
         */
        public void setRelaxedQueryChars(List<Character> relaxedQueryChars) {
			this.relaxedQueryChars = relaxedQueryChars;
		}

		/**
         * Returns the connection timeout duration.
         *
         * @return the connection timeout duration
         */
        public Duration getConnectionTimeout() {
			return this.connectionTimeout;
		}

		/**
         * Sets the connection timeout for the Tomcat class.
         * 
         * @param connectionTimeout the duration of the connection timeout
         */
        public void setConnectionTimeout(Duration connectionTimeout) {
			this.connectionTimeout = connectionTimeout;
		}

		/**
         * Returns the value of the rejectIllegalHeader property.
         * 
         * @return the value of the rejectIllegalHeader property
         * 
         * @deprecated This method has been deprecated since version 3.2.0 and will be removed in a future release.
         *             The setting has been deprecated in Tomcat.
         *             Please use an alternative method or property instead.
         *             This method may throw UnsupportedOperationException in future releases.
         *             Please refer to the Tomcat documentation for more information.
         * 
         * @since 3.2.0
         */
        @Deprecated(since = "3.2.0", forRemoval = true)
		@DeprecatedConfigurationProperty(reason = "The setting has been deprecated in Tomcat", since = "3.2.0")
		public boolean isRejectIllegalHeader() {
			return this.rejectIllegalHeader;
		}

		/**
         * Sets the flag to reject illegal headers.
         * 
         * @param rejectIllegalHeader the flag indicating whether to reject illegal headers
         * 
         * @deprecated This method has been deprecated since version 3.2.0 and will be removed in a future release.
         *             Please use an alternative method instead.
         */
        @Deprecated(since = "3.2.0", forRemoval = true)
		public void setRejectIllegalHeader(boolean rejectIllegalHeader) {
			this.rejectIllegalHeader = rejectIllegalHeader;
		}

		/**
         * Returns the resource associated with this Tomcat instance.
         *
         * @return the resource associated with this Tomcat instance
         */
        public Resource getResource() {
			return this.resource;
		}

		/**
         * Returns the MBean registry associated with this Tomcat instance.
         * 
         * @return the MBean registry
         */
        public Mbeanregistry getMbeanregistry() {
			return this.mbeanregistry;
		}

		/**
         * Returns the remote IP address.
         *
         * @return the remote IP address
         */
        public Remoteip getRemoteip() {
			return this.remoteip;
		}

		/**
         * Returns the maximum size of the HTTP response header.
         *
         * @return the maximum size of the HTTP response header
         */
        public DataSize getMaxHttpResponseHeaderSize() {
			return this.maxHttpResponseHeaderSize;
		}

		/**
         * Sets the maximum size of the HTTP response header.
         * 
         * @param maxHttpResponseHeaderSize the maximum size of the HTTP response header
         */
        public void setMaxHttpResponseHeaderSize(DataSize maxHttpResponseHeaderSize) {
			this.maxHttpResponseHeaderSize = maxHttpResponseHeaderSize;
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
			 * Whether to check for log file existence so it can be recreated if an
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

			/**
             * Returns the current status of the enabled flag.
             *
             * @return true if the enabled flag is set, false otherwise.
             */
            public boolean isEnabled() {
				return this.enabled;
			}

			/**
             * Sets the enabled status of the Accesslog.
             * 
             * @param enabled the enabled status to be set
             */
            public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			/**
             * Returns the condition used in the if statement.
             *
             * @return the condition used in the if statement
             */
            public String getConditionIf() {
				return this.conditionIf;
			}

			/**
             * Sets the condition for the "if" statement.
             * 
             * @param conditionIf the condition for the "if" statement
             */
            public void setConditionIf(String conditionIf) {
				this.conditionIf = conditionIf;
			}

			/**
             * Returns the value of the conditionUnless property.
             *
             * @return the value of the conditionUnless property
             */
            public String getConditionUnless() {
				return this.conditionUnless;
			}

			/**
             * Sets the conditionUnless property of the Accesslog class.
             * 
             * @param conditionUnless the conditionUnless to set
             */
            public void setConditionUnless(String conditionUnless) {
				this.conditionUnless = conditionUnless;
			}

			/**
             * Returns the pattern used by the Accesslog.
             *
             * @return the pattern used by the Accesslog
             */
            public String getPattern() {
				return this.pattern;
			}

			/**
             * Sets the pattern for the Accesslog.
             * 
             * @param pattern the pattern to be set for the Accesslog
             */
            public void setPattern(String pattern) {
				this.pattern = pattern;
			}

			/**
             * Returns the directory path of the Accesslog.
             *
             * @return the directory path of the Accesslog
             */
            public String getDirectory() {
				return this.directory;
			}

			/**
             * Sets the directory for the Accesslog.
             * 
             * @param directory the directory path to set
             */
            public void setDirectory(String directory) {
				this.directory = directory;
			}

			/**
             * Returns the prefix of the Accesslog.
             *
             * @return the prefix of the Accesslog
             */
            public String getPrefix() {
				return this.prefix;
			}

			/**
             * Sets the prefix for the Accesslog.
             * 
             * @param prefix the prefix to be set for the Accesslog
             */
            public void setPrefix(String prefix) {
				this.prefix = prefix;
			}

			/**
             * Returns the suffix of the Accesslog.
             *
             * @return the suffix of the Accesslog
             */
            public String getSuffix() {
				return this.suffix;
			}

			/**
             * Sets the suffix for the Accesslog.
             * 
             * @param suffix the suffix to be set for the Accesslog
             */
            public void setSuffix(String suffix) {
				this.suffix = suffix;
			}

			/**
             * Returns the encoding used for the Accesslog.
             *
             * @return the encoding used for the Accesslog
             */
            public String getEncoding() {
				return this.encoding;
			}

			/**
             * Sets the encoding for the Accesslog.
             * 
             * @param encoding the encoding to be set
             */
            public void setEncoding(String encoding) {
				this.encoding = encoding;
			}

			/**
             * Returns the locale of the Accesslog.
             *
             * @return the locale of the Accesslog
             */
            public String getLocale() {
				return this.locale;
			}

			/**
             * Sets the locale for the Accesslog.
             * 
             * @param locale the locale to be set
             */
            public void setLocale(String locale) {
				this.locale = locale;
			}

			/**
             * Returns a boolean value indicating whether the check exists or not.
             * 
             * @return true if the check exists, false otherwise
             */
            public boolean isCheckExists() {
				return this.checkExists;
			}

			/**
             * Sets the value of the checkExists flag.
             * 
             * @param checkExists the new value for the checkExists flag
             */
            public void setCheckExists(boolean checkExists) {
				this.checkExists = checkExists;
			}

			/**
             * Returns a boolean value indicating whether the Accesslog is set to rotate.
             * 
             * @return true if the Accesslog is set to rotate, false otherwise.
             */
            public boolean isRotate() {
				return this.rotate;
			}

			/**
             * Sets the rotate flag for the Accesslog.
             * 
             * @param rotate true if the Accesslog should rotate, false otherwise
             */
            public void setRotate(boolean rotate) {
				this.rotate = rotate;
			}

			/**
             * Returns a boolean value indicating whether the file should be renamed on rotation.
             *
             * @return true if the file should be renamed on rotation, false otherwise
             */
            public boolean isRenameOnRotate() {
				return this.renameOnRotate;
			}

			/**
             * Sets the value indicating whether to rename the file on rotation.
             * 
             * @param renameOnRotate true to rename the file on rotation, false otherwise
             */
            public void setRenameOnRotate(boolean renameOnRotate) {
				this.renameOnRotate = renameOnRotate;
			}

			/**
             * Returns the maximum number of days.
             *
             * @return the maximum number of days
             */
            public int getMaxDays() {
				return this.maxDays;
			}

			/**
             * Sets the maximum number of days for the Accesslog.
             * 
             * @param maxDays the maximum number of days to be set
             */
            public void setMaxDays(int maxDays) {
				this.maxDays = maxDays;
			}

			/**
             * Returns the file date format used by the Accesslog class.
             * 
             * @return the file date format
             */
            public String getFileDateFormat() {
				return this.fileDateFormat;
			}

			/**
             * Sets the file date format for the Accesslog class.
             * 
             * @param fileDateFormat the file date format to be set
             */
            public void setFileDateFormat(String fileDateFormat) {
				this.fileDateFormat = fileDateFormat;
			}

			/**
             * Returns a boolean value indicating whether the IPv6 address is in canonical form.
             *
             * @return true if the IPv6 address is in canonical form, false otherwise.
             */
            public boolean isIpv6Canonical() {
				return this.ipv6Canonical;
			}

			/**
             * Sets the flag indicating whether the IPv6 address should be displayed in canonical form.
             * 
             * @param ipv6Canonical true if the IPv6 address should be displayed in canonical form, false otherwise
             */
            public void setIpv6Canonical(boolean ipv6Canonical) {
				this.ipv6Canonical = ipv6Canonical;
			}

			/**
             * Returns a boolean value indicating whether the request attributes are enabled.
             * 
             * @return true if the request attributes are enabled, false otherwise
             */
            public boolean isRequestAttributesEnabled() {
				return this.requestAttributesEnabled;
			}

			/**
             * Sets whether request attributes are enabled for the Accesslog class.
             * 
             * @param requestAttributesEnabled true if request attributes are enabled, false otherwise
             */
            public void setRequestAttributesEnabled(boolean requestAttributesEnabled) {
				this.requestAttributesEnabled = requestAttributesEnabled;
			}

			/**
             * Returns a boolean value indicating whether the Accesslog is buffered or not.
             *
             * @return true if the Accesslog is buffered, false otherwise.
             */
            public boolean isBuffered() {
				return this.buffered;
			}

			/**
             * Sets the value indicating whether the Accesslog is buffered or not.
             * 
             * @param buffered the value indicating whether the Accesslog is buffered or not
             */
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

			/**
			 * Maximum capacity of the thread pool's backing queue.
			 */
			private int maxQueueCapacity = 2147483647;

			/**
             * Returns the maximum value.
             *
             * @return the maximum value
             */
            public int getMax() {
				return this.max;
			}

			/**
             * Sets the maximum value for the Threads class.
             * 
             * @param max the maximum value to be set
             */
            public void setMax(int max) {
				this.max = max;
			}

			/**
             * Returns the minimum spare value.
             *
             * @return the minimum spare value
             */
            public int getMinSpare() {
				return this.minSpare;
			}

			/**
             * Sets the minimum number of spare threads.
             * 
             * @param minSpare the minimum number of spare threads to set
             */
            public void setMinSpare(int minSpare) {
				this.minSpare = minSpare;
			}

			/**
             * Returns the maximum capacity of the queue.
             *
             * @return the maximum capacity of the queue
             */
            public int getMaxQueueCapacity() {
				return this.maxQueueCapacity;
			}

			/**
             * Sets the maximum capacity of the queue.
             * 
             * @param maxQueueCapacity the maximum capacity of the queue
             */
            public void setMaxQueueCapacity(int maxQueueCapacity) {
				this.maxQueueCapacity = maxQueueCapacity;
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

			/**
             * Returns a boolean value indicating whether caching is allowed for this resource.
             *
             * @return {@code true} if caching is allowed, {@code false} otherwise.
             */
            public boolean isAllowCaching() {
				return this.allowCaching;
			}

			/**
             * Sets whether caching is allowed for the resource.
             * 
             * @param allowCaching true if caching is allowed, false otherwise
             */
            public void setAllowCaching(boolean allowCaching) {
				this.allowCaching = allowCaching;
			}

			/**
             * Returns the time-to-live (TTL) duration for the cache.
             *
             * @return the cache TTL duration
             */
            public Duration getCacheTtl() {
				return this.cacheTtl;
			}

			/**
             * Sets the time-to-live (TTL) for the cache.
             * 
             * @param cacheTtl the duration of time that the cache should be considered valid
             */
            public void setCacheTtl(Duration cacheTtl) {
				this.cacheTtl = cacheTtl;
			}

		}

		/**
         * Mbeanregistry class.
         */
        public static class Mbeanregistry {

			/**
			 * Whether Tomcat's MBean Registry should be enabled.
			 */
			private boolean enabled;

			/**
             * Returns the current status of the enabled flag.
             *
             * @return true if the flag is enabled, false otherwise.
             */
            public boolean isEnabled() {
				return this.enabled;
			}

			/**
             * Sets the enabled status of the Mbeanregistry.
             * 
             * @param enabled the enabled status to be set
             */
            public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

		}

		/**
         * Remoteip class.
         */
        public static class Remoteip {

			/**
			 * Regular expression that matches proxies that are to be trusted.
			 */
			private String internalProxies = "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|" // 10/8
					+ "192\\.168\\.\\d{1,3}\\.\\d{1,3}|" // 192.168/16
					+ "169\\.254\\.\\d{1,3}\\.\\d{1,3}|" // 169.254/16
					+ "127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|" // 127/8
					+ "100\\.6[4-9]{1}\\.\\d{1,3}\\.\\d{1,3}|" // 100.64.0.0/10
					+ "100\\.[7-9]{1}\\d{1}\\.\\d{1,3}\\.\\d{1,3}|" // 100.64.0.0/10
					+ "100\\.1[0-1]{1}\\d{1}\\.\\d{1,3}\\.\\d{1,3}|" // 100.64.0.0/10
					+ "100\\.12[0-7]{1}\\.\\d{1,3}\\.\\d{1,3}|" // 100.64.0.0/10
					+ "172\\.1[6-9]{1}\\.\\d{1,3}\\.\\d{1,3}|" // 172.16/12
					+ "172\\.2[0-9]{1}\\.\\d{1,3}\\.\\d{1,3}|" // 172.16/12
					+ "172\\.3[0-1]{1}\\.\\d{1,3}\\.\\d{1,3}|" // 172.16/12
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
			 * instance, 'X-FORWARDED-FOR'.
			 */
			private String remoteIpHeader;

			/**
			 * Regular expression defining proxies that are trusted when they appear in
			 * the "remote-ip-header" header.
			 */
			private String trustedProxies;

			/**
             * Returns the internal proxies used by the Remoteip class.
             * 
             * @return the internal proxies as a String
             */
            public String getInternalProxies() {
				return this.internalProxies;
			}

			/**
             * Sets the internal proxies for the Remoteip class.
             * 
             * @param internalProxies the internal proxies to be set
             */
            public void setInternalProxies(String internalProxies) {
				this.internalProxies = internalProxies;
			}

			/**
             * Returns the protocol header of the Remoteip object.
             * 
             * @return the protocol header of the Remoteip object
             */
            public String getProtocolHeader() {
				return this.protocolHeader;
			}

			/**
             * Sets the protocol header for the Remoteip class.
             * 
             * @param protocolHeader the protocol header to be set
             */
            public void setProtocolHeader(String protocolHeader) {
				this.protocolHeader = protocolHeader;
			}

			/**
             * Returns the value of the protocol header for HTTPS requests.
             * 
             * @return the value of the protocol header for HTTPS requests
             */
            public String getProtocolHeaderHttpsValue() {
				return this.protocolHeaderHttpsValue;
			}

			/**
             * Returns the value of the host header.
             *
             * @return the value of the host header
             */
            public String getHostHeader() {
				return this.hostHeader;
			}

			/**
             * Sets the value of the Host header for the Remoteip class.
             * 
             * @param hostHeader the value to be set as the Host header
             */
            public void setHostHeader(String hostHeader) {
				this.hostHeader = hostHeader;
			}

			/**
             * Sets the value of the protocol header for HTTPS requests.
             * 
             * @param protocolHeaderHttpsValue the value of the protocol header for HTTPS requests
             */
            public void setProtocolHeaderHttpsValue(String protocolHeaderHttpsValue) {
				this.protocolHeaderHttpsValue = protocolHeaderHttpsValue;
			}

			/**
             * Returns the port header value.
             *
             * @return the port header value
             */
            public String getPortHeader() {
				return this.portHeader;
			}

			/**
             * Sets the port header value for the Remoteip class.
             * 
             * @param portHeader the port header value to be set
             */
            public void setPortHeader(String portHeader) {
				this.portHeader = portHeader;
			}

			/**
             * Returns the remote IP header.
             *
             * @return the remote IP header
             */
            public String getRemoteIpHeader() {
				return this.remoteIpHeader;
			}

			/**
             * Sets the value of the remote IP header.
             * 
             * @param remoteIpHeader the remote IP header value to be set
             */
            public void setRemoteIpHeader(String remoteIpHeader) {
				this.remoteIpHeader = remoteIpHeader;
			}

			/**
             * Returns the trusted proxies used by the Remoteip class.
             * 
             * @return the trusted proxies as a String
             */
            public String getTrustedProxies() {
				return this.trustedProxies;
			}

			/**
             * Sets the trusted proxies for the Remoteip class.
             * 
             * @param trustedProxies the trusted proxies to be set
             */
            public void setTrustedProxies(String trustedProxies) {
				this.trustedProxies = trustedProxies;
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

		/**
		 * Maximum size of the HTTP response header.
		 */
		private DataSize maxHttpResponseHeaderSize = DataSize.ofKilobytes(8);

		/**
		 * Maximum number of connections that the server accepts and processes at any
		 * given time.
		 */
		private int maxConnections = -1;

		/**
         * Returns the access log associated with this Jetty instance.
         *
         * @return the access log
         */
        public Accesslog getAccesslog() {
			return this.accesslog;
		}

		/**
         * Returns the Threads object associated with this Jetty instance.
         *
         * @return the Threads object
         */
        public Threads getThreads() {
			return this.threads;
		}

		/**
         * Returns the maximum size of an HTTP form post.
         * 
         * @return the maximum size of an HTTP form post
         */
        public DataSize getMaxHttpFormPostSize() {
			return this.maxHttpFormPostSize;
		}

		/**
         * Sets the maximum size of an HTTP form post request.
         * 
         * @param maxHttpFormPostSize the maximum size of an HTTP form post request
         */
        public void setMaxHttpFormPostSize(DataSize maxHttpFormPostSize) {
			this.maxHttpFormPostSize = maxHttpFormPostSize;
		}

		/**
         * Returns the connection idle timeout.
         * 
         * @return the connection idle timeout
         */
        public Duration getConnectionIdleTimeout() {
			return this.connectionIdleTimeout;
		}

		/**
         * Sets the idle timeout for connections.
         * 
         * @param connectionIdleTimeout the duration of idle time after which a connection will be closed
         */
        public void setConnectionIdleTimeout(Duration connectionIdleTimeout) {
			this.connectionIdleTimeout = connectionIdleTimeout;
		}

		/**
         * Returns the maximum size of the HTTP response header.
         *
         * @return the maximum size of the HTTP response header
         */
        public DataSize getMaxHttpResponseHeaderSize() {
			return this.maxHttpResponseHeaderSize;
		}

		/**
         * Sets the maximum size of the HTTP response header.
         * 
         * @param maxHttpResponseHeaderSize the maximum size of the HTTP response header
         */
        public void setMaxHttpResponseHeaderSize(DataSize maxHttpResponseHeaderSize) {
			this.maxHttpResponseHeaderSize = maxHttpResponseHeaderSize;
		}

		/**
         * Returns the maximum number of connections allowed.
         *
         * @return the maximum number of connections
         */
        public int getMaxConnections() {
			return this.maxConnections;
		}

		/**
         * Sets the maximum number of connections allowed.
         * 
         * @param maxConnections the maximum number of connections allowed
         */
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

			/**
             * Returns the current status of the enabled flag.
             *
             * @return true if the enabled flag is set, false otherwise.
             */
            public boolean isEnabled() {
				return this.enabled;
			}

			/**
             * Sets the enabled status of the Accesslog.
             * 
             * @param enabled the enabled status to be set
             */
            public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			/**
             * Returns the format of the Accesslog.
             *
             * @return the format of the Accesslog
             */
            public FORMAT getFormat() {
				return this.format;
			}

			/**
             * Sets the format of the Accesslog.
             * 
             * @param format the format to set
             */
            public void setFormat(FORMAT format) {
				this.format = format;
			}

			/**
             * Returns the custom format of the Accesslog.
             *
             * @return the custom format of the Accesslog
             */
            public String getCustomFormat() {
				return this.customFormat;
			}

			/**
             * Sets the custom format for the Accesslog.
             * 
             * @param customFormat the custom format to be set
             */
            public void setCustomFormat(String customFormat) {
				this.customFormat = customFormat;
			}

			/**
             * Returns the filename associated with the Accesslog object.
             *
             * @return the filename of the Accesslog object
             */
            public String getFilename() {
				return this.filename;
			}

			/**
             * Sets the filename for the Accesslog.
             * 
             * @param filename the name of the file to be set
             */
            public void setFilename(String filename) {
				this.filename = filename;
			}

			/**
             * Returns the file date format used by the Accesslog class.
             * 
             * @return the file date format
             */
            public String getFileDateFormat() {
				return this.fileDateFormat;
			}

			/**
             * Sets the file date format for the Accesslog class.
             * 
             * @param fileDateFormat the file date format to be set
             */
            public void setFileDateFormat(String fileDateFormat) {
				this.fileDateFormat = fileDateFormat;
			}

			/**
             * Returns the retention period of the Accesslog.
             *
             * @return the retention period in days
             */
            public int getRetentionPeriod() {
				return this.retentionPeriod;
			}

			/**
             * Sets the retention period for the access log.
             * 
             * @param retentionPeriod the retention period in days
             */
            public void setRetentionPeriod(int retentionPeriod) {
				this.retentionPeriod = retentionPeriod;
			}

			/**
             * Returns a boolean value indicating whether the append flag is set.
             * 
             * @return true if the append flag is set, false otherwise
             */
            public boolean isAppend() {
				return this.append;
			}

			/**
             * Sets the append flag for the Accesslog.
             * 
             * @param append the boolean value indicating whether to append to an existing log file or create a new one
             */
            public void setAppend(boolean append) {
				this.append = append;
			}

			/**
             * Returns the list of ignore paths.
             *
             * @return the list of ignore paths
             */
            public List<String> getIgnorePaths() {
				return this.ignorePaths;
			}

			/**
             * Sets the list of paths to be ignored.
             * 
             * @param ignorePaths the list of paths to be ignored
             */
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

			/**
             * Returns the number of acceptors.
             *
             * @return the number of acceptors
             */
            public Integer getAcceptors() {
				return this.acceptors;
			}

			/**
             * Sets the number of acceptors for the Threads class.
             * 
             * @param acceptors the number of acceptors to be set
             */
            public void setAcceptors(Integer acceptors) {
				this.acceptors = acceptors;
			}

			/**
             * Returns the number of selectors.
             *
             * @return the number of selectors
             */
            public Integer getSelectors() {
				return this.selectors;
			}

			/**
             * Sets the number of selectors for the Threads class.
             * 
             * @param selectors the number of selectors to be set
             */
            public void setSelectors(Integer selectors) {
				this.selectors = selectors;
			}

			/**
             * Sets the minimum value for the thread.
             * 
             * @param min the minimum value to be set
             */
            public void setMin(Integer min) {
				this.min = min;
			}

			/**
             * Returns the minimum value.
             *
             * @return the minimum value
             */
            public Integer getMin() {
				return this.min;
			}

			/**
             * Sets the maximum value for the thread.
             * 
             * @param max the maximum value to be set
             */
            public void setMax(Integer max) {
				this.max = max;
			}

			/**
             * Returns the maximum value.
             *
             * @return the maximum value
             */
            public Integer getMax() {
				return this.max;
			}

			/**
             * Returns the maximum capacity of the queue.
             *
             * @return the maximum capacity of the queue
             */
            public Integer getMaxQueueCapacity() {
				return this.maxQueueCapacity;
			}

			/**
             * Sets the maximum capacity of the queue.
             * 
             * @param maxQueueCapacity the maximum capacity of the queue
             */
            public void setMaxQueueCapacity(Integer maxQueueCapacity) {
				this.maxQueueCapacity = maxQueueCapacity;
			}

			/**
             * Sets the idle timeout for the thread.
             * 
             * @param idleTimeout the duration of idle time before the thread times out
             */
            public void setIdleTimeout(Duration idleTimeout) {
				this.idleTimeout = idleTimeout;
			}

			/**
             * Returns the idle timeout duration.
             *
             * @return the idle timeout duration
             */
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
		 * Maximum length that can be decoded for an HTTP request's initial line.
		 */
		private DataSize maxInitialLineLength = DataSize.ofKilobytes(4);

		/**
		 * Maximum number of requests that can be made per connection. By default, a
		 * connection serves unlimited number of requests.
		 */
		private Integer maxKeepAliveRequests;

		/**
		 * Whether to validate headers when decoding requests.
		 */
		private boolean validateHeaders = true;

		/**
		 * Idle timeout of the Netty channel. When not specified, an infinite timeout is
		 * used.
		 */
		private Duration idleTimeout;

		/**
         * Returns the connection timeout duration.
         *
         * @return the connection timeout duration
         */
        public Duration getConnectionTimeout() {
			return this.connectionTimeout;
		}

		/**
         * Sets the connection timeout for the Netty class.
         * 
         * @param connectionTimeout the duration of the connection timeout
         */
        public void setConnectionTimeout(Duration connectionTimeout) {
			this.connectionTimeout = connectionTimeout;
		}

		/**
         * Returns the maximum content length for HTTP/2 cleartext (h2c) connections.
         * 
         * @return the maximum content length for h2c connections
         */
        public DataSize getH2cMaxContentLength() {
			return this.h2cMaxContentLength;
		}

		/**
         * Sets the maximum content length for HTTP/2 cleartext (non-encrypted) connections.
         * 
         * @param h2cMaxContentLength the maximum content length for HTTP/2 cleartext connections
         */
        public void setH2cMaxContentLength(DataSize h2cMaxContentLength) {
			this.h2cMaxContentLength = h2cMaxContentLength;
		}

		/**
         * Returns the initial buffer size.
         *
         * @return the initial buffer size
         */
        public DataSize getInitialBufferSize() {
			return this.initialBufferSize;
		}

		/**
         * Sets the initial buffer size for the Netty class.
         * 
         * @param initialBufferSize the initial buffer size to be set
         */
        public void setInitialBufferSize(DataSize initialBufferSize) {
			this.initialBufferSize = initialBufferSize;
		}

		/**
         * Returns the maximum length of the initial line in a data size format.
         *
         * @return the maximum length of the initial line
         */
        public DataSize getMaxInitialLineLength() {
			return this.maxInitialLineLength;
		}

		/**
         * Sets the maximum initial line length for this Netty class.
         * 
         * @param maxInitialLineLength the maximum initial line length to be set
         */
        public void setMaxInitialLineLength(DataSize maxInitialLineLength) {
			this.maxInitialLineLength = maxInitialLineLength;
		}

		/**
         * Returns the maximum number of keep-alive requests allowed.
         *
         * @return the maximum number of keep-alive requests
         */
        public Integer getMaxKeepAliveRequests() {
			return this.maxKeepAliveRequests;
		}

		/**
         * Sets the maximum number of keep-alive requests allowed.
         * 
         * @param maxKeepAliveRequests the maximum number of keep-alive requests allowed
         */
        public void setMaxKeepAliveRequests(Integer maxKeepAliveRequests) {
			this.maxKeepAliveRequests = maxKeepAliveRequests;
		}

		/**
         * Returns the flag indicating whether to validate headers.
         *
         * @return {@code true} if headers should be validated, {@code false} otherwise
         */
        public boolean isValidateHeaders() {
			return this.validateHeaders;
		}

		/**
         * Sets whether to validate headers.
         * 
         * @param validateHeaders true to validate headers, false otherwise
         */
        public void setValidateHeaders(boolean validateHeaders) {
			this.validateHeaders = validateHeaders;
		}

		/**
         * Returns the idle timeout duration.
         *
         * @return the idle timeout duration
         */
        public Duration getIdleTimeout() {
			return this.idleTimeout;
		}

		/**
         * Sets the idle timeout for the connection.
         * 
         * @param idleTimeout the duration of idle time after which the connection will be closed
         */
        public void setIdleTimeout(Duration idleTimeout) {
			this.idleTimeout = idleTimeout;
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
		 * application that requires it. Has no effect when server.undertow.decode-slash
		 * is set.
		 */
		private boolean allowEncodedSlash = false;

		/**
		 * Whether encoded slash characters (%2F) should be decoded. Decoding can cause
		 * security problems if a front-end proxy does not perform the same decoding. Only
		 * enable this if you have a legacy application that requires it. When set,
		 * server.undertow.allow-encoded-slash has no effect.
		 */
		private Boolean decodeSlash;

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

		/**
         * Returns the maximum size of an HTTP POST request that can be handled by the server.
         *
         * @return the maximum size of an HTTP POST request
         */
        public DataSize getMaxHttpPostSize() {
			return this.maxHttpPostSize;
		}

		/**
         * Sets the maximum size of an HTTP POST request that can be processed by the server.
         * 
         * @param maxHttpPostSize the maximum size of an HTTP POST request
         */
        public void setMaxHttpPostSize(DataSize maxHttpPostSize) {
			this.maxHttpPostSize = maxHttpPostSize;
		}

		/**
         * Returns the size of the buffer used by the Undertow server.
         *
         * @return the size of the buffer used by the Undertow server
         */
        public DataSize getBufferSize() {
			return this.bufferSize;
		}

		/**
         * Sets the buffer size for this Undertow instance.
         * 
         * @param bufferSize the buffer size to be set
         */
        public void setBufferSize(DataSize bufferSize) {
			this.bufferSize = bufferSize;
		}

		/**
         * Returns the value of the directBuffers property.
         *
         * @return the value of the directBuffers property
         */
        public Boolean getDirectBuffers() {
			return this.directBuffers;
		}

		/**
         * Sets whether to use direct buffers for this Undertow instance.
         * 
         * @param directBuffers a boolean value indicating whether to use direct buffers
         */
        public void setDirectBuffers(Boolean directBuffers) {
			this.directBuffers = directBuffers;
		}

		/**
         * Returns a boolean value indicating whether the eager filter initialization is enabled.
         *
         * @return {@code true} if eager filter initialization is enabled, {@code false} otherwise
         */
        public boolean isEagerFilterInit() {
			return this.eagerFilterInit;
		}

		/**
         * Sets the flag indicating whether the filter initialization should be eager or lazy.
         * 
         * @param eagerFilterInit the flag indicating whether the filter initialization should be eager or lazy
         */
        public void setEagerFilterInit(boolean eagerFilterInit) {
			this.eagerFilterInit = eagerFilterInit;
		}

		/**
         * Returns the maximum number of parameters allowed.
         *
         * @return the maximum number of parameters
         */
        public int getMaxParameters() {
			return this.maxParameters;
		}

		/**
         * Sets the maximum number of parameters that can be passed in a request.
         * 
         * @param maxParameters the maximum number of parameters to be set
         */
        public void setMaxParameters(Integer maxParameters) {
			this.maxParameters = maxParameters;
		}

		/**
         * Returns the maximum number of headers allowed in a request.
         *
         * @return the maximum number of headers
         */
        public int getMaxHeaders() {
			return this.maxHeaders;
		}

		/**
         * Sets the maximum number of headers allowed in a HTTP request.
         * 
         * @param maxHeaders the maximum number of headers allowed
         */
        public void setMaxHeaders(int maxHeaders) {
			this.maxHeaders = maxHeaders;
		}

		/**
         * Returns the maximum number of cookies allowed.
         *
         * @return the maximum number of cookies
         */
        public Integer getMaxCookies() {
			return this.maxCookies;
		}

		/**
         * Sets the maximum number of cookies that can be stored.
         * 
         * @param maxCookies the maximum number of cookies
         */
        public void setMaxCookies(Integer maxCookies) {
			this.maxCookies = maxCookies;
		}

		/**
         * Gets the value of the allowEncodedSlash property.
         *
         * @return the value of the allowEncodedSlash property
         * @deprecated This property is deprecated and will be removed in version 3.0.3. Use the replacement property "server.undertow.decode-slash" instead.
         * @since 3.0.3
         */
        @DeprecatedConfigurationProperty(replacement = "server.undertow.decode-slash", since = "3.0.3")
		@Deprecated(forRemoval = true, since = "3.0.3")
		public boolean isAllowEncodedSlash() {
			return this.allowEncodedSlash;
		}

		/**
         * Sets whether or not to allow encoded slashes in the URL.
         *
         * @param allowEncodedSlash true to allow encoded slashes, false otherwise
         * @deprecated This method is deprecated and will be removed in version 3.0.3.
         *             Please use an alternative method instead.
         */
        @Deprecated(forRemoval = true, since = "3.0.3")
		public void setAllowEncodedSlash(boolean allowEncodedSlash) {
			this.allowEncodedSlash = allowEncodedSlash;
		}

		/**
         * Returns the value of the decodeSlash property.
         *
         * @return the value of the decodeSlash property
         */
        public Boolean getDecodeSlash() {
			return this.decodeSlash;
		}

		/**
         * Sets whether or not to decode slashes in the URL.
         * 
         * @param decodeSlash true to decode slashes, false otherwise
         */
        public void setDecodeSlash(Boolean decodeSlash) {
			this.decodeSlash = decodeSlash;
		}

		/**
         * Returns whether the URL should be decoded.
         *
         * @return {@code true} if the URL should be decoded, {@code false} otherwise
         */
        public boolean isDecodeUrl() {
			return this.decodeUrl;
		}

		/**
         * Sets whether the URL should be decoded or not.
         * 
         * @param decodeUrl a boolean value indicating whether the URL should be decoded
         */
        public void setDecodeUrl(Boolean decodeUrl) {
			this.decodeUrl = decodeUrl;
		}

		/**
         * Returns the character set used for decoding the URL.
         *
         * @return the character set used for decoding the URL
         */
        public Charset getUrlCharset() {
			return this.urlCharset;
		}

		/**
         * Sets the character encoding for the URL.
         * 
         * @param urlCharset the character encoding for the URL
         */
        public void setUrlCharset(Charset urlCharset) {
			this.urlCharset = urlCharset;
		}

		/**
         * Returns a boolean value indicating whether the keep-alive option is always set.
         *
         * @return true if the keep-alive option is always set, false otherwise
         */
        public boolean isAlwaysSetKeepAlive() {
			return this.alwaysSetKeepAlive;
		}

		/**
         * Sets whether to always set the keep-alive header in HTTP responses.
         * 
         * @param alwaysSetKeepAlive true to always set the keep-alive header, false otherwise
         */
        public void setAlwaysSetKeepAlive(boolean alwaysSetKeepAlive) {
			this.alwaysSetKeepAlive = alwaysSetKeepAlive;
		}

		/**
         * Returns the timeout duration for no requests in the Undertow class.
         *
         * @return the timeout duration for no requests
         */
        public Duration getNoRequestTimeout() {
			return this.noRequestTimeout;
		}

		/**
         * Sets the timeout duration for no request.
         * 
         * @param noRequestTimeout the timeout duration for no request
         */
        public void setNoRequestTimeout(Duration noRequestTimeout) {
			this.noRequestTimeout = noRequestTimeout;
		}

		/**
         * Returns a boolean value indicating whether the path should be preserved on forward.
         *
         * @return true if the path should be preserved on forward, false otherwise
         */
        public boolean isPreservePathOnForward() {
			return this.preservePathOnForward;
		}

		/**
         * Sets whether to preserve the path on forward.
         * 
         * @param preservePathOnForward true to preserve the path on forward, false otherwise
         */
        public void setPreservePathOnForward(boolean preservePathOnForward) {
			this.preservePathOnForward = preservePathOnForward;
		}

		/**
         * Returns the access log associated with this Undertow instance.
         *
         * @return the access log
         */
        public Accesslog getAccesslog() {
			return this.accesslog;
		}

		/**
         * Returns the Threads object associated with this Undertow instance.
         *
         * @return the Threads object
         */
        public Threads getThreads() {
			return this.threads;
		}

		/**
         * Returns the options associated with this Undertow instance.
         *
         * @return the options associated with this Undertow instance
         */
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

			/**
             * Returns the current status of the enabled flag.
             *
             * @return true if the enabled flag is set, false otherwise.
             */
            public boolean isEnabled() {
				return this.enabled;
			}

			/**
             * Sets the enabled status of the Accesslog.
             * 
             * @param enabled the enabled status to be set
             */
            public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			/**
             * Returns the pattern used by the Accesslog.
             *
             * @return the pattern used by the Accesslog
             */
            public String getPattern() {
				return this.pattern;
			}

			/**
             * Sets the pattern for the Accesslog.
             * 
             * @param pattern the pattern to be set for the Accesslog
             */
            public void setPattern(String pattern) {
				this.pattern = pattern;
			}

			/**
             * Returns the prefix of the Accesslog.
             *
             * @return the prefix of the Accesslog
             */
            public String getPrefix() {
				return this.prefix;
			}

			/**
             * Sets the prefix for the Accesslog.
             * 
             * @param prefix the prefix to be set for the Accesslog
             */
            public void setPrefix(String prefix) {
				this.prefix = prefix;
			}

			/**
             * Returns the suffix of the Accesslog.
             *
             * @return the suffix of the Accesslog
             */
            public String getSuffix() {
				return this.suffix;
			}

			/**
             * Sets the suffix for the Accesslog.
             * 
             * @param suffix the suffix to be set for the Accesslog
             */
            public void setSuffix(String suffix) {
				this.suffix = suffix;
			}

			/**
             * Returns the directory associated with the Accesslog object.
             * 
             * @return the directory associated with the Accesslog object
             */
            public File getDir() {
				return this.dir;
			}

			/**
             * Sets the directory for the Accesslog.
             * 
             * @param dir the directory to set
             */
            public void setDir(File dir) {
				this.dir = dir;
			}

			/**
             * Returns a boolean value indicating whether the Accesslog is set to rotate.
             * 
             * @return true if the Accesslog is set to rotate, false otherwise.
             */
            public boolean isRotate() {
				return this.rotate;
			}

			/**
             * Sets the rotate flag for the Accesslog.
             * 
             * @param rotate true if the Accesslog should rotate, false otherwise
             */
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

			/**
             * Returns the value of the io variable.
             *
             * @return the value of the io variable
             */
            public Integer getIo() {
				return this.io;
			}

			/**
             * Sets the value of the io property.
             * 
             * @param io the new value for the io property
             */
            public void setIo(Integer io) {
				this.io = io;
			}

			/**
             * Returns the number of workers.
             *
             * @return the number of workers
             */
            public Integer getWorker() {
				return this.worker;
			}

			/**
             * Sets the number of workers for the Threads class.
             * 
             * @param worker the number of workers to be set
             */
            public void setWorker(Integer worker) {
				this.worker = worker;
			}

		}

		/**
         * Options class.
         */
        public static class Options {

			/**
			 * Socket options as defined in org.xnio.Options.
			 */
			private final Map<String, String> socket = new LinkedHashMap<>();

			/**
			 * Server options as defined in io.undertow.UndertowOptions.
			 */
			private final Map<String, String> server = new LinkedHashMap<>();

			/**
             * Returns the server map.
             *
             * @return the server map
             */
            public Map<String, String> getServer() {
				return this.server;
			}

			/**
             * Returns the socket map.
             *
             * @return the socket map
             */
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
