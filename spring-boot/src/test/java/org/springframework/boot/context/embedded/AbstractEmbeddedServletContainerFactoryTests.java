/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.embedded;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import javax.servlet.GenericServlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.InOrder;
import org.springframework.boot.context.embedded.Ssl.ClientAuth;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.SocketUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.concurrent.ListenableFuture;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Base for testing classes that extends {@link AbstractEmbeddedServletContainerFactory}.
 *
 * @author Phillip Webb
 * @author Greg Turnquist
 * @author Andy Wilkinson
 */
public abstract class AbstractEmbeddedServletContainerFactoryTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	protected EmbeddedServletContainer container;

	@After
	public void teardown() {
		if (this.container != null) {
			try {
				this.container.stop();
			}
			catch (Exception ex) {
			}
		}
	}

	@Test
	public void startServlet() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.start();
		assertThat(getResponse(getLocalUrl("/hello")), equalTo("Hello World"));
	}

	@Test
	public void emptyServerWhenPortIsMinusOne() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setPort(-1);
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.start();
		assertThat(this.container.getPort(), lessThan(0)); // Jetty is -2
	}

	@Test
	public void stopServlet() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.start();
		int port = this.container.getPort();
		this.container.stop();
		this.thrown.expect(IOException.class);
		String response = getResponse(getLocalUrl(port, "/hello"));
		throw new RuntimeException("Unexpected response on port " + port + " : "
				+ response);
	}

	@Test
	public void restartWithKeepAlive() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.start();
		HttpComponentsAsyncClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsAsyncClientHttpRequestFactory();
		ListenableFuture<ClientHttpResponse> response1 = clientHttpRequestFactory
				.createAsyncRequest(new URI(getLocalUrl("/hello")), HttpMethod.GET)
				.executeAsync();
		assertThat(response1.get(10, TimeUnit.SECONDS).getRawStatusCode(), equalTo(200));

		this.container.stop();
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.start();

		ListenableFuture<ClientHttpResponse> response2 = clientHttpRequestFactory
				.createAsyncRequest(new URI(getLocalUrl("/hello")), HttpMethod.GET)
				.executeAsync();
		assertThat(response2.get(10, TimeUnit.SECONDS).getRawStatusCode(), equalTo(200));
	}

	@Test
	public void startServletAndFilter() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory.getEmbeddedServletContainer(
				exampleServletRegistration(), new FilterRegistrationBean(
						new ExampleFilter()));
		this.container.start();
		assertThat(getResponse(getLocalUrl("/hello")), equalTo("[Hello World]"));
	}

	@Test
	public void startBlocksUntilReadyToServe() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		final Date[] date = new Date[1];
		this.container = factory
				.getEmbeddedServletContainer(new ServletContextInitializer() {
					@Override
					public void onStartup(ServletContext servletContext)
							throws ServletException {
						try {
							Thread.sleep(500);
							date[0] = new Date();
						}
						catch (InterruptedException ex) {
							throw new ServletException(ex);
						}
					}
				});
		this.container.start();
		assertThat(date[0], notNullValue());
	}

	@Test
	public void loadOnStartAfterContextIsInitialized() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		final InitCountingServlet servlet = new InitCountingServlet();
		this.container = factory
				.getEmbeddedServletContainer(new ServletContextInitializer() {
					@Override
					public void onStartup(ServletContext servletContext)
							throws ServletException {
						servletContext.addServlet("test", servlet).setLoadOnStartup(1);
					}
				});
		assertThat(servlet.getInitCount(), equalTo(0));
		this.container.start();
		assertThat(servlet.getInitCount(), equalTo(1));
	}

	@Test
	public void specificPort() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		int specificPort = SocketUtils.findAvailableTcpPort(40000);
		factory.setPort(specificPort);
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.start();
		assertThat(getResponse("http://localhost:" + specificPort + "/hello"),
				equalTo("Hello World"));
		assertEquals(specificPort, this.container.getPort());
	}

	@Test
	public void specificContextRoot() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setContextPath("/say");
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.start();
		assertThat(getResponse(getLocalUrl("/say/hello")), equalTo("Hello World"));
	}

	@Test
	public void contextPathMustStartWithSlash() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ContextPath must start with '/ and not end with '/'");
		getFactory().setContextPath("missingslash");
	}

	@Test
	public void contextPathMustNotEndWithSlash() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ContextPath must start with '/ and not end with '/'");
		getFactory().setContextPath("extraslash/");
	}

	@Test
	public void contextRootPathMustNotBeSlash() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown
				.expectMessage("Root ContextPath must be specified using an empty string");
		getFactory().setContextPath("/");
	}

	@Test
	public void doubleStop() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.start();
		this.container.stop();
		this.container.stop();
	}

	@Test
	public void multipleConfigurations() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		ServletContextInitializer[] initializers = new ServletContextInitializer[6];
		for (int i = 0; i < initializers.length; i++) {
			initializers[i] = mock(ServletContextInitializer.class);
		}
		factory.setInitializers(Arrays.asList(initializers[2], initializers[3]));
		factory.addInitializers(initializers[4], initializers[5]);
		this.container = factory.getEmbeddedServletContainer(initializers[0],
				initializers[1]);
		this.container.start();
		InOrder ordered = inOrder((Object[]) initializers);
		for (ServletContextInitializer initializer : initializers) {
			ordered.verify(initializer).onStartup((ServletContext) anyObject());
		}
	}

	@Test
	public void documentRoot() throws Exception {
		FileCopyUtils.copy("test",
				new FileWriter(this.temporaryFolder.newFile("test.txt")));
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setDocumentRoot(this.temporaryFolder.getRoot());
		this.container = factory.getEmbeddedServletContainer();
		this.container.start();
		assertThat(getResponse(getLocalUrl("/test.txt")), equalTo("test"));
	}

	@Test
	public void mimeType() throws Exception {
		FileCopyUtils.copy("test",
				new FileWriter(this.temporaryFolder.newFile("test.xxcss")));
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setDocumentRoot(this.temporaryFolder.getRoot());
		MimeMappings mimeMappings = new MimeMappings();
		mimeMappings.add("xxcss", "text/css");
		factory.setMimeMappings(mimeMappings);
		this.container = factory.getEmbeddedServletContainer();
		this.container.start();
		ClientHttpResponse response = getClientResponse(getLocalUrl("/test.xxcss"));
		assertThat(response.getHeaders().getContentType().toString(), equalTo("text/css"));
		response.close();
	}

	@Test
	public void errorPage() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.addErrorPages(new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/hello"));
		this.container = factory.getEmbeddedServletContainer(
				exampleServletRegistration(), errorServletRegistration());
		this.container.start();
		assertThat(getResponse(getLocalUrl("/hello")), equalTo("Hello World"));
		assertThat(getResponse(getLocalUrl("/bang")), equalTo("Hello World"));
	}

	@Test
	public void basicSsl() throws Exception {
		testBasicSllWithKeystore("src/test/resources/test.jks");
	}

	protected final void testBasicSllWithKeystore(String keyStore) throws Exception {
		FileCopyUtils.copy("test",
				new FileWriter(this.temporaryFolder.newFile("test.txt")));

		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setDocumentRoot(this.temporaryFolder.getRoot());

		Ssl ssl = new Ssl();
		ssl.setKeyStore(keyStore);
		ssl.setKeyStorePassword("secret");
		ssl.setKeyPassword("password");
		factory.setSsl(ssl);

		this.container = factory.getEmbeddedServletContainer();
		this.container.start();

		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder().loadTrustMaterial(null,
						new TrustSelfSignedStrategy()).build());

		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory)
				.build();

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
				httpClient);

		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory),
				equalTo("test"));
	}

	@Test
	public void pkcs12KeyStoreAndTrustStore() throws Exception {
		FileCopyUtils.copy("test",
				new FileWriter(this.temporaryFolder.newFile("test.txt")));

		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setDocumentRoot(this.temporaryFolder.getRoot());

		Ssl ssl = new Ssl();
		ssl.setKeyStore("src/test/resources/test.p12");
		ssl.setKeyStorePassword("secret");
		ssl.setKeyStoreType("pkcs12");
		ssl.setTrustStore("src/test/resources/test.p12");
		ssl.setTrustStorePassword("secret");
		ssl.setTrustStoreType("pkcs12");
		ssl.setClientAuth(ClientAuth.NEED);
		factory.setSsl(ssl);

		this.container = factory.getEmbeddedServletContainer();
		this.container.start();

		KeyStore keyStore = KeyStore.getInstance("pkcs12");
		keyStore.load(new FileInputStream(new File("src/test/resources/test.p12")),
				"secret".toCharArray());

		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder()
						.loadTrustMaterial(null, new TrustSelfSignedStrategy())
						.loadKeyMaterial(keyStore, "secret".toCharArray()).build());

		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory)
				.build();

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
				httpClient);

		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory),
				equalTo("test"));
	}

	@Test
	public void sslNeedsClientAuthenticationSucceedsWithClientCertificate()
			throws Exception {
		FileCopyUtils.copy("test",
				new FileWriter(this.temporaryFolder.newFile("test.txt")));

		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setDocumentRoot(this.temporaryFolder.getRoot());

		Ssl ssl = new Ssl();
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyStorePassword("secret");
		ssl.setKeyPassword("password");
		ssl.setClientAuth(ClientAuth.NEED);
		ssl.setTrustStore("src/test/resources/test.jks");
		ssl.setTrustStorePassword("secret");
		factory.setSsl(ssl);

		this.container = factory.getEmbeddedServletContainer();
		this.container.start();

		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(new FileInputStream(new File("src/test/resources/test.jks")),
				"secret".toCharArray());

		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder()
						.loadTrustMaterial(null, new TrustSelfSignedStrategy())
						.loadKeyMaterial(keyStore, "password".toCharArray()).build());

		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory)
				.build();

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
				httpClient);

		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory),
				equalTo("test"));
	}

	@Test(expected = IOException.class)
	public void sslNeedsClientAuthenticationFailsWithoutClientCertificate()
			throws Exception {
		FileCopyUtils.copy("test",
				new FileWriter(this.temporaryFolder.newFile("test.txt")));

		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setDocumentRoot(this.temporaryFolder.getRoot());

		Ssl ssl = new Ssl();
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyStorePassword("secret");
		ssl.setKeyPassword("password");
		ssl.setClientAuth(ClientAuth.NEED);
		ssl.setTrustStore("src/test/resources/test.jks");
		ssl.setTrustStorePassword("secret");
		factory.setSsl(ssl);

		this.container = factory.getEmbeddedServletContainer();
		this.container.start();

		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder().loadTrustMaterial(null,
						new TrustSelfSignedStrategy()).build());

		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory)
				.build();

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
				httpClient);

		getResponse(getLocalUrl("https", "/test.txt"), requestFactory);
	}

	@Test
	public void sslWantsClientAuthenticationSucceedsWithClientCertificate()
			throws Exception {
		FileCopyUtils.copy("test",
				new FileWriter(this.temporaryFolder.newFile("test.txt")));

		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setDocumentRoot(this.temporaryFolder.getRoot());

		Ssl ssl = new Ssl();
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyStorePassword("secret");
		ssl.setKeyPassword("password");
		ssl.setClientAuth(ClientAuth.WANT);
		ssl.setTrustStore("src/test/resources/test.jks");
		ssl.setTrustStorePassword("secret");
		factory.setSsl(ssl);

		this.container = factory.getEmbeddedServletContainer();
		this.container.start();

		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(new FileInputStream(new File("src/test/resources/test.jks")),
				"secret".toCharArray());

		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder()
						.loadTrustMaterial(null, new TrustSelfSignedStrategy())
						.loadKeyMaterial(keyStore, "password".toCharArray()).build());

		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory)
				.build();

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
				httpClient);

		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory),
				equalTo("test"));
	}

	@Test
	public void sslWantsClientAuthenticationSucceedsWithoutClientCertificate()
			throws Exception {
		FileCopyUtils.copy("test",
				new FileWriter(this.temporaryFolder.newFile("test.txt")));

		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setDocumentRoot(this.temporaryFolder.getRoot());

		Ssl ssl = new Ssl();
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyStorePassword("secret");
		ssl.setKeyPassword("password");
		ssl.setClientAuth(ClientAuth.WANT);
		ssl.setTrustStore("src/test/resources/test.jks");
		ssl.setTrustStorePassword("secret");
		factory.setSsl(ssl);

		this.container = factory.getEmbeddedServletContainer();
		this.container.start();

		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder().loadTrustMaterial(null,
						new TrustSelfSignedStrategy()).build());

		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory)
				.build();

		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
				httpClient);

		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory),
				equalTo("test"));
	}

	@Test
	public void defaultSessionTimeout() throws Exception {
		assertThat(getFactory().getSessionTimeout(), equalTo(30 * 60));
	}

	protected String getLocalUrl(String resourcePath) {
		return getLocalUrl("http", resourcePath);
	}

	protected String getLocalUrl(String scheme, String resourcePath) {
		return scheme + "://localhost:" + this.container.getPort() + resourcePath;
	}

	protected String getLocalUrl(int port, String resourcePath) {
		return "http://localhost:" + port + resourcePath;
	}

	protected String getResponse(String url) throws IOException, URISyntaxException {
		ClientHttpResponse response = getClientResponse(url);
		try {
			return StreamUtils.copyToString(response.getBody(), Charset.forName("UTF-8"));
		}
		finally {
			response.close();
		}
	}

	protected String getResponse(String url,
			HttpComponentsClientHttpRequestFactory requestFactory) throws IOException,
			URISyntaxException {
		ClientHttpResponse response = getClientResponse(url, requestFactory);
		try {
			return StreamUtils.copyToString(response.getBody(), Charset.forName("UTF-8"));
		}
		finally {
			response.close();
		}
	}

	protected ClientHttpResponse getClientResponse(String url) throws IOException,
			URISyntaxException {
		return getClientResponse(url, new HttpComponentsClientHttpRequestFactory());
	}

	protected ClientHttpResponse getClientResponse(String url,
			HttpComponentsClientHttpRequestFactory requestFactory) throws IOException,
			URISyntaxException {
		ClientHttpRequest request = requestFactory.createRequest(new URI(url),
				HttpMethod.GET);
		ClientHttpResponse response = request.execute();
		return response;
	}

	protected abstract AbstractEmbeddedServletContainerFactory getFactory();

	protected ServletContextInitializer exampleServletRegistration() {
		return new ServletRegistrationBean(new ExampleServlet(), "/hello");
	}

	@SuppressWarnings("serial")
	private ServletContextInitializer errorServletRegistration() {
		ServletRegistrationBean bean = new ServletRegistrationBean(new ExampleServlet() {
			@Override
			public void service(ServletRequest request, ServletResponse response)
					throws ServletException, IOException {
				throw new RuntimeException("Planned");
			}
		}, "/bang");
		bean.setName("error");
		return bean;
	}

	@SuppressWarnings("serial")
	private static class InitCountingServlet extends GenericServlet {

		private int initCount;

		@Override
		public void init() throws ServletException {
			this.initCount++;
		}

		@Override
		public void service(ServletRequest req, ServletResponse res)
				throws ServletException, IOException {
		}

		public int getInitCount() {
			return this.initCount;
		}
	};
}
