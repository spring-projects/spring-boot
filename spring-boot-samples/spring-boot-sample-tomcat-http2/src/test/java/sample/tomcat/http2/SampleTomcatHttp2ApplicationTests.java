/*
 * Copyright 2012-2017 the original author or authors.
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

package sample.tomcat.http2;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for sample application that verify HTTP/1.1 and HTTP/2 support.
 *
 * @author Paul Vorbach
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
public class SampleTomcatHttp2ApplicationTests {

	@LocalServerPort
	private int port;

	private OkHttpClient okHttp11Client;
	private OkHttpClient okHttp2Client;

	@Before
	public void setUp() throws Exception {
		this.okHttp11Client = createInsecureOkHttpClient(Protocol.HTTP_1_1);
		this.okHttp2Client = createInsecureOkHttpClient(Protocol.HTTP_1_1, Protocol.HTTP_2);
	}

	@Test
	public void testHttp11() throws Exception {
		final Request request = new Request.Builder()
				.url(String.format("https://localhost:%d/", port))
				.build();

		final Response response = okHttp11Client.newCall(request).execute();

		assertThat(response.protocol()).isEqualTo(Protocol.HTTP_1_1);
		assertThat(response.code()).isEqualTo(HttpStatus.OK.value());
		assertThat(response.body().string()).isEqualTo("Hello, world");
	}

	/**
	 * Tests that the server has HTTP/2 enabled.
	 * <p>
	 * This only works when both tomcat-native is in PATH <em>and</em> it is running on
	 * Java 9, since OkHttp makes use of Java 9's support for ALPN.
	 */
	@Ignore
	@Test
	public void testHttp2() throws Exception {
		final Request request = new Request.Builder()
				.url(String.format("https://localhost:%d/", port))
				.build();

		final Response response = okHttp2Client.newCall(request).execute();

		assertThat(response.protocol()).isEqualTo(Protocol.HTTP_2);
		assertThat(response.code()).isEqualTo(HttpStatus.OK.value());
		assertThat(response.body().string()).isEqualTo("Hello, world");
	}

	private static OkHttpClient createInsecureOkHttpClient(Protocol... supportedProtocols)
			throws NoSuchAlgorithmException, KeyManagementException {

		final HostnameVerifier acceptAllHostnamesVerifier = (hostname, sslSession) -> true;
		final InsecureTrustManager insecureTrustManager = new InsecureTrustManager();
		final SSLContext sslContext = createSslContext(insecureTrustManager);

		return new OkHttpClient.Builder()
				.protocols(Arrays.asList(supportedProtocols))
				.hostnameVerifier(acceptAllHostnamesVerifier)
				.sslSocketFactory(sslContext.getSocketFactory(), insecureTrustManager)
				.build();
	}

	private static SSLContext createSslContext(TrustManager trustManager)
			throws KeyManagementException, NoSuchAlgorithmException {

		final SSLContext sslContext = SSLContext.getInstance("TLSv1");

		sslContext.init(null, new TrustManager[] { trustManager }, new SecureRandom());

		return sslContext;
	}

	private static class InsecureTrustManager implements X509TrustManager {

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws CertificateException {
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[] {};
		}
	}

}
