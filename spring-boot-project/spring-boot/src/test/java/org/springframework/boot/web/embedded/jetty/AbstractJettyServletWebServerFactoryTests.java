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

package org.springframework.boot.web.embedded.jetty;

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;

import org.apache.jasper.servlet.JspServlet;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactory;
import org.springframework.boot.web.servlet.server.AbstractServletWebServerFactoryTests;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for {@link JettyServletWebServerFactory} tests.
 *
 * @author Phillip Webb
 */
abstract class AbstractJettyServletWebServerFactoryTests extends AbstractServletWebServerFactoryTests {

	@Override
	protected JettyServletWebServerFactory getFactory() {
		return new JettyServletWebServerFactory(0);
	}

	@Override
	protected void addConnector(int port, AbstractServletWebServerFactory factory) {
		((JettyServletWebServerFactory) factory).addServerCustomizers((server) -> {
			ServerConnector connector = new ServerConnector(server);
			connector.setPort(port);
			server.addConnector(connector);
		});
	}

	@Override
	protected JspServlet getJspServlet() throws Exception {
		WebAppContext context = (WebAppContext) ((JettyWebServer) this.webServer).getServer().getHandler();
		ServletHolder holder = context.getServletHandler().getServlet("jsp");
		if (holder == null) {
			return null;
		}
		holder.start();
		holder.initialize();
		return (JspServlet) holder.getServlet();
	}

	@Override
	protected Map<String, String> getActualMimeMappings() {
		WebAppContext context = (WebAppContext) ((JettyWebServer) this.webServer).getServer().getHandler();
		return context.getMimeTypes().getMimeMap();
	}

	@Override
	protected Charset getCharset(Locale locale) {
		WebAppContext context = (WebAppContext) ((JettyWebServer) this.webServer).getServer().getHandler();
		String charsetName = context.getLocaleEncoding(locale);
		return (charsetName != null) ? Charset.forName(charsetName) : null;
	}

	@Override
	protected void handleExceptionCausedByBlockedPortOnPrimaryConnector(RuntimeException ex, int blockedPort) {
		assertThat(ex).isInstanceOf(PortInUseException.class);
		assertThat(((PortInUseException) ex).getPort()).isEqualTo(blockedPort);
	}

	@Override
	protected void handleExceptionCausedByBlockedPortOnSecondaryConnector(RuntimeException ex, int blockedPort) {
		this.handleExceptionCausedByBlockedPortOnPrimaryConnector(ex, blockedPort);
	}

}
