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

package org.springframework.boot.context.embedded;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.CodeSource;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.servlet.GenericServlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.InputStreamFactory;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.jasper.EmbeddedServletOptions;
import org.apache.jasper.servlet.JspServlet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.InOrder;

import org.springframework.boot.ApplicationHome;
import org.springframework.boot.ApplicationTemp;
import org.springframework.boot.context.embedded.Ssl.ClientAuth;
import org.springframework.boot.testutil.InternalOutputCapture;
import org.springframework.boot.web.servlet.ErrorPage;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.SocketUtils;
import org.springframework.util.StreamUtils;
import org.springframework.util.concurrent.ListenableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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

	@Rule
	public InternalOutputCapture output = new InternalOutputCapture();

	protected EmbeddedServletContainer container;

	private final HttpClientContext httpClientContext = HttpClientContext.create();

	@BeforeClass
	@AfterClass
	public static void uninstallUrlStreamHandlerFactory() {
		ReflectionTestUtils.setField(TomcatURLStreamHandlerFactory.class, "instance",
				null);
		ReflectionTestUtils.setField(URL.class, "factory", null);
	}

	@After
	public void tearDown() {
		if (this.container != null) {
			try {
				this.container.stop();
			}
			catch (Exception ex) {
				// Ignore
			}
		}
	}

	@Test
	public void startServlet() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.start();
		assertThat(getResponse(getLocalUrl("/hello"))).isEqualTo("Hello World");
	}

	@Test
	public void startCalledTwice() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.start();
		int port = this.container.getPort();
		this.container.start();
		assertThat(this.container.getPort()).isEqualTo(port);
		assertThat(getResponse(getLocalUrl("/hello"))).isEqualTo("Hello World");
		assertThat(this.output.toString()).containsOnlyOnce("started on port");
	}

	@Test
	public void stopCalledTwice() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.start();
		this.container.stop();
		this.container.stop();
	}

	@Test
	public void emptyServerWhenPortIsMinusOne() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setPort(-1);
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.start();
		assertThat(this.container.getPort()).isLessThan(0); // Jetty is -2
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
		throw new RuntimeException(
				"Unexpected response on port " + port + " : " + response);
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
		assertThat(response1.get(10, TimeUnit.SECONDS).getRawStatusCode()).isEqualTo(200);

		this.container.stop();
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.start();

		ListenableFuture<ClientHttpResponse> response2 = clientHttpRequestFactory
				.createAsyncRequest(new URI(getLocalUrl("/hello")), HttpMethod.GET)
				.executeAsync();
		assertThat(response2.get(10, TimeUnit.SECONDS).getRawStatusCode()).isEqualTo(200);
		clientHttpRequestFactory.destroy();
	}

	@Test
	public void startServletAndFilter() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory.getEmbeddedServletContainer(exampleServletRegistration(),
				new FilterRegistrationBean(new ExampleFilter()));
		this.container.start();
		assertThat(getResponse(getLocalUrl("/hello"))).isEqualTo("[Hello World]");
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
		assertThat(date[0]).isNotNull();
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
		assertThat(servlet.getInitCount()).isEqualTo(0);
		this.container.start();
		assertThat(servlet.getInitCount()).isEqualTo(1);
	}

	@Test
	public void specificPort() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		int specificPort = SocketUtils.findAvailableTcpPort(41000);
		factory.setPort(specificPort);
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.start();
		assertThat(getResponse("http://localhost:" + specificPort + "/hello"))
				.isEqualTo("Hello World");
		assertThat(this.container.getPort()).isEqualTo(specificPort);
	}

	@Test
	public void specificContextRoot() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setContextPath("/say");
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.start();
		assertThat(getResponse(getLocalUrl("/say/hello"))).isEqualTo("Hello World");
	}

	@Test
	public void contextPathMustStartWithSlash() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ContextPath must start with '/' and not end with '/'");
		getFactory().setContextPath("missingslash");
	}

	@Test
	public void contextPathMustNotEndWithSlash() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("ContextPath must start with '/' and not end with '/'");
		getFactory().setContextPath("extraslash/");
	}

	@Test
	public void contextRootPathMustNotBeSlash() throws Exception {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage(
				"Root ContextPath must be specified using an empty string");
		getFactory().setContextPath("/");
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
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		addTestTxtFile(factory);
		this.container = factory.getEmbeddedServletContainer();
		this.container.start();
		assertThat(getResponse(getLocalUrl("/test.txt"))).isEqualTo("test");
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
		assertThat(response.getHeaders().getContentType().toString())
				.isEqualTo("text/css");
		response.close();
	}

	@Test
	public void errorPage() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.addErrorPages(new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/hello"));
		this.container = factory.getEmbeddedServletContainer(exampleServletRegistration(),
				errorServletRegistration());
		this.container.start();
		assertThat(getResponse(getLocalUrl("/hello"))).isEqualTo("Hello World");
		assertThat(getResponse(getLocalUrl("/bang"))).isEqualTo("Hello World");
	}

	@Test
	public void errorPageFromPutRequest() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.addErrorPages(new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/hello"));
		this.container = factory.getEmbeddedServletContainer(exampleServletRegistration(),
				errorServletRegistration());
		this.container.start();
		assertThat(getResponse(getLocalUrl("/hello"), HttpMethod.PUT))
				.isEqualTo("Hello World");
		assertThat(getResponse(getLocalUrl("/bang"), HttpMethod.PUT))
				.isEqualTo("Hello World");
	}

	@Test
	public void basicSslFromClassPath() throws Exception {
		testBasicSslWithKeyStore("classpath:test.jks");
	}

	@Test
	public void basicSslFromFileSystem() throws Exception {
		testBasicSslWithKeyStore("src/test/resources/test.jks");
	}

	@Test
	public void sslDisabled() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		Ssl ssl = getSsl(null, "password", "classpath:test.jks");
		ssl.setEnabled(false);
		factory.setSsl(ssl);
		this.container = factory.getEmbeddedServletContainer(
				new ServletRegistrationBean(new ExampleServlet(true, false), "/hello"));
		this.container.start();
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder()
						.loadTrustMaterial(null, new TrustSelfSignedStrategy()).build());
		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory)
				.build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
				httpClient);
		this.thrown.expect(SSLException.class);
		getResponse(getLocalUrl("https", "/hello"), requestFactory);
	}

	@Test
	public void sslGetScheme() throws Exception { // gh-2232
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setSsl(getSsl(null, "password", "src/test/resources/test.jks"));
		this.container = factory.getEmbeddedServletContainer(
				new ServletRegistrationBean(new ExampleServlet(true, false), "/hello"));
		this.container.start();
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder()
						.loadTrustMaterial(null, new TrustSelfSignedStrategy()).build());
		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory)
				.build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
				httpClient);
		assertThat(getResponse(getLocalUrl("https", "/hello"), requestFactory))
				.contains("scheme=https");
	}

	@Test
	public void sslKeyAlias() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		Ssl ssl = getSsl(null, "password", "test-alias", "src/test/resources/test.jks");
		factory.setSsl(ssl);
		ServletRegistrationBean registration = new ServletRegistrationBean(
				new ExampleServlet(true, false), "/hello");
		this.container = factory.getEmbeddedServletContainer(registration);
		this.container.start();
		TrustStrategy trustStrategy = new SerialNumberValidatingTrustSelfSignedStrategy(
				"77e7c302");
		SSLContext sslContext = new SSLContextBuilder()
				.loadTrustMaterial(null, trustStrategy).build();
		HttpClient httpClient = HttpClients.custom()
				.setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext)).build();
		String response = getResponse(getLocalUrl("https", "/hello"),
				new HttpComponentsClientHttpRequestFactory(httpClient));
		assertThat(response).contains("scheme=https");
	}

	@Test
	public void serverHeaderIsDisabledByDefaultWhenUsingSsl() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setSsl(getSsl(null, "password", "src/test/resources/test.jks"));
		this.container = factory.getEmbeddedServletContainer(
				new ServletRegistrationBean(new ExampleServlet(true, false), "/hello"));
		this.container.start();
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder()
						.loadTrustMaterial(null, new TrustSelfSignedStrategy()).build());
		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory)
				.build();
		ClientHttpResponse response = getClientResponse(getLocalUrl("https", "/hello"),
				HttpMethod.GET, new HttpComponentsClientHttpRequestFactory(httpClient));
		assertThat(response.getHeaders().get("Server")).isNullOrEmpty();
	}

	@Test
	public void serverHeaderCanBeCustomizedWhenUsingSsl() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setServerHeader("MyServer");
		factory.setSsl(getSsl(null, "password", "src/test/resources/test.jks"));
		this.container = factory.getEmbeddedServletContainer(
				new ServletRegistrationBean(new ExampleServlet(true, false), "/hello"));
		this.container.start();
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder()
						.loadTrustMaterial(null, new TrustSelfSignedStrategy()).build());
		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory)
				.build();
		ClientHttpResponse response = getClientResponse(getLocalUrl("https", "/hello"),
				HttpMethod.GET, new HttpComponentsClientHttpRequestFactory(httpClient));
		assertThat(response.getHeaders().get("Server")).containsExactly("MyServer");
	}

	protected final void testBasicSslWithKeyStore(String keyStore) throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		addTestTxtFile(factory);
		factory.setSsl(getSsl(null, "password", keyStore));
		this.container = factory.getEmbeddedServletContainer();
		this.container.start();
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder()
						.loadTrustMaterial(null, new TrustSelfSignedStrategy()).build());
		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory)
				.build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
				httpClient);
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory))
				.isEqualTo("test");
	}

	@Test
	public void pkcs12KeyStoreAndTrustStore() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		addTestTxtFile(factory);
		factory.setSsl(getSsl(ClientAuth.NEED, null, "classpath:test.p12",
				"classpath:test.p12", null, null));
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
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory))
				.isEqualTo("test");
	}

	@Test
	public void sslNeedsClientAuthenticationSucceedsWithClientCertificate()
			throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		addTestTxtFile(factory);
		factory.setSsl(getSsl(ClientAuth.NEED, "password", "classpath:test.jks",
				"classpath:test.jks", null, null));
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
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory))
				.isEqualTo("test");
	}

	@Test(expected = IOException.class)
	public void sslNeedsClientAuthenticationFailsWithoutClientCertificate()
			throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		addTestTxtFile(factory);
		factory.setSsl(getSsl(ClientAuth.NEED, "password", "classpath:test.jks"));
		this.container = factory.getEmbeddedServletContainer();
		this.container.start();
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder()
						.loadTrustMaterial(null, new TrustSelfSignedStrategy()).build());
		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory)
				.build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
				httpClient);
		getResponse(getLocalUrl("https", "/test.txt"), requestFactory);
	}

	@Test
	public void sslWantsClientAuthenticationSucceedsWithClientCertificate()
			throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		addTestTxtFile(factory);
		factory.setSsl(getSsl(ClientAuth.WANT, "password", "classpath:test.jks"));
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
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory))
				.isEqualTo("test");
	}

	@Test
	public void sslWantsClientAuthenticationSucceedsWithoutClientCertificate()
			throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		addTestTxtFile(factory);
		factory.setSsl(getSsl(ClientAuth.WANT, "password", "classpath:test.jks"));
		this.container = factory.getEmbeddedServletContainer();
		this.container.start();
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder()
						.loadTrustMaterial(null, new TrustSelfSignedStrategy()).build());
		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory)
				.build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
				httpClient);
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory))
				.isEqualTo("test");
	}

	@Test
	public void sslWithCustomSslStoreProvider() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		addTestTxtFile(factory);
		Ssl ssl = new Ssl();
		ssl.setClientAuth(ClientAuth.NEED);
		ssl.setKeyPassword("password");
		factory.setSsl(ssl);
		SslStoreProvider sslStoreProvider = mock(SslStoreProvider.class);
		given(sslStoreProvider.getKeyStore()).willReturn(loadStore());
		given(sslStoreProvider.getTrustStore()).willReturn(loadStore());
		factory.setSslStoreProvider(sslStoreProvider);
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
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory))
				.isEqualTo("test");
		verify(sslStoreProvider).getKeyStore();
		verify(sslStoreProvider).getTrustStore();
	}

	@Test
	public void disableJspServletRegistration() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.getJspServlet().setRegistered(false);
		this.container = factory.getEmbeddedServletContainer();
		assertThat(getJspServlet()).isNull();
	}

	@Test
	public void cannotReadClassPathFiles() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.start();
		ClientHttpResponse response = getClientResponse(
				getLocalUrl("/org/springframework/boot/SpringApplication.class"));
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	public void codeSourceArchivePath() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		CodeSource codeSource = new CodeSource(new URL("file", "", "/some/test/path/"),
				(Certificate[]) null);
		File codeSourceArchive = factory.getCodeSourceArchive(codeSource);
		assertThat(codeSourceArchive).isEqualTo(new File("/some/test/path/"));
	}

	@Test
	public void codeSourceArchivePathContainingSpaces() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		CodeSource codeSource = new CodeSource(
				new URL("file", "", "/test/path/with%20space/"), (Certificate[]) null);
		File codeSourceArchive = factory.getCodeSourceArchive(codeSource);
		assertThat(codeSourceArchive).isEqualTo(new File("/test/path/with space/"));
	}

	protected Ssl getSsl(ClientAuth clientAuth, String keyPassword, String keyStore) {
		return getSsl(clientAuth, keyPassword, keyStore, null, null, null);
	}

	private Ssl getSsl(ClientAuth clientAuth, String keyPassword, String keyAlias,
			String keyStore) {
		return getSsl(clientAuth, keyPassword, keyAlias, keyStore, null, null, null);
	}

	private Ssl getSsl(ClientAuth clientAuth, String keyPassword, String keyStore,
			String trustStore, String[] supportedProtocols, String[] ciphers) {
		return getSsl(clientAuth, keyPassword, null, keyStore, trustStore,
				supportedProtocols, ciphers);
	}

	private Ssl getSsl(ClientAuth clientAuth, String keyPassword, String keyAlias,
			String keyStore, String trustStore, String[] supportedProtocols,
			String[] ciphers) {
		Ssl ssl = new Ssl();
		ssl.setClientAuth(clientAuth);
		if (keyPassword != null) {
			ssl.setKeyPassword(keyPassword);
		}
		if (keyAlias != null) {
			ssl.setKeyAlias(keyAlias);
		}
		if (keyStore != null) {
			ssl.setKeyStore(keyStore);
			ssl.setKeyStorePassword("secret");
			ssl.setKeyStoreType(getStoreType(keyStore));
		}
		if (trustStore != null) {
			ssl.setTrustStore(trustStore);
			ssl.setTrustStorePassword("secret");
			ssl.setTrustStoreType(getStoreType(trustStore));
		}
		if (ciphers != null) {
			ssl.setCiphers(ciphers);
		}
		if (supportedProtocols != null) {
			ssl.setEnabledProtocols(supportedProtocols);
		}
		return ssl;
	}

	protected void testRestrictedSSLProtocolsAndCipherSuites(String[] protocols,
			String[] ciphers) throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setSsl(getSsl(null, "password", "src/test/resources/test.jks", null,
				protocols, ciphers));
		this.container = factory.getEmbeddedServletContainer(
				new ServletRegistrationBean(new ExampleServlet(true, false), "/hello"));
		this.container.start();

		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder()
						.loadTrustMaterial(null, new TrustSelfSignedStrategy()).build());

		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory)
				.build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
				httpClient);

		assertThat(getResponse(getLocalUrl("https", "/hello"), requestFactory))
				.contains("scheme=https");
	}

	private String getStoreType(String keyStore) {
		return (keyStore.endsWith(".p12") ? "pkcs12" : null);
	}

	@Test
	public void defaultSessionTimeout() throws Exception {
		assertThat(getFactory().getSessionTimeout()).isEqualTo(30 * 60);
	}

	@Test
	public void persistSession() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setPersistSession(true);
		this.container = factory
				.getEmbeddedServletContainer(sessionServletRegistration());
		this.container.start();
		String s1 = getResponse(getLocalUrl("/session"));
		String s2 = getResponse(getLocalUrl("/session"));
		this.container.stop();
		this.container = factory
				.getEmbeddedServletContainer(sessionServletRegistration());
		this.container.start();
		String s3 = getResponse(getLocalUrl("/session"));
		String message = "Session error s1=" + s1 + " s2=" + s2 + " s3=" + s3;
		assertThat(s2.split(":")[0]).as(message).isEqualTo(s1.split(":")[1]);
		assertThat(s3.split(":")[0]).as(message).isEqualTo(s2.split(":")[1]);
	}

	@Test
	public void persistSessionInSpecificSessionStoreDir() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		File sessionStoreDir = this.temporaryFolder.newFolder();
		factory.setPersistSession(true);
		factory.setSessionStoreDir(sessionStoreDir);
		this.container = factory
				.getEmbeddedServletContainer(sessionServletRegistration());
		this.container.start();
		getResponse(getLocalUrl("/session"));
		this.container.stop();
		File[] dirContents = sessionStoreDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return !(".".equals(name) || "..".equals(name));
			}

		});
		assertThat(dirContents.length).isGreaterThan(0);
	}

	@Test
	public void getValidSessionStoreWhenSessionStoreNotSet() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		File dir = factory.getValidSessionStoreDir(false);
		assertThat(dir.getName()).isEqualTo("servlet-sessions");
		assertThat(dir.getParentFile()).isEqualTo(new ApplicationTemp().getDir());
	}

	@Test
	public void getValidSessionStoreWhenSessionStoreIsRelative() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setSessionStoreDir(new File("sessions"));
		File dir = factory.getValidSessionStoreDir(false);
		assertThat(dir.getName()).isEqualTo("sessions");
		assertThat(dir.getParentFile()).isEqualTo(new ApplicationHome().getDir());
	}

	@Test
	public void getValidSessionStoreWhenSessionStoreReferencesFile() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setSessionStoreDir(this.temporaryFolder.newFile());
		this.thrown.expect(IllegalStateException.class);
		this.thrown.expectMessage("points to a file");
		factory.getValidSessionStoreDir(false);
	}

	@Test
	public void compression() throws Exception {
		assertThat(doTestCompression(10000, null, null)).isTrue();
	}

	@Test
	public void noCompressionForSmallResponse() throws Exception {
		assertThat(doTestCompression(100, null, null)).isFalse();
	}

	@Test
	public void noCompressionForMimeType() throws Exception {
		String[] mimeTypes = new String[] { "text/html", "text/xml", "text/css" };
		assertThat(doTestCompression(10000, mimeTypes, null)).isFalse();
	}

	@Test
	public void noCompressionForUserAgent() throws Exception {
		assertThat(doTestCompression(10000, null, new String[] { "testUserAgent" }))
				.isFalse();
	}

	@Test
	public void compressionWithoutContentSizeHeader() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		Compression compression = new Compression();
		compression.setEnabled(true);
		factory.setCompression(compression);
		this.container = factory.getEmbeddedServletContainer(
				new ServletRegistrationBean(new ExampleServlet(false, true), "/hello"));
		this.container.start();
		TestGzipInputStreamFactory inputStreamFactory = new TestGzipInputStreamFactory();
		Map<String, InputStreamFactory> contentDecoderMap = Collections
				.singletonMap("gzip", (InputStreamFactory) inputStreamFactory);
		getResponse(getLocalUrl("/hello"),
				new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create()
						.setContentDecoderRegistry(contentDecoderMap).build()));
		assertThat(inputStreamFactory.wasCompressionUsed()).isTrue();
	}

	@Test
	public void mimeMappingsAreCorrectlyConfigured() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory.getEmbeddedServletContainer();
		Map<String, String> configuredMimeMappings = getActualMimeMappings();
		Set<Entry<String, String>> entrySet = configuredMimeMappings.entrySet();
		Collection<MimeMappings.Mapping> expectedMimeMappings = getExpectedMimeMappings();
		for (Entry<String, String> entry : entrySet) {
			assertThat(expectedMimeMappings)
					.contains(new MimeMappings.Mapping(entry.getKey(), entry.getValue()));
		}
		for (MimeMappings.Mapping mapping : expectedMimeMappings) {
			assertThat(configuredMimeMappings).containsEntry(mapping.getExtension(),
					mapping.getMimeType());
		}
		assertThat(configuredMimeMappings.size()).isEqualTo(expectedMimeMappings.size());
	}

	@Test
	public void rootServletContextResource() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		final AtomicReference<URL> rootResource = new AtomicReference<URL>();
		this.container = factory
				.getEmbeddedServletContainer(new ServletContextInitializer() {
					@Override
					public void onStartup(ServletContext servletContext)
							throws ServletException {
						try {
							rootResource.set(servletContext.getResource("/"));
						}
						catch (MalformedURLException ex) {
							throw new ServletException(ex);
						}
					}
				});
		this.container.start();
		assertThat(rootResource.get()).isNotNull();
	}

	@Test
	public void customServerHeader() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setServerHeader("MyServer");
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.start();
		ClientHttpResponse response = getClientResponse(getLocalUrl("/hello"));
		assertThat(response.getHeaders().getFirst("server")).isEqualTo("MyServer");
	}

	@Test
	public void serverHeaderIsDisabledByDefault() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.start();
		ClientHttpResponse response = getClientResponse(getLocalUrl("/hello"));
		assertThat(response.getHeaders().getFirst("server")).isNull();
	}

	@Test
	public void portClashOfPrimaryConnectorResultsInPortInUseException()
			throws IOException {
		doWithBlockedPort(new BlockedPortAction() {

			@Override
			public void run(int port) {
				try {
					AbstractEmbeddedServletContainerFactory factory = getFactory();
					factory.setPort(port);
					AbstractEmbeddedServletContainerFactoryTests.this.container = factory
							.getEmbeddedServletContainer();
					AbstractEmbeddedServletContainerFactoryTests.this.container.start();
					fail();
				}
				catch (RuntimeException ex) {
					handleExceptionCausedByBlockedPort(ex, port);
				}
			}

		});
	}

	@Test
	public void portClashOfSecondaryConnectorResultsInPortInUseException()
			throws IOException {
		doWithBlockedPort(new BlockedPortAction() {

			@Override
			public void run(int port) {
				try {
					AbstractEmbeddedServletContainerFactory factory = getFactory();
					factory.setPort(SocketUtils.findAvailableTcpPort(40000));
					addConnector(port, factory);
					AbstractEmbeddedServletContainerFactoryTests.this.container = factory
							.getEmbeddedServletContainer();
					AbstractEmbeddedServletContainerFactoryTests.this.container.start();
					fail();
				}
				catch (RuntimeException ex) {
					handleExceptionCausedByBlockedPort(ex, port);
				}
			}

		});
	}

	@Test
	public void localeCharsetMappingsAreConfigured() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		Map<Locale, Charset> mappings = new HashMap<Locale, Charset>();
		mappings.put(Locale.GERMAN, Charset.forName("UTF-8"));
		factory.setLocaleCharsetMappings(mappings);
		this.container = factory.getEmbeddedServletContainer();
		assertThat(getCharset(Locale.GERMAN).toString()).isEqualTo("UTF-8");
		assertThat(getCharset(Locale.ITALIAN)).isNull();
	}

	@Test
	public void jspServletInitParameters() throws Exception {
		Map<String, String> initParameters = new HashMap<String, String>();
		initParameters.put("a", "alpha");
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.getJspServlet().setInitParameters(initParameters);
		this.container = factory.getEmbeddedServletContainer();
		Assume.assumeThat(getJspServlet(), notNullValue());
		JspServlet jspServlet = getJspServlet();
		assertThat(jspServlet.getInitParameter("a")).isEqualTo("alpha");
	}

	@Test
	public void jspServletIsNotInDevelopmentModeByDefault() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory.getEmbeddedServletContainer();
		Assume.assumeThat(getJspServlet(), notNullValue());
		JspServlet jspServlet = getJspServlet();
		EmbeddedServletOptions options = (EmbeddedServletOptions) ReflectionTestUtils
				.getField(jspServlet, "options");
		assertThat(options.getDevelopment()).isEqualTo(false);
	}

	@Test
	public void explodedWarFileDocumentRootWhenRunningFromExplodedWar() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		File webInfClasses = this.temporaryFolder.newFolder("test.war", "WEB-INF", "lib",
				"spring-boot.jar");
		File documentRoot = factory.getExplodedWarFileDocumentRoot(webInfClasses);
		assertThat(documentRoot)
				.isEqualTo(webInfClasses.getParentFile().getParentFile().getParentFile());
	}

	@Test
	public void explodedWarFileDocumentRootWhenRunningFromPackagedWar() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		File codeSourceFile = this.temporaryFolder.newFile("test.war");
		File documentRoot = factory.getExplodedWarFileDocumentRoot(codeSourceFile);
		assertThat(documentRoot).isNull();
	}

	@Test
	public void servletContextListenerContextDestroyedIsCalledWhenContainerIsStopped()
			throws Exception {
		final ServletContextListener listener = mock(ServletContextListener.class);
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory
				.getEmbeddedServletContainer(new ServletContextInitializer() {

					@Override
					public void onStartup(ServletContext servletContext)
							throws ServletException {
						servletContext.addListener(listener);
					}

				});
		this.container.start();
		this.container.stop();
		verify(listener).contextDestroyed(any(ServletContextEvent.class));
	}

	protected abstract void addConnector(int port,
			AbstractEmbeddedServletContainerFactory factory);

	protected abstract void handleExceptionCausedByBlockedPort(RuntimeException ex,
			int blockedPort);

	private boolean doTestCompression(int contentSize, String[] mimeTypes,
			String[] excludedUserAgents) throws Exception {
		String testContent = setUpFactoryForCompression(contentSize, mimeTypes,
				excludedUserAgents);
		TestGzipInputStreamFactory inputStreamFactory = new TestGzipInputStreamFactory();
		Map<String, InputStreamFactory> contentDecoderMap = Collections
				.singletonMap("gzip", (InputStreamFactory) inputStreamFactory);
		String response = getResponse(getLocalUrl("/test.txt"),
				new HttpComponentsClientHttpRequestFactory(
						HttpClientBuilder.create().setUserAgent("testUserAgent")
								.setContentDecoderRegistry(contentDecoderMap).build()));
		assertThat(response).isEqualTo(testContent);
		return inputStreamFactory.wasCompressionUsed();
	}

	protected String setUpFactoryForCompression(int contentSize, String[] mimeTypes,
			String[] excludedUserAgents) throws Exception {
		char[] chars = new char[contentSize];
		Arrays.fill(chars, 'F');
		String testContent = new String(chars);
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		FileCopyUtils.copy(testContent,
				new FileWriter(this.temporaryFolder.newFile("test.txt")));
		factory.setDocumentRoot(this.temporaryFolder.getRoot());
		Compression compression = new Compression();
		compression.setEnabled(true);
		if (mimeTypes != null) {
			compression.setMimeTypes(mimeTypes);
		}
		if (excludedUserAgents != null) {
			compression.setExcludedUserAgents(excludedUserAgents);
		}
		factory.setCompression(compression);
		this.container = factory.getEmbeddedServletContainer();
		this.container.start();
		return testContent;
	}

	protected abstract Map<String, String> getActualMimeMappings();

	protected Collection<MimeMappings.Mapping> getExpectedMimeMappings() {
		return MimeMappings.DEFAULT.getAll();
	}

	protected abstract Charset getCharset(Locale locale);

	private void addTestTxtFile(AbstractEmbeddedServletContainerFactory factory)
			throws IOException {
		FileCopyUtils.copy("test",
				new FileWriter(this.temporaryFolder.newFile("test.txt")));
		factory.setDocumentRoot(this.temporaryFolder.getRoot());
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

	protected String getResponse(String url, String... headers)
			throws IOException, URISyntaxException {
		return getResponse(url, HttpMethod.GET, headers);
	}

	protected String getResponse(String url, HttpMethod method, String... headers)
			throws IOException, URISyntaxException {
		ClientHttpResponse response = getClientResponse(url, method, headers);
		try {
			return StreamUtils.copyToString(response.getBody(), Charset.forName("UTF-8"));
		}
		finally {
			response.close();
		}
	}

	protected String getResponse(String url,
			HttpComponentsClientHttpRequestFactory requestFactory, String... headers)
			throws IOException, URISyntaxException {
		return getResponse(url, HttpMethod.GET, requestFactory, headers);
	}

	protected String getResponse(String url, HttpMethod method,
			HttpComponentsClientHttpRequestFactory requestFactory, String... headers)
			throws IOException, URISyntaxException {
		ClientHttpResponse response = getClientResponse(url, method, requestFactory,
				headers);
		try {
			return StreamUtils.copyToString(response.getBody(), Charset.forName("UTF-8"));
		}
		finally {
			response.close();
		}
	}

	protected ClientHttpResponse getClientResponse(String url, String... headers)
			throws IOException, URISyntaxException {
		return getClientResponse(url, HttpMethod.GET, headers);
	}

	protected ClientHttpResponse getClientResponse(String url, HttpMethod method,
			String... headers) throws IOException, URISyntaxException {
		return getClientResponse(url, method,
				new HttpComponentsClientHttpRequestFactory() {

					@Override
					protected HttpContext createHttpContext(HttpMethod httpMethod,
							URI uri) {
						return AbstractEmbeddedServletContainerFactoryTests.this.httpClientContext;
					}

				}, headers);
	}

	protected ClientHttpResponse getClientResponse(String url, HttpMethod method,
			HttpComponentsClientHttpRequestFactory requestFactory, String... headers)
			throws IOException, URISyntaxException {
		ClientHttpRequest request = requestFactory.createRequest(new URI(url), method);
		request.getHeaders().add("Cookie", "JSESSIONID=" + "123");
		for (String header : headers) {
			String[] parts = header.split(":");
			request.getHeaders().add(parts[0], parts[1]);
		}
		ClientHttpResponse response = request.execute();
		return response;
	}

	protected void assertForwardHeaderIsUsed(EmbeddedServletContainerFactory factory)
			throws IOException, URISyntaxException {
		this.container = factory.getEmbeddedServletContainer(
				new ServletRegistrationBean(new ExampleServlet(true, false), "/hello"));
		this.container.start();
		assertThat(getResponse(getLocalUrl("/hello"), "X-Forwarded-For:140.211.11.130"))
				.contains("remoteaddr=140.211.11.130");
	}

	protected abstract AbstractEmbeddedServletContainerFactory getFactory();

	protected abstract org.apache.jasper.servlet.JspServlet getJspServlet()
			throws Exception;

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

	protected final ServletContextInitializer sessionServletRegistration() {
		ServletRegistrationBean bean = new ServletRegistrationBean(new ExampleServlet() {

			@Override
			public void service(ServletRequest request, ServletResponse response)
					throws ServletException, IOException {
				HttpSession session = ((HttpServletRequest) request).getSession(true);
				long value = System.currentTimeMillis();
				Object existing = session.getAttribute("boot");
				session.setAttribute("boot", value);
				PrintWriter writer = response.getWriter();
				writer.append(String.valueOf(existing) + ":" + value);
			}

		}, "/session");
		bean.setName("session");
		return bean;
	}

	protected final void doWithBlockedPort(BlockedPortAction action) throws IOException {
		int port = SocketUtils.findAvailableTcpPort(40000);
		ServerSocket serverSocket = new ServerSocket();
		for (int i = 0; i < 10; i++) {
			try {
				serverSocket.bind(new InetSocketAddress(port));
				break;
			}
			catch (Exception ex) {
			}
		}
		try {
			action.run(port);
		}
		finally {
			serverSocket.close();
		}
	}

	private KeyStore loadStore() throws KeyStoreException, IOException,
			NoSuchAlgorithmException, CertificateException {
		KeyStore keyStore = KeyStore.getInstance("JKS");
		Resource resource = new ClassPathResource("test.jks");
		InputStream inputStream = resource.getInputStream();
		try {
			keyStore.load(inputStream, "secret".toCharArray());
			return keyStore;
		}
		finally {
			inputStream.close();
		}
	}

	private class TestGzipInputStreamFactory implements InputStreamFactory {

		private final AtomicBoolean requested = new AtomicBoolean(false);

		@Override
		public InputStream create(InputStream in) throws IOException {
			if (this.requested.get()) {
				throw new IllegalStateException(
						"On deflated InputStream already requested");
			}
			this.requested.set(true);
			return new GZIPInputStream(in);
		}

		public boolean wasCompressionUsed() {
			return this.requested.get();
		}

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

	public interface BlockedPortAction {

		void run(int port);

	}

	/**
	 * {@link TrustSelfSignedStrategy} that also validates certificate serial number.
	 */
	private static final class SerialNumberValidatingTrustSelfSignedStrategy
			extends TrustSelfSignedStrategy {

		private final String serialNumber;

		private SerialNumberValidatingTrustSelfSignedStrategy(String serialNumber) {
			this.serialNumber = serialNumber;
		}

		@Override
		public boolean isTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
			String hexSerialNumber = chain[0].getSerialNumber().toString(16);
			boolean isMatch = hexSerialNumber.equals(this.serialNumber);
			return super.isTrusted(chain, authType) && isMatch;
		}

	}

}
