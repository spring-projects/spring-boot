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

package sample.tomcat.multiconnector;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.catalina.Service;
import org.apache.catalina.connector.Connector;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import sample.tomcat.multiconnector.SampleTomcatTwoConnectorsApplicationTests.Ports;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration tests for {@link SampleTomcatTwoConnectorsApplication}.
 *
 * @author Brock Mills
 * @author Andy Wilkinson
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext
@Import(Ports.class)
public class SampleTomcatTwoConnectorsApplicationTests {

	@LocalServerPort
	private int port;

	@Autowired
	private ApplicationContext context;

	@BeforeClass
	public static void setUp() {

		try {
			// setup ssl context to ignore certificate errors
			SSLContext ctx = SSLContext.getInstance("TLS");
			X509TrustManager tm = new X509TrustManager() {

				@Override
				public void checkClientTrusted(java.security.cert.X509Certificate[] chain,
						String authType) throws java.security.cert.CertificateException {
				}

				@Override
				public void checkServerTrusted(java.security.cert.X509Certificate[] chain,
						String authType) throws java.security.cert.CertificateException {
				}

				@Override
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}
			};
			ctx.init(null, new TrustManager[] { tm }, null);
			SSLContext.setDefault(ctx);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	@Test
	public void testHello() throws Exception {
		RestTemplate template = new RestTemplate();
		final MySimpleClientHttpRequestFactory factory = new MySimpleClientHttpRequestFactory(
				(hostname, session) -> true);
		template.setRequestFactory(factory);
		Ports ports = this.context.getBean(Ports.class);
		assertThat(ports.getHttpsPort()).isEqualTo(this.port);
		assertThat(ports.getHttpPort()).isNotEqualTo(this.port);
		ResponseEntity<String> entity = template.getForEntity(
				"http://localhost:" + ports.getHttpPort() + "/hello", String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getBody()).isEqualTo("hello");
		ResponseEntity<String> httpsEntity = template
				.getForEntity("https://localhost:" + this.port + "/hello", String.class);
		assertThat(httpsEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(httpsEntity.getBody()).isEqualTo("hello");
	}

	/**
	 * Http Request Factory for ignoring SSL hostname errors. Not for production use!
	 */
	class MySimpleClientHttpRequestFactory extends SimpleClientHttpRequestFactory {

		private final HostnameVerifier verifier;

		public MySimpleClientHttpRequestFactory(final HostnameVerifier verifier) {
			this.verifier = verifier;
		}

		@Override
		protected void prepareConnection(final HttpURLConnection connection,
				final String httpMethod) throws IOException {
			if (connection instanceof HttpsURLConnection) {
				((HttpsURLConnection) connection).setHostnameVerifier(this.verifier);
			}
			super.prepareConnection(connection, httpMethod);
		}
	}

	@TestConfiguration
	static class Ports implements ApplicationListener<WebServerInitializedEvent> {

		private int httpPort;

		private int httpsPort;

		@Override
		public void onApplicationEvent(WebServerInitializedEvent event) {
			Service service = ((TomcatWebServer) event.getWebServer()).getTomcat()
					.getService();
			for (Connector connector : service.findConnectors()) {
				if (connector.getSecure()) {
					this.httpsPort = connector.getLocalPort();
				}
				else {
					this.httpPort = connector.getLocalPort();
				}
			}
		}

		int getHttpPort() {
			return this.httpPort;
		}

		int getHttpsPort() {
			return this.httpsPort;
		}

	}

}
