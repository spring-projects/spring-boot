/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.context.embedded.undertow;

import java.lang.reflect.Field;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.Undertow.Builder;
import io.undertow.attribute.RequestHeaderAttribute;
import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xnio.channels.BoundChannel;

import org.springframework.boot.context.embedded.Compression;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.PortInUseException;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * {@link EmbeddedServletContainer} that can be used to control an embedded Undertow
 * server. Typically this class should be created using
 * {@link UndertowEmbeddedServletContainerFactory} and not directly.
 *
 * @author Ivan Sopov
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Christoph Dreis
 * @since 1.2.0
 * @see UndertowEmbeddedServletContainerFactory
 */
public class UndertowEmbeddedServletContainer implements EmbeddedServletContainer {

	private static final Log logger = LogFactory
			.getLog(UndertowEmbeddedServletContainer.class);

	private final Object monitor = new Object();

	private final Builder builder;

	private final DeploymentManager manager;

	private final String contextPath;

	private final boolean useForwardHeaders;

	private final boolean autoStart;

	private final Compression compression;

	private final String serverHeader;

	private Undertow undertow;

	private volatile boolean started = false;

	/**
	 * Create a new {@link UndertowEmbeddedServletContainer} instance.
	 * @param builder the builder
	 * @param manager the deployment manager
	 * @param contextPath the root context path
	 * @param autoStart if the server should be started
	 * @param compression compression configuration
	 */
	public UndertowEmbeddedServletContainer(Builder builder, DeploymentManager manager,
			String contextPath, boolean autoStart, Compression compression) {
		this(builder, manager, contextPath, false, autoStart, compression);
	}

	/**
	 * Create a new {@link UndertowEmbeddedServletContainer} instance.
	 * @param builder the builder
	 * @param manager the deployment manager
	 * @param contextPath the root context path
	 * @param useForwardHeaders if x-forward headers should be used
	 * @param autoStart if the server should be started
	 * @param compression compression configuration
	 */
	public UndertowEmbeddedServletContainer(Builder builder, DeploymentManager manager,
			String contextPath, boolean useForwardHeaders, boolean autoStart,
			Compression compression) {
		this(builder, manager, contextPath, useForwardHeaders, autoStart, compression,
				null);
	}

	/**
	 * Create a new {@link UndertowEmbeddedServletContainer} instance.
	 * @param builder the builder
	 * @param manager the deployment manager
	 * @param contextPath the root context path
	 * @param useForwardHeaders if x-forward headers should be used
	 * @param autoStart if the server should be started
	 * @param compression compression configuration
	 * @param serverHeader string to be used in HTTP header
	 */
	public UndertowEmbeddedServletContainer(Builder builder, DeploymentManager manager,
			String contextPath, boolean useForwardHeaders, boolean autoStart,
			Compression compression, String serverHeader) {
		this.builder = builder;
		this.manager = manager;
		this.contextPath = contextPath;
		this.useForwardHeaders = useForwardHeaders;
		this.autoStart = autoStart;
		this.compression = compression;
		this.serverHeader = serverHeader;
	}

	@Override
	public void start() throws EmbeddedServletContainerException {
		synchronized (this.monitor) {
			if (this.started) {
				return;
			}
			try {
				if (!this.autoStart) {
					return;
				}
				if (this.undertow == null) {
					this.undertow = createUndertowServer();
				}
				this.undertow.start();
				this.started = true;
				UndertowEmbeddedServletContainer.logger
						.info("Undertow started on port(s) " + getPortsDescription());
			}
			catch (Exception ex) {
				try {
					if (findBindException(ex) != null) {
						List<Port> failedPorts = getConfiguredPorts();
						List<Port> actualPorts = getActualPorts();
						failedPorts.removeAll(actualPorts);
						if (failedPorts.size() == 1) {
							throw new PortInUseException(
									failedPorts.iterator().next().getNumber());
						}
					}
					throw new EmbeddedServletContainerException(
							"Unable to start embedded Undertow", ex);
				}
				finally {
					stopSilently();
				}
			}
		}
	}

	private void stopSilently() {
		try {
			if (this.manager != null) {
				this.manager.stop();
			}
			if (this.undertow != null) {
				this.undertow.stop();
			}
		}
		catch (Exception ex) {
			// Ignore
		}
	}

	private BindException findBindException(Exception ex) {
		Throwable candidate = ex;
		while (candidate != null) {
			if (candidate instanceof BindException) {
				return (BindException) candidate;
			}
			candidate = candidate.getCause();
		}
		return null;
	}

	private Undertow createUndertowServer() throws ServletException {
		HttpHandler httpHandler = this.manager.start();
		httpHandler = getContextHandler(httpHandler);
		if (this.useForwardHeaders) {
			httpHandler = Handlers.proxyPeerAddress(httpHandler);
		}
		if (StringUtils.hasText(this.serverHeader)) {
			httpHandler = Handlers.header(httpHandler, "Server", this.serverHeader);
		}
		this.builder.setHandler(httpHandler);
		return this.builder.build();
	}

	private HttpHandler getContextHandler(HttpHandler httpHandler) {
		HttpHandler contextHandler = configurationCompressionIfNecessary(httpHandler);
		if (StringUtils.isEmpty(this.contextPath)) {
			return contextHandler;
		}
		return Handlers.path().addPrefixPath(this.contextPath, contextHandler);
	}

	private HttpHandler configurationCompressionIfNecessary(HttpHandler httpHandler) {
		if (this.compression == null || !this.compression.getEnabled()) {
			return httpHandler;
		}
		ContentEncodingRepository repository = new ContentEncodingRepository();
		repository.addEncodingHandler("gzip", new GzipEncodingProvider(), 50,
				Predicates.and(getCompressionPredicates(this.compression)));
		return new EncodingHandler(repository).setNext(httpHandler);
	}

	private Predicate[] getCompressionPredicates(Compression compression) {
		List<Predicate> predicates = new ArrayList<Predicate>();
		predicates.add(new MaxSizePredicate(compression.getMinResponseSize()));
		predicates.add(new CompressibleMimeTypePredicate(compression.getMimeTypes()));
		if (compression.getExcludedUserAgents() != null) {
			for (String agent : compression.getExcludedUserAgents()) {
				RequestHeaderAttribute agentHeader = new RequestHeaderAttribute(
						new HttpString(HttpHeaders.USER_AGENT));
				predicates.add(Predicates.not(Predicates.regex(agentHeader, agent)));
			}
		}
		return predicates.toArray(new Predicate[predicates.size()]);
	}

	private String getPortsDescription() {
		List<Port> ports = getActualPorts();
		if (!ports.isEmpty()) {
			return StringUtils.collectionToDelimitedString(ports, " ");
		}
		return "unknown";
	}

	private List<Port> getActualPorts() {
		List<Port> ports = new ArrayList<Port>();
		try {
			if (!this.autoStart) {
				ports.add(new Port(-1, "unknown"));
			}
			else {
				for (BoundChannel channel : extractChannels()) {
					ports.add(getPortFromChannel(channel));
				}
			}
		}
		catch (Exception ex) {
			// Continue
		}
		return ports;
	}

	@SuppressWarnings("unchecked")
	private List<BoundChannel> extractChannels() {
		Field channelsField = ReflectionUtils.findField(Undertow.class, "channels");
		ReflectionUtils.makeAccessible(channelsField);
		return (List<BoundChannel>) ReflectionUtils.getField(channelsField,
				this.undertow);
	}

	private Port getPortFromChannel(BoundChannel channel) {
		SocketAddress socketAddress = channel.getLocalAddress();
		if (socketAddress instanceof InetSocketAddress) {
			Field field = ReflectionUtils.findField(channel.getClass(), "ssl");
			String protocol = (field != null) ? "https" : "http";
			return new Port(((InetSocketAddress) socketAddress).getPort(), protocol);
		}
		return null;
	}

	private List<Port> getConfiguredPorts() {
		List<Port> ports = new ArrayList<Port>();
		for (Object listener : extractListeners()) {
			try {
				ports.add(getPortFromListener(listener));
			}
			catch (Exception ex) {
				// Continue
			}
		}
		return ports;
	}

	@SuppressWarnings("unchecked")
	private List<Object> extractListeners() {
		Field listenersField = ReflectionUtils.findField(Undertow.class, "listeners");
		ReflectionUtils.makeAccessible(listenersField);
		return (List<Object>) ReflectionUtils.getField(listenersField, this.undertow);
	}

	private Port getPortFromListener(Object listener) {
		Field typeField = ReflectionUtils.findField(listener.getClass(), "type");
		ReflectionUtils.makeAccessible(typeField);
		String protocol = ReflectionUtils.getField(typeField, listener).toString();
		Field portField = ReflectionUtils.findField(listener.getClass(), "port");
		ReflectionUtils.makeAccessible(portField);
		int port = (Integer) ReflectionUtils.getField(portField, listener);
		return new Port(port, protocol);
	}

	@Override
	public void stop() throws EmbeddedServletContainerException {
		synchronized (this.monitor) {
			if (!this.started) {
				return;
			}
			this.started = false;
			try {
				this.manager.stop();
				this.manager.undeploy();
				this.undertow.stop();
			}
			catch (Exception ex) {
				throw new EmbeddedServletContainerException("Unable to stop undertow",
						ex);
			}
		}
	}

	@Override
	public int getPort() {
		List<Port> ports = getActualPorts();
		if (ports.isEmpty()) {
			return 0;
		}
		return ports.get(0).getNumber();
	}

	/**
	 * An active Undertow port.
	 */
	private static final class Port {

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
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Port other = (Port) obj;
			if (this.number != other.number) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			return this.number;
		}

		@Override
		public String toString() {
			return this.number + " (" + this.protocol + ")";
		}

	}

	private static class CompressibleMimeTypePredicate implements Predicate {

		private final List<MimeType> mimeTypes;

		CompressibleMimeTypePredicate(String[] mimeTypes) {
			this.mimeTypes = new ArrayList<MimeType>(mimeTypes.length);
			for (String mimeTypeString : mimeTypes) {
				this.mimeTypes.add(MimeTypeUtils.parseMimeType(mimeTypeString));
			}
		}

		@Override
		public boolean resolve(HttpServerExchange value) {
			String contentType = value.getResponseHeaders()
					.getFirst(HttpHeaders.CONTENT_TYPE);
			if (contentType != null) {
				for (MimeType mimeType : this.mimeTypes) {
					if (mimeType
							.isCompatibleWith(MimeTypeUtils.parseMimeType(contentType))) {
						return true;
					}
				}
			}
			return false;
		}

	}

	/**
	 * Predicate that returns true if the Content-Size of a request is above a given value
	 * or is missing.
	 */
	private static class MaxSizePredicate implements Predicate {

		private final Predicate maxContentSize;

		MaxSizePredicate(int size) {
			this.maxContentSize = Predicates.maxContentSize(size);
		}

		@Override
		public boolean resolve(HttpServerExchange value) {
			if (value.getResponseHeaders().contains(Headers.CONTENT_LENGTH)) {
				return this.maxContentSize.resolve(value);
			}
			return true;
		}

	}

}
