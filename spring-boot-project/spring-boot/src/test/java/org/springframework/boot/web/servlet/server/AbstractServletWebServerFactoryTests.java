/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.web.servlet.server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.servlet.AsyncContext;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.GenericServlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration.Dynamic;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.SessionCookieConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.catalina.webresources.TomcatURLStreamHandlerFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.InputStreamFactory;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.jasper.EmbeddedServletOptions;
import org.apache.jasper.servlet.JspServlet;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;

import org.springframework.boot.system.ApplicationHome;
import org.springframework.boot.system.ApplicationTemp;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.boot.testsupport.web.servlet.ExampleFilter;
import org.springframework.boot.testsupport.web.servlet.ExampleServlet;
import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.Ssl.ClientAuth;
import org.springframework.boot.web.server.SslStoreProvider;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.Session.SessionTrackingMode;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.SocketUtils;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Base for testing classes that extends {@link AbstractServletWebServerFactory}.
 *
 * @author Phillip Webb
 * @author Greg Turnquist
 * @author Andy Wilkinson
 * @author Raja Kolli
 */
@ExtendWith(OutputCaptureExtension.class)
public abstract class AbstractServletWebServerFactoryTests {

	@TempDir
	protected File tempDir;

	protected WebServer webServer;

	private final HttpClientContext httpClientContext = HttpClientContext.create();

	private final Supplier<HttpClientBuilder> httpClientBuilder = () -> HttpClients.custom()
			.setRetryHandler(new StandardHttpRequestRetryHandler(10, false) {

				@Override
				public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
					boolean retry = super.retryRequest(exception, executionCount, context);
					if (retry) {
						try {
							Thread.sleep(200);
						}
						catch (InterruptedException ex) {
							Thread.currentThread().interrupt();
						}
					}
					return retry;
				}

			});

	@AfterEach
	void tearDown() {
		if (this.webServer != null) {
			try {
				this.webServer.stop();
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		if (ClassUtils.isPresent("org.apache.catalina.webresources.TomcatURLStreamHandlerFactory",
				getClass().getClassLoader())) {
			ReflectionTestUtils.setField(TomcatURLStreamHandlerFactory.class, "instance", null);
		}
		ReflectionTestUtils.setField(URL.class, "factory", null);
	}

	@AfterEach
	void clearUrlStreamHandlerFactory() {
		if (ClassUtils.isPresent("org.apache.catalina.webresources.TomcatURLStreamHandlerFactory",
				getClass().getClassLoader())) {
			ReflectionTestUtils.setField(TomcatURLStreamHandlerFactory.class, "instance", null);
			ReflectionTestUtils.setField(URL.class, "factory", null);
		}
	}

	@Test
	void startServlet() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer(exampleServletRegistration());
		this.webServer.start();
		assertThat(getResponse(getLocalUrl("/hello"))).isEqualTo("Hello World");
	}

	@Test
	void startCalledTwice(CapturedOutput output) throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer(exampleServletRegistration());
		this.webServer.start();
		int port = this.webServer.getPort();
		this.webServer.start();
		assertThat(this.webServer.getPort()).isEqualTo(port);
		assertThat(getResponse(getLocalUrl("/hello"))).isEqualTo("Hello World");
		assertThat(output).containsOnlyOnce("started on port");
	}

	@Test
	void stopCalledTwice() {
		AbstractServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer(exampleServletRegistration());
		this.webServer.start();
		this.webServer.stop();
		this.webServer.stop();
	}

	@Test
	void emptyServerWhenPortIsMinusOne() {
		AbstractServletWebServerFactory factory = getFactory();
		factory.setPort(-1);
		this.webServer = factory.getWebServer(exampleServletRegistration());
		this.webServer.start();
		assertThat(this.webServer.getPort()).isEqualTo(-1);
	}

	@Test
	void stopServlet() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer(exampleServletRegistration());
		this.webServer.start();
		int port = this.webServer.getPort();
		this.webServer.stop();
		assertThatIOException().isThrownBy(() -> getResponse(getLocalUrl(port, "/hello")));
	}

	@Test
	void startServletAndFilter() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer(exampleServletRegistration(),
				new FilterRegistrationBean<>(new ExampleFilter()));
		this.webServer.start();
		assertThat(getResponse(getLocalUrl("/hello"))).isEqualTo("[Hello World]");
	}

	@Test
	void startBlocksUntilReadyToServe() {
		AbstractServletWebServerFactory factory = getFactory();
		final Date[] date = new Date[1];
		this.webServer = factory.getWebServer((servletContext) -> {
			try {
				Thread.sleep(500);
				date[0] = new Date();
			}
			catch (InterruptedException ex) {
				throw new ServletException(ex);
			}
		});
		this.webServer.start();
		assertThat(date[0]).isNotNull();
	}

	@Test
	void loadOnStartAfterContextIsInitialized() {
		AbstractServletWebServerFactory factory = getFactory();
		final InitCountingServlet servlet = new InitCountingServlet();
		this.webServer = factory
				.getWebServer((servletContext) -> servletContext.addServlet("test", servlet).setLoadOnStartup(1));
		assertThat(servlet.getInitCount()).isEqualTo(0);
		this.webServer.start();
		assertThat(servlet.getInitCount()).isEqualTo(1);
	}

	@Test
	void portIsMinusOneWhenConnectionIsClosed() {
		AbstractServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer();
		this.webServer.start();
		assertThat(this.webServer.getPort()).isGreaterThan(0);
		this.webServer.stop();
		assertThat(this.webServer.getPort()).isEqualTo(-1);
	}

	@Test
	void specificPort() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		int specificPort = doWithRetry(() -> {
			int port = SocketUtils.findAvailableTcpPort(41000);
			factory.setPort(port);
			this.webServer = factory.getWebServer(exampleServletRegistration());
			this.webServer.start();
			return port;
		});
		assertThat(getResponse("http://localhost:" + specificPort + "/hello")).isEqualTo("Hello World");
		assertThat(this.webServer.getPort()).isEqualTo(specificPort);
	}

	@Test
	void specificContextRoot() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.setContextPath("/say");
		this.webServer = factory.getWebServer(exampleServletRegistration());
		this.webServer.start();
		assertThat(getResponse(getLocalUrl("/say/hello"))).isEqualTo("Hello World");
	}

	@Test
	void contextPathIsLoggedOnStartup(CapturedOutput output) {
		AbstractServletWebServerFactory factory = getFactory();
		factory.setContextPath("/custom");
		this.webServer = factory.getWebServer(exampleServletRegistration());
		this.webServer.start();
		assertThat(output).containsOnlyOnce("with context path '/custom'");
	}

	@Test
	void contextPathMustStartWithSlash() {
		assertThatIllegalArgumentException().isThrownBy(() -> getFactory().setContextPath("missingslash"))
				.withMessageContaining("ContextPath must start with '/' and not end with '/'");
	}

	@Test
	void contextPathMustNotEndWithSlash() {
		assertThatIllegalArgumentException().isThrownBy(() -> getFactory().setContextPath("extraslash/"))
				.withMessageContaining("ContextPath must start with '/' and not end with '/'");
	}

	@Test
	void contextRootPathMustNotBeSlash() {
		assertThatIllegalArgumentException().isThrownBy(() -> getFactory().setContextPath("/"))
				.withMessageContaining("Root ContextPath must be specified using an empty string");
	}

	@Test
	void multipleConfigurations() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		ServletContextInitializer[] initializers = new ServletContextInitializer[6];
		Arrays.setAll(initializers, (i) -> mock(ServletContextInitializer.class));
		factory.setInitializers(Arrays.asList(initializers[2], initializers[3]));
		factory.addInitializers(initializers[4], initializers[5]);
		this.webServer = factory.getWebServer(initializers[0], initializers[1]);
		this.webServer.start();
		InOrder ordered = inOrder((Object[]) initializers);
		for (ServletContextInitializer initializer : initializers) {
			ordered.verify(initializer).onStartup(any(ServletContext.class));
		}
	}

	@Test
	void documentRoot() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		addTestTxtFile(factory);
		this.webServer = factory.getWebServer();
		this.webServer.start();
		assertThat(getResponse(getLocalUrl("/test.txt"))).isEqualTo("test");
	}

	@Test
	void mimeType() throws Exception {
		FileCopyUtils.copy("test", new FileWriter(new File(this.tempDir, "test.xxcss")));
		AbstractServletWebServerFactory factory = getFactory();
		factory.setRegisterDefaultServlet(true);
		factory.setDocumentRoot(this.tempDir);
		MimeMappings mimeMappings = new MimeMappings();
		mimeMappings.add("xxcss", "text/css");
		factory.setMimeMappings(mimeMappings);
		this.webServer = factory.getWebServer();
		this.webServer.start();
		ClientHttpResponse response = getClientResponse(getLocalUrl("/test.xxcss"));
		assertThat(response.getHeaders().getContentType().toString()).isEqualTo("text/css");
		response.close();
	}

	@Test
	void errorPage() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.addErrorPages(new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/hello"));
		this.webServer = factory.getWebServer(exampleServletRegistration(), errorServletRegistration());
		this.webServer.start();
		assertThat(getResponse(getLocalUrl("/hello"))).isEqualTo("Hello World");
		assertThat(getResponse(getLocalUrl("/bang"))).isEqualTo("Hello World");
	}

	@Test
	void errorPageFromPutRequest() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.addErrorPages(new ErrorPage(HttpStatus.INTERNAL_SERVER_ERROR, "/hello"));
		this.webServer = factory.getWebServer(exampleServletRegistration(), errorServletRegistration());
		this.webServer.start();
		assertThat(getResponse(getLocalUrl("/hello"), HttpMethod.PUT)).isEqualTo("Hello World");
		assertThat(getResponse(getLocalUrl("/bang"), HttpMethod.PUT)).isEqualTo("Hello World");
	}

	@Test
	void basicSslFromClassPath() throws Exception {
		testBasicSslWithKeyStore("classpath:test.jks");
	}

	@Test
	void basicSslFromFileSystem() throws Exception {
		testBasicSslWithKeyStore("src/test/resources/test.jks");
	}

	@Test
	void sslDisabled() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		Ssl ssl = getSsl(null, "password", "classpath:test.jks");
		ssl.setEnabled(false);
		factory.setSsl(ssl);
		this.webServer = factory.getWebServer(new ServletRegistrationBean<>(new ExampleServlet(true, false), "/hello"));
		this.webServer.start();
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build());
		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory).build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		assertThatExceptionOfType(SSLException.class)
				.isThrownBy(() -> getResponse(getLocalUrl("https", "/hello"), requestFactory));
	}

	@Test
	void sslGetScheme() throws Exception { // gh-2232
		AbstractServletWebServerFactory factory = getFactory();
		factory.setSsl(getSsl(null, "password", "src/test/resources/test.jks"));
		this.webServer = factory.getWebServer(new ServletRegistrationBean<>(new ExampleServlet(true, false), "/hello"));
		this.webServer.start();
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build());
		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory).build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		assertThat(getResponse(getLocalUrl("https", "/hello"), requestFactory)).contains("scheme=https");
	}

	@Test
	void sslKeyAlias() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		Ssl ssl = getSsl(null, "password", "test-alias", "src/test/resources/test.jks");
		factory.setSsl(ssl);
		ServletRegistrationBean<ExampleServlet> registration = new ServletRegistrationBean<>(
				new ExampleServlet(true, false), "/hello");
		this.webServer = factory.getWebServer(registration);
		this.webServer.start();
		TrustStrategy trustStrategy = new SerialNumberValidatingTrustSelfSignedStrategy("3a3aaec8");
		SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, trustStrategy).build();
		HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext))
				.build();
		String response = getResponse(getLocalUrl("https", "/hello"),
				new HttpComponentsClientHttpRequestFactory(httpClient));
		assertThat(response).contains("scheme=https");
	}

	@Test
	void sslWithInvalidAliasFailsDuringStartup() {
		AbstractServletWebServerFactory factory = getFactory();
		Ssl ssl = getSsl(null, "password", "test-alias-404", "src/test/resources/test.jks");
		factory.setSsl(ssl);
		ServletRegistrationBean<ExampleServlet> registration = new ServletRegistrationBean<>(
				new ExampleServlet(true, false), "/hello");
		ThrowingCallable call = () -> factory.getWebServer(registration).start();
		assertThatSslWithInvalidAliasCallFails(call);
	}

	protected void assertThatSslWithInvalidAliasCallFails(ThrowingCallable call) {
		assertThatThrownBy(call).hasStackTraceContaining("Keystore does not contain specified alias 'test-alias-404'");
	}

	@Test
	void serverHeaderIsDisabledByDefaultWhenUsingSsl() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.setSsl(getSsl(null, "password", "src/test/resources/test.jks"));
		this.webServer = factory.getWebServer(new ServletRegistrationBean<>(new ExampleServlet(true, false), "/hello"));
		this.webServer.start();
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build());
		HttpClient httpClient = this.httpClientBuilder.get().setSSLSocketFactory(socketFactory).build();
		ClientHttpResponse response = getClientResponse(getLocalUrl("https", "/hello"), HttpMethod.GET,
				new HttpComponentsClientHttpRequestFactory(httpClient));
		assertThat(response.getHeaders().get("Server")).isNullOrEmpty();
	}

	@Test
	void serverHeaderCanBeCustomizedWhenUsingSsl() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.setServerHeader("MyServer");
		factory.setSsl(getSsl(null, "password", "src/test/resources/test.jks"));
		this.webServer = factory.getWebServer(new ServletRegistrationBean<>(new ExampleServlet(true, false), "/hello"));
		this.webServer.start();
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build());
		HttpClient httpClient = this.httpClientBuilder.get().setSSLSocketFactory(socketFactory)
				.setRetryHandler(new DefaultHttpRequestRetryHandler(10, false)).build();
		ClientHttpResponse response = getClientResponse(getLocalUrl("https", "/hello"), HttpMethod.GET,
				new HttpComponentsClientHttpRequestFactory(httpClient));
		assertThat(response.getHeaders().get("Server")).containsExactly("MyServer");
	}

	protected final void testBasicSslWithKeyStore(String keyStore) throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		addTestTxtFile(factory);
		factory.setSsl(getSsl(null, "password", keyStore));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build());
		HttpClient httpClient = this.httpClientBuilder.get().setSSLSocketFactory(socketFactory).build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory)).isEqualTo("test");
	}

	@Test
	void pkcs12KeyStoreAndTrustStore() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		addTestTxtFile(factory);
		factory.setSsl(getSsl(ClientAuth.NEED, null, "classpath:test.p12", "classpath:test.p12", null, null));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		KeyStore keyStore = KeyStore.getInstance("pkcs12");
		loadStore(keyStore, new FileSystemResource("src/test/resources/test.p12"));
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy())
						.loadKeyMaterial(keyStore, "secret".toCharArray()).build());
		HttpClient httpClient = this.httpClientBuilder.get().setSSLSocketFactory(socketFactory).build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory)).isEqualTo("test");
	}

	@Test
	void sslNeedsClientAuthenticationSucceedsWithClientCertificate() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.setRegisterDefaultServlet(true);
		addTestTxtFile(factory);
		factory.setSsl(getSsl(ClientAuth.NEED, "password", "classpath:test.jks", "classpath:test.jks", null, null));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		loadStore(keyStore, new FileSystemResource("src/test/resources/test.jks"));
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy())
						.loadKeyMaterial(keyStore, "password".toCharArray()).build());
		HttpClient httpClient = this.httpClientBuilder.get().setSSLSocketFactory(socketFactory).build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory)).isEqualTo("test");
	}

	@Test
	void sslNeedsClientAuthenticationFailsWithoutClientCertificate() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		addTestTxtFile(factory);
		factory.setSsl(getSsl(ClientAuth.NEED, "password", "classpath:test.jks"));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build());
		HttpClient httpClient = this.httpClientBuilder.get().setSSLSocketFactory(socketFactory).build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		String localUrl = getLocalUrl("https", "/test.txt");
		assertThatIOException().isThrownBy(() -> getResponse(localUrl, requestFactory));
	}

	@Test
	void sslWantsClientAuthenticationSucceedsWithClientCertificate() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		addTestTxtFile(factory);
		factory.setSsl(
				getSsl(ClientAuth.WANT, "password", "classpath:test.jks", null, new String[] { "TLSv1.2" }, null));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		loadStore(keyStore, new FileSystemResource("src/test/resources/test.jks"));
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy())
						.loadKeyMaterial(keyStore, "password".toCharArray()).build());
		HttpClient httpClient = this.httpClientBuilder.get().setSSLSocketFactory(socketFactory).build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory)).isEqualTo("test");
	}

	@Test
	void sslWantsClientAuthenticationSucceedsWithoutClientCertificate() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		addTestTxtFile(factory);
		factory.setSsl(getSsl(ClientAuth.WANT, "password", "classpath:test.jks"));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build());
		HttpClient httpClient = this.httpClientBuilder.get().setSSLSocketFactory(socketFactory).build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory)).isEqualTo("test");
	}

	@Test
	void sslWithCustomSslStoreProvider() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		addTestTxtFile(factory);
		Ssl ssl = new Ssl();
		ssl.setClientAuth(ClientAuth.NEED);
		ssl.setKeyPassword("password");
		factory.setSsl(ssl);
		SslStoreProvider sslStoreProvider = mock(SslStoreProvider.class);
		given(sslStoreProvider.getKeyStore()).willReturn(loadStore());
		given(sslStoreProvider.getTrustStore()).willReturn(loadStore());
		factory.setSslStoreProvider(sslStoreProvider);
		this.webServer = factory.getWebServer();
		this.webServer.start();
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		loadStore(keyStore, new FileSystemResource("src/test/resources/test.jks"));
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy())
						.loadKeyMaterial(keyStore, "password".toCharArray()).build());
		HttpClient httpClient = this.httpClientBuilder.get().setSSLSocketFactory(socketFactory).build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory)).isEqualTo("test");
		verify(sslStoreProvider, atLeastOnce()).getKeyStore();
		verify(sslStoreProvider, atLeastOnce()).getTrustStore();
	}

	@Test
	void disableJspServletRegistration() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.getJsp().setRegistered(false);
		this.webServer = factory.getWebServer();
		assertThat(getJspServlet()).isNull();
	}

	@Test
	void cannotReadClassPathFiles() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer(exampleServletRegistration());
		this.webServer.start();
		ClientHttpResponse response = getClientResponse(
				getLocalUrl("/org/springframework/boot/SpringApplication.class"));
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	protected Ssl getSsl(ClientAuth clientAuth, String keyPassword, String keyStore) {
		return getSsl(clientAuth, keyPassword, keyStore, null, null, null);
	}

	protected Ssl getSsl(ClientAuth clientAuth, String keyPassword, String keyAlias, String keyStore) {
		return getSsl(clientAuth, keyPassword, keyAlias, keyStore, null, null, null);
	}

	private Ssl getSsl(ClientAuth clientAuth, String keyPassword, String keyStore, String trustStore,
			String[] supportedProtocols, String[] ciphers) {
		return getSsl(clientAuth, keyPassword, null, keyStore, trustStore, supportedProtocols, ciphers);
	}

	private Ssl getSsl(ClientAuth clientAuth, String keyPassword, String keyAlias, String keyStore, String trustStore,
			String[] supportedProtocols, String[] ciphers) {
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

	protected void testRestrictedSSLProtocolsAndCipherSuites(String[] protocols, String[] ciphers) throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.setSsl(getSsl(null, "password", "src/test/resources/restricted.jks", null, protocols, ciphers));
		this.webServer = factory.getWebServer(new ServletRegistrationBean<>(new ExampleServlet(true, false), "/hello"));
		this.webServer.start();
		SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
				new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build());
		HttpClient httpClient = this.httpClientBuilder.get().setSSLSocketFactory(socketFactory).build();
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
		assertThat(getResponse(getLocalUrl("https", "/hello"), requestFactory)).contains("scheme=https");
	}

	private String getStoreType(String keyStore) {
		return keyStore.endsWith(".p12") ? "pkcs12" : null;
	}

	@Test
	void defaultSessionTimeout() {
		assertThat(getFactory().getSession().getTimeout()).hasMinutes(30);
	}

	@Test
	void persistSession() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.getSession().setPersistent(true);
		this.webServer = factory.getWebServer(sessionServletRegistration());
		this.webServer.start();
		String s1 = getResponse(getLocalUrl("/session"));
		String s2 = getResponse(getLocalUrl("/session"));
		this.webServer.stop();
		this.webServer = factory.getWebServer(sessionServletRegistration());
		this.webServer.start();
		String s3 = getResponse(getLocalUrl("/session"));
		String message = "Session error s1=" + s1 + " s2=" + s2 + " s3=" + s3;
		assertThat(s2.split(":")[0]).as(message).isEqualTo(s1.split(":")[1]);
		assertThat(s3.split(":")[0]).as(message).isEqualTo(s2.split(":")[1]);
	}

	@Test
	void persistSessionInSpecificSessionStoreDir() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		File sessionStoreDir = new File(this.tempDir, "sessions");
		sessionStoreDir.mkdir();
		factory.getSession().setPersistent(true);
		factory.getSession().setStoreDir(sessionStoreDir);
		this.webServer = factory.getWebServer(sessionServletRegistration());
		this.webServer.start();
		getResponse(getLocalUrl("/session"));
		this.webServer.stop();
		File[] dirContents = sessionStoreDir.listFiles((dir, name) -> !(".".equals(name) || "..".equals(name)));
		assertThat(dirContents).isNotEmpty();
	}

	@Test
	void getValidSessionStoreWhenSessionStoreNotSet() {
		AbstractServletWebServerFactory factory = getFactory();
		File dir = factory.getValidSessionStoreDir(false);
		assertThat(dir.getName()).isEqualTo("servlet-sessions");
		assertThat(dir.getParentFile()).isEqualTo(new ApplicationTemp().getDir());
	}

	@Test
	void getValidSessionStoreWhenSessionStoreIsRelative() {
		AbstractServletWebServerFactory factory = getFactory();
		factory.getSession().setStoreDir(new File("sessions"));
		File dir = factory.getValidSessionStoreDir(false);
		assertThat(dir.getName()).isEqualTo("sessions");
		assertThat(dir.getParentFile()).isEqualTo(new ApplicationHome().getDir());
	}

	@Test
	void getValidSessionStoreWhenSessionStoreReferencesFile() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		File file = new File(this.tempDir, "file");
		file.createNewFile();
		factory.getSession().setStoreDir(file);
		assertThatIllegalStateException().isThrownBy(() -> factory.getValidSessionStoreDir(false))
				.withMessageContaining("points to a file");
	}

	@Test
	void sessionCookieConfiguration() {
		AbstractServletWebServerFactory factory = getFactory();
		factory.getSession().getCookie().setName("testname");
		factory.getSession().getCookie().setDomain("testdomain");
		factory.getSession().getCookie().setPath("/testpath");
		factory.getSession().getCookie().setComment("testcomment");
		factory.getSession().getCookie().setHttpOnly(true);
		factory.getSession().getCookie().setSecure(true);
		factory.getSession().getCookie().setMaxAge(Duration.ofSeconds(60));
		final AtomicReference<SessionCookieConfig> configReference = new AtomicReference<>();
		this.webServer = factory.getWebServer((context) -> configReference.set(context.getSessionCookieConfig()));
		SessionCookieConfig sessionCookieConfig = configReference.get();
		assertThat(sessionCookieConfig.getName()).isEqualTo("testname");
		assertThat(sessionCookieConfig.getDomain()).isEqualTo("testdomain");
		assertThat(sessionCookieConfig.getPath()).isEqualTo("/testpath");
		assertThat(sessionCookieConfig.getComment()).isEqualTo("testcomment");
		assertThat(sessionCookieConfig.isHttpOnly()).isTrue();
		assertThat(sessionCookieConfig.isSecure()).isTrue();
		assertThat(sessionCookieConfig.getMaxAge()).isEqualTo(60);
	}

	@Test
	void sslSessionTracking() {
		AbstractServletWebServerFactory factory = getFactory();
		Ssl ssl = new Ssl();
		ssl.setEnabled(true);
		ssl.setKeyStore("src/test/resources/test.jks");
		ssl.setKeyPassword("password");
		factory.setSsl(ssl);
		factory.getSession().setTrackingModes(EnumSet.of(SessionTrackingMode.SSL));
		AtomicReference<ServletContext> contextReference = new AtomicReference<>();
		this.webServer = factory.getWebServer(contextReference::set);
		assertThat(contextReference.get().getEffectiveSessionTrackingModes())
				.isEqualTo(EnumSet.of(javax.servlet.SessionTrackingMode.SSL));
	}

	@Test
	void compressionOfResponseToGetRequest() throws Exception {
		assertThat(doTestCompression(10000, null, null)).isTrue();
	}

	@Test
	void compressionOfResponseToPostRequest() throws Exception {
		assertThat(doTestCompression(10000, null, null, HttpMethod.POST)).isTrue();
	}

	@Test
	void noCompressionForSmallResponse() throws Exception {
		assertThat(doTestCompression(100, null, null)).isFalse();
	}

	@Test
	void noCompressionForMimeType() throws Exception {
		String[] mimeTypes = new String[] { "text/html", "text/xml", "text/css" };
		assertThat(doTestCompression(10000, mimeTypes, null)).isFalse();
	}

	@Test
	void noCompressionForUserAgent() throws Exception {
		assertThat(doTestCompression(10000, null, new String[] { "testUserAgent" })).isFalse();
	}

	@Test
	void compressionWithoutContentSizeHeader() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		Compression compression = new Compression();
		compression.setEnabled(true);
		factory.setCompression(compression);
		this.webServer = factory.getWebServer(new ServletRegistrationBean<>(new ExampleServlet(false, true), "/hello"));
		this.webServer.start();
		TestGzipInputStreamFactory inputStreamFactory = new TestGzipInputStreamFactory();
		Map<String, InputStreamFactory> contentDecoderMap = Collections.singletonMap("gzip", inputStreamFactory);
		getResponse(getLocalUrl("/hello"), new HttpComponentsClientHttpRequestFactory(
				this.httpClientBuilder.get().setContentDecoderRegistry(contentDecoderMap).build()));
		assertThat(inputStreamFactory.wasCompressionUsed()).isTrue();
	}

	@Test
	void mimeMappingsAreCorrectlyConfigured() {
		AbstractServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer();
		Map<String, String> configuredMimeMappings = getActualMimeMappings();
		Collection<MimeMappings.Mapping> expectedMimeMappings = MimeMappings.DEFAULT.getAll();
		configuredMimeMappings.forEach(
				(key, value) -> assertThat(expectedMimeMappings).contains(new MimeMappings.Mapping(key, value)));
		for (MimeMappings.Mapping mapping : expectedMimeMappings) {
			assertThat(configuredMimeMappings).containsEntry(mapping.getExtension(), mapping.getMimeType());
		}
		assertThat(configuredMimeMappings.size()).isEqualTo(expectedMimeMappings.size());
	}

	@Test
	void rootServletContextResource() {
		AbstractServletWebServerFactory factory = getFactory();
		final AtomicReference<URL> rootResource = new AtomicReference<>();
		this.webServer = factory.getWebServer((servletContext) -> {
			try {
				rootResource.set(servletContext.getResource("/"));
			}
			catch (MalformedURLException ex) {
				throw new ServletException(ex);
			}
		});
		this.webServer.start();
		assertThat(rootResource.get()).isNotNull();
	}

	@Test
	void customServerHeader() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.setServerHeader("MyServer");
		this.webServer = factory.getWebServer(exampleServletRegistration());
		this.webServer.start();
		ClientHttpResponse response = getClientResponse(getLocalUrl("/hello"));
		assertThat(response.getHeaders().getFirst("server")).isEqualTo("MyServer");
	}

	@Test
	void serverHeaderIsDisabledByDefault() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer(exampleServletRegistration());
		this.webServer.start();
		ClientHttpResponse response = getClientResponse(getLocalUrl("/hello"));
		assertThat(response.getHeaders().getFirst("server")).isNull();
	}

	@Test
	protected void portClashOfPrimaryConnectorResultsInPortInUseException() throws Exception {
		doWithBlockedPort((port) -> {
			assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
				AbstractServletWebServerFactory factory = getFactory();
				factory.setPort(port);
				AbstractServletWebServerFactoryTests.this.webServer = factory.getWebServer();
				AbstractServletWebServerFactoryTests.this.webServer.start();
			}).satisfies((ex) -> handleExceptionCausedByBlockedPortOnPrimaryConnector(ex, port));
		});
	}

	@Test
	void portClashOfSecondaryConnectorResultsInPortInUseException() throws Exception {
		doWithBlockedPort((port) -> {
			assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
				AbstractServletWebServerFactory factory = getFactory();
				addConnector(port, factory);
				AbstractServletWebServerFactoryTests.this.webServer = factory.getWebServer();
				AbstractServletWebServerFactoryTests.this.webServer.start();
			}).satisfies((ex) -> handleExceptionCausedByBlockedPortOnSecondaryConnector(ex, port));
		});
	}

	@Test
	void malformedAddress() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.setAddress(InetAddress.getByName("255.255.255.255"));
		assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> {
			this.webServer = factory.getWebServer();
			this.webServer.start();
		}).isNotInstanceOf(PortInUseException.class);
	}

	@Test
	void localeCharsetMappingsAreConfigured() {
		AbstractServletWebServerFactory factory = getFactory();
		Map<Locale, Charset> mappings = new HashMap<>();
		mappings.put(Locale.GERMAN, StandardCharsets.UTF_8);
		factory.setLocaleCharsetMappings(mappings);
		this.webServer = factory.getWebServer();
		assertThat(getCharset(Locale.GERMAN)).isEqualTo(StandardCharsets.UTF_8);
		assertThat(getCharset(Locale.ITALIAN)).isNull();
	}

	@Test
	void jspServletInitParameters() throws Exception {
		Map<String, String> initParameters = new HashMap<>();
		initParameters.put("a", "alpha");
		AbstractServletWebServerFactory factory = getFactory();
		factory.getJsp().setInitParameters(initParameters);
		this.webServer = factory.getWebServer();
		Assumptions.assumeFalse(getJspServlet() == null);
		JspServlet jspServlet = getJspServlet();
		assertThat(jspServlet.getInitParameter("a")).isEqualTo("alpha");
	}

	@Test
	void jspServletIsNotInDevelopmentModeByDefault() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer();
		Assumptions.assumeFalse(getJspServlet() == null);
		JspServlet jspServlet = getJspServlet();
		EmbeddedServletOptions options = (EmbeddedServletOptions) ReflectionTestUtils.getField(jspServlet, "options");
		assertThat(options.getDevelopment()).isFalse();
	}

	@Test
	void faultyFilterCausesStartFailure() {
		AbstractServletWebServerFactory factory = getFactory();
		factory.addInitializers((servletContext) -> servletContext.addFilter("faulty", new Filter() {

			@Override
			public void init(FilterConfig filterConfig) throws ServletException {
				throw new ServletException("Faulty filter");
			}

			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
					throws IOException, ServletException {
				chain.doFilter(request, response);
			}

			@Override
			public void destroy() {
			}

		}));
		assertThatExceptionOfType(WebServerException.class).isThrownBy(() -> factory.getWebServer().start());
	}

	@Test
	void sessionConfiguration() {
		AbstractServletWebServerFactory factory = getFactory();
		factory.getSession().setTimeout(Duration.ofSeconds(123));
		factory.getSession().setTrackingModes(EnumSet.of(SessionTrackingMode.COOKIE, SessionTrackingMode.URL));
		factory.getSession().getCookie().setName("testname");
		factory.getSession().getCookie().setDomain("testdomain");
		factory.getSession().getCookie().setPath("/testpath");
		factory.getSession().getCookie().setComment("testcomment");
		factory.getSession().getCookie().setHttpOnly(true);
		factory.getSession().getCookie().setSecure(true);
		factory.getSession().getCookie().setMaxAge(Duration.ofMinutes(1));
		AtomicReference<ServletContext> contextReference = new AtomicReference<>();
		factory.getWebServer(contextReference::set).start();
		ServletContext servletContext = contextReference.get();
		assertThat(servletContext.getEffectiveSessionTrackingModes())
				.isEqualTo(EnumSet.of(javax.servlet.SessionTrackingMode.COOKIE, javax.servlet.SessionTrackingMode.URL));
		assertThat(servletContext.getSessionCookieConfig().getName()).isEqualTo("testname");
		assertThat(servletContext.getSessionCookieConfig().getDomain()).isEqualTo("testdomain");
		assertThat(servletContext.getSessionCookieConfig().getPath()).isEqualTo("/testpath");
		assertThat(servletContext.getSessionCookieConfig().getComment()).isEqualTo("testcomment");
		assertThat(servletContext.getSessionCookieConfig().isHttpOnly()).isTrue();
		assertThat(servletContext.getSessionCookieConfig().isSecure()).isTrue();
		assertThat(servletContext.getSessionCookieConfig().getMaxAge()).isEqualTo(60);
	}

	@Test
	void servletContextListenerContextDestroyedIsCalledWhenContainerIsStopped() throws Exception {
		ServletContextListener listener = mock(ServletContextListener.class);
		this.webServer = getFactory().getWebServer((servletContext) -> servletContext.addListener(listener));
		this.webServer.start();
		this.webServer.stop();
		verify(listener).contextDestroyed(any(ServletContextEvent.class));
	}

	@Test
	void exceptionThrownOnLoadFailureIsRethrown() {
		AbstractServletWebServerFactory factory = getFactory();
		this.webServer = factory
				.getWebServer((context) -> context.addServlet("failing", FailingServlet.class).setLoadOnStartup(0));
		assertThatExceptionOfType(WebServerException.class).isThrownBy(this.webServer::start)
				.satisfies(this::wrapsFailingServletException);
	}

	@Test
	void whenThereAreNoInFlightRequestsShutDownGracefullyInvokesCallbackWithIdle() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.setShutdown(Shutdown.GRACEFUL);
		this.webServer = factory.getWebServer();
		this.webServer.start();
		AtomicReference<GracefulShutdownResult> result = new AtomicReference<>();
		this.webServer.shutDownGracefully(result::set);
		Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> GracefulShutdownResult.IDLE == result.get());
	}

	@Test
	void whenARequestRemainsInFlightThenShutDownGracefullyDoesNotInvokeCallbackUntilTheRequestCompletes()
			throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.setShutdown(Shutdown.GRACEFUL);
		BlockingServlet blockingServlet = new BlockingServlet();
		this.webServer = factory.getWebServer((context) -> {
			Dynamic registration = context.addServlet("blockingServlet", blockingServlet);
			registration.addMapping("/blocking");
		});
		this.webServer.start();
		int port = this.webServer.getPort();
		Future<Object> request = initiateGetRequest(port, "/blocking");
		blockingServlet.awaitQueue();
		AtomicReference<GracefulShutdownResult> result = new AtomicReference<>();
		this.webServer.shutDownGracefully(result::set);
		blockingServlet.admitOne();
		assertThat(request.get()).isInstanceOf(HttpResponse.class);
		Awaitility.await().atMost(Duration.ofSeconds(30)).until(() -> GracefulShutdownResult.IDLE == result.get());
	}

	@Test
	void whenAnAsyncRequestRemainsInFlightThenShutDownGracefullyDoesNotInvokeCallbackUntilRequestCompletes()
			throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.setShutdown(Shutdown.GRACEFUL);
		BlockingAsyncServlet blockingAsyncServlet = new BlockingAsyncServlet();
		this.webServer = factory.getWebServer((context) -> {
			Dynamic registration = context.addServlet("blockingServlet", blockingAsyncServlet);
			registration.addMapping("/blockingAsync");
			registration.setAsyncSupported(true);
		});
		this.webServer.start();
		int port = this.webServer.getPort();
		Future<Object> request = initiateGetRequest(port, "/blockingAsync");
		blockingAsyncServlet.awaitQueue();
		AtomicReference<GracefulShutdownResult> result = new AtomicReference<>();
		this.webServer.shutDownGracefully(result::set);
		Thread.sleep(5000);
		assertThat(result.get()).isNull();
		assertThat(request.isDone()).isFalse();
		blockingAsyncServlet.admitOne();
		assertThat(request.get()).isInstanceOf(HttpResponse.class);
		Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> GracefulShutdownResult.IDLE == result.get());
	}

	@Test
	void whenARequestIsActiveThenStopWillComplete() throws InterruptedException, BrokenBarrierException {
		AbstractServletWebServerFactory factory = getFactory();
		BlockingServlet blockingServlet = new BlockingServlet();
		this.webServer = factory
				.getWebServer((context) -> context.addServlet("blockingServlet", blockingServlet).addMapping("/"));
		this.webServer.start();
		int port = this.webServer.getPort();
		initiateGetRequest(port, "/");
		blockingServlet.awaitQueue();
		this.webServer.stop();
		try {
			blockingServlet.admitOne();
		}
		catch (RuntimeException ex) {

		}
	}

	protected Future<Object> initiateGetRequest(int port, String path) {
		return initiateGetRequest(HttpClients.createMinimal(), port, path);
	}

	protected Future<Object> initiateGetRequest(HttpClient httpClient, int port, String path) {
		RunnableFuture<Object> getRequest = new FutureTask<>(() -> {
			try {
				HttpResponse response = httpClient.execute(new HttpGet("http://localhost:" + port + path));
				response.getEntity().getContent().close();
				return response;
			}
			catch (Exception ex) {
				return ex;
			}
		});
		new Thread(getRequest, "GET " + path).start();
		return getRequest;
	}

	private void wrapsFailingServletException(WebServerException ex) {
		Throwable cause = ex.getCause();
		while (cause != null) {
			if (cause instanceof FailingServletException) {
				return;
			}
			cause = cause.getCause();
		}
		fail("Exception did not wrap FailingServletException");
	}

	protected abstract void addConnector(int port, AbstractServletWebServerFactory factory);

	protected abstract void handleExceptionCausedByBlockedPortOnPrimaryConnector(RuntimeException ex, int blockedPort);

	protected abstract void handleExceptionCausedByBlockedPortOnSecondaryConnector(RuntimeException ex,
			int blockedPort);

	private boolean doTestCompression(int contentSize, String[] mimeTypes, String[] excludedUserAgents)
			throws Exception {
		return doTestCompression(contentSize, mimeTypes, excludedUserAgents, HttpMethod.GET);
	}

	private boolean doTestCompression(int contentSize, String[] mimeTypes, String[] excludedUserAgents,
			HttpMethod method) throws Exception {
		String testContent = setUpFactoryForCompression(contentSize, mimeTypes, excludedUserAgents);
		TestGzipInputStreamFactory inputStreamFactory = new TestGzipInputStreamFactory();
		Map<String, InputStreamFactory> contentDecoderMap = Collections.singletonMap("gzip", inputStreamFactory);
		String response = getResponse(getLocalUrl("/test.txt"), method,
				new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create().setUserAgent("testUserAgent")
						.setContentDecoderRegistry(contentDecoderMap).build()));
		assertThat(response).isEqualTo(testContent);
		return inputStreamFactory.wasCompressionUsed();
	}

	private String setUpFactoryForCompression(int contentSize, String[] mimeTypes, String[] excludedUserAgents) {
		char[] chars = new char[contentSize];
		Arrays.fill(chars, 'F');
		String testContent = new String(chars);
		AbstractServletWebServerFactory factory = getFactory();
		Compression compression = new Compression();
		compression.setEnabled(true);
		if (mimeTypes != null) {
			compression.setMimeTypes(mimeTypes);
		}
		if (excludedUserAgents != null) {
			compression.setExcludedUserAgents(excludedUserAgents);
		}
		factory.setCompression(compression);
		factory.addInitializers(new ServletRegistrationBean<HttpServlet>(new HttpServlet() {

			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
				resp.setContentType("text/plain");
				resp.setContentLength(testContent.length());
				resp.getWriter().write(testContent);
				resp.getWriter().flush();
			}

		}, "/test.txt"));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		return testContent;
	}

	protected abstract Map<String, String> getActualMimeMappings();

	protected abstract Charset getCharset(Locale locale);

	private void addTestTxtFile(AbstractServletWebServerFactory factory) throws IOException {
		FileCopyUtils.copy("test", new FileWriter(new File(this.tempDir, "test.txt")));
		factory.setDocumentRoot(this.tempDir);
		factory.setRegisterDefaultServlet(true);
	}

	protected String getLocalUrl(String resourcePath) {
		return getLocalUrl("http", resourcePath);
	}

	protected String getLocalUrl(String scheme, String resourcePath) {
		return scheme + "://localhost:" + this.webServer.getPort() + resourcePath;
	}

	protected String getLocalUrl(int port, String resourcePath) {
		return "http://localhost:" + port + resourcePath;
	}

	protected String getResponse(String url, String... headers) throws IOException, URISyntaxException {
		return getResponse(url, HttpMethod.GET, headers);
	}

	protected String getResponse(String url, HttpMethod method, String... headers)
			throws IOException, URISyntaxException {
		try (ClientHttpResponse response = getClientResponse(url, method, headers)) {
			return StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
		}
	}

	protected String getResponse(String url, HttpComponentsClientHttpRequestFactory requestFactory, String... headers)
			throws IOException, URISyntaxException {
		return getResponse(url, HttpMethod.GET, requestFactory, headers);
	}

	protected String getResponse(String url, HttpMethod method, HttpComponentsClientHttpRequestFactory requestFactory,
			String... headers) throws IOException, URISyntaxException {
		try (ClientHttpResponse response = getClientResponse(url, method, requestFactory, headers)) {
			return StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
		}
	}

	protected ClientHttpResponse getClientResponse(String url, String... headers)
			throws IOException, URISyntaxException {
		return getClientResponse(url, HttpMethod.GET, headers);
	}

	protected ClientHttpResponse getClientResponse(String url, HttpMethod method, String... headers)
			throws IOException, URISyntaxException {
		return getClientResponse(url, method,
				new HttpComponentsClientHttpRequestFactory(this.httpClientBuilder.get().build()) {

					@Override
					protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
						return AbstractServletWebServerFactoryTests.this.httpClientContext;
					}

				}, headers);
	}

	protected ClientHttpResponse getClientResponse(String url, HttpMethod method,
			HttpComponentsClientHttpRequestFactory requestFactory, String... headers)
			throws IOException, URISyntaxException {
		ClientHttpRequest request = requestFactory.createRequest(new URI(url), method);
		for (String header : headers) {
			String[] parts = header.split(":");
			request.getHeaders().add(parts[0], parts[1]);
		}
		return request.execute();
	}

	protected void assertForwardHeaderIsUsed(ServletWebServerFactory factory) throws IOException, URISyntaxException {
		this.webServer = factory.getWebServer(new ServletRegistrationBean<>(new ExampleServlet(true, false), "/hello"));
		this.webServer.start();
		assertThat(getResponse(getLocalUrl("/hello"), "X-Forwarded-For:140.211.11.130"))
				.contains("remoteaddr=140.211.11.130");
	}

	protected abstract AbstractServletWebServerFactory getFactory();

	protected abstract org.apache.jasper.servlet.JspServlet getJspServlet() throws Exception;

	protected ServletContextInitializer exampleServletRegistration() {
		return new ServletRegistrationBean<>(new ExampleServlet(), "/hello");
	}

	@SuppressWarnings("serial")
	private ServletContextInitializer errorServletRegistration() {
		ServletRegistrationBean<ExampleServlet> bean = new ServletRegistrationBean<>(new ExampleServlet() {

			@Override
			public void service(ServletRequest request, ServletResponse response) {
				throw new RuntimeException("Planned");
			}

		}, "/bang");
		bean.setName("error");
		return bean;
	}

	protected final ServletContextInitializer sessionServletRegistration() {
		ServletRegistrationBean<ExampleServlet> bean = new ServletRegistrationBean<>(new ExampleServlet() {

			@Override
			public void service(ServletRequest request, ServletResponse response) throws IOException {
				HttpSession session = ((HttpServletRequest) request).getSession(true);
				long value = System.currentTimeMillis();
				Object existing = session.getAttribute("boot");
				session.setAttribute("boot", value);
				PrintWriter writer = response.getWriter();
				writer.append(String.valueOf(existing)).append(":").append(String.valueOf(value));
			}

		}, "/session");
		bean.setName("session");
		return bean;
	}

	private <T> T doWithRetry(Callable<T> action) throws Exception {
		Exception lastFailure = null;
		for (int i = 0; i < 10; i++) {
			try {
				return action.call();
			}
			catch (Exception ex) {
				lastFailure = ex;
			}
		}
		throw new IllegalStateException("Action was not successful in 10 attempts", lastFailure);
	}

	protected final void doWithBlockedPort(BlockedPortAction action) throws Exception {
		ServerSocket serverSocket = new ServerSocket();
		int blockedPort = doWithRetry(() -> {
			int port = SocketUtils.findAvailableTcpPort(40000);
			serverSocket.bind(new InetSocketAddress(port));
			return port;
		});
		try {
			action.run(blockedPort);
		}
		finally {
			serverSocket.close();
		}
	}

	private KeyStore loadStore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
		KeyStore keyStore = KeyStore.getInstance("JKS");
		Resource resource = new ClassPathResource("test.jks");
		loadStore(keyStore, resource);
		return keyStore;
	}

	private void loadStore(KeyStore keyStore, Resource resource)
			throws IOException, NoSuchAlgorithmException, CertificateException {
		try (InputStream stream = resource.getInputStream()) {
			keyStore.load(stream, "secret".toCharArray());
		}
	}

	private class TestGzipInputStreamFactory implements InputStreamFactory {

		private final AtomicBoolean requested = new AtomicBoolean();

		@Override
		public InputStream create(InputStream in) throws IOException {
			if (this.requested.get()) {
				throw new IllegalStateException("On deflated InputStream already requested");
			}
			this.requested.set(true);
			return new GZIPInputStream(in);
		}

		boolean wasCompressionUsed() {
			return this.requested.get();
		}

	}

	@SuppressWarnings("serial")
	static class InitCountingServlet extends GenericServlet {

		private int initCount;

		@Override
		public void init() {
			this.initCount++;
		}

		@Override
		public void service(ServletRequest req, ServletResponse res) {
		}

		int getInitCount() {
			return this.initCount;
		}

	}

	interface BlockedPortAction {

		void run(int port);

	}

	/**
	 * {@link TrustSelfSignedStrategy} that also validates certificate serial number.
	 */
	private static final class SerialNumberValidatingTrustSelfSignedStrategy extends TrustSelfSignedStrategy {

		private final String serialNumber;

		private SerialNumberValidatingTrustSelfSignedStrategy(String serialNumber) {
			this.serialNumber = serialNumber;
		}

		@Override
		public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
			String hexSerialNumber = chain[0].getSerialNumber().toString(16);
			boolean isMatch = hexSerialNumber.equals(this.serialNumber);
			return super.isTrusted(chain, authType) && isMatch;
		}

	}

	public static class FailingServlet extends HttpServlet {

		@Override
		public void init() throws ServletException {
			throw new FailingServletException();
		}

	}

	public static class FailingServletContextListener implements ServletContextListener {

		@Override
		public void contextInitialized(ServletContextEvent sce) {
			throw new FailingServletException();
		}

	}

	static class FailingServletException extends RuntimeException {

		FailingServletException() {
			super("Init Failure");
		}

	}

	protected static class BlockingServlet extends HttpServlet {

		private final BlockingQueue<CyclicBarrier> barriers = new ArrayBlockingQueue<>(10);

		public BlockingServlet() {

		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			CyclicBarrier barrier = new CyclicBarrier(2);
			this.barriers.add(barrier);
			try {
				barrier.await();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			catch (BrokenBarrierException ex) {
				throw new ServletException(ex);
			}
		}

		public void admitOne() {
			try {
				CyclicBarrier barrier = this.barriers.take();
				if (!barrier.isBroken()) {
					barrier.await();
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			catch (BrokenBarrierException ex) {
				throw new RuntimeException(ex);
			}
		}

		public void awaitQueue() throws InterruptedException {
			while (this.barriers.isEmpty()) {
				Thread.sleep(100);
			}
		}

		public void awaitQueue(int size) throws InterruptedException {
			while (this.barriers.size() < size) {
				Thread.sleep(100);
			}
		}

	}

	static class BlockingAsyncServlet extends HttpServlet {

		private final BlockingQueue<CyclicBarrier> barriers = new ArrayBlockingQueue<>(10);

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			CyclicBarrier barrier = new CyclicBarrier(2);
			this.barriers.add(barrier);
			AsyncContext async = req.startAsync();
			new Thread(() -> {
				try {
					barrier.await();
				}
				catch (InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
				catch (BrokenBarrierException ex) {

				}
				async.complete();
			}).start();
		}

		private void admitOne() {
			try {
				this.barriers.take().await();
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
			catch (BrokenBarrierException ex) {
				throw new RuntimeException(ex);
			}
		}

		private void awaitQueue() throws InterruptedException {
			while (this.barriers.isEmpty()) {
				Thread.sleep(100);
			}
		}

	}

}
