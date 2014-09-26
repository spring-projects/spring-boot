/*
 * Copyright 2012-2014 the original author or authors.
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
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * {@link ConfigurationProperties properties} for a web server (e.g. port and path
 * settings). Will be used to customize an {@link EmbeddedServletContainerFactory} when an
 * {@link EmbeddedServletContainerCustomizerBeanPostProcessor} is active.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 */
@ConfigurationProperties(prefix = "server", ignoreUnknownFields = false)
public class ServerProperties implements EmbeddedServletContainerCustomizer {

	private Integer port;

	private InetAddress address;

	private Integer sessionTimeout;

	private String contextPath;

	private Ssl ssl;

	@NotNull
	private String servletPath = "/";

	private final Tomcat tomcat = new Tomcat();

	public Tomcat getTomcat() {
		return this.tomcat;
	}

	public String getContextPath() {
		return this.contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
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
		if (getSessionTimeout() != null) {
			container.setSessionTimeout(getSessionTimeout());
		}
		if (getSsl() != null) {
			container.setSsl(getSsl());
		}
		if (container instanceof TomcatEmbeddedServletContainerFactory) {
			getTomcat()
					.customizeTomcat((TomcatEmbeddedServletContainerFactory) container);
		}
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

		private String accessLogPattern;

		private boolean accessLogEnabled = false;

		private String internalProxies = "10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}|" // 10/8
				+ "192\\.168\\.\\d{1,3}\\.\\d{1,3}|" // 192.168/16
				+ "169\\.254\\.\\d{1,3}\\.\\d{1,3}|" // 169.254/16
				+ "127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"; // 127/8

		private String protocolHeader;

		private String portHeader;

		private String remoteIpHeader;

		private File basedir;

		private int backgroundProcessorDelay = 30; // seconds

		private int maxThreads = 0; // Number of threads in protocol handler

		private int maxHttpHeaderSize = 0; // bytes

		private String uriEncoding;

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
}
