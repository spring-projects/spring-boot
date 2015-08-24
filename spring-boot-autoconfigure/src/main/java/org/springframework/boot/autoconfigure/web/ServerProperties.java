/*
 * Copyright 2012-2015 the original author or authors.
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.validation.constraints.NotNull;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.boot.autoconfigure.web.ServerProperties.Session.Cookie;
import org.springframework.boot.context.embedded.Compression;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.InitParameterConfiguringServletContextInitializer;
import org.springframework.boot.context.embedded.JspServlet;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

/**
 * {@link ConfigurationProperties} for a web server (e.g. port and path settings). Will be
 * used to customize an {@link EmbeddedServletContainerFactory} when an
 * {@link EmbeddedServletContainerCustomizerBeanPostProcessor} is active.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Ivan Sopov
 * @author Marcos Barbero
 */
@ConfigurationProperties(prefix = "server", ignoreUnknownFields = false)
public class ServerProperties implements EmbeddedServletContainerCustomizer, Ordered {

	/**
	 * Server HTTP port.
	 */
	private Integer port;

	/**
	 * Network address to which the server should bind to.
	 */
	private InetAddress address;

	/**
	 * Context path of the application.
	 */
	private String contextPath;

	/**
	 * Display name of the application.
	 */
	private String displayName = "application";

	private Session session = new Session();

	@NestedConfigurationProperty
	private Ssl ssl;

	/**
	 * Path of the main dispatcher servlet.
	 */
	@NotNull
	private String servletPath = "/";

	private final Tomcat tomcat = new Tomcat();

	private final Undertow undertow = new Undertow();

	@NestedConfigurationProperty
	private Compression compression = new Compression();

	@NestedConfigurationProperty
	private JspServlet jspServlet;

	/**
	 * ServletContext parameters.
	 */
	private final Map<String, String> contextParameters = new HashMap<String, String>();

	@Override
	public int getOrder() {
		return 0;
	}

	public Tomcat getTomcat() {
		return this.tomcat;
	}

	public Undertow getUndertow() {
		return this.undertow;
	}

	public Compression getCompression() {
		return this.compression;
	}

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

	public String getDisplayName() {
		return this.displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getServletPath() {
		return this.servletPath;
	}

	public String getServletMapping() {
		if (this.servletPath.equals("") || this.servletPath.equals("/")) {
			return "/";
		}
		if (this.servletPath.contains("*")) {
			return this.servletPath;
		}
		if (this.servletPath.endsWith("/")) {
			return this.servletPath + "*";
		}
		return this.servletPath + "/*";
	}

	public String getServletPrefix() {
		String result = this.servletPath;
		if (result.contains("*")) {
			result = result.substring(0, result.indexOf("*"));
		}
		if (result.endsWith("/")) {
			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

	public void setServletPath(String servletPath) {
		this.servletPath = servletPath;
	}

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

	/**
	 * Get the session timeout
	 * @return the session timeout
	 * @deprecated since 1.3.0 in favor of {@code session.timeout}.
	 */
	@Deprecated
	@DeprecatedConfigurationProperty(replacement = "server.session.timeout")
	public Integer getSessionTimeout() {
		return this.session.getTimeout();
	}

	/**
	 * Set the session timeout
	 * @param sessionTimeout the session timeout
	 * @deprecated since 1.3.0 in favor of {@code session.timeout}.
	 */
	@Deprecated
	public void setSessionTimeout(Integer sessionTimeout) {
		this.session.setTimeout(sessionTimeout);
	}

	public Session getSession() {
		return this.session;
	}

	public Ssl getSsl() {
		return this.ssl;
	}

	public void setSsl(Ssl ssl) {
		this.ssl = ssl;
	}

	public JspServlet getJspServlet() {
		return this.jspServlet;
	}

	public void setJspServlet(JspServlet jspServlet) {
		this.jspServlet = jspServlet;
	}

	public Map<String, String> getContextParameters() {
		return this.contextParameters;
	}

	public void setLoader(String value) {
		// no op to support Tomcat running as a traditional container (not embedded)
	}

	@Override
	public void customize(ConfigurableEmbeddedServletContainer container) {
		if (getPort() != null) {
			container.setPort(getPort());
		}
		if (getAddress() != null) {
			container.setAddress(getAddress());
		}
		if (getContextPath() != null) {
			container.setContextPath(getContextPath());
		}
		if (getDisplayName() != null) {
			container.setDisplayName(getDisplayName());
		}
		if (getSession().getTimeout() != null) {
			container.setSessionTimeout(getSession().getTimeout());
		}
		container.setPersistSession(getSession().isPersistent());
		if (getSsl() != null) {
			container.setSsl(getSsl());
		}
		if (getJspServlet() != null) {
			container.setJspServlet(getJspServlet());
		}
		if (getCompression() != null) {
			container.setCompression(getCompression());
		}
		if (container instanceof TomcatEmbeddedServletContainerFactory) {
			getTomcat()
			.customizeTomcat((TomcatEmbeddedServletContainerFactory) container);
		}
		if (container instanceof UndertowEmbeddedServletContainerFactory) {
			getUndertow().customizeUndertow(
					(UndertowEmbeddedServletContainerFactory) container);
		}
		container.addInitializers(new SessionConfiguringInitializer(this.session));
		container.addInitializers(new InitParameterConfiguringServletContextInitializer(
				getContextParameters()));
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

	public String getPath(String path) {
		String prefix = getServletPrefix();
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		return prefix + path;
	}

	public static class Session {

		/**
		 * Session timeout in seconds.
		 */
		private Integer timeout;

		/**
		 * Session tracking modes (one or more of the following: "cookie", "url", "ssl")
		 */
		private Set<SessionTrackingMode> trackingModes;

		/**
		 * Persist session data between restarts.
		 */
		private boolean persistent;

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

	}

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
		 * Configured as a RemoteIpValve only if remoteIpHeader is also set.
		 */
		private String protocolHeader;

		/**
		 * Value of the protocol header that indicates that the incoming request uses SSL.
		 */
		private String protocolHeaderHttpsValue = "https";

		/**
		 * Name of the HTTP header used to override the original port value.
		 */
		private String portHeader = "x-forwarded-port";

		/**
		 * Name of the http header from which the remote ip is extracted. Configured as a
		 * RemoteIpValve only if remoteIpHeader is also set.
		 */
		private String remoteIpHeader = "x-forwarded-for";

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
		 * Maximum size in bytes of the HTTP message header.
		 */
		private int maxHttpHeaderSize = 0; // bytes

		/**
		 * Character encoding to use to decode the URI.
		 */
		private Charset uriEncoding;

		public int getMaxThreads() {
			return this.maxThreads;
		}

		public void setMaxThreads(int maxThreads) {
			this.maxThreads = maxThreads;
		}

		public int getMaxHttpHeaderSize() {
			return this.maxHttpHeaderSize;
		}

		public void setMaxHttpHeaderSize(int maxHttpHeaderSize) {
			this.maxHttpHeaderSize = maxHttpHeaderSize;
		}

		public Accesslog getAccesslog() {
			return this.accesslog;
		}

		/**
		 * Specify if access log is enabled.
		 * @return {@code true} if access log is enabled
		 * @deprecated since 1.3.0 in favor of {@code server.tomcat.accesslog.enabled}
		 */
		@Deprecated
		@DeprecatedConfigurationProperty(replacement = "server.tomcat.accesslog.enabled")
		public boolean getAccessLogEnabled() {
			return this.accesslog.isEnabled();
		}

		/**
		 * Set if access log is enabled.
		 * @param accessLogEnabled the access log enable flag
		 * @deprecated since 1.3.0 in favor of {@code server.tomcat.accesslog.enabled}
		 */
		@Deprecated
		public void setAccessLogEnabled(boolean accessLogEnabled) {
			getAccesslog().setEnabled(accessLogEnabled);
		}

		/**
		 * Get the format pattern for access logs.
		 * @return the format pattern for access logs
		 * @deprecated since 1.3.0 in favor of {@code server.tomcat.accesslog.pattern}
		 */
		@Deprecated
		@DeprecatedConfigurationProperty(replacement = "server.tomcat.accesslog.pattern")
		public String getAccessLogPattern() {
			return this.accesslog.getPattern();
		}

		/**
		 * Set the format pattern for access logs
		 * @param accessLogPattern the pattern for access logs
		 * @deprecated since 1.3.0 in favor of {@code server.tomcat.accesslog.pattern}
		 */
		@Deprecated
		public void setAccessLogPattern(String accessLogPattern) {
			this.accesslog.setPattern(accessLogPattern);
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

		void customizeTomcat(TomcatEmbeddedServletContainerFactory factory) {
			if (getBasedir() != null) {
				factory.setBaseDirectory(getBasedir());
			}
			customizeBackgroundProcessorDelay(factory);
			customizeHeaders(factory);
			if (this.maxThreads > 0) {
				customizeMaxThreads(factory);
			}
			if (this.maxHttpHeaderSize > 0) {
				customizeMaxHttpHeaderSize(factory);
			}
			if (this.accesslog.enabled) {
				customizeAccessLog(factory);
			}
			if (getUriEncoding() != null) {
				factory.setUriEncoding(getUriEncoding());
			}
		}

		private void customizeBackgroundProcessorDelay(
				TomcatEmbeddedServletContainerFactory factory) {
			factory.addContextCustomizers(new TomcatContextCustomizer() {

				@Override
				public void customize(Context context) {
					context.setBackgroundProcessorDelay(Tomcat.this.backgroundProcessorDelay);
				}

			});
		}

		private void customizeHeaders(TomcatEmbeddedServletContainerFactory factory) {
			String remoteIpHeader = getRemoteIpHeader();
			String protocolHeader = getProtocolHeader();
			if (StringUtils.hasText(remoteIpHeader)
					&& StringUtils.hasText(protocolHeader)) {
				RemoteIpValve valve = new RemoteIpValve();
				valve.setRemoteIpHeader(remoteIpHeader);
				valve.setProtocolHeader(protocolHeader);
				// The internal proxies default to a white list of "safe" internal IP
				// addresses
				valve.setInternalProxies(getInternalProxies());
				valve.setPortHeader(getPortHeader());
				valve.setProtocolHeaderHttpsValue(getProtocolHeaderHttpsValue());
				// ... so it's safe to add this valve by default.
				factory.addContextValves(valve);
			}
		}

		@SuppressWarnings("rawtypes")
		private void customizeMaxThreads(TomcatEmbeddedServletContainerFactory factory) {
			factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
				@Override
				public void customize(Connector connector) {

					ProtocolHandler handler = connector.getProtocolHandler();
					if (handler instanceof AbstractProtocol) {
						AbstractProtocol protocol = (AbstractProtocol) handler;
						protocol.setMaxThreads(Tomcat.this.maxThreads);
					}

				}
			});
		}

		@SuppressWarnings("rawtypes")
		private void customizeMaxHttpHeaderSize(
				TomcatEmbeddedServletContainerFactory factory) {
			factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {

				@Override
				public void customize(Connector connector) {
					ProtocolHandler handler = connector.getProtocolHandler();
					if (handler instanceof AbstractHttp11Protocol) {
						AbstractHttp11Protocol protocol = (AbstractHttp11Protocol) handler;
						protocol.setMaxHttpHeaderSize(Tomcat.this.maxHttpHeaderSize);
					}
				}

			});
		}

		private void customizeAccessLog(TomcatEmbeddedServletContainerFactory factory) {
			AccessLogValve valve = new AccessLogValve();
			valve.setPattern(this.accesslog.getPattern());
			valve.setDirectory(this.accesslog.getDirectory());
			valve.setPrefix(this.accesslog.getPrefix());
			valve.setSuffix(this.accesslog.getSuffix());
			factory.addContextValves(valve);
		}

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
		}

	}

	public static class Undertow {

		/**
		 * Size of each buffer in bytes.
		 */
		private Integer bufferSize;

		/**
		 * Number of buffer per region.
		 */
		private Integer buffersPerRegion;

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

		private final Accesslog accesslog = new Accesslog();

		public Integer getBufferSize() {
			return this.bufferSize;
		}

		public void setBufferSize(Integer bufferSize) {
			this.bufferSize = bufferSize;
		}

		public Integer getBuffersPerRegion() {
			return this.buffersPerRegion;
		}

		public void setBuffersPerRegion(Integer buffersPerRegion) {
			this.buffersPerRegion = buffersPerRegion;
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

		public Accesslog getAccesslog() {
			return this.accesslog;
		}

		/**
		 * Get the format pattern for access logs.
		 * @return the format pattern for access logs
		 * @deprecated since 1.3.0 in favor of {@code server.undertow.accesslog.pattern}
		 */
		@Deprecated
		@DeprecatedConfigurationProperty(replacement = "server.undertow.accesslog.pattern")
		public String getAccessLogPattern() {
			return this.accesslog.getPattern();
		}

		/**
		 * Set the format pattern for access logs
		 * @param accessLogPattern the pattern for access logs
		 * @deprecated since 1.3.0 in favor of {@code server.undertow.accesslog.pattern}
		 */
		@Deprecated
		public void setAccessLogPattern(String accessLogPattern) {
			this.accesslog.setPattern(accessLogPattern);
		}

		/**
		 * Specify if access log is enabled.
		 * @return {@code true} if access log is enabled
		 * @deprecated since 1.3.0 in favor of {@code server.undertow.accesslog.enabled}
		 */
		@Deprecated
		@DeprecatedConfigurationProperty(replacement = "server.undertow.accesslog.enabled")
		public boolean isAccessLogEnabled() {
			return this.accesslog.isEnabled();
		}

		/**
		 * Set if access log is enabled.
		 * @param accessLogEnabled the access log enable flag
		 * @deprecated since 1.3.0 in favor of {@code server.undertow.accesslog.enabled}
		 */
		@Deprecated
		public void setAccessLogEnabled(boolean accessLogEnabled) {
			getAccesslog().setEnabled(accessLogEnabled);
		}

		/**
		 * Get the access log directory.
		 * @return the access log directory
		 * @deprecated since 1.3.0 in favor of {@code server.undertow.accesslog.dir}
		 */
		@Deprecated
		@DeprecatedConfigurationProperty(replacement = "server.undertow.accesslog.dir")
		public File getAccessLogDir() {
			return this.accesslog.getDir();
		}

		/**
		 * Set the access log directory.
		 * @param accessLogDir the access log directory
		 * @deprecated since 1.3.0 in favor of {@code server.tomcat.accesslog.dir}
		 */
		@Deprecated
		public void setAccessLogDir(File accessLogDir) {
			getAccesslog().setDir(accessLogDir);
		}

		void customizeUndertow(UndertowEmbeddedServletContainerFactory factory) {
			factory.setBufferSize(this.bufferSize);
			factory.setBuffersPerRegion(this.buffersPerRegion);
			factory.setIoThreads(this.ioThreads);
			factory.setWorkerThreads(this.workerThreads);
			factory.setDirectBuffers(this.directBuffers);
			factory.setAccessLogDirectory(this.accesslog.dir);
			factory.setAccessLogPattern(this.accesslog.pattern);
			factory.setAccessLogEnabled(this.accesslog.enabled);
		}

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
			 * Undertow access log directory.
			 */
			private File dir = new File("logs");

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

			public File getDir() {
				return this.dir;
			}

			public void setDir(File dir) {
				this.dir = dir;
			}

		}

	}

	/**
	 * {@link ServletContextInitializer} to apply appropriate parts of the {@link Session}
	 * configuration.
	 */
	private static class SessionConfiguringInitializer implements
	ServletContextInitializer {

		private final Session session;

		public SessionConfiguringInitializer(Session session) {
			this.session = session;
		}

		@Override
		public void onStartup(ServletContext servletContext) throws ServletException {
			if (this.session.getTrackingModes() != null) {
				servletContext.setSessionTrackingModes(this.session.getTrackingModes());
			}
			configureSessionCookie(servletContext.getSessionCookieConfig());
		}

		private void configureSessionCookie(SessionCookieConfig config) {
			Cookie cookie = this.session.getCookie();
			if (cookie.getName() != null) {
				config.setName(cookie.getName());
			}
			if (cookie.getDomain() != null) {
				config.setDomain(cookie.getDomain());
			}
			if (cookie.getPath() != null) {
				config.setPath(cookie.getPath());
			}
			if (cookie.getComment() != null) {
				config.setComment(cookie.getComment());
			}
			if (cookie.getHttpOnly() != null) {
				config.setHttpOnly(cookie.getHttpOnly());
			}
			if (cookie.getSecure() != null) {
				config.setSecure(cookie.getSecure());
			}
			if (cookie.getMaxAge() != null) {
				config.setMaxAge(cookie.getMaxAge());
			}
		}

	}
}
