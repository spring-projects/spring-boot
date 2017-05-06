/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.web.embedded.tomcat;

import java.io.File;

import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.apache.coyote.AbstractProtocol;

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
public class TomcatReactiveWebServerFactory extends AbstractReactiveWebServerFactory {

	/**
	 * The class name of default protocol used.
	 */
	public static final String DEFAULT_PROTOCOL = "org.apache.coyote.http11.Http11NioProtocol";

	private String protocol = DEFAULT_PROTOCOL;

	/**
	 * Create a new {@link TomcatServletWebServerFactory} instance.
	 */
	public TomcatReactiveWebServerFactory() {
		super();
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
		Tomcat tomcatServer = createTomcatServer();
		TomcatHttpHandlerAdapter servlet = new TomcatHttpHandlerAdapter(httpHandler);
		prepareContext(tomcatServer.getHost(), servlet);
		return new TomcatWebServer(tomcatServer, getPort() >= 0);
	}

	private Tomcat createTomcatServer() {
		Tomcat tomcat = new Tomcat();
		File baseDir = createTempDir("tomcat");
		tomcat.setBaseDir(baseDir.getAbsolutePath());
		Connector connector = new Connector(this.protocol);
		tomcat.getService().addConnector(connector);
		customizeConnector(connector);
		tomcat.setConnector(connector);
		tomcat.getHost().setAutoDeploy(false);
		return tomcat;
	}

	protected void prepareContext(Host host, TomcatHttpHandlerAdapter servlet) {
		File docBase = createTempDir("tomcat-docbase");
		TomcatEmbeddedContext context = new TomcatEmbeddedContext();
		context.setPath("");
		context.setDocBase(docBase.getAbsolutePath());
		context.addLifecycleListener(new Tomcat.FixContextListener());
		context.setParentClassLoader(ClassUtils.getDefaultClassLoader());
		WebappLoader loader = new WebappLoader(context.getParentClassLoader());
		loader.setLoaderClass(TomcatEmbeddedWebappClassLoader.class.getName());
		loader.setDelegate(true);
		context.setLoader(loader);
		Tomcat.addServlet(context, "httpHandlerServlet", servlet);
		context.addServletMappingDecoded("/", "httpHandlerServlet");
		host.addChild(context);
	}

	// Needs to be protected so it can be used by subclasses
	protected void customizeConnector(Connector connector) {
		int port = (getPort() >= 0 ? getPort() : 0);
		connector.setPort(port);
		if (StringUtils.hasText(this.getServerHeader())) {
			connector.setAttribute("server", this.getServerHeader());
		}
		if (connector.getProtocolHandler() instanceof AbstractProtocol) {
			customizeProtocol((AbstractProtocol<?>) connector.getProtocolHandler());
		}

		// If ApplicationContext is slow to start we want Tomcat not to bind to the socket
		// prematurely...
		connector.setProperty("bindOnInit", "false");
	}

	private void customizeProtocol(AbstractProtocol<?> protocol) {
		if (getAddress() != null) {
			protocol.setAddress(getAddress());
		}
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
