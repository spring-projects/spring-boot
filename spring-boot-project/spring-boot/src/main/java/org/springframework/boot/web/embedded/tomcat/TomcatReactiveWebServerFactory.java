/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.web.embedded.tomcat;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.http2.Http2Protocol;
import org.apache.tomcat.util.scan.StandardJarScanFilter;

import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.TomcatHttpHandlerAdapter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * {@link ReactiveWebServerFactory} that can be used to create a {@link TomcatWebServer}.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class TomcatReactiveWebServerFactory extends AbstractReactiveWebServerFactory
		implements ConfigurableTomcatWebServerFactory {

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * The class name of default protocol used.
	 */
	public static final String DEFAULT_PROTOCOL = "org.apache.coyote.http11.Http11NioProtocol";

	private File baseDirectory;

	private List<Valve> engineValves = new ArrayList<>();

	private List<LifecycleListener> contextLifecycleListeners = new ArrayList<>(
			Collections.singleton(new AprLifecycleListener()));

	private List<TomcatContextCustomizer> tomcatContextCustomizers = new ArrayList<>();

	private List<TomcatConnectorCustomizer> tomcatConnectorCustomizers = new ArrayList<>();

	private String protocol = DEFAULT_PROTOCOL;

	private Charset uriEncoding = DEFAULT_CHARSET;

	private int backgroundProcessorDelay;

	/**
	 * Create a new {@link TomcatServletWebServerFactory} instance.
	 */
	public TomcatReactiveWebServerFactory() {
	}

	/**
	 * Create a new {@link TomcatServletWebServerFactory} that listens for requests using
	 * the specified port.
	 * @param port the port to listen on
	 */
	public TomcatReactiveWebServerFactory(int port) {
		super(port);
	}

	@Override
	public WebServer getWebServer(HttpHandler httpHandler) {
		Tomcat tomcat = new Tomcat();
		File baseDir = (this.baseDirectory != null) ? this.baseDirectory
				: createTempDir("tomcat");
		tomcat.setBaseDir(baseDir.getAbsolutePath());
		Connector connector = new Connector(this.protocol);
		tomcat.getService().addConnector(connector);
		customizeConnector(connector);
		tomcat.setConnector(connector);
		tomcat.getHost().setAutoDeploy(false);
		configureEngine(tomcat.getEngine());
		TomcatHttpHandlerAdapter servlet = new TomcatHttpHandlerAdapter(httpHandler);
		prepareContext(tomcat.getHost(), servlet);
		return new TomcatWebServer(tomcat, getPort() >= 0);
	}

	private void configureEngine(Engine engine) {
		engine.setBackgroundProcessorDelay(this.backgroundProcessorDelay);
		for (Valve valve : this.engineValves) {
			engine.getPipeline().addValve(valve);
		}
	}

	protected void prepareContext(Host host, TomcatHttpHandlerAdapter servlet) {
		File docBase = createTempDir("tomcat-docbase");
		TomcatEmbeddedContext context = new TomcatEmbeddedContext();
		context.setPath("");
		context.setDocBase(docBase.getAbsolutePath());
		context.addLifecycleListener(new Tomcat.FixContextListener());
		context.setParentClassLoader(ClassUtils.getDefaultClassLoader());
		skipAllTldScanning(context);
		WebappLoader loader = new WebappLoader(context.getParentClassLoader());
		loader.setLoaderClass(TomcatEmbeddedWebappClassLoader.class.getName());
		loader.setDelegate(true);
		context.setLoader(loader);
		Tomcat.addServlet(context, "httpHandlerServlet", servlet).setAsyncSupported(true);
		context.addServletMappingDecoded("/", "httpHandlerServlet");
		host.addChild(context);
		configureContext(context);
	}

	private void skipAllTldScanning(TomcatEmbeddedContext context) {
		StandardJarScanFilter filter = new StandardJarScanFilter();
		filter.setTldSkip("*.jar");
		context.getJarScanner().setJarScanFilter(filter);
	}

	/**
	 * Configure the Tomcat {@link Context}.
	 * @param context the Tomcat context
	 */
	protected void configureContext(Context context) {
		this.contextLifecycleListeners.forEach(context::addLifecycleListener);
		new DisableReferenceClearingContextCustomizer().customize(context);
		this.tomcatContextCustomizers
				.forEach((customizer) -> customizer.customize(context));
	}

	protected void customizeConnector(Connector connector) {
		int port = (getPort() >= 0) ? getPort() : 0;
		connector.setPort(port);
		if (StringUtils.hasText(this.getServerHeader())) {
			connector.setAttribute("server", this.getServerHeader());
		}
		if (connector.getProtocolHandler() instanceof AbstractProtocol) {
			customizeProtocol((AbstractProtocol<?>) connector.getProtocolHandler());
		}
		if (getUriEncoding() != null) {
			connector.setURIEncoding(getUriEncoding().name());
		}
		// Don't bind to the socket prematurely if ApplicationContext is slow to start
		connector.setProperty("bindOnInit", "false");
		if (getSsl() != null && getSsl().isEnabled()) {
			customizeSsl(connector);
		}
		TomcatConnectorCustomizer compression = new CompressionConnectorCustomizer(
				getCompression());
		compression.customize(connector);
		for (TomcatConnectorCustomizer customizer : this.tomcatConnectorCustomizers) {
			customizer.customize(connector);
		}
	}

	private void customizeProtocol(AbstractProtocol<?> protocol) {
		if (getAddress() != null) {
			protocol.setAddress(getAddress());
		}
	}

	private void customizeSsl(Connector connector) {
		new SslConnectorCustomizer(getSsl(), getSslStoreProvider()).customize(connector);
		if (getHttp2() != null && getHttp2().isEnabled()) {
			connector.addUpgradeProtocol(new Http2Protocol());
		}
	}

	@Override
	public void setBaseDirectory(File baseDirectory) {
		this.baseDirectory = baseDirectory;
	}

	@Override
	public void setBackgroundProcessorDelay(int delay) {
		this.backgroundProcessorDelay = delay;
	}

	/**
	 * Set {@link TomcatContextCustomizer}s that should be applied to the Tomcat
	 * {@link Context}. Calling this method will replace any existing customizers.
	 * @param tomcatContextCustomizers the customizers to set
	 */
	public void setTomcatContextCustomizers(
			Collection<? extends TomcatContextCustomizer> tomcatContextCustomizers) {
		Assert.notNull(tomcatContextCustomizers,
				"TomcatContextCustomizers must not be null");
		this.tomcatContextCustomizers = new ArrayList<>(tomcatContextCustomizers);
	}

	/**
	 * Returns a mutable collection of the {@link TomcatContextCustomizer}s that will be
	 * applied to the Tomcat {@link Context}.
	 * @return the listeners that will be applied
	 */
	public Collection<TomcatContextCustomizer> getTomcatContextCustomizers() {
		return this.tomcatContextCustomizers;
	}

	/**
	 * Add {@link TomcatContextCustomizer}s that should be added to the Tomcat
	 * {@link Context}.
	 * @param tomcatContextCustomizers the customizers to add
	 */
	@Override
	public void addContextCustomizers(
			TomcatContextCustomizer... tomcatContextCustomizers) {
		Assert.notNull(tomcatContextCustomizers,
				"TomcatContextCustomizers must not be null");
		this.tomcatContextCustomizers.addAll(Arrays.asList(tomcatContextCustomizers));
	}

	/**
	 * Set {@link TomcatConnectorCustomizer}s that should be applied to the Tomcat
	 * {@link Connector}. Calling this method will replace any existing customizers.
	 * @param tomcatConnectorCustomizers the customizers to set
	 */
	public void setTomcatConnectorCustomizers(
			Collection<? extends TomcatConnectorCustomizer> tomcatConnectorCustomizers) {
		Assert.notNull(tomcatConnectorCustomizers,
				"TomcatConnectorCustomizers must not be null");
		this.tomcatConnectorCustomizers = new ArrayList<>(tomcatConnectorCustomizers);
	}

	/**
	 * Add {@link TomcatConnectorCustomizer}s that should be added to the Tomcat
	 * {@link Connector}.
	 * @param tomcatConnectorCustomizers the customizers to add
	 */
	@Override
	public void addConnectorCustomizers(
			TomcatConnectorCustomizer... tomcatConnectorCustomizers) {
		Assert.notNull(tomcatConnectorCustomizers,
				"TomcatConnectorCustomizers must not be null");
		this.tomcatConnectorCustomizers.addAll(Arrays.asList(tomcatConnectorCustomizers));
	}

	/**
	 * Returns a mutable collection of the {@link TomcatConnectorCustomizer}s that will be
	 * applied to the Tomcat {@link Connector}.
	 * @return the customizers that will be applied
	 */
	public Collection<TomcatConnectorCustomizer> getTomcatConnectorCustomizers() {
		return this.tomcatConnectorCustomizers;
	}

	@Override
	public void addEngineValves(Valve... engineValves) {
		Assert.notNull(engineValves, "Valves must not be null");
		this.engineValves.addAll(Arrays.asList(engineValves));
	}

	/**
	 * Returns a mutable collection of the {@link Valve}s that will be applied to the
	 * Tomcat {@link Engine}.
	 * @return the engine valves that will be applied
	 */
	public List<Valve> getEngineValves() {
		return this.engineValves;
	}

	/**
	 * Set the character encoding to use for URL decoding. If not specified 'UTF-8' will
	 * be used.
	 * @param uriEncoding the uri encoding to set
	 */
	@Override
	public void setUriEncoding(Charset uriEncoding) {
		this.uriEncoding = uriEncoding;
	}

	/**
	 * Returns the character encoding to use for URL decoding.
	 * @return the URI encoding
	 */
	public Charset getUriEncoding() {
		return this.uriEncoding;
	}

	/**
	 * Set {@link LifecycleListener}s that should be applied to the Tomcat
	 * {@link Context}. Calling this method will replace any existing listeners.
	 * @param contextLifecycleListeners the listeners to set
	 */
	public void setContextLifecycleListeners(
			Collection<? extends LifecycleListener> contextLifecycleListeners) {
		Assert.notNull(contextLifecycleListeners,
				"ContextLifecycleListeners must not be null");
		this.contextLifecycleListeners = new ArrayList<>(contextLifecycleListeners);
	}

	/**
	 * Returns a mutable collection of the {@link LifecycleListener}s that will be applied
	 * to the Tomcat {@link Context}.
	 * @return the context lifecycle listeners that will be applied
	 */
	public Collection<LifecycleListener> getContextLifecycleListeners() {
		return this.contextLifecycleListeners;
	}

	/**
	 * Add {@link LifecycleListener}s that should be added to the Tomcat {@link Context}.
	 * @param contextLifecycleListeners the listeners to add
	 */
	public void addContextLifecycleListeners(
			LifecycleListener... contextLifecycleListeners) {
		Assert.notNull(contextLifecycleListeners,
				"ContextLifecycleListeners must not be null");
		this.contextLifecycleListeners.addAll(Arrays.asList(contextLifecycleListeners));
	}

	/**
	 * Factory method called to create the {@link TomcatWebServer}. Subclasses can
	 * override this method to return a different {@link TomcatWebServer} or apply
	 * additional processing to the Tomcat server.
	 * @param tomcat the Tomcat server.
	 * @return a new {@link TomcatWebServer} instance
	 */
	protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
		return new TomcatWebServer(tomcat, getPort() >= 0);
	}

	/**
	 * The Tomcat protocol to use when create the {@link Connector}.
	 * @param protocol the protocol
	 * @see Connector#Connector(String)
	 */
	public void setProtocol(String protocol) {
		Assert.hasLength(protocol, "Protocol must not be empty");
		this.protocol = protocol;
	}

}
