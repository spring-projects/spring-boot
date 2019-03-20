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

package org.springframework.boot.context.embedded.jetty;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.jasper.servlet.JspServlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.Test;
import org.mockito.InOrder;

import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactoryTests;
import org.springframework.boot.context.embedded.Compression;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.context.embedded.PortInUseException;
import org.springframework.boot.context.embedded.Ssl;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JettyEmbeddedServletContainerFactory} and
 * {@link JettyEmbeddedServletContainer}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Henri Kerola
 */
public class JettyEmbeddedServletContainerFactoryTests
		extends AbstractEmbeddedServletContainerFactoryTests {

	@Override
	protected JettyEmbeddedServletContainerFactory getFactory() {
		return new JettyEmbeddedServletContainerFactory(0);
	}

	@Test
	public void jettyConfigurations() throws Exception {
		JettyEmbeddedServletContainerFactory factory = getFactory();
		Configuration[] configurations = new Configuration[4];
		for (int i = 0; i < configurations.length; i++) {
			configurations[i] = mock(Configuration.class);
		}
		factory.setConfigurations(Arrays.asList(configurations[0], configurations[1]));
		factory.addConfigurations(configurations[2], configurations[3]);
		this.container = factory.getEmbeddedServletContainer();
		InOrder ordered = inOrder((Object[]) configurations);
		for (Configuration configuration : configurations) {
			ordered.verify(configuration).configure((WebAppContext) anyObject());
		}
	}

	@Test
	public void jettyCustomizations() throws Exception {
		JettyEmbeddedServletContainerFactory factory = getFactory();
		JettyServerCustomizer[] configurations = new JettyServerCustomizer[4];
		for (int i = 0; i < configurations.length; i++) {
			configurations[i] = mock(JettyServerCustomizer.class);
		}
		factory.setServerCustomizers(Arrays.asList(configurations[0], configurations[1]));
		factory.addServerCustomizers(configurations[2], configurations[3]);
		this.container = factory.getEmbeddedServletContainer();
		InOrder ordered = inOrder((Object[]) configurations);
		for (JettyServerCustomizer configuration : configurations) {
			ordered.verify(configuration).customize((Server) anyObject());
		}
	}

	@Test
	public void specificIPAddressNotReverseResolved() throws Exception {
		JettyEmbeddedServletContainerFactory factory = getFactory();
		InetAddress localhost = InetAddress.getLocalHost();
		factory.setAddress(InetAddress.getByAddress(localhost.getAddress()));
		this.container = factory.getEmbeddedServletContainer();
		this.container.start();
		Connector connector = ((JettyEmbeddedServletContainer) this.container).getServer()
				.getConnectors()[0];
		assertThat(((ServerConnector) connector).getHost())
				.isEqualTo(localhost.getHostAddress());
	}

	@Test
	public void sessionTimeout() throws Exception {
		JettyEmbeddedServletContainerFactory factory = getFactory();
		factory.setSessionTimeout(10);
		assertTimeout(factory, 10);
	}

	@Test
	public void sessionTimeoutInMins() throws Exception {
		JettyEmbeddedServletContainerFactory factory = getFactory();
		factory.setSessionTimeout(1, TimeUnit.MINUTES);
		assertTimeout(factory, 60);
	}

	@Test
	public void sslCiphersConfiguration() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyStorePassword("secret");
		ssl.setKeyPassword("password");
		ssl.setCiphers(new String[] { "ALPHA", "BRAVO", "CHARLIE" });

		JettyEmbeddedServletContainerFactory factory = getFactory();
		factory.setSsl(ssl);

		this.container = factory.getEmbeddedServletContainer();
		this.container.start();

		JettyEmbeddedServletContainer jettyContainer = (JettyEmbeddedServletContainer) this.container;
		ServerConnector connector = (ServerConnector) jettyContainer.getServer()
				.getConnectors()[0];
		SslConnectionFactory connectionFactory = connector
				.getConnectionFactory(SslConnectionFactory.class);
		assertThat(connectionFactory.getSslContextFactory().getIncludeCipherSuites())
				.containsExactly("ALPHA", "BRAVO", "CHARLIE");
		assertThat(connectionFactory.getSslContextFactory().getExcludeCipherSuites())
				.isEmpty();
	}

	@Test
	public void stopCalledWithoutStart() throws Exception {
		JettyEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.stop();
		Server server = ((JettyEmbeddedServletContainer) this.container).getServer();
		assertThat(server.isStopped()).isTrue();
	}

	@Override
	protected void addConnector(final int port,
			AbstractEmbeddedServletContainerFactory factory) {
		((JettyEmbeddedServletContainerFactory) factory)
				.addServerCustomizers(new JettyServerCustomizer() {

					@Override
					public void customize(Server server) {
						ServerConnector connector = new ServerConnector(server);
						connector.setPort(port);
						server.addConnector(connector);
					}

				});
	}

	@Test
	public void sslEnabledMultiProtocolsConfiguration() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyStorePassword("secret");
		ssl.setKeyPassword("password");
		ssl.setCiphers(new String[] { "ALPHA", "BRAVO", "CHARLIE" });
		ssl.setEnabledProtocols(new String[] { "TLSv1.1", "TLSv1.2" });

		JettyEmbeddedServletContainerFactory factory = getFactory();
		factory.setSsl(ssl);

		this.container = factory.getEmbeddedServletContainer();
		this.container.start();

		JettyEmbeddedServletContainer jettyContainer = (JettyEmbeddedServletContainer) this.container;
		ServerConnector connector = (ServerConnector) jettyContainer.getServer()
				.getConnectors()[0];
		SslConnectionFactory connectionFactory = connector
				.getConnectionFactory(SslConnectionFactory.class);

		assertThat(connectionFactory.getSslContextFactory().getIncludeProtocols())
				.isEqualTo(new String[] { "TLSv1.1", "TLSv1.2" });
	}

	@Test
	public void sslEnabledProtocolsConfiguration() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyStorePassword("secret");
		ssl.setKeyPassword("password");
		ssl.setCiphers(new String[] { "ALPHA", "BRAVO", "CHARLIE" });
		ssl.setEnabledProtocols(new String[] { "TLSv1.1" });

		JettyEmbeddedServletContainerFactory factory = getFactory();
		factory.setSsl(ssl);

		this.container = factory.getEmbeddedServletContainer();
		this.container.start();

		JettyEmbeddedServletContainer jettyContainer = (JettyEmbeddedServletContainer) this.container;
		ServerConnector connector = (ServerConnector) jettyContainer.getServer()
				.getConnectors()[0];
		SslConnectionFactory connectionFactory = connector
				.getConnectionFactory(SslConnectionFactory.class);

		assertThat(connectionFactory.getSslContextFactory().getIncludeProtocols())
				.isEqualTo(new String[] { "TLSv1.1" });
	}

	@Test
	public void sslEnabledSpecificIPAddress() throws Exception {
		Ssl ssl = new Ssl();
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyStorePassword("secret");
		ssl.setKeyPassword("password");

		JettyEmbeddedServletContainerFactory factory = getFactory();
		factory.setSsl(ssl);
		factory.setAddress(
				InetAddress.getByAddress(InetAddress.getLocalHost().getAddress()));

		this.container = factory.getEmbeddedServletContainer();
		this.container.start();

		JettyEmbeddedServletContainer jettyContainer = (JettyEmbeddedServletContainer) this.container;
		ServerConnector connector = (ServerConnector) jettyContainer.getServer()
				.getConnectors()[0];
		assertThat(connector.getHost()).isEqualTo(factory.getAddress().getHostAddress());
	}

	private void assertTimeout(JettyEmbeddedServletContainerFactory factory,
			int expected) {
		this.container = factory.getEmbeddedServletContainer();
		JettyEmbeddedServletContainer jettyContainer = (JettyEmbeddedServletContainer) this.container;
		Handler[] handlers = jettyContainer.getServer()
				.getChildHandlersByClass(WebAppContext.class);
		WebAppContext webAppContext = (WebAppContext) handlers[0];
		int actual = webAppContext.getSessionHandler().getMaxInactiveInterval();
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void wrappedHandlers() throws Exception {
		JettyEmbeddedServletContainerFactory factory = getFactory();
		factory.setServerCustomizers(Arrays.asList(new JettyServerCustomizer() {
			@Override
			public void customize(Server server) {
				Handler handler = server.getHandler();
				HandlerWrapper wrapper = new HandlerWrapper();
				wrapper.setHandler(handler);
				HandlerCollection collection = new HandlerCollection();
				collection.addHandler(wrapper);
				server.setHandler(collection);
			}
		}));
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.start();
		assertThat(getResponse(getLocalUrl("/hello"))).isEqualTo("Hello World");
	}

	@Test
	public void basicSslClasspathKeyStore() throws Exception {
		testBasicSslWithKeyStore("classpath:test.jks");
	}

	@Test
	public void useForwardHeaders() throws Exception {
		JettyEmbeddedServletContainerFactory factory = getFactory();
		factory.setUseForwardHeaders(true);
		assertForwardHeaderIsUsed(factory);
	}

	@Test
	public void defaultThreadPool() throws Exception {
		JettyEmbeddedServletContainerFactory factory = getFactory();
		factory.setThreadPool(null);
		assertThat(factory.getThreadPool()).isNull();
		this.container = factory.getEmbeddedServletContainer();
		assertThat(((JettyEmbeddedServletContainer) this.container).getServer()
				.getThreadPool()).isNotNull();
	}

	@Test
	public void customThreadPool() throws Exception {
		JettyEmbeddedServletContainerFactory factory = getFactory();
		ThreadPool threadPool = mock(ThreadPool.class);
		factory.setThreadPool(threadPool);
		this.container = factory.getEmbeddedServletContainer();
		assertThat(((JettyEmbeddedServletContainer) this.container).getServer()
				.getThreadPool()).isSameAs(threadPool);
	}

	@Test
	public void faultyFilterCausesStartFailure() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.addInitializers(new ServletContextInitializer() {

			@Override
			public void onStartup(ServletContext servletContext) throws ServletException {
				servletContext.addFilter("faulty", new Filter() {

					@Override
					public void init(FilterConfig filterConfig) throws ServletException {
						throw new ServletException("Faulty filter");
					}

					@Override
					public void doFilter(ServletRequest request, ServletResponse response,
							FilterChain chain) throws IOException, ServletException {
						chain.doFilter(request, response);
					}

					@Override
					public void destroy() {
					}

				});
			}

		});
		this.thrown.expect(EmbeddedServletContainerException.class);
		JettyEmbeddedServletContainer jettyContainer = (JettyEmbeddedServletContainer) factory
				.getEmbeddedServletContainer();
		try {
			jettyContainer.start();
		}
		finally {
			QueuedThreadPool threadPool = (QueuedThreadPool) jettyContainer.getServer()
					.getThreadPool();
			assertThat(threadPool.isRunning()).isFalse();
		}
	}

	@Test
	public void faultyListenerCausesStartFailure() throws Exception {
		JettyEmbeddedServletContainerFactory factory = getFactory();
		factory.addServerCustomizers(new JettyServerCustomizer() {

			@Override
			public void customize(Server server) {
				Collection<WebAppContext> contexts = server.getBeans(WebAppContext.class);
				contexts.iterator().next().addEventListener(new ServletContextListener() {

					@Override
					public void contextInitialized(ServletContextEvent event) {
						throw new RuntimeException();
					}

					@Override
					public void contextDestroyed(ServletContextEvent event) {
					}

				});
			}

		});
		this.thrown.expect(EmbeddedServletContainerException.class);
		JettyEmbeddedServletContainer jettyContainer = (JettyEmbeddedServletContainer) factory
				.getEmbeddedServletContainer();
		try {
			jettyContainer.start();
		}
		finally {
			QueuedThreadPool threadPool = (QueuedThreadPool) jettyContainer.getServer()
					.getThreadPool();
			assertThat(threadPool.isRunning()).isFalse();
		}
	}

	@Test
	public void startFailsWhenThreadPoolIsTooSmall() throws Exception {
		JettyEmbeddedServletContainerFactory factory = getFactory();
		factory.addServerCustomizers(new JettyServerCustomizer() {

			@Override
			public void customize(Server server) {
				QueuedThreadPool threadPool = server.getBean(QueuedThreadPool.class);
				threadPool.setMaxThreads(2);
				threadPool.setMinThreads(2);
			}

		});
		this.thrown.expectCause(any(IllegalStateException.class));
		factory.getEmbeddedServletContainer().start();
	}

	@Override
	@SuppressWarnings("serial")
	// Workaround for Jetty issue - https://bugs.eclipse.org/bugs/show_bug.cgi?id=470646
	protected String setUpFactoryForCompression(final int contentSize, String[] mimeTypes,
			String[] excludedUserAgents) throws Exception {
		char[] chars = new char[contentSize];
		Arrays.fill(chars, 'F');
		final String testContent = new String(chars);
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		Compression compression = new Compression();
		compression.setEnabled(true);
		if (mimeTypes != null) {
			compression.setMimeTypes(mimeTypes);
		}
		if (excludedUserAgents != null) {
			compression.setExcludedUserAgents(excludedUserAgents);
		}
		factory.setCompression(compression);
		this.container = factory.getEmbeddedServletContainer(
				new ServletRegistrationBean(new HttpServlet() {
					@Override
					protected void doGet(HttpServletRequest req, HttpServletResponse resp)
							throws ServletException, IOException {
						resp.setContentLength(contentSize);
						resp.setHeader(HttpHeaders.CONTENT_TYPE, "text/plain");
						resp.getWriter().print(testContent);
					}
				}, "/test.txt"));
		this.container.start();
		return testContent;
	}

	@Override
	protected JspServlet getJspServlet() throws Exception {
		WebAppContext context = (WebAppContext) ((JettyEmbeddedServletContainer) this.container)
				.getServer().getHandler();
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
		WebAppContext context = (WebAppContext) ((JettyEmbeddedServletContainer) this.container)
				.getServer().getHandler();
		return context.getMimeTypes().getMimeMap();
	}

	@Override
	protected Charset getCharset(Locale locale) {
		WebAppContext context = (WebAppContext) ((JettyEmbeddedServletContainer) this.container)
				.getServer().getHandler();
		String charsetName = context.getLocaleEncoding(locale);
		return (charsetName != null) ? Charset.forName(charsetName) : null;
	}

	@Override
	protected void handleExceptionCausedByBlockedPort(RuntimeException ex,
			int blockedPort) {
		assertThat(ex).isInstanceOf(PortInUseException.class);
		assertThat(((PortInUseException) ex).getPort()).isEqualTo(blockedPort);
	}

}
