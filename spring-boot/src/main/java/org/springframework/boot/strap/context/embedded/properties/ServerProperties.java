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

package org.springframework.boot.strap.context.embedded.properties;

import java.io.File;
import java.net.InetAddress;

import javax.validation.constraints.NotNull;

import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.RemoteIpValve;
import org.springframework.boot.strap.context.embedded.ConfigurableEmbeddedServletContainerFactory;
import org.springframework.boot.strap.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.strap.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.strap.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.strap.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.strap.context.properties.ConfigurationProperties;
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

	private int port = 8080;

	private InetAddress address;

	private int sessionTimeout = 30;

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

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public InetAddress getAddress() {
		return this.address;
	}

	public void setAddress(InetAddress address) {
		this.address = address;
	}

	public int getSessionTimeout() {
		return this.sessionTimeout;
	}

	public void setSessionTimeout(int sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}

	public void setLoader(String value) {
		// no op to support Tomcat running as a traditional container (not embedded)
	}

	@Override
	public void customize(ConfigurableEmbeddedServletContainerFactory factory) {
		factory.setPort(getPort());
		factory.setAddress(getAddress());
		factory.setContextPath(getContextPath());
		factory.setSessionTimeout(getSessionTimeout());
		if (factory instanceof TomcatEmbeddedServletContainerFactory) {
			getTomcat().customizeTomcat((TomcatEmbeddedServletContainerFactory) factory);
		}
	}

	public static class Tomcat {

		private String accessLogPattern;

		private String protocolHeader = "x-forwarded-proto";

		private String remoteIpHeader = "x-forwarded-for";

		private File basedir;

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

			String remoteIpHeader = getRemoteIpHeader();
			String protocolHeader = getProtocolHeader();
			if (StringUtils.hasText(remoteIpHeader)
					|| StringUtils.hasText(protocolHeader)) {
				RemoteIpValve valve = new RemoteIpValve();
				valve.setRemoteIpHeader(remoteIpHeader);
				valve.setProtocolHeader(protocolHeader);
				factory.addContextValves(valve);
			}

			String accessLogPattern = getAccessLogPattern();
			if (accessLogPattern != null) {
				AccessLogValve valve = new AccessLogValve();
				valve.setPattern(accessLogPattern);
				valve.setSuffix(".log");
				factory.addContextValves(valve);
			}
		}

	}

}
