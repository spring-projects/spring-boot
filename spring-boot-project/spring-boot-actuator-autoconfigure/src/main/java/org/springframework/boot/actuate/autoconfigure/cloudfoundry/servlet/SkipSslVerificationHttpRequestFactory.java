/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.cloudfoundry.servlet;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * {@link SimpleClientHttpRequestFactory} that skips SSL certificate verification.
 *
 * @author Madhura Bhave
 */
class SkipSslVerificationHttpRequestFactory extends SimpleClientHttpRequestFactory {

	/**
     * Prepares the connection for making an HTTP request.
     * 
     * @param connection the HttpURLConnection object representing the connection
     * @param httpMethod the HTTP method to be used for the request
     * @throws IOException if an I/O error occurs while preparing the connection
     */
    @Override
	protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
		if (connection instanceof HttpsURLConnection httpsURLConnection) {
			prepareHttpsConnection(httpsURLConnection);
		}
		super.prepareConnection(connection, httpMethod);
	}

	/**
     * Prepares the HTTPS connection by setting the hostname verifier and SSL socket factory.
     * 
     * @param connection the HTTPS connection to be prepared
     */
    private void prepareHttpsConnection(HttpsURLConnection connection) {
		connection.setHostnameVerifier(new SkipHostnameVerifier());
		try {
			connection.setSSLSocketFactory(createSslSocketFactory());
		}
		catch (Exception ex) {
			// Ignore
		}
	}

	/**
     * Creates a custom SSL socket factory that skips SSL verification.
     * 
     * @return the SSL socket factory
     * @throws Exception if an error occurs while creating the SSL socket factory
     */
    private SSLSocketFactory createSslSocketFactory() throws Exception {
		SSLContext context = SSLContext.getInstance("TLS");
		context.init(null, new TrustManager[] { new SkipX509TrustManager() }, new SecureRandom());
		return context.getSocketFactory();
	}

	/**
     * SkipHostnameVerifier class.
     */
    private static final class SkipHostnameVerifier implements HostnameVerifier {

		/**
         * Verifies the hostname of the server.
         * 
         * @param s the hostname to be verified
         * @param sslSession the SSL session associated with the connection
         * @return true if the hostname is verified, false otherwise
         */
        @Override
		public boolean verify(String s, SSLSession sslSession) {
			return true;
		}

	}

	/**
     * SkipX509TrustManager class.
     */
    private static final class SkipX509TrustManager implements X509TrustManager {

		/**
         * Returns an array of X509Certificates that are accepted issuers for this trust manager.
         *
         * @return an array of X509Certificates that are accepted issuers for this trust manager.
         */
        @Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}

		/**
         * This method is used to check the trustworthiness of a client's X509 certificate.
         * 
         * @param chain    the X509 certificate chain presented by the client
         * @param authType the authentication type used
         */
        @Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) {
		}

		/**
         * This method is used to check the trustworthiness of the server's X509 certificate chain.
         * 
         * @param chain    the X509 certificate chain of the server
         * @param authType the authentication type used
         */
        @Override
		public void checkServerTrusted(X509Certificate[] chain, String authType) {
		}

	}

}
