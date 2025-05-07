/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.web.server.autoconfigure;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.Cookie;
import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.servlet.Jsp;
import org.springframework.boot.web.server.servlet.Session;
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
@ConfigurationProperties("server")
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
	 * Maximum size of the HTTP request header. Refer to the documentation for your chosen
	 * embedded server for details of exactly how this limit is applied. For example,
	 * Netty applies the limit separately to each individual header in the request whereas
	 * Tomcat applies the limit to the combined size of the request line and all of the
	 * header names and values in the request.
	 */
	private DataSize maxHttpRequestHeaderSize = DataSize.ofKilobytes(8);

	/**
	 * Type of shutdown that the server will support.
	 */
	private Shutdown shutdown = Shutdown.GRACEFUL;

	@NestedConfigurationProperty
	private Ssl ssl;

	@NestedConfigurationProperty
	private final Compression compression = new Compression();

	/**
	 * Custom MIME mappings in addition to the default MIME mappings.
	 */
	private final MimeMappings mimeMappings = new MimeMappings();

	@NestedConfigurationProperty
	private final Http2 http2 = new Http2();

	private final Servlet servlet = new Servlet();

	private final Reactive reactive = new Reactive();

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

	public DataSize getMaxHttpRequestHeaderSize() {
		return this.maxHttpRequestHeaderSize;
	}

	public void setMaxHttpRequestHeaderSize(DataSize maxHttpRequestHeaderSize) {
		this.maxHttpRequestHeaderSize = maxHttpRequestHeaderSize;
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

	public MimeMappings getMimeMappings() {
		return this.mimeMappings;
	}

	public void setMimeMappings(Map<String, String> customMappings) {
		customMappings.forEach(this.mimeMappings::add);
	}

	public Http2 getHttp2() {
		return this.http2;
	}

	public Servlet getServlet() {
		return this.servlet;
	}

	public Reactive getReactive() {
		return this.reactive;
	}

	public ForwardHeadersStrategy getForwardHeadersStrategy() {
		return this.forwardHeadersStrategy;
	}

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
			String candidate = null;
			if (StringUtils.hasLength(contextPath)) {
				candidate = contextPath.strip();
			}
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
	 * Reactive server properties.
	 */
	public static class Reactive {

		private final Session session = new Session();

		public Session getSession() {
			return this.session;
		}

		public static class Session {

			/**
			 * Session timeout. If a duration suffix is not specified, seconds will be
			 * used.
			 */
			@DurationUnit(ChronoUnit.SECONDS)
			private Duration timeout = Duration.ofMinutes(30);

			/**
			 * Maximum number of sessions that can be stored.
			 */
			private int maxSessions = 10000;

			@NestedConfigurationProperty
			private final Cookie cookie = new Cookie();

			public Duration getTimeout() {
				return this.timeout;
			}

			public void setTimeout(Duration timeout) {
				this.timeout = timeout;
			}

			public int getMaxSessions() {
				return this.maxSessions;
			}

			public void setMaxSessions(int maxSessions) {
				this.maxSessions = maxSessions;
			}

			public Cookie getCookie() {
				return this.cookie;
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

	public static class Encoding {

		/**
		 * Mapping of locale to charset for response encoding.
		 */
		private Map<Locale, Charset> mapping;

		public Map<Locale, Charset> getMapping() {
			return this.mapping;
		}

		public void setMapping(Map<Locale, Charset> mapping) {
			this.mapping = mapping;
		}

	}

}
