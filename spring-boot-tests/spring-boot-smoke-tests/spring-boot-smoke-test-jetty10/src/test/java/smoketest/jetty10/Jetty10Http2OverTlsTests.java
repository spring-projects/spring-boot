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

package smoketest.jetty10;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequests;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.ssl.ClientTlsStrategyBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.ssl.SSLContexts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for HTTP/2 over TLS (h2) with Jetty 10.
 *
 * @author Andy Wilkinson
 */
@EnabledForJreRange(min = JRE.JAVA_11)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT,
		properties = { "server.http2.enabled=true", "server.ssl.enabled=true",
				"server.ssl.keystore=classpath:sample.jks", "server.ssl.key-store-password=secret",
				"server.ssl.key-password=password" })
class Jetty10Http2OverTlsTests {

	@LocalServerPort
	private int port;

	@Test
	void httpOverTlsGetWhenHttp2AndSslAreEnabledSucceeds() throws Exception {
		SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(new TrustAllStrategy()).build();
		TlsStrategy tlsStrategy = ClientTlsStrategyBuilder.create().setSslContext(sslContext).build();
		try (CloseableHttpAsyncClient http2Client = HttpAsyncClients.customHttp2().setTlsStrategy(tlsStrategy)
				.build()) {
			http2Client.start();
			SimpleHttpRequest request = SimpleHttpRequests.get("https://localhost:" + this.port);
			request.setBody("Hello World", ContentType.TEXT_PLAIN);
			SimpleHttpResponse response = http2Client.execute(request, new FutureCallback<SimpleHttpResponse>() {

				@Override
				public void failed(Exception ex) {
				}

				@Override
				public void completed(SimpleHttpResponse result) {
				}

				@Override
				public void cancelled() {
				}

			}).get();
			assertThat(response.getCode()).isEqualTo(200);
			assertThat(response.getBodyText()).isEqualTo("Hello World");
		}
	}

}
