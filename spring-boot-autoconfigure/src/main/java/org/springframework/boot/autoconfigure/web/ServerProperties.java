/*
 * Copyright 2012-2013 the original author or authors.
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

import javax.validation.constraints.NotNull;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.SocketUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ConfigurationProperties properties} for a web server (e.g. port and path
 * settings). Will be used to customize an {@link EmbeddedServletContainerFactory} when an
 * {@link EmbeddedServletContainerCustomizerBeanPostProcessor} is active.
 * 
 * @author Dave Syer
 */
@ConfigurationProperties(name = "server", ignoreUnknownFields = false)
public class ServerProperties implements EmbeddedServletContainerCustomizer {

	private Integer port;

	private InetAddress address;

	private Integer sessionTimeout;

	private boolean scan = false;

	@NotNull
	private String contextPath = "";

	private Tomcat tomcat = new Tomcat();

	public Tomcat getTomcat() {
		return this.tomcat;
	}

	public String getContextPath() {
		return this.contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public Integer getPort() {
		return this.port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public boolean getScan() {
		return this.scan;
	}

	public void setScan(boolean scan) {
		this.scan = scan;
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

	public void setLoader(String value) {
		// no op to support Tomcat running as a traditional container (not embedded)
	}

	@Override
	public void customize(ConfigurableEmbeddedServletContainerFactory factory) {
		Integer port = getPort();
		if (this.scan) {
			port = SocketUtils.findAvailableTcpPort(port != null ? 8080 : port);
		}
		if (port != null) {
			factory.setPort(port);
		}
		if (getAddress() != null) {
			factory.setAddress(getAddress());
		}
		if (getContextPath() != null) {
			factory.setContextPath(getContextPath());
		}
		if (getSessionTimeout() != null) {
			factory.setSessionTimeout(getSessionTimeout());
		}
		if (factory instanceof TomcatEmbeddedServletContainerFactory) {
			getTomcat().customizeTomcat((TomcatEmbeddedServletContainerFactory) factory);
		}
	}

	public static class Tomcat {

		private String accessLogPattern;

		private boolean accessLogEnabled = false;

		private String protocolHeader = "x-forwarded-proto";

		private String remoteIpHeader = "x-forwarded-for";

		private File basedir;

		private int backgroundProcessorDelay = 30; // seconds

		private int maxThreads = 0; // Number of threads in protocol handler

		public int getMaxThreads() {
			return this.maxThreads;
		}

		public void setMaxThreads(int maxThreads) {
			this.maxThreads = maxThreads;
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

		public String getProtocolHeader() {
			return this.protocolHeader;
		}

		public void setProtocolHeader(String protocolHeader) {
			this.protocolHeader = protocolHeader;
		}

		public String getRemoteIpHeader() {
			return this.remoteIpHeader;
		}

		public void setRemoteIpHeader(String remoteIpHeader) {
			this.remoteIpHeader = remoteIpHeader;
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
				factory.addContextValves(valve);
			}

			if (this.maxThreads > 0) {
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
		}
	}

}
