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

import java.lang.reflect.Field;
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
import io.undertow.util.HttpString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xnio.channels.BoundChannel;

import org.springframework.boot.context.embedded.Compression;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
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
 * @since 1.2.0
 * @see UndertowEmbeddedServletContainerFactory
 */
public class UndertowEmbeddedServletContainer implements EmbeddedServletContainer {

	private static final Log logger = LogFactory
			.getLog(UndertowEmbeddedServletContainer.class);

	private final Builder builder;

	private final DeploymentManager manager;

	private final String contextPath;

	private final boolean useForwardHeaders;

	private final boolean autoStart;

	private final Compression compression;

	private Undertow undertow;

	private boolean started = false;

	public UndertowEmbeddedServletContainer(Builder builder, DeploymentManager manager,
			String contextPath, int port, boolean autoStart, Compression compression) {
		this(builder, manager, contextPath, port, false, autoStart, compression);
	}

	public UndertowEmbeddedServletContainer(Builder builder, DeploymentManager manager,
			String contextPath, int port, boolean useForwardHeaders, boolean autoStart,
			Compression compression) {
		this.builder = builder;
		this.manager = manager;
		this.contextPath = contextPath;
		this.useForwardHeaders = useForwardHeaders;
		this.autoStart = autoStart;
		this.compression = compression;
	}

	@Override
	public synchronized void start() throws EmbeddedServletContainerException {
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
		catch (ServletException ex) {
			throw new EmbeddedServletContainerException(
					"Unable to start embedded Undertow", ex);
		}
	}

	private Undertow createUndertowServer() throws ServletException {
		HttpHandler httpHandler = this.manager.start();
		httpHandler = getContextHandler(httpHandler);
		if (this.useForwardHeaders) {
			httpHandler = Handlers.proxyPeerAddress(httpHandler);
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
		predicates.add(Predicates.maxContentSize(compression.getMinResponseSize()));
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
		List<Port> ports = getPorts();
		if (!ports.isEmpty()) {
			return StringUtils.collectionToDelimitedString(ports, " ");
		}
		return "unknown";
	}

	private List<Port> getPorts() {
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
		String protocol = ReflectionUtils.findField(channel.getClass(), "ssl") != null
				? "https" : "http";
		SocketAddress socketAddress = channel.getLocalAddress();
		if (socketAddress instanceof InetSocketAddress) {
			return new Port(((InetSocketAddress) socketAddress).getPort(), protocol);
		}
		return null;
	}

	@Override
	public synchronized void stop() throws EmbeddedServletContainerException {
		if (this.started) {
			try {
				this.started = false;
				this.manager.stop();
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
		List<Port> ports = getPorts();
		if (ports.isEmpty()) {
			return 0;
		}
		return ports.get(0).getNumber();
	}

	/**
	 * An active undertow port.
	 */
	private final static class Port {

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

}
