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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.NotNull;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.InitParameterConfiguringServletContextInitializer;
import org.springframework.boot.context.embedded.JspServlet;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
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
	 * Session timeout in seconds.
	 */
	private Integer sessionTimeout;

	/**
	 * Context path of the application.
	 */
	private String contextPath;

	/**
	 * Display name of the application.
	 */
	private String displayName = "application";

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

	public String getContextPath() {
		return this.contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
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

	public Integer getSessionTimeout() {
		return this.sessionTimeout;
	}

	public void setSessionTimeout(Integer sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
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
		if (getSessionTimeout() != null) {
			container.setSessionTimeout(getSessionTimeout());
		}
		if (getSsl() != null) {
			container.setSsl(getSsl());
		}
		if (getJspServlet() != null) {
			container.setJspServlet(getJspServlet());
		}
		if (container instanceof TomcatEmbeddedServletContainerFactory) {
			getTomcat()
					.customizeTomcat((TomcatEmbeddedServletContainerFactory) container);
		}
		if (container instanceof UndertowEmbeddedServletContainerFactory) {
			getUndertow().customizeUndertow(
					(UndertowEmbeddedServletContainerFactory) container);
		}
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

	public static class Tomcat {

		/**
		 * Format pattern for access logs.
		 */
		private String accessLogPattern;

		/**
		 * Enable access log.
		 */
		private boolean accessLogEnabled = false;

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
		 * Name of the HTTP header used to override the original port value.
		 */
		private String portHeader;

		/**
		 * Name of the http header from which the remote ip is extracted. Configured as a
		 * RemoteIpValve only if remoteIpHeader is also set.
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
		 * Maximum size in bytes of the HTTP message header.
		 */
		private int maxHttpHeaderSize = 0; // bytes

		/**
		 * Character encoding to use to decode the URI.
		 */
		private String uriEncoding;

		/**
		 * Controls response compression. Acceptable values are "off" to disable
		 * compression, "on" to enable compression of responses over 2048 bytes, "force"
		 * to force response compression, or an integer value to enable compression of
		 * responses with content length that is at least that value.
		 */
		private String compression = "off";

		/**
		 * Comma-separated list of MIME types for which compression is used.
		 */
		private String compressableMimeTypes = "text/html,text/xml,text/plain";

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

		public boolean getAccessLogEnabled() {
			return this.accessLogEnabled;
		}

		public void setAccessLogEnabled(boolean accessLogEnabled) {
			this.accessLogEnabled = accessLogEnabled;
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

		public String getAccessLogPattern() {
			return this.accessLogPattern;
		}

		public void setAccessLogPattern(String accessLogPattern) {
			this.accessLogPattern = accessLogPattern;
		}

		public String getCompressableMimeTypes() {
			return this.compressableMimeTypes;
		}

		public void setCompressableMimeTypes(String compressableMimeTypes) {
			this.compressableMimeTypes = compressableMimeTypes;
		}

		public String getCompression() {
			return this.compression;
		}

		public void setCompression(String compression) {
			this.compression = compression;
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

		public String getUriEncoding() {
			return this.uriEncoding;
		}

		public void setUriEncoding(String uriEncoding) {
			this.uriEncoding = uriEncoding;
		}

		void customizeTomcat(TomcatEmbeddedServletContainerFactory factory) {
			if (getBasedir() != null) {
				factory.setBaseDirectory(getBasedir());
			}

			factory.addContextCustomizers(new TomcatContextCustomizer() {
				@Override
				public void customize(Context context) {
					context.setBackgroundProcessorDelay(Tomcat.this.backgroundProcessorDelay);
				}
			});

			String remoteIpHeader = getRemoteIpHeader();
			String protocolHeader = getProtocolHeader();
			if (StringUtils.hasText(remoteIpHeader)
					|| StringUtils.hasText(protocolHeader)) {
				RemoteIpValve valve = new RemoteIpValve();
				valve.setRemoteIpHeader(remoteIpHeader);
				valve.setProtocolHeader(protocolHeader);
				valve.setInternalProxies(getInternalProxies());
				valve.setPortHeader(getPortHeader());
				factory.addContextValves(valve);
			}

			if (this.maxThreads > 0) {
				factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
					@Override
					public void customize(Connector connector) {
						ProtocolHandler handler = connector.getProtocolHandler();
						if (handler instanceof AbstractProtocol) {
							@SuppressWarnings("rawtypes")
							AbstractProtocol protocol = (AbstractProtocol) handler;
							protocol.setMaxThreads(Tomcat.this.maxThreads);
						}
					}
				});
			}

			if (this.maxHttpHeaderSize > 0) {
				factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
					@Override
					public void customize(Connector connector) {
						ProtocolHandler handler = connector.getProtocolHandler();
						if (handler instanceof AbstractHttp11Protocol) {
							@SuppressWarnings("rawtypes")
							AbstractHttp11Protocol protocol = (AbstractHttp11Protocol) handler;
							protocol.setMaxHttpHeaderSize(Tomcat.this.maxHttpHeaderSize);
						}
					}
				});
			}

			factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {

				@Override
				public void customize(Connector connector) {
					ProtocolHandler handler = connector.getProtocolHandler();
					if (handler instanceof AbstractHttp11Protocol) {
						@SuppressWarnings("rawtypes")
						AbstractHttp11Protocol protocol = (AbstractHttp11Protocol) handler;
						protocol.setCompression(coerceCompression(Tomcat.this.compression));
						protocol.setCompressableMimeTypes(Tomcat.this.compressableMimeTypes);
					}
				}

				private String coerceCompression(String compression) {
					if ("true".equalsIgnoreCase(compression)) {
						return "on";
					}
					if ("false".equalsIgnoreCase(compression)) {
						return "off";
					}
					return compression;
				}

			});

			if (this.accessLogEnabled) {
				AccessLogValve valve = new AccessLogValve();
				String accessLogPattern = getAccessLogPattern();
				if (accessLogPattern != null) {
					valve.setPattern(accessLogPattern);
				}
				else {
					valve.setPattern("common");
				}
				valve.setSuffix(".log");
				factory.addContextValves(valve);
			}
			if (getUriEncoding() != null) {
				factory.setUriEncoding(getUriEncoding());
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

		private Boolean directBuffers;

		/**
		 * Format pattern for access logs.
		 */
		private String accessLogPattern;

		/**
		 * Enable access log.
		 */
		private boolean accessLogEnabled = false;

		/**
		 * Undertow access log directory. If not specified a temporary directory will be used.
		 */
		private File accessLogDir;

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

		public String getAccessLogPattern() {
			return accessLogPattern;
		}

		public void setAccessLogPattern(String accessLogPattern) {
			this.accessLogPattern = accessLogPattern;
		}

		public boolean isAccessLogEnabled() {
			return accessLogEnabled;
		}

		public void setAccessLogEnabled(boolean accessLogEnabled) {
			this.accessLogEnabled = accessLogEnabled;
		}

		public File getAccessLogDir() {
			return accessLogDir;
		}

		public void setAccessLogDir(File accessLogDir) {
			this.accessLogDir = accessLogDir;
		}

		void customizeUndertow(UndertowEmbeddedServletContainerFactory factory) {
			factory.setBufferSize(this.bufferSize);
			factory.setBuffersPerRegion(this.buffersPerRegion);
			factory.setIoThreads(this.ioThreads);
			factory.setWorkerThreads(this.workerThreads);
			factory.setDirectBuffers(this.directBuffers);
			factory.setAccessLogDirectory(this.accessLogDir);
			factory.setAccessLogPattern(this.accessLogPattern);
			factory.setAccessLogEnabled(this.accessLogEnabled);
		}

	}

}
