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

package org.springframework.boot.context.embedded.undertow;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.server.HttpHandler;
import io.undertow.servlet.api.DeploymentManager;

import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EmbeddedServletContainer} that can be used to control an embedded Undertow
 * server. Typically this class should be created using
 * {@link UndertowEmbeddedServletContainerFactory} and not directly.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 * @since 1.2.0
 * @see UndertowEmbeddedServletContainerFactory
 */
public class UndertowEmbeddedServletContainer implements EmbeddedServletContainer {

	private static final Log logger = LogFactory
			.getLog(UndertowEmbeddedServletContainer.class);

	private final Builder builder;

	private final DeploymentManager manager;

	private final String contextPath;

	private final boolean autoStart;

	private Undertow undertow;

	private boolean started = false;

	public UndertowEmbeddedServletContainer(Builder builder, DeploymentManager manager,
			String contextPath, int port, boolean autoStart) {
		this.builder = builder;
		this.manager = manager;
		this.contextPath = contextPath;
		this.autoStart = autoStart;
	}

	@Override
	public synchronized void start() throws EmbeddedServletContainerException {
		if (!this.autoStart) {
			return;
		}
		if (this.undertow == null) {
			this.undertow = createUndertowServer();
		}
		this.undertow.start();
		this.started = true;
		UndertowEmbeddedServletContainer.logger.info("Undertow started on port(s) "
				+ getPortsDescription());
	}

	private Undertow createUndertowServer() {
		try {
			HttpHandler servletHandler = this.manager.start();
			this.builder.setHandler(getContextHandler(servletHandler));
			return this.builder.build();
		}
		catch (ServletException ex) {
			throw new EmbeddedServletContainerException(
					"Unable to start embdedded Undertow", ex);
		}
	}

	private HttpHandler getContextHandler(HttpHandler servletHandler) {
		if (StringUtils.isEmpty(this.contextPath)) {
			return servletHandler;
		}
		return Handlers.path().addPrefixPath(this.contextPath, servletHandler);
	}

	private String getPortsDescription() {
		List<Port> ports = getPorts();
		if (!ports.isEmpty()) {
			return StringUtils.collectionToDelimitedString(ports, " ");
		}
		return "unknown";
	}

	@SuppressWarnings("rawtypes")
	private List<Port> getPorts() {
		List<Port> ports = new ArrayList<Port>();
		try {
			// Use reflection if possible to get the underlying XNIO channels
			if (!this.autoStart) {
				ports.add(new Port(-1, "unknown"));
			}
			else {
				Field channelsField = ReflectionUtils.findField(Undertow.class,
						"channels");
				ReflectionUtils.makeAccessible(channelsField);
				List channels = (List) ReflectionUtils.getField(channelsField,
						this.undertow);
				for (Object channel : channels) {
					Port port = getPortFromChannel(channel);
					if (port != null) {
						ports.add(port);
					}
				}
			}
		}
		catch (Exception ex) {
			// Continue
		}
		return ports;
	}

	private Port getPortFromChannel(Object channel) {
		Object tcpServer = channel;
		String protocol = "http";
		Field sslContext = ReflectionUtils.findField(channel.getClass(), "sslContext");
		if (sslContext != null) {
			tcpServer = getTcpServer(channel);
			protocol = "https";
		}
		ServerSocket socket = getSocket(tcpServer);
		if (socket != null) {
			return new Port(socket.getLocalPort(), protocol);
		}
		return null;
	}

	private Object getTcpServer(Object channel) {
		Field field = ReflectionUtils.findField(channel.getClass(), "tcpServer");
		ReflectionUtils.makeAccessible(field);
		return ReflectionUtils.getField(field, channel);
	}

	private ServerSocket getSocket(Object tcpServer) {
		Field socketField = ReflectionUtils.findField(tcpServer.getClass(), "socket");
		if (socketField == null) {
			return null;
		}
		ReflectionUtils.makeAccessible(socketField);
		return (ServerSocket) ReflectionUtils.getField(socketField, tcpServer);
	}

	@Override
	public synchronized void stop() throws EmbeddedServletContainerException {
		if (this.started) {
			this.started = false;
			this.undertow.stop();
		}
	}

	@Override
	public int getPort() {
		List<Port> ports = getPorts();
		if (ports.isEmpty()) {
			return 0;
		}
		return ports.get(0).getNumber();
	}

	/**
	 * An active undertow port.
	 */
	private static class Port {

		private final int number;

		private final String protocol;

		private Port(int number, String protocol) {
			this.number = number;
			this.protocol = protocol;
		}

		public int getNumber() {
			return this.number;
		}

		@Override
		public String toString() {
			return this.number + " (" + this.protocol + ")";
		}

	}

}
