/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.resttestclient;

import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * Testing utility that trusts all TLS connections during tests. This class should not be
 * in your production classpath as {@code "spring-boot-resttestclient"} is a test
 * dependency in the first place.
 * <p>
 * This can be configured on a test client when binding to a live server using
 * {@link RestTestClient#bindToServer(ClientHttpRequestFactory)}.
 *
 * @author Brian Clozel
 * @since 4.2.0
 */
public final class TrustAllTlsRequestFactory {

	private TrustAllTlsRequestFactory() {

	}

	public static ClientHttpRequestFactory create() {
		try {
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[] { new TrustAllX509TrustManager() }, new SecureRandom());
			HttpClient httpClient = HttpClient.newBuilder().sslContext(sslContext).build();
			return new JdkClientHttpRequestFactory(httpClient);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to create trust-all SSL request factory", ex);
		}
	}

	private static final class TrustAllX509TrustManager implements X509TrustManager {

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) {
		}

	}

}
