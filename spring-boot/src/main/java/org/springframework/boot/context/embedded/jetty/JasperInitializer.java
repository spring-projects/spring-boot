/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.context.embedded.jetty;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

import javax.servlet.ServletContainerInitializer;

import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.webapp.WebAppContext;

import org.springframework.util.ClassUtils;

/**
 * Jetty {@link AbstractLifeCycle} to initialize Jasper.
 *
 * @author Vladimir Tsanev
 * @author Phillip Webb
 */
class JasperInitializer extends AbstractLifeCycle {

	private static final String[] INITIALIZER_CLASSES = {
			"org.eclipse.jetty.apache.jsp.JettyJasperInitializer",
			"org.apache.jasper.servlet.JasperInitializer" };

	private final WebAppContext context;

	private final ServletContainerInitializer initializer;

	JasperInitializer(WebAppContext context) {
		this.context = context;
		this.initializer = newInitializer();
	}

	private ServletContainerInitializer newInitializer() {
		for (String className : INITIALIZER_CLASSES) {
			try {
				Class<?> initializerClass = ClassUtils.forName(className, null);
				return (ServletContainerInitializer) initializerClass.newInstance();
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		return null;
	}

	@Override
	protected void doStart() throws Exception {
		if (this.initializer == null) {
			return;
		}
		try {
			URL.setURLStreamHandlerFactory(new WarUrlStreamHandlerFactory());
		}
		catch (Error ex) {
			// Ignore
		}
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(this.context.getClassLoader());
			try {
				setExtendedListenerTypes(true);
				this.initializer.onStartup(null, this.context.getServletContext());
			}
			finally {
				setExtendedListenerTypes(false);
			}
		}
		finally {
			Thread.currentThread().setContextClassLoader(classLoader);
		}
	}

	private void setExtendedListenerTypes(boolean extended) {
		try {
			this.context.getServletContext().setExtendedListenerTypes(extended);
		}
		catch (NoSuchMethodError ex) {
			// Not available on Jetty 8
		}
	}

	/**
	 * {@link URLStreamHandlerFactory} to support {@literal war} protocol.
	 */
	private static class WarUrlStreamHandlerFactory implements URLStreamHandlerFactory {

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
	private static class WarUrlStreamHandler extends URLStreamHandler {

		@Override
		protected void parseURL(URL u, String spec, int start, int limit) {
			String path = "jar:" + spec.substring("war:".length());
			int separator = path.indexOf("*/");
			if (separator >= 0) {
				path = path.substring(0, separator) + "!/"
						+ path.substring(separator + 2);
			}
			setURL(u, u.getProtocol(), "", -1, null, null, path, null, null);
		}

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

		protected WarURLConnection(URL url) throws IOException {
			super(url);
			this.connection = new URL(url.getFile()).openConnection();
		}

		@Override
		public void connect() throws IOException {
			if (!this.connected) {
				this.connection.connect();
				this.connected = true;
			}
		}

		@Override
		public InputStream getInputStream() throws IOException {
			connect();
			return this.connection.getInputStream();
		}

	}

}
