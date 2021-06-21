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

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.http2.client.HTTP2Client;
import org.eclipse.jetty.http2.client.http.HttpClientTransportOverHTTP2;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

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
				"server.ssl.key-password=password", "logging.level.org.eclipse.jetty=debug" })
public class Jetty10Http2OverTlsTests {

	@LocalServerPort
	private int port;

	@Test
	void httpOverTlsGetWhenHttp2AndSslAreEnabledSucceeds() throws Exception {
		SslContextFactory.Client sslContextFactory = new SslContextFactory.Client();
		sslContextFactory.setTrustAll(true);
		ClientConnector clientConnector = new ClientConnector();
		clientConnector.setSslContextFactory(sslContextFactory);
		HttpClient client = new HttpClient(new HttpClientTransportOverHTTP2(new HTTP2Client(clientConnector)));
		client.start();
		try {
			ContentResponse response = client.GET("https://localhost:" + this.port + "/");
			assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
			assertThat(response.getContentAsString()).isEqualTo("Hello World");
		}
		finally {
			client.stop();
		}
	}

}
