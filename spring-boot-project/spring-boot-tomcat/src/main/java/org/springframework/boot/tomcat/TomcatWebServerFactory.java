/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.tomcat;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.catalina.Context;
import org.apache.catalina.Engine;
import org.apache.catalina.Executor;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.AprLifecycleListener;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http2.Http2Protocol;
import org.apache.tomcat.util.modeler.Registry;

import org.springframework.boot.util.LambdaSafe;
import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.core.NativeDetector;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for factories that produce a {@link TomcatWebServer}.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
public class TomcatWebServerFactory extends AbstractConfigurableWebServerFactory
		implements ConfigurableTomcatWebServerFactory {

	private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * The class name of default protocol used.
	 */
	public static final String DEFAULT_PROTOCOL = "org.apache.coyote.http11.Http11NioProtocol";

	private final Log logger = LogFactory.getLog(getClass());

	private File baseDirectory;

	private int backgroundProcessorDelay;

	private List<Valve> engineValves = new ArrayList<>();

	private List<Valve> contextValves = new ArrayList<>();

	private List<LifecycleListener> contextLifecycleListeners = new ArrayList<>();

	private final List<LifecycleListener> serverLifecycleListeners = getDefaultServerLifecycleListeners();

	private Set<TomcatContextCustomizer> contextCustomizers = new LinkedHashSet<>();

	private Set<TomcatConnectorCustomizer> connectorCustomizers = new LinkedHashSet<>();

	private Set<TomcatProtocolHandlerCustomizer<?>> protocolHandlerCustomizers = new LinkedHashSet<>();

	private List<Connector> additionalConnectors = new ArrayList<>();

	private Charset uriEncoding = DEFAULT_CHARSET;

	private String protocol = DEFAULT_PROTOCOL;

	private boolean disableMBeanRegistry = true;

	protected TomcatWebServerFactory() {
	}

	protected TomcatWebServerFactory(int port) {
		super(port);
	}

	private static List<LifecycleListener> getDefaultServerLifecycleListeners() {
		ArrayList<LifecycleListener> lifecycleListeners = new ArrayList<>();
		if (!NativeDetector.inNativeImage()) {
			AprLifecycleListener aprLifecycleListener = new AprLifecycleListener();
			if (AprLifecycleListener.isAprAvailable()) {
				lifecycleListeners.add(aprLifecycleListener);
			}
		}
		return lifecycleListeners;
	}

	@Override
	public void setBaseDirectory(File baseDirectory) {
		this.baseDirectory = baseDirectory;
	}

	public File getBaseDirectory() {
		return this.baseDirectory;
	}

	/**
	 * Returns a mutable collection of the {@link Valve}s that will be applied to the
	 * Tomcat {@link Engine}.
	 * @return the engine valves that will be applied
	 */
	public Collection<Valve> getEngineValves() {
		return this.engineValves;
	}

	/**
	 * Set {@link Valve}s that should be applied to the Tomcat {@link Engine}. Calling
	 * this method will replace any existing valves.
	 * @param engineValves the valves to set
	 */
	public void setEngineValves(Collection<? extends Valve> engineValves) {
		Assert.notNull(engineValves, "'engineValves' must not be null");
		this.engineValves = new ArrayList<>(engineValves);
	}

	@Override
	public void addEngineValves(Valve... engineValves) {
		Assert.notNull(engineValves, "'engineValves' must not be null");
		this.engineValves.addAll(Arrays.asList(engineValves));
	}

	public Charset getUriEncoding() {
		return this.uriEncoding;
	}

	@Override
	public void setUriEncoding(Charset uriEncoding) {
		this.uriEncoding = uriEncoding;
	}

	public int getBackgroundProcessorDelay() {
		return this.backgroundProcessorDelay;
	}

	@Override
	public void setBackgroundProcessorDelay(int delay) {
		this.backgroundProcessorDelay = delay;
	}

	public String getProtocol() {
		return this.protocol;
	}

	/**
	 * The Tomcat protocol to use when create the {@link Connector}.
	 * @param protocol the protocol
	 * @see Connector#Connector(String)
	 */
	public void setProtocol(String protocol) {
		Assert.hasLength(protocol, "'protocol' must not be empty");
		this.protocol = protocol;
	}

	/**
	 * Returns a mutable collection of the {@link Valve}s that will be applied to the
	 * Tomcat {@link Context}.
	 * @return the context valves that will be applied
	 * @see #getEngineValves()
	 */
	public Collection<Valve> getContextValves() {
		return this.contextValves;
	}

	/**
	 * Set {@link Valve}s that should be applied to the Tomcat {@link Context}. Calling
	 * this method will replace any existing valves.
	 * @param contextValves the valves to set
	 */
	public void setContextValves(Collection<? extends Valve> contextValves) {
		Assert.notNull(contextValves, "'contextValves' must not be null");
		this.contextValves = new ArrayList<>(contextValves);
	}

	/**
	 * Add {@link Valve}s that should be applied to the Tomcat {@link Context}.
	 * @param contextValves the valves to add
	 */
	public void addContextValves(Valve... contextValves) {
		Assert.notNull(contextValves, "'contextValves' must not be null");
		this.contextValves.addAll(Arrays.asList(contextValves));
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
	 * Set {@link LifecycleListener}s that should be applied to the Tomcat
	 * {@link Context}. Calling this method will replace any existing listeners.
	 * @param contextLifecycleListeners the listeners to set
	 */
	public void setContextLifecycleListeners(Collection<? extends LifecycleListener> contextLifecycleListeners) {
		Assert.notNull(contextLifecycleListeners, "'contextLifecycleListeners' must not be null");
		this.contextLifecycleListeners = new ArrayList<>(contextLifecycleListeners);
	}

	/**
	 * Add {@link LifecycleListener}s that should be added to the Tomcat {@link Context}.
	 * @param contextLifecycleListeners the listeners to add
	 */
	public void addContextLifecycleListeners(LifecycleListener... contextLifecycleListeners) {
		Assert.notNull(contextLifecycleListeners, "'contextLifecycleListeners' must not be null");
		this.contextLifecycleListeners.addAll(Arrays.asList(contextLifecycleListeners));
	}

	public List<LifecycleListener> getServerLifecycleListeners() {
		return this.serverLifecycleListeners;
	}

	/**
	 * Returns a mutable collection of the {@link TomcatContextCustomizer}s that will be
	 * applied to the Tomcat {@link Context}.
	 * @return the listeners that will be applied
	 */
	public Collection<TomcatContextCustomizer> getContextCustomizers() {
		return this.contextCustomizers;
	}

	/**
	 * Set {@link TomcatContextCustomizer}s that should be applied to the Tomcat
	 * {@link Context}. Calling this method will replace any existing customizers.
	 * @param contextCustomizers the customizers to set
	 */
	public void setContextCustomizers(Collection<? extends TomcatContextCustomizer> contextCustomizers) {
		Assert.notNull(contextCustomizers, "'contextCustomizers' must not be null");
		this.contextCustomizers = new LinkedHashSet<>(contextCustomizers);
	}

	@Override
	public void addContextCustomizers(TomcatContextCustomizer... contextCustomizers) {
		Assert.notNull(contextCustomizers, "'contextCustomizers' must not be null");
		this.contextCustomizers.addAll(Arrays.asList(contextCustomizers));
	}

	/**
	 * Returns a mutable collection of the {@link TomcatConnectorCustomizer}s that will be
	 * applied to the Tomcat {@link Connector}.
	 * @return the customizers that will be applied
	 */
	public Set<TomcatConnectorCustomizer> getConnectorCustomizers() {
		return this.connectorCustomizers;
	}

	/**
	 * Set {@link TomcatConnectorCustomizer}s that should be applied to the Tomcat
	 * {@link Connector}. Calling this method will replace any existing customizers.
	 * @param connectorCustomizers the customizers to set
	 */
	public void setConnectorCustomizers(Collection<? extends TomcatConnectorCustomizer> connectorCustomizers) {
		Assert.notNull(connectorCustomizers, "'connectorCustomizers' must not be null");
		this.connectorCustomizers = new LinkedHashSet<>(connectorCustomizers);
	}

	@Override
	public void addConnectorCustomizers(TomcatConnectorCustomizer... connectorCustomizers) {
		Assert.notNull(connectorCustomizers, "'connectorCustomizers' must not be null");
		this.connectorCustomizers.addAll(Arrays.asList(connectorCustomizers));
	}

	/**
	 * Returns a mutable collection of the {@link TomcatProtocolHandlerCustomizer}s that
	 * will be applied to the Tomcat {@link Connector}.
	 * @return the customizers that will be applied
	 */
	public Set<TomcatProtocolHandlerCustomizer<?>> getProtocolHandlerCustomizers() {
		return this.protocolHandlerCustomizers;
	}

	/**
	 * Set {@link TomcatProtocolHandlerCustomizer}s that should be applied to the Tomcat
	 * {@link Connector}. Calling this method will replace any existing customizers.
	 * @param protocolHandlerCustomizers the customizers to set
	 */
	public void setProtocolHandlerCustomizers(
			Collection<? extends TomcatProtocolHandlerCustomizer<?>> protocolHandlerCustomizers) {
		Assert.notNull(protocolHandlerCustomizers, "'protocolHandlerCustomizers' must not be null");
		this.protocolHandlerCustomizers = new LinkedHashSet<>(protocolHandlerCustomizers);
	}

	@Override
	public void addProtocolHandlerCustomizers(TomcatProtocolHandlerCustomizer<?>... protocolHandlerCustomizers) {
		Assert.notNull(protocolHandlerCustomizers, "'protocolHandlerCustomizers' must not be null");
		this.protocolHandlerCustomizers.addAll(Arrays.asList(protocolHandlerCustomizers));
	}

	/**
	 * Returns a mutable collection of the {@link Connector}s that will be added to the
	 * Tomcat server.
	 * @return the additional connectors
	 */
	public List<Connector> getAdditionalConnectors() {
		return this.additionalConnectors;
	}

	/**
	 * Set additional {@link Connector}s that should be added to the Tomcat server .
	 * Calling this method will replace any existing additional connectors.
	 * @param additionalConnectors the additionalConnectors to set
	 */
	public void setAdditionalConnectors(Collection<? extends Connector> additionalConnectors) {
		Assert.notNull(additionalConnectors, "'additionalConnectors' must not be null");
		this.additionalConnectors = new ArrayList<>(additionalConnectors);
	}

	/**
	 * Add {@link Connector}s in addition to the default connector, e.g. for SSL or AJP.
	 * <p>
	 * {@link #getConnectorCustomizers Connector customizers} are not applied to
	 * connectors added this way.
	 * @param connectors the connectors to add
	 */
	public void addAdditionalConnectors(Connector... connectors) {
		Assert.notNull(connectors, "'connectors' must not be null");
		this.additionalConnectors.addAll(Arrays.asList(connectors));
	}

	/**
	 * Returns whether the factory should disable Tomcat's MBean registry prior to
	 * creating the server.
	 * @return whether to disable Tomcat's MBean registry
	 */
	public boolean isDisableMBeanRegistry() {
		return this.disableMBeanRegistry;
	}

	/**
	 * Set whether the factory should disable Tomcat's MBean registry prior to creating
	 * the server.
	 * @param disableMBeanRegistry whether to disable the MBean registry
	 */
	public void setDisableMBeanRegistry(boolean disableMBeanRegistry) {
		this.disableMBeanRegistry = disableMBeanRegistry;
	}

	protected Tomcat createTomcat() {
		if (this.isDisableMBeanRegistry()) {
			Registry.disableRegistry();
		}
		Tomcat tomcat = new Tomcat();
		File baseDir = (getBaseDirectory() != null) ? getBaseDirectory() : createTempDir("tomcat");
		tomcat.setBaseDir(baseDir.getAbsolutePath());
		for (LifecycleListener listener : getServerLifecycleListeners()) {
			tomcat.getServer().addLifecycleListener(listener);
		}
		Connector connector = new Connector(getProtocol());
		connector.setThrowOnFailure(true);
		tomcat.getService().addConnector(connector);
		customizeConnector(connector);
		tomcat.setConnector(connector);
		registerConnectorExecutor(tomcat, connector);
		tomcat.getHost().setAutoDeploy(false);
		configureEngine(tomcat.getEngine());
		for (Connector additionalConnector : this.getAdditionalConnectors()) {
			tomcat.getService().addConnector(additionalConnector);
			registerConnectorExecutor(tomcat, additionalConnector);
		}
		return tomcat;
	}

	protected void customizeConnector(Connector connector) {
		int port = Math.max(getPort(), 0);
		connector.setPort(port);
		if (StringUtils.hasText(getServerHeader())) {
			connector.setProperty("server", getServerHeader());
		}
		if (connector.getProtocolHandler() instanceof AbstractProtocol<?> abstractProtocol) {
			customizeProtocol(abstractProtocol);
		}
		invokeProtocolHandlerCustomizers(connector.getProtocolHandler());
		if (getUriEncoding() != null) {
			connector.setURIEncoding(getUriEncoding().name());
		}
		if (getHttp2() != null && getHttp2().isEnabled()) {
			connector.addUpgradeProtocol(new Http2Protocol());
		}
		if (Ssl.isEnabled(getSsl())) {
			customizeSsl(connector);
		}
		TomcatConnectorCustomizer compression = new CompressionConnectorCustomizer(getCompression());
		compression.customize(connector);
		for (TomcatConnectorCustomizer customizer : this.getConnectorCustomizers()) {
			customizer.customize(connector);
		}
	}

	private void customizeProtocol(AbstractProtocol<?> protocol) {
		if (getAddress() != null) {
			protocol.setAddress(getAddress());
		}
	}

	@SuppressWarnings("unchecked")
	private void invokeProtocolHandlerCustomizers(ProtocolHandler protocolHandler) {
		LambdaSafe
			.callbacks(TomcatProtocolHandlerCustomizer.class, this.getProtocolHandlerCustomizers(), protocolHandler)
			.invoke((customizer) -> customizer.customize(protocolHandler));
	}

	private void customizeSsl(Connector connector) {
		SslConnectorCustomizer customizer = new SslConnectorCustomizer(this.logger, connector,
				getSsl().getClientAuth());
		customizer.customize(getSslBundle(), getServerNameSslBundles());
		addBundleUpdateHandler(null, getSsl().getBundle(), customizer);
		getSsl().getServerNameBundles()
			.forEach((serverNameSslBundle) -> addBundleUpdateHandler(serverNameSslBundle.serverName(),
					serverNameSslBundle.bundle(), customizer));
	}

	private void addBundleUpdateHandler(String serverName, String sslBundleName, SslConnectorCustomizer customizer) {
		if (StringUtils.hasText(sslBundleName)) {
			getSslBundles().addBundleUpdateHandler(sslBundleName,
					(sslBundle) -> customizer.update(serverName, sslBundle));
		}
	}

	private void registerConnectorExecutor(Tomcat tomcat, Connector connector) {
		if (connector.getProtocolHandler().getExecutor() instanceof Executor executor) {
			tomcat.getService().addExecutor(executor);
		}
	}

	private void configureEngine(Engine engine) {
		engine.setBackgroundProcessorDelay(getBackgroundProcessorDelay());
		for (Valve valve : getEngineValves()) {
			engine.getPipeline().addValve(valve);
		}
	}

}
