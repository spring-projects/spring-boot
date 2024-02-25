/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.web.embedded.jetty;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import jakarta.servlet.ServletContainerInitializer;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.util.component.AbstractLifeCycle;

import org.springframework.util.ClassUtils;

/**
 * Jetty {@link AbstractLifeCycle} to initialize Jasper.
 *
 * @author Vladimir Tsanev
 * @author Phillip Webb
 */
class JasperInitializer extends AbstractLifeCycle {

	private static final String[] INITIALIZER_CLASSES = { "org.eclipse.jetty.apache.jsp.JettyJasperInitializer",
			"org.apache.jasper.servlet.JasperInitializer" };

	private final WebAppContext context;

	private final ServletContainerInitializer initializer;

	/**
	 * Constructs a new JasperInitializer with the specified WebAppContext.
	 * @param context the WebAppContext to be initialized
	 */
	JasperInitializer(WebAppContext context) {
		this.context = context;
		this.initializer = newInitializer();
	}

	/**
	 * Creates a new instance of a {@link ServletContainerInitializer} by iterating
	 * through a list of class names.
	 * @return a new instance of a {@link ServletContainerInitializer}, or {@code null} if
	 * none of the class names could be instantiated
	 */
	private ServletContainerInitializer newInitializer() {
		for (String className : INITIALIZER_CLASSES) {
			try {
				Class<?> initializerClass = ClassUtils.forName(className, null);
				return (ServletContainerInitializer) initializerClass.getDeclaredConstructor().newInstance();
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		return null;
	}

	/**
	 * Starts the initialization process.
	 * @throws Exception if an error occurs during initialization
	 */
	@Override
	protected void doStart() throws Exception {
		if (this.initializer == null) {
			return;
		}
		if (ClassUtils.isPresent("org.apache.catalina.webresources.TomcatURLStreamHandlerFactory",
				getClass().getClassLoader())) {
			org.apache.catalina.webresources.TomcatURLStreamHandlerFactory.register();
		}
		else {
			try {
				URL.setURLStreamHandlerFactory(new WarUrlStreamHandlerFactory());
			}
			catch (Error ex) {
				// Ignore
			}
		}
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(this.context.getClassLoader());
			try {
				this.context.getContext().setExtendedListenerTypes(true);
				this.initializer.onStartup(null, this.context.getServletContext());
			}
			finally {
				this.context.getContext().setExtendedListenerTypes(false);
			}
		}
		finally {
			Thread.currentThread().setContextClassLoader(classLoader);
		}
	}

	/**
	 * {@link URLStreamHandlerFactory} to support {@literal war} protocol.
	 */
	private static final class WarUrlStreamHandlerFactory implements URLStreamHandlerFactory {

		/**
		 * Creates a URL stream handler for the specified protocol.
		 * @param protocol the protocol for which the URL stream handler is to be created
		 * @return the URL stream handler for the specified protocol, or null if no
		 * handler is found
		 */
		@Override
		public URLStreamHandler createURLStreamHandler(String protocol) {
			if ("war".equals(protocol)) {
				return new WarUrlStreamHandler();
			}
			return null;
		}

	}

	/**
	 * {@link URLStreamHandler} for {@literal war} protocol compatible with jasper's
	 * {@link URL urls} produced by
	 * {@link org.apache.tomcat.util.scan.JarFactory#getJarEntryURL(URL, String)}.
	 */
	private static final class WarUrlStreamHandler extends URLStreamHandler {

		/**
		 * Parses the URL and sets the necessary properties for a WAR URL.
		 * @param u the URL object to be parsed
		 * @param spec the string representation of the URL
		 * @param start the starting index of the substring to be parsed
		 * @param limit the ending index of the substring to be parsed
		 */
		@Override
		protected void parseURL(URL u, String spec, int start, int limit) {
			String path = "jar:" + spec.substring("war:".length());
			int separator = path.indexOf("*/");
			if (separator >= 0) {
				path = path.substring(0, separator) + "!/" + path.substring(separator + 2);
			}
			setURL(u, u.getProtocol(), "", -1, null, null, path, null, null);
		}

		/**
		 * Opens a connection to the specified URL.
		 * @param u the URL to open a connection to
		 * @return a URLConnection object representing the connection to the specified URL
		 * @throws IOException if an I/O error occurs while opening the connection
		 */
		@Override
		protected URLConnection openConnection(URL u) throws IOException {
			return new WarURLConnection(u);
		}

	}

	/**
	 * {@link URLConnection} to support {@literal war} protocol.
	 */
	private static class WarURLConnection extends URLConnection {

		private final URLConnection connection;

		/**
		 * Constructs a new WarURLConnection with the specified URL.
		 * @param url the URL to connect to
		 * @throws IOException if an I/O error occurs while opening the connection
		 */
		protected WarURLConnection(URL url) throws IOException {
			super(url);
			this.connection = new URL(url.getFile()).openConnection();
		}

		/**
		 * Establishes a connection to the server.
		 * @throws IOException if an I/O error occurs while connecting
		 */
		@Override
		public void connect() throws IOException {
			if (!this.connected) {
				this.connection.connect();
				this.connected = true;
			}
		}

		/**
		 * Returns an input stream that reads from this URL connection.
		 * @return an input stream that reads from this URL connection
		 * @throws IOException if an I/O error occurs while creating the input stream
		 */
		@Override
		public InputStream getInputStream() throws IOException {
			connect();
			return this.connection.getInputStream();
		}

	}

}
