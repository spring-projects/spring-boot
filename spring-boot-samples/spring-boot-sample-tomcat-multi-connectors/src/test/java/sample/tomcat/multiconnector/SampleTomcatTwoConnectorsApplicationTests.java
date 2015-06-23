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

package sample.tomcat.multiconnector;

import java.io.IOException;
import java.net.HttpURLConnection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

import sample.tomcat.multiconnector.SampleTomcatTwoConnectorsApplication;
import static org.junit.Assert.assertEquals;

/**
 * Basic integration tests for 2 connector demo application.
 *
 * @author Brock Mills
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SampleTomcatTwoConnectorsApplication.class)
@WebAppConfiguration
@IntegrationTest("server.port=0")
@DirtiesContext
public class SampleTomcatTwoConnectorsApplicationTests {

	@Value("${local.server.port}")
	private String port;

	@Autowired
	private ApplicationContext context;

	@BeforeClass
	public static void setUp() {

		try {
			// setup ssl context to ignore certificate errors
			SSLContext ctx = SSLContext.getInstance("TLS");
			X509TrustManager tm = new X509TrustManager() {

				@Override
				public void checkClientTrusted(
						java.security.cert.X509Certificate[] chain, String authType)
						throws java.security.cert.CertificateException {
				}

				@Override
				public void checkServerTrusted(
						java.security.cert.X509Certificate[] chain, String authType)
						throws java.security.cert.CertificateException {
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
				new HostnameVerifier() {

					@Override
					public boolean verify(final String hostname, final SSLSession session) {
						return true; // these guys are alright by me...
					}
				});
		template.setRequestFactory(factory);

		ResponseEntity<String> entity = template.getForEntity("http://localhost:"
				+ this.port + "/hello", String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("hello", entity.getBody());

		ResponseEntity<String> httpsEntity = template.getForEntity("https://localhost:"
				+ this.context.getBean("port") + "/hello", String.class);
		assertEquals(HttpStatus.OK, httpsEntity.getStatusCode());
		assertEquals("hello", httpsEntity.getBody());

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

}
