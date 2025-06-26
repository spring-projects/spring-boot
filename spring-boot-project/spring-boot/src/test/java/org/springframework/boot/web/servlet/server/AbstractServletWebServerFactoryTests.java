/*
 * Copyright 2012-present the original author or authors.
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
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.GenericServlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration.Dynamic;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.entity.InputStreamFactory;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.client5.http.ssl.TlsSocketStrategy;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.jasper.EmbeddedServletOptions;
import org.apache.jasper.servlet.JspServlet;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.awaitility.Awaitility;
import org.eclipse.jetty.client.ContentResponse;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.transport.HttpClientTransportOverHTTP2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;

import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreBundle;
import org.springframework.boot.ssl.jks.JksSslStoreDetails;
import org.springframework.boot.ssl.pem.PemSslStoreBundle;
import org.springframework.boot.ssl.pem.PemSslStoreDetails;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.boot.system.ApplicationTemp;
import org.springframework.boot.testsupport.classpath.resources.ResourcePath;
import org.springframework.boot.testsupport.classpath.resources.WithPackageResources;
import org.springframework.boot.testsupport.junit.EnabledOnLocale;
import org.springframework.boot.testsupport.system.CapturedOutput;
import org.springframework.boot.testsupport.system.OutputCaptureExtension;
import org.springframework.boot.testsupport.web.servlet.DirtiesUrlFactories;
import org.springframework.boot.testsupport.web.servlet.ExampleFilter;
import org.springframework.boot.testsupport.web.servlet.ExampleServlet;
import org.springframework.boot.web.server.Compression;
import org.springframework.boot.web.server.Cookie.SameSite;
import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.GracefulShutdownResult;
import org.springframework.boot.web.server.Http2;
import org.springframework.boot.web.server.MimeMappings;
import org.springframework.boot.web.server.PortInUseException;
import org.springframework.boot.web.server.Shutdown;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.Ssl.ClientAuth;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.server.Session.SessionTrackingMode;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Base for testing classes that extends {@link AbstractServletWebServerFactory}.
 *
 * @author Phillip Webb
 * @author Greg Turnquist
 * @author Andy Wilkinson
 * @author Raja Kolli
 * @author Scott Frederick
 * @author Moritz Halbritter
 */
@ExtendWith(OutputCaptureExtension.class)
@DirtiesUrlFactories
public abstract class AbstractServletWebServerFactoryTests {

	@TempDir
	protected File tempDir;

	protected WebServer webServer;

	private final HttpClientContext httpClientContext = HttpClientContext.create();

	private final Supplier<HttpClientBuilder> httpClientBuilder = () -> HttpClients.custom()
		.setRetryStrategy(new DefaultHttpRequestRetryStrategy(10, TimeValue.of(200, TimeUnit.MILLISECONDS)));

	@AfterEach
	void tearDown() {
		if (this.webServer != null) {
			try {
				this.webServer.stop();
				try {
					this.webServer.destroy();
				}
				catch (Exception ex) {
					// Ignore
				}
			}
			catch (Exception ex) {
				// Ignore
			}
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
	protected void restartAfterStop() throws IOException, URISyntaxException {
		AbstractServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer(exampleServletRegistration());
		this.webServer.start();
		assertThat(getResponse(getLocalUrl("/hello"))).isEqualTo("Hello World");
		int port = this.webServer.getPort();
		this.webServer.stop();
		assertThatIOException().isThrownBy(() -> getResponse(getLocalUrl(port, "/hello")));
		this.webServer.start();
		assertThat(getResponse(getLocalUrl("/hello"))).isEqualTo("Hello World");
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
	void stopServlet() {
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
		assertThat(servlet.getInitCount()).isZero();
		this.webServer.start();
		assertThat(servlet.getInitCount()).isOne();
	}

	@Test
	void portIsMinusOneWhenConnectionIsClosed() {
		AbstractServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer();
		this.webServer.start();
		assertThat(this.webServer.getPort()).isGreaterThan(0);
		this.webServer.destroy();
		assertThat(this.webServer.getPort()).isEqualTo(-1);
	}

	@Test
	void specificPort() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		int specificPort = doWithRetry(() -> {
			factory.setPort(0);
			this.webServer = factory.getWebServer(exampleServletRegistration());
			this.webServer.start();
			return this.webServer.getPort();
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
			then(initializer).should(ordered).onStartup(any(ServletContext.class));
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
		assertThat(response.getHeaders().getContentType()).hasToString("text/css");
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
	@WithPackageResources("test.jks")
	void basicSslFromClassPath() throws Exception {
		testBasicSslWithKeyStore("classpath:test.jks");
	}

	@Test
	@WithPackageResources("test.jks")
	void basicSslFromFileSystem(@ResourcePath("test.jks") String keyStore) throws Exception {
		testBasicSslWithKeyStore(keyStore);
	}

	@Test
	@WithPackageResources("test.jks")
	void sslDisabled() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		Ssl ssl = getSsl(null, "password", "classpath:test.jks");
		ssl.setEnabled(false);
		factory.setSsl(ssl);
		this.webServer = factory.getWebServer(new ServletRegistrationBean<>(new ExampleServlet(true, false), "/hello"));
		this.webServer.start();
		HttpComponentsClientHttpRequestFactory requestFactory = createHttpComponentsRequestFactory(
				createTrustSelfSignedTlsSocketStrategy());
		assertThatExceptionOfType(SSLException.class)
			.isThrownBy(() -> getResponse(getLocalUrl("https", "/hello"), requestFactory));
	}

	@Test
	@WithPackageResources("test.jks")
	void sslGetScheme() throws Exception { // gh-2232
		AbstractServletWebServerFactory factory = getFactory();
		factory.setSsl(getSsl(null, "password", "classpath:test.jks"));
		this.webServer = factory.getWebServer(new ServletRegistrationBean<>(new ExampleServlet(true, false), "/hello"));
		this.webServer.start();
		HttpComponentsClientHttpRequestFactory requestFactory = createHttpComponentsRequestFactory(
				createTrustSelfSignedTlsSocketStrategy());
		assertThat(getResponse(getLocalUrl("https", "/hello"), requestFactory)).contains("scheme=https");
	}

	@Test
	@WithPackageResources("test.jks")
	void sslKeyAlias() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		Ssl ssl = getSsl(null, "password", "test-alias", "classpath:test.jks");
		factory.setSsl(ssl);
		ServletRegistrationBean<ExampleServlet> registration = new ServletRegistrationBean<>(
				new ExampleServlet(true, false), "/hello");
		this.webServer = factory.getWebServer(registration);
		this.webServer.start();
		TrustStrategy trustStrategy = new SerialNumberValidatingTrustSelfSignedStrategy("14ca9ba6abe2a70d");
		SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, trustStrategy).build();
		PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
			.setTlsSocketStrategy(new DefaultClientTlsStrategy(sslContext))
			.build();
		HttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager).build();
		String response = getResponse(getLocalUrl("https", "/hello"),
				new HttpComponentsClientHttpRequestFactory(httpClient));
		assertThat(response).contains("scheme=https");
	}

	@Test
	@WithPackageResources("test.jks")
	void sslWithInvalidAliasFailsDuringStartup() {
		AbstractServletWebServerFactory factory = getFactory();
		Ssl ssl = getSsl(null, "password", "test-alias-404", "classpath:test.jks");
		factory.setSsl(ssl);
		ServletRegistrationBean<ExampleServlet> registration = new ServletRegistrationBean<>(
				new ExampleServlet(true, false), "/hello");
		ThrowingCallable call = () -> factory.getWebServer(registration).start();
		assertThatSslWithInvalidAliasCallFails(call);
	}

	protected void assertThatSslWithInvalidAliasCallFails(ThrowingCallable call) {
		assertThatException().isThrownBy(call)
			.withStackTraceContaining("Keystore does not contain alias 'test-alias-404'");
	}

	@Test
	@WithPackageResources("test.jks")
	void serverHeaderIsDisabledByDefaultWhenUsingSsl() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.setSsl(getSsl(null, "password", "classpath:test.jks"));
		this.webServer = factory.getWebServer(new ServletRegistrationBean<>(new ExampleServlet(true, false), "/hello"));
		this.webServer.start();
		PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
			.setTlsSocketStrategy(createTrustSelfSignedTlsSocketStrategy())
			.build();
		HttpClient httpClient = this.httpClientBuilder.get().setConnectionManager(connectionManager).build();
		ClientHttpResponse response = getClientResponse(getLocalUrl("https", "/hello"), HttpMethod.GET,
				new HttpComponentsClientHttpRequestFactory(httpClient));
		assertThat(response.getHeaders().get("Server")).isNullOrEmpty();
	}

	@Test
	@WithPackageResources("test.jks")
	void serverHeaderCanBeCustomizedWhenUsingSsl() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.setServerHeader("MyServer");
		factory.setSsl(getSsl(null, "password", "classpath:test.jks"));
		this.webServer = factory.getWebServer(new ServletRegistrationBean<>(new ExampleServlet(true, false), "/hello"));
		this.webServer.start();
		PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
			.setTlsSocketStrategy(createTrustSelfSignedTlsSocketStrategy())
			.build();
		HttpClient httpClient = this.httpClientBuilder.get().setConnectionManager(connectionManager).build();
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
		HttpComponentsClientHttpRequestFactory requestFactory = createHttpComponentsRequestFactory(
				createTrustSelfSignedTlsSocketStrategy());
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory)).isEqualTo("test");
	}

	@Test
	@WithPackageResources("test.p12")
	void pkcs12KeyStoreAndTrustStore(@ResourcePath("test.p12") File keyStoreFile) throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		addTestTxtFile(factory);
		factory.setSsl(getSsl(ClientAuth.NEED, null, "classpath:test.p12", "classpath:test.p12", null, null));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		KeyStore keyStore = KeyStore.getInstance("pkcs12");
		loadStore(keyStore, new FileSystemResource(keyStoreFile));
		SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy())
			.loadKeyMaterial(keyStore, "secret".toCharArray())
			.build();
		HttpComponentsClientHttpRequestFactory requestFactory = createHttpComponentsRequestFactory(
				new DefaultClientTlsStrategy(sslContext));
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory)).isEqualTo("test");
	}

	@Test
	@WithPackageResources({ "test.p12", "test-cert.pem", "test-key.pem" })
	void pemKeyStoreAndTrustStore(@ResourcePath("test.p12") File keyStoreFile) throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		addTestTxtFile(factory);
		factory.setSsl(getSsl("classpath:test-cert.pem", "classpath:test-key.pem"));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		KeyStore keyStore = KeyStore.getInstance("pkcs12");
		loadStore(keyStore, new FileSystemResource(keyStoreFile));
		SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy())
			.loadKeyMaterial(keyStore, "secret".toCharArray())
			.build();
		HttpComponentsClientHttpRequestFactory requestFactory = createHttpComponentsRequestFactory(
				new DefaultClientTlsStrategy(sslContext));
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory)).isEqualTo("test");
	}

	@Test
	@WithPackageResources("test.p12")
	void pkcs12KeyStoreAndTrustStoreFromBundle(@ResourcePath("test.p12") File keyStoreFile) throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		addTestTxtFile(factory);
		factory.setSsl(Ssl.forBundle("test"));
		factory.setSslBundles(
				new DefaultSslBundleRegistry("test", createJksSslBundle("classpath:test.p12", "classpath:test.p12")));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		KeyStore keyStore = KeyStore.getInstance("pkcs12");
		loadStore(keyStore, new FileSystemResource(keyStoreFile));
		SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy())
			.loadKeyMaterial(keyStore, "secret".toCharArray())
			.build();
		HttpComponentsClientHttpRequestFactory requestFactory = createHttpComponentsRequestFactory(
				new DefaultClientTlsStrategy(sslContext));
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory)).isEqualTo("test");
	}

	@Test
	@WithPackageResources({ "test.p12", "test-cert.pem", "test-key.pem" })
	void pemKeyStoreAndTrustStoreFromBundle(@ResourcePath("test.p12") File keyStoreFile) throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		addTestTxtFile(factory);
		factory.setSsl(Ssl.forBundle("test"));
		factory.setSslBundles(new DefaultSslBundleRegistry("test",
				createPemSslBundle("classpath:test-cert.pem", "classpath:test-key.pem")));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		KeyStore keyStore = KeyStore.getInstance("pkcs12");
		loadStore(keyStore, new FileSystemResource(keyStoreFile));
		SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy())
			.loadKeyMaterial(keyStore, "secret".toCharArray())
			.build();
		HttpComponentsClientHttpRequestFactory requestFactory = createHttpComponentsRequestFactory(
				new DefaultClientTlsStrategy(sslContext));
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory)).isEqualTo("test");
	}

	@Test
	@WithPackageResources("test.jks")
	void sslNeedsClientAuthenticationSucceedsWithClientCertificate(@ResourcePath("test.jks") File keyStoreFile)
			throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.setRegisterDefaultServlet(true);
		addTestTxtFile(factory);
		factory.setSsl(getSsl(ClientAuth.NEED, "password", "classpath:test.jks", "classpath:test.jks", null, null));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		loadStore(keyStore, new FileSystemResource(keyStoreFile));
		SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy())
			.loadKeyMaterial(keyStore, "password".toCharArray())
			.build();
		HttpComponentsClientHttpRequestFactory requestFactory = createHttpComponentsRequestFactory(
				new DefaultClientTlsStrategy(sslContext));
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory)).isEqualTo("test");
	}

	@Test
	@WithPackageResources("test.jks")
	void sslNeedsClientAuthenticationFailsWithoutClientCertificate() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		addTestTxtFile(factory);
		factory.setSsl(getSsl(ClientAuth.NEED, "password", "classpath:test.jks"));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		HttpComponentsClientHttpRequestFactory requestFactory = createHttpComponentsRequestFactory(
				createTrustSelfSignedTlsSocketStrategy());
		String localUrl = getLocalUrl("https", "/test.txt");
		assertThatIOException().isThrownBy(() -> getResponse(localUrl, requestFactory));
	}

	@Test
	@WithPackageResources("test.jks")
	void sslWantsClientAuthenticationSucceedsWithClientCertificate(@ResourcePath("test.jks") File keyStoreFile)
			throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		addTestTxtFile(factory);
		factory
			.setSsl(getSsl(ClientAuth.WANT, "password", "classpath:test.jks", null, new String[] { "TLSv1.2" }, null));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		loadStore(keyStore, new FileSystemResource(keyStoreFile));
		SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy())
			.loadKeyMaterial(keyStore, "password".toCharArray())
			.build();
		HttpComponentsClientHttpRequestFactory requestFactory = createHttpComponentsRequestFactory(
				new DefaultClientTlsStrategy(sslContext));
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory)).isEqualTo("test");
	}

	@Test
	@WithPackageResources("test.jks")
	void sslWantsClientAuthenticationSucceedsWithoutClientCertificate() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		addTestTxtFile(factory);
		factory.setSsl(getSsl(ClientAuth.WANT, "password", "classpath:test.jks"));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		HttpComponentsClientHttpRequestFactory requestFactory = createHttpComponentsRequestFactory(
				createTrustSelfSignedTlsSocketStrategy());
		assertThat(getResponse(getLocalUrl("https", "/test.txt"), requestFactory)).isEqualTo("test");
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

	protected Ssl getSsl(ClientAuth clientAuth, String keyPassword, String keyStore, String trustStore,
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

	private Ssl getSsl(String cert, String privateKey) {
		Ssl ssl = new Ssl();
		ssl.setClientAuth(ClientAuth.NEED);
		ssl.setCertificate(cert);
		ssl.setCertificatePrivateKey(privateKey);
		ssl.setTrustCertificate(cert);
		return ssl;
	}

	private SslBundle createJksSslBundle(String keyStore, String trustStore) {
		JksSslStoreDetails keyStoreDetails = getJksStoreDetails(keyStore);
		JksSslStoreDetails trustStoreDetails = getJksStoreDetails(trustStore);
		SslStoreBundle stores = new JksSslStoreBundle(keyStoreDetails, trustStoreDetails);
		return SslBundle.of(stores);
	}

	private JksSslStoreDetails getJksStoreDetails(String location) {
		return new JksSslStoreDetails(getStoreType(location), null, location, "secret");
	}

	protected SslBundle createPemSslBundle(String cert, String privateKey) {
		PemSslStoreDetails keyStoreDetails = PemSslStoreDetails.forCertificate(cert).withPrivateKey(privateKey);
		PemSslStoreDetails trustStoreDetails = PemSslStoreDetails.forCertificate(cert);
		SslStoreBundle stores = new PemSslStoreBundle(keyStoreDetails, trustStoreDetails);
		return SslBundle.of(stores);
	}

	protected HttpComponentsClientHttpRequestFactory createHttpComponentsRequestFactory(
			TlsSocketStrategy tlsSocketStrategy) {
		PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
			.setTlsSocketStrategy(tlsSocketStrategy)
			.build();
		HttpClient httpClient = this.httpClientBuilder.get().setConnectionManager(connectionManager).build();
		return new HttpComponentsClientHttpRequestFactory(httpClient);
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
		this.webServer.destroy();
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
		this.webServer.destroy();
		File[] dirContents = sessionStoreDir.listFiles((dir, name) -> !(".".equals(name) || "..".equals(name)));
		assertThat(dirContents).isNotEmpty();
	}

	@Test
	void getValidSessionStoreWhenSessionStoreNotSet() {
		AbstractServletWebServerFactory factory = getFactory();
		File dir = factory.getValidSessionStoreDir(false);
		assertThat(dir).hasName("servlet-sessions");
		assertThat(dir).hasParent(new ApplicationTemp().getDir());
	}

	@Test
	void getValidSessionStoreWhenSessionStoreIsRelative() {
		AbstractServletWebServerFactory factory = getFactory();
		factory.getSession().setStoreDir(new File("sessions"));
		File dir = factory.getValidSessionStoreDir(false);
		assertThat(dir).hasName("sessions");
		assertThat(dir).hasParent(new ApplicationHome().getDir());
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
		factory.getSession().getCookie().setHttpOnly(true);
		factory.getSession().getCookie().setSecure(true);
		factory.getSession().getCookie().setPartitioned(true);
		factory.getSession().getCookie().setMaxAge(Duration.ofSeconds(60));
		final AtomicReference<SessionCookieConfig> configReference = new AtomicReference<>();
		this.webServer = factory.getWebServer((context) -> configReference.set(context.getSessionCookieConfig()));
		SessionCookieConfig sessionCookieConfig = configReference.get();
		assertThat(sessionCookieConfig.getName()).isEqualTo("testname");
		assertThat(sessionCookieConfig.getDomain()).isEqualTo("testdomain");
		assertThat(sessionCookieConfig.getPath()).isEqualTo("/testpath");
		assertThat(sessionCookieConfig.isHttpOnly()).isTrue();
		assertThat(sessionCookieConfig.isSecure()).isTrue();
		assertThat(sessionCookieConfig.getAttribute("Partitioned")).isEqualTo("true");
		assertThat(sessionCookieConfig.getMaxAge()).isEqualTo(60);
	}

	@ParameterizedTest
	@EnumSource
	void sessionCookieSameSiteAttributeCanBeConfiguredAndOnlyAffectsSessionCookies(SameSite sameSite) throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.getSession().getCookie().setSameSite(sameSite);
		factory.addInitializers(new ServletRegistrationBean<>(new CookieServlet(false), "/"));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		ClientHttpResponse clientResponse = getClientResponse(getLocalUrl("/"));
		List<String> setCookieHeaders = clientResponse.getHeaders().get("Set-Cookie");
		assertThat(setCookieHeaders).satisfiesExactlyInAnyOrder(
				(header) -> assertThat(header).contains("JSESSIONID").contains("SameSite=" + sameSite.attributeValue()),
				(header) -> assertThat(header).contains("test=test").doesNotContain("SameSite"));
	}

	@ParameterizedTest
	@EnumSource
	void sessionCookieSameSiteAttributeCanBeConfiguredAndOnlyAffectsSessionCookiesWhenUsingCustomName(SameSite sameSite)
			throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.getSession().getCookie().setName("THESESSION");
		factory.getSession().getCookie().setSameSite(sameSite);
		factory.addInitializers(new ServletRegistrationBean<>(new CookieServlet(false), "/"));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		ClientHttpResponse clientResponse = getClientResponse(getLocalUrl("/"));
		List<String> setCookieHeaders = clientResponse.getHeaders().get("Set-Cookie");
		assertThat(setCookieHeaders).satisfiesExactlyInAnyOrder(
				(header) -> assertThat(header).contains("THESESSION").contains("SameSite=" + sameSite.attributeValue()),
				(header) -> assertThat(header).contains("test=test").doesNotContain("SameSite"));
	}

	@Test
	void cookieSameSiteSuppliers() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		factory.addCookieSameSiteSuppliers(CookieSameSiteSupplier.ofLax().whenHasName("relaxed"));
		factory.addCookieSameSiteSuppliers(CookieSameSiteSupplier.ofNone().whenHasName("empty"));
		factory.addCookieSameSiteSuppliers(CookieSameSiteSupplier.ofStrict().whenHasName("controlled"));
		factory.addInitializers(new ServletRegistrationBean<>(new CookieServlet(true), "/"));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		ClientHttpResponse clientResponse = getClientResponse(getLocalUrl("/"));
		assertThat(clientResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		List<String> setCookieHeaders = clientResponse.getHeaders().get("Set-Cookie");
		assertThat(setCookieHeaders).satisfiesExactlyInAnyOrder(
				(header) -> assertThat(header).contains("JSESSIONID").doesNotContain("SameSite"),
				(header) -> assertThat(header).contains("test=test").doesNotContain("SameSite"),
				(header) -> assertThat(header).contains("relaxed=test").contains("SameSite=Lax"),
				(header) -> assertThat(header).contains("empty=test").contains("SameSite=None"),
				(header) -> assertThat(header).contains("controlled=test").contains("SameSite=Strict"));
	}

	@Test
	void cookieSameSiteSuppliersShouldNotAffectSessionCookie() throws IOException, URISyntaxException {
		AbstractServletWebServerFactory factory = getFactory();
		factory.getSession().getCookie().setSameSite(SameSite.LAX);
		factory.getSession().getCookie().setName("SESSIONCOOKIE");
		factory.addCookieSameSiteSuppliers(CookieSameSiteSupplier.ofStrict());
		factory.addInitializers(new ServletRegistrationBean<>(new CookieServlet(false), "/"));
		this.webServer = factory.getWebServer();
		this.webServer.start();
		ClientHttpResponse clientResponse = getClientResponse(getLocalUrl("/"));
		assertThat(clientResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		List<String> setCookieHeaders = clientResponse.getHeaders().get("Set-Cookie");
		assertThat(setCookieHeaders).satisfiesExactlyInAnyOrder(
				(header) -> assertThat(header).contains("SESSIONCOOKIE").contains("SameSite=Lax"),
				(header) -> assertThat(header).contains("test=test").contains("SameSite=Strict"));
	}

	@Test
	protected void sslSessionTracking() {
		AbstractServletWebServerFactory factory = getFactory();
		Ssl ssl = new Ssl();
		ssl.setEnabled(true);
		ssl.setKeyStore("src/test/resources/org/springframework/boot/web/server/test.jks");
		ssl.setKeyPassword("password");
		factory.setSsl(ssl);
		factory.getSession().setTrackingModes(EnumSet.of(SessionTrackingMode.SSL));
		AtomicReference<ServletContext> contextReference = new AtomicReference<>();
		this.webServer = factory.getWebServer(contextReference::set);
		assertThat(contextReference.get().getEffectiveSessionTrackingModes())
			.isEqualTo(EnumSet.of(jakarta.servlet.SessionTrackingMode.SSL));
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
	protected void noCompressionForUserAgent() throws Exception {
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
		LinkedHashMap<String, InputStreamFactory> contentDecoderMap = new LinkedHashMap<>();
		contentDecoderMap.put("gzip", inputStreamFactory);
		getResponse(getLocalUrl("/hello"), new HttpComponentsClientHttpRequestFactory(
				this.httpClientBuilder.get().setContentDecoderRegistry(contentDecoderMap).build()));
		assertThat(inputStreamFactory.wasCompressionUsed()).isTrue();
	}

	@Test
	void mimeMappingsAreCorrectlyConfigured() {
		AbstractServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer();
		Collection<MimeMappings.Mapping> configuredMimeMappings = getActualMimeMappings().entrySet()
			.stream()
			.map((entry) -> new MimeMappings.Mapping(entry.getKey(), entry.getValue()))
			.toList();
		Collection<MimeMappings.Mapping> expectedMimeMappings = MimeMappings.DEFAULT.getAll();
		assertThat(configuredMimeMappings).containsExactlyInAnyOrderElementsOf(expectedMimeMappings);
	}

	@Test
	void additionalMimeMappingsCanBeConfigured() {
		AbstractServletWebServerFactory factory = getFactory();
		MimeMappings additionalMimeMappings = new MimeMappings();
		additionalMimeMappings.add("a", "alpha");
		additionalMimeMappings.add("b", "bravo");
		factory.addMimeMappings(additionalMimeMappings);
		this.webServer = factory.getWebServer();
		Collection<MimeMappings.Mapping> configuredMimeMappings = getActualMimeMappings().entrySet()
			.stream()
			.map((entry) -> new MimeMappings.Mapping(entry.getKey(), entry.getValue()))
			.toList();
		List<MimeMappings.Mapping> expectedMimeMappings = new ArrayList<>(MimeMappings.DEFAULT.getAll());
		expectedMimeMappings.addAll(additionalMimeMappings.getAll());
		assertThat(configuredMimeMappings).containsExactlyInAnyOrderElementsOf(expectedMimeMappings);
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
	@EnabledOnLocale(language = "en")
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
	@EnabledOnLocale(language = "en")
	protected void portClashOfSecondaryConnectorResultsInPortInUseException() throws Exception {
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
		factory.setAddress(InetAddress.getByName("129.129.129.129"));
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
		factory.getSession().getCookie().setHttpOnly(true);
		factory.getSession().getCookie().setSecure(true);
		factory.getSession().getCookie().setPartitioned(false);
		factory.getSession().getCookie().setMaxAge(Duration.ofMinutes(1));
		AtomicReference<ServletContext> contextReference = new AtomicReference<>();
		factory.getWebServer(contextReference::set).start();
		ServletContext servletContext = contextReference.get();
		assertThat(servletContext.getEffectiveSessionTrackingModes())
			.isEqualTo(EnumSet.of(jakarta.servlet.SessionTrackingMode.COOKIE, jakarta.servlet.SessionTrackingMode.URL));
		assertThat(servletContext.getSessionCookieConfig().getName()).isEqualTo("testname");
		assertThat(servletContext.getSessionCookieConfig().getDomain()).isEqualTo("testdomain");
		assertThat(servletContext.getSessionCookieConfig().getPath()).isEqualTo("/testpath");
		assertThat(servletContext.getSessionCookieConfig().isHttpOnly()).isTrue();
		assertThat(servletContext.getSessionCookieConfig().isSecure()).isTrue();
		assertThat(servletContext.getSessionCookieConfig().getMaxAge()).isEqualTo(60);
		assertThat(servletContext.getSessionCookieConfig().getAttribute("Partitioned")).isEqualTo("false");
	}

	@Test
	protected void servletContextListenerContextDestroyedIsNotCalledWhenContainerIsStopped() throws Exception {
		ServletContextListener listener = mock(ServletContextListener.class);
		this.webServer = getFactory().getWebServer((servletContext) -> servletContext.addListener(listener));
		this.webServer.start();
		this.webServer.stop();
		then(listener).should(never()).contextDestroyed(any(ServletContextEvent.class));
	}

	@Test
	void servletContextListenerContextDestroyedIsCalledWhenContainerIsDestroyed() throws Exception {
		ServletContextListener listener = mock(ServletContextListener.class);
		this.webServer = getFactory().getWebServer((servletContext) -> servletContext.addListener(listener));
		this.webServer.start();
		this.webServer.destroy();
		then(listener).should().contextDestroyed(any(ServletContextEvent.class));
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
	void whenARequestIsActiveThenStopWillComplete() throws InterruptedException {
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
			// Ignore
		}
	}

	@Test
	protected void whenHttp2IsEnabledAndSslIsDisabledThenH2cCanBeUsed() throws Exception {
		AbstractServletWebServerFactory factory = getFactory();
		Http2 http2 = new Http2();
		http2.setEnabled(true);
		factory.setHttp2(http2);
		this.webServer = factory.getWebServer(exampleServletRegistration());
		this.webServer.start();
		try (org.eclipse.jetty.client.HttpClient client = new org.eclipse.jetty.client.HttpClient(
				new HttpClientTransportOverHTTP2(new HTTP2Client()))) {
			client.start();
			ContentResponse response = client.GET("http://localhost:" + this.webServer.getPort() + "/hello");
			assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
			assertThat(response.getContentAsString()).isEqualTo("Hello World");
		}
	}

	@Test
	protected void whenHttp2IsEnabledAndSslIsDisabledThenHttp11CanStillBeUsed() throws IOException, URISyntaxException {
		AbstractServletWebServerFactory factory = getFactory();
		Http2 http2 = new Http2();
		http2.setEnabled(true);
		factory.setHttp2(http2);
		this.webServer = factory.getWebServer(exampleServletRegistration());
		this.webServer.start();
		assertThat(getResponse("http://localhost:" + this.webServer.getPort() + "/hello")).isEqualTo("Hello World");
	}

	@Test
	void whenARequestIsActiveAfterGracefulShutdownEndsThenStopWillComplete() throws InterruptedException {
		AbstractServletWebServerFactory factory = getFactory();
		factory.setShutdown(Shutdown.GRACEFUL);
		BlockingServlet blockingServlet = new BlockingServlet();
		this.webServer = factory
			.getWebServer((context) -> context.addServlet("blockingServlet", blockingServlet).addMapping("/"));
		this.webServer.start();
		int port = this.webServer.getPort();
		initiateGetRequest(port, "/");
		blockingServlet.awaitQueue();
		AtomicReference<GracefulShutdownResult> result = new AtomicReference<>();
		this.webServer.shutDownGracefully(result::set);
		this.webServer.stop();
		assertThat(Awaitility.await().atMost(Duration.ofSeconds(30)).until(result::get, Objects::nonNull))
			.isEqualTo(GracefulShutdownResult.REQUESTS_ACTIVE);
		try {
			blockingServlet.admitOne();
		}
		catch (RuntimeException ex) {
			// Ignore
		}
	}

	@Test
	void startedLogMessageWithSinglePort() {
		AbstractServletWebServerFactory factory = getFactory();
		this.webServer = factory.getWebServer();
		this.webServer.start();
		assertThat(startedLogMessage()).matches("(Jetty|Tomcat|Undertow) started on port " + this.webServer.getPort()
				+ " \\(http(/1.1)?\\) with context path '/'");
	}

	@Test
	void startedLogMessageWithSinglePortAndContextPath() {
		AbstractServletWebServerFactory factory = getFactory();
		factory.setContextPath("/test");
		this.webServer = factory.getWebServer();
		this.webServer.start();
		assertThat(startedLogMessage()).matches("(Jetty|Tomcat|Undertow) started on port " + this.webServer.getPort()
				+ " \\(http(/1.1)?\\) with context path '/test'");
	}

	@Test
	void startedLogMessageWithMultiplePorts() {
		AbstractServletWebServerFactory factory = getFactory();
		addConnector(0, factory);
		this.webServer = factory.getWebServer();
		this.webServer.start();
		assertThat(startedLogMessage()).matches("(Jetty|Tomcat|Undertow) started on ports " + this.webServer.getPort()
				+ " \\(http(/1.1)?\\), [0-9]+ \\(http(/1.1)?\\) with context path '/'");
	}

	@Test
	void servletComponentsAreInitializedWithTheSameThreadContextClassLoader() {
		AbstractServletWebServerFactory factory = getFactory();
		ThreadContextClassLoaderCapturingServlet servlet = new ThreadContextClassLoaderCapturingServlet();
		ThreadContextClassLoaderCapturingFilter filter = new ThreadContextClassLoaderCapturingFilter();
		ThreadContextClassLoaderCapturingListener listener = new ThreadContextClassLoaderCapturingListener();
		this.webServer = factory.getWebServer((context) -> {
			context.addServlet("tcclCapturingServlet", servlet).setLoadOnStartup(0);
			context.addFilter("tcclCapturingFilter", filter);
			context.addListener(listener);
		});
		this.webServer.start();
		assertThat(servlet.contextClassLoader).isNotNull();
		assertThat(filter.contextClassLoader).isNotNull();
		assertThat(listener.contextClassLoader).isNotNull();
		assertThat(new HashSet<>(
				Arrays.asList(servlet.contextClassLoader, filter.contextClassLoader, listener.contextClassLoader)))
			.hasSize(1);
	}

	protected Future<Object> initiateGetRequest(int port, String path) {
		return initiateGetRequest(HttpClients.createMinimal(), port, path);
	}

	protected Future<Object> initiateGetRequest(HttpClient httpClient, int port, String path) {
		RunnableFuture<Object> getRequest = new FutureTask<>(() -> {
			try {
				return httpClient.execute(new HttpGet("http://localhost:" + port + path),
						(HttpClientResponseHandler<HttpResponse>) (response) -> {
							response.getEntity().getContent().close();
							return response;
						});
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
		LinkedHashMap<String, InputStreamFactory> contentDecoderMap = new LinkedHashMap<>();
		contentDecoderMap.put("gzip", inputStreamFactory);
		String response = getResponse(getLocalUrl("/test.txt"), method,
				new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create()
					.setUserAgent("testUserAgent")
					.setContentDecoderRegistry(contentDecoderMap)
					.build()));
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
		factory.addInitializers(new ServletRegistrationBean<>(new HttpServlet() {

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

	protected void addTestTxtFile(AbstractServletWebServerFactory factory) throws IOException {
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
		try (serverSocket) {
			int blockedPort = doWithRetry(() -> {
				serverSocket.bind(null);
				return serverSocket.getLocalPort();
			});
			action.run(blockedPort);
		}
	}

	private void loadStore(KeyStore keyStore, Resource resource)
			throws IOException, NoSuchAlgorithmException, CertificateException {
		try (InputStream stream = resource.getInputStream()) {
			keyStore.load(stream, "secret".toCharArray());
		}
	}

	protected abstract String startedLogMessage();

	protected TlsSocketStrategy createTrustSelfSignedTlsSocketStrategy()
			throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
		SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
		return new DefaultClientTlsStrategy(sslContext);
	}

	private final class TestGzipInputStreamFactory implements InputStreamFactory {

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
		public boolean isTrusted(X509Certificate[] chain, String authType) {
			String hexSerialNumber = chain[0].getSerialNumber().toString(16);
			boolean isMatch = hexSerialNumber.equalsIgnoreCase(this.serialNumber);
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

		private final BlockingQueue<Blocker> blockers = new ArrayBlockingQueue<>(10);

		public BlockingServlet() {

		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			Blocker blocker = new Blocker();
			this.blockers.add(blocker);
			blocker.await();
		}

		public void admitOne() throws InterruptedException {
			this.blockers.take().clear();
		}

		public void awaitQueue() throws InterruptedException {
			while (this.blockers.isEmpty()) {
				Thread.sleep(100);
			}
		}

		public void awaitQueue(int size) throws InterruptedException {
			while (this.blockers.size() < size) {
				Thread.sleep(100);
			}
		}

	}

	static class BlockingAsyncServlet extends HttpServlet {

		private final BlockingQueue<Blocker> blockers = new ArrayBlockingQueue<>(10);

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
			Blocker blocker = new Blocker();
			this.blockers.add(blocker);
			AsyncContext async = req.startAsync();
			new Thread(() -> {
				blocker.await();
				async.complete();
			}).start();
		}

		private void admitOne() throws InterruptedException {
			this.blockers.take().clear();
		}

		private void awaitQueue() throws InterruptedException {
			while (this.blockers.isEmpty()) {
				Thread.sleep(100);
			}
		}

	}

	private static final class Blocker {

		private boolean block = true;

		private final Object monitor = new Object();

		private void await() {
			synchronized (this.monitor) {
				while (this.block) {
					try {
						this.monitor.wait();
					}
					catch (InterruptedException ex) {
						System.out.println("Interrupted!");
						// Keep waiting
					}
				}
			}
		}

		private void clear() {
			synchronized (this.monitor) {
				this.block = false;
				this.monitor.notifyAll();
			}
		}

	}

	static final class CookieServlet extends HttpServlet {

		private final boolean addSupplierTestCookies;

		CookieServlet(boolean addSupplierTestCookies) {
			this.addSupplierTestCookies = addSupplierTestCookies;
		}

		@Override
		protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
			req.getSession(true);
			resp.addCookie(new Cookie("test", "test"));
			if (this.addSupplierTestCookies) {
				resp.addCookie(new Cookie("relaxed", "test"));
				resp.addCookie(new Cookie("empty", "test"));
				resp.addCookie(new Cookie("controlled", "test"));
			}
		}

	}

	protected static class TrustSelfSignedStrategy implements TrustStrategy {

		public TrustSelfSignedStrategy() {
		}

		@Override
		public boolean isTrusted(X509Certificate[] chain, String authType) {
			return chain.length == 1;
		}

	}

	static class ThreadContextClassLoaderCapturingServlet extends HttpServlet {

		private ClassLoader contextClassLoader;

		@Override
		public void init(ServletConfig config) throws ServletException {
			this.contextClassLoader = Thread.currentThread().getContextClassLoader();
		}

	}

	static class ThreadContextClassLoaderCapturingListener implements ServletContextListener {

		private ClassLoader contextClassLoader;

		@Override
		public void contextInitialized(ServletContextEvent sce) {
			this.contextClassLoader = Thread.currentThread().getContextClassLoader();
		}

	}

	static class ThreadContextClassLoaderCapturingFilter implements Filter {

		private ClassLoader contextClassLoader;

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
			this.contextClassLoader = Thread.currentThread().getContextClassLoader();
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
			chain.doFilter(request, response);
		}

	}

}
