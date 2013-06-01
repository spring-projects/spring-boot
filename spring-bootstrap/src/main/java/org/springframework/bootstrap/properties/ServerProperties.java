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

package org.springframework.bootstrap.properties;

import java.io.File;
import java.net.InetAddress;

import javax.validation.constraints.NotNull;

import org.springframework.bootstrap.context.annotation.ConfigurationProperties;

/**
 * Properties for the web server (e.g. port and path settings).
 * 
 * @author Dave Syer
 */
@ConfigurationProperties(name = "server", ignoreUnknownFields = false)
public class ServerProperties {

	private int port = 8080;

	private InetAddress address;

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

	public void setLoader(String value) {
		// no op
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

	}

}
