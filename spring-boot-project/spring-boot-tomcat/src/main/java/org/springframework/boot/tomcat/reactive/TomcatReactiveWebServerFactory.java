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

package org.springframework.boot.tomcat.reactive;

import java.io.File;

import org.apache.catalina.Context;
import org.apache.catalina.Host;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.loader.WebappLoader;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.tomcat.util.scan.StandardJarScanFilter;

import org.springframework.boot.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.tomcat.DisableReferenceClearingContextCustomizer;
import org.springframework.boot.tomcat.TomcatEmbeddedContext;
import org.springframework.boot.tomcat.TomcatEmbeddedWebappClassLoader;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.tomcat.TomcatWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.reactive.ConfigurableReactiveWebServerFactory;
import org.springframework.boot.web.server.reactive.ReactiveWebServerFactory;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.TomcatHttpHandlerAdapter;
import org.springframework.util.ClassUtils;

/**
 * {@link ReactiveWebServerFactory} that can be used to create a {@link TomcatWebServer}.
 *
 * @author Brian Clozel
 * @author HaiTao Zhang
 * @author Moritz Halbritter
 * @author Scott Frederick
 * @since 4.0.0
 */
public class TomcatReactiveWebServerFactory extends TomcatWebServerFactory
		implements ConfigurableTomcatWebServerFactory, ConfigurableReactiveWebServerFactory {

	/**
	 * Create a new {@link TomcatReactiveWebServerFactory} instance.
	 */
	public TomcatReactiveWebServerFactory() {
	}

	/**
	 * Create a new {@link TomcatReactiveWebServerFactory} that listens for requests using
	 * the specified port.
	 * @param port the port to listen on
	 */
	public TomcatReactiveWebServerFactory(int port) {
		super(port);
	}

	@Override
	public WebServer getWebServer(HttpHandler httpHandler) {
		Tomcat tomcat = createTomcat();
		TomcatHttpHandlerAdapter servlet = new TomcatHttpHandlerAdapter(httpHandler);
		prepareContext(tomcat.getHost(), servlet);
		return getTomcatWebServer(tomcat);
	}

	protected void prepareContext(Host host, TomcatHttpHandlerAdapter servlet) {
		File docBase = createTempDir("tomcat-docbase");
		TomcatEmbeddedContext context = new TomcatEmbeddedContext();
		WebResourceRoot resourceRoot = new StandardRoot(context);
		ignoringNoSuchMethodError(() -> resourceRoot.setReadOnly(true));
		context.setResources(resourceRoot);
		context.setPath("");
		context.setDocBase(docBase.getAbsolutePath());
		context.addLifecycleListener(new Tomcat.FixContextListener());
		ClassLoader parentClassLoader = ClassUtils.getDefaultClassLoader();
		context.setParentClassLoader(parentClassLoader);
		skipAllTldScanning(context);
		WebappLoader loader = new WebappLoader();
		loader.setLoaderInstance(new TomcatEmbeddedWebappClassLoader(parentClassLoader));
		loader.setDelegate(true);
		context.setLoader(loader);
		Tomcat.addServlet(context, "httpHandlerServlet", servlet).setAsyncSupported(true);
		context.addServletMappingDecoded("/", "httpHandlerServlet");
		host.addChild(context);
		configureContext(context);
	}

	private void ignoringNoSuchMethodError(Runnable method) {
		try {
			method.run();
		}
		catch (NoSuchMethodError ex) {
		}
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
		this.getContextLifecycleListeners().forEach(context::addLifecycleListener);
		new DisableReferenceClearingContextCustomizer().customize(context);
		this.getContextCustomizers().forEach((customizer) -> customizer.customize(context));
	}

	/**
	 * Factory method called to create the {@link TomcatWebServer}. Subclasses can
	 * override this method to return a different {@link TomcatWebServer} or apply
	 * additional processing to the Tomcat server.
	 * @param tomcat the Tomcat server.
	 * @return a new {@link TomcatWebServer} instance
	 */
	protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
		return new TomcatWebServer(tomcat, getPort() >= 0, getShutdown());
	}

}
