/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.context.embedded;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLException;
import javax.servlet.GenericServlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.InputStreamFactory;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.InOrder;

import org.springframework.boot.ApplicationHome;
import org.springframework.boot.ApplicationTemp;
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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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

	private final HttpClientContext httpClientContext = HttpClientContext.create();

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
		this.container = factory.getEmbeddedServletContainer(exampleServletRegistration(),
				new FilterRegistrationBean(new ExampleFilter()));
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
		int specificPort = SocketUtils.findAvailableTcpPort(41000);
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
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		addTestTxtFile(factory);
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
		assertThat(response.getHeaders().getContentType().toString(),
				equalTo("text/css"));
		response.close();
	}

	@Test
	public void errorPage() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.addErrorPages(new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/hello"));
		this.container = factory.getEmbeddedServletContainer(exampleServletRegistration(),
				errorServletRegistration());
		this.container.start();
		assertThat(getResponse(getLocalUrl("/hello")), equalTo("Hello World"));
		assertThat(getResponse(getLocalUrl("/bang")), equalTo("Hello World"));
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
				new ServletRegistrationBean(new ExampleServlet(true), "/hello"));
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
				new ServletRegistrationBean(new ExampleServlet(true), "/hello"));
		this.container.start();
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder()
						.loadTrustMaterial(null, new TrustSelfSignedStrategy()).build());
		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory)
				.build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(
				httpClient);
		assertThat(getResponse(getLocalUrl("https", "/hello"), requestFactory),
				containsString("scheme=https"));
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
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory),
				equalTo("test"));
	}

	@Test
	public void pkcs12KeyStoreAndTrustStore() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		addTestTxtFile(factory);
		factory.setSsl(getSsl(ClientAuth.NEED, null, "classpath:test.p12",
				"classpath:test.p12"));
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
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		addTestTxtFile(factory);
		factory.setSsl(getSsl(ClientAuth.NEED, "password", "classpath:test.jks",
				"classpath:test.jks"));
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
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory),
				equalTo("test"));
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
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory),
				equalTo("test"));
	}

	@Test
	public void disableJspServletRegistration() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.getJspServlet().setRegistered(false);
		this.container = factory.getEmbeddedServletContainer();
		assertThat(getJspServlet(), is(nullValue()));
	}

	@Test
	public void cannotReadClassPathFiles() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory
				.getEmbeddedServletContainer(exampleServletRegistration());
		this.container.start();
		ClientHttpResponse response = getClientResponse(
				getLocalUrl("/org/springframework/boot/SpringApplication.class"));
		assertThat(response.getStatusCode(), equalTo(HttpStatus.NOT_FOUND));
	}

	private Ssl getSsl(ClientAuth clientAuth, String keyPassword, String keyStore) {
		return getSsl(clientAuth, keyPassword, keyStore, null);
	}

	private Ssl getSsl(ClientAuth clientAuth, String keyPassword, String keyStore,
			String trustStore) {
		Ssl ssl = new Ssl();
		ssl.setClientAuth(clientAuth);
		if (keyPassword != null) {
			ssl.setKeyPassword(keyPassword);
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
		return ssl;
	}

	private String getStoreType(String keyStore) {
		return (keyStore.endsWith(".p12") ? "pkcs12" : null);
	}

	@Test
	public void defaultSessionTimeout() throws Exception {
		assertThat(getFactory().getSessionTimeout(), equalTo(30 * 60));
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
		System.out.println(s1);
		System.out.println(s2);
		System.out.println(s3);
		String message = "Session error s1=" + s1 + " s2=" + s2 + " s3=" + s3;
		assertThat(message, s2.split(":")[0], equalTo(s1.split(":")[1]));
		assertThat(message, s3.split(":")[0], equalTo(s2.split(":")[1]));
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
		assertThat(dirContents.length, greaterThan(0));
	}

	@Test
	public void getValidSessionStoreWhenSessionStoreNotSet() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		File dir = factory.getValidSessionStoreDir(false);
		assertThat(dir.getName(), equalTo("servlet-sessions"));
		assertThat(dir.getParentFile(), equalTo(new ApplicationTemp().getDir()));
	}

	@Test
	public void getValidSessionStoreWhenSessionStoreIsRelative() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		factory.setSessionStoreDir(new File("sessions"));
		File dir = factory.getValidSessionStoreDir(false);
		assertThat(dir.getName(), equalTo("sessions"));
		assertThat(dir.getParentFile(), equalTo(new ApplicationHome().getDir()));
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
		assertTrue(doTestCompression(10000, null, null));
	}

	@Test
	public void noCompressionForSmallResponse() throws Exception {
		assertFalse(doTestCompression(100, null, null));
	}

	@Test
	public void noCompressionForMimeType() throws Exception {
		String[] mimeTypes = new String[] { "text/html", "text/xml", "text/css" };
		assertFalse(doTestCompression(10000, mimeTypes, null));
	}

	@Test
	public void noCompressionForUserAgent() throws Exception {
		assertFalse(doTestCompression(10000, null, new String[] { "testUserAgent" }));
	}

	@Test
	public void mimeMappingsAreCorrectlyConfigured() throws Exception {
		AbstractEmbeddedServletContainerFactory factory = getFactory();
		this.container = factory.getEmbeddedServletContainer();
		Map<String, String> configuredMimeMappings = getActualMimeMappings();
		Set<Entry<String, String>> entrySet = configuredMimeMappings.entrySet();
		Collection<MimeMappings.Mapping> expectedMimeMappings = getExpectedMimeMappings();
		for (Entry<String, String> entry : entrySet) {
			assertThat(expectedMimeMappings,
					hasItem(new MimeMappings.Mapping(entry.getKey(), entry.getValue())));
		}
		for (MimeMappings.Mapping mapping : expectedMimeMappings) {
			assertThat(configuredMimeMappings,
					hasEntry(mapping.getExtension(), mapping.getMimeType()));
		}
		assertThat(configuredMimeMappings.size(),
				is(equalTo(expectedMimeMappings.size())));
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
		assertThat(rootResource.get(), is(not(nullValue())));
	}

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
		assertThat(response, equalTo(testContent));
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
		ClientHttpResponse response = getClientResponse(url, headers);
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
		ClientHttpResponse response = getClientResponse(url, requestFactory, headers);
		try {
			return StreamUtils.copyToString(response.getBody(), Charset.forName("UTF-8"));
		}
		finally {
			response.close();
		}
	}

	protected ClientHttpResponse getClientResponse(String url, String... headers)
			throws IOException, URISyntaxException {
		return getClientResponse(url, new HttpComponentsClientHttpRequestFactory() {

			@Override
			protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
				return AbstractEmbeddedServletContainerFactoryTests.this.httpClientContext;
			}

		}, headers);
	}

	protected ClientHttpResponse getClientResponse(String url,
			HttpComponentsClientHttpRequestFactory requestFactory, String... headers)
					throws IOException, URISyntaxException {
		ClientHttpRequest request = requestFactory.createRequest(new URI(url),
				HttpMethod.GET);
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
				new ServletRegistrationBean(new ExampleServlet(true), "/hello"));
		this.container.start();
		assertThat(getResponse(getLocalUrl("/hello"), "X-Forwarded-For:140.211.11.130"),
				containsString("remoteaddr=140.211.11.130"));
	}

	protected abstract AbstractEmbeddedServletContainerFactory getFactory();

	protected abstract Object getJspServlet();

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

}
