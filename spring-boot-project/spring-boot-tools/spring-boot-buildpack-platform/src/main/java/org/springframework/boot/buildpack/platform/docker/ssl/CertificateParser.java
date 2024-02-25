/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker.ssl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for X.509 certificates in PEM format.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
final class CertificateParser {

	private static final String HEADER = "-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+";

	private static final String BASE64_TEXT = "([a-z0-9+/=\\r\\n]+)";

	private static final String FOOTER = "-+END\\s+.*CERTIFICATE[^-]*-+";

	private static final Pattern PATTERN = Pattern.compile(HEADER + BASE64_TEXT + FOOTER, Pattern.CASE_INSENSITIVE);

	/**
     * Private constructor for the CertificateParser class.
     */
    private CertificateParser() {
	}

	/**
	 * Load certificates from the specified file paths.
	 * @param paths one or more paths to certificate files
	 * @return certificates parsed from specified file paths
	 */
	static X509Certificate[] parse(Path... paths) {
		CertificateFactory factory = getCertificateFactory();
		List<X509Certificate> certificates = new ArrayList<>();
		for (Path path : paths) {
			readCertificates(path, factory, certificates::add);
		}
		return certificates.toArray(new X509Certificate[0]);
	}

	/**
     * Returns the X.509 certificate factory.
     * 
     * @return the X.509 certificate factory
     * @throws IllegalStateException if unable to get X.509 certificate factory
     */
    private static CertificateFactory getCertificateFactory() {
		try {
			return CertificateFactory.getInstance("X.509");
		}
		catch (CertificateException ex) {
			throw new IllegalStateException("Unable to get X.509 certificate factory", ex);
		}
	}

	/**
     * Reads certificates from a file specified by the given path using the provided CertificateFactory.
     * The certificates are then passed to the consumer for further processing.
     *
     * @param path     the path to the file containing the certificates
     * @param factory  the CertificateFactory used to generate X509Certificates
     * @param consumer the consumer to accept the X509Certificates
     * @throws IllegalStateException if there is an error reading the certificates from the file
     */
    private static void readCertificates(Path path, CertificateFactory factory, Consumer<X509Certificate> consumer) {
		try {
			String text = Files.readString(path);
			Matcher matcher = PATTERN.matcher(text);
			while (matcher.find()) {
				String encodedText = matcher.group(1);
				byte[] decodedBytes = decodeBase64(encodedText);
				ByteArrayInputStream inputStream = new ByteArrayInputStream(decodedBytes);
				while (inputStream.available() > 0) {
					consumer.accept((X509Certificate) factory.generateCertificate(inputStream));
				}
			}
		}
		catch (CertificateException | IOException ex) {
			throw new IllegalStateException("Error reading certificate from '" + path + "' : " + ex.getMessage(), ex);
		}
	}

	/**
     * Decodes a Base64 encoded string into a byte array.
     * 
     * @param content the Base64 encoded string to decode
     * @return the decoded byte array
     */
    private static byte[] decodeBase64(String content) {
		byte[] bytes = content.replaceAll("\r", "").replaceAll("\n", "").getBytes();
		return Base64.getDecoder().decode(bytes);
	}

}
