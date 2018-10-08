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

package org.springframework.boot.autoconfigure.influx;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.Collection;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import org.influxdb.InfluxDB;
import org.influxdb.impl.InfluxDBImpl;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for InfluxDB.
 *
 * @author Sergey Kuptsov
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 * @author Ali Dehghani
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass(InfluxDB.class)
@ConditionalOnMissingBean(InfluxDB.class)
@EnableConfigurationProperties(InfluxDbProperties.class)
public class InfluxDbAutoConfiguration {

	/**
	 * Password for in-memory Java Keystore which would contain the possible X.509
	 * certificate for InfluxDB.
	 */
	private static final char[] KEYSTORE_PASS = "password".toCharArray();

	/**
	 * Encapsulates connection properties for InfluxDB client.
	 */
	private final InfluxDbProperties properties;

	/**
	 * The HTTP client builder which will be used to interact with InfluxDB server.
	 */
	private final OkHttpClient.Builder builder;

	/**
	 * Initializes the {@link InfluxDbAutoConfiguration} in a way that it could,
	 * hopefully, be able to interact with desired InfluxDB server.
	 *
	 * @param properties Encapsulates the connection properties
	 * @param builder The OK HTTP client builder
	 */
	public InfluxDbAutoConfiguration(InfluxDbProperties properties,
			ObjectProvider<OkHttpClient.Builder> builder) {
		this.properties = properties;
		this.builder = builder.getIfAvailable(OkHttpClient.Builder::new);

		if (isSslEnabled()) {
			String url = this.properties.getUrl();
			if (url == null || !url.toLowerCase().startsWith("https")) {
				throw new IllegalArgumentException(
						"InfluxDB's URL should starts with https when SSL is enabled");
			}

			configureSslForInfluxDbClient();
		}
	}

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty("spring.influx.url")
	public InfluxDB influxDb() {
		return new InfluxDBImpl(this.properties.getUrl(), this.properties.getUser(),
				this.properties.getPassword(), this.builder);
	}

	/**
	 * Tests whether SSL should be enabled for InfluxDB client or not.
	 *
	 * @return {@code true} if SSL should be configured for InfluxDB client, {@code false}
	 * otherwise.
	 */
	private boolean isSslEnabled() {
		return this.properties != null && this.properties.getSsl() != null
				&& this.properties.getSsl().isEnabled();
	}

	/**
	 * Configures the HTTP client with the certificate encapsulated in the
	 * {@link #properties}.
	 */
	private void configureSslForInfluxDbClient() {
		X509TrustManager trustManager;
		SSLSocketFactory sslSocketFactory;
		try {
			trustManager = trustManagerForCertificates(getCertificateAsStream());
			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, new TrustManager[] { trustManager }, null);
			sslSocketFactory = sslContext.getSocketFactory();
		}
		catch (GeneralSecurityException e) {
			throw new RuntimeException(
					"Failed to load the given certificate for InfluxDB", e);
		}

		this.builder.sslSocketFactory(sslSocketFactory, trustManager);
	}

	/**
	 * Creates a {@link X509TrustManager} from the {@link InputStream} which contains a
	 * X.509 certificate.
	 *
	 * @param inputStream The {@link InputStream} to extract the certificates from
	 * @return A {@link X509TrustManager} instance containing the given certificates
	 * @throws GeneralSecurityException If we fail to create a {@link X509TrustManager}
	 */
	private X509TrustManager trustManagerForCertificates(InputStream inputStream)
			throws GeneralSecurityException {
		Collection<? extends Certificate> certificates = extractX509Certificates(
				inputStream);

		KeyStore keyStore = newEmptyKeyStore();
		int index = 0;
		for (Certificate certificate : certificates) {
			String certificateAlias = Integer.toString(index++);
			keyStore.setCertificateEntry(certificateAlias, certificate);
		}

		return getFirstX509TrustManager(keyStore);
	}

	/**
	 * Loads the {@link X509TrustManager} from the given {@link KeyStore}.
	 *
	 * @param keyStore The keystore that contains the certificate
	 * @return The corresponding {@link X509TrustManager}
	 * @throws GeneralSecurityException If we fail to load the certificate
	 */
	private X509TrustManager getFirstX509TrustManager(KeyStore keyStore)
			throws GeneralSecurityException {
		KeyManagerFactory keyManagerFactory = KeyManagerFactory
				.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyManagerFactory.init(keyStore, KEYSTORE_PASS);
		TrustManagerFactory trustManagerFactory = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(keyStore);
		TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
		if (trustManagers.length != 1
				|| !(trustManagers[0] instanceof X509TrustManager)) {
			throw new IllegalStateException(
					"Unexpected default trust managers for InfluxDB:"
							+ Arrays.toString(trustManagers));
		}

		return (X509TrustManager) trustManagers[0];
	}

	/**
	 * Extract all X.509 certificates from the given {@link InputStream}.
	 *
	 * @param inputStream The stream to extract certificates from
	 * @return Collection of all X.509 certificates in the given {@code inputStream}
	 * @throws CertificateException If for whatever reason we fail to extract X.509
	 * certificates
	 */
	private Collection<? extends Certificate> extractX509Certificates(
			InputStream inputStream) throws CertificateException {
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		Collection<? extends Certificate> certificates = certificateFactory
				.generateCertificates(inputStream);
		if (certificates.isEmpty()) {
			throw new IllegalArgumentException(
					"Couldn't find any InfluxDB certificate in the given certificate file");
		}

		return certificates;
	}

	/**
	 * Loads the given path to certificate file (Encapsulated in the
	 * {@link InfluxDbProperties.Ssl#getCertificate()}) into an instance of
	 * {@link InputStream}.
	 *
	 * @return The {@link InputStream} corresponding to certificate path
	 * @throws IllegalArgumentException If something is wrong with the given certificate
	 * path, maybe it's a blank string or there is no such file.
	 */
	private InputStream getCertificateAsStream() {
		Resource certificate = this.properties.getSsl().getCertificate();
		if (certificate == null) {
			throw new IllegalArgumentException(
					"Since SSL is enabled for InfluxDB, provide the path to certificate file");
		}

		if (!certificate.exists()) {
			throw new IllegalArgumentException(
					"Couldn't find the InfluxDB certificate file: "
							+ certificate.getFilename());
		}

		try {
			return certificate.getInputStream();
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Couldn't load the InfluxDB certificate",
					e);
		}
	}

	/**
	 * Creates an empty and brand new {@link KeyStore} protected by
	 * {@link #KEYSTORE_PASS}.
	 *
	 * @return The empty keystore
	 */
	private KeyStore newEmptyKeyStore() {
		try {
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(null, KEYSTORE_PASS);

			return keyStore;
		}
		catch (Exception e) {
			throw new RuntimeException(
					"Failed to create a keystore to hold the InfluxDB's certificate");
		}
	}
}
