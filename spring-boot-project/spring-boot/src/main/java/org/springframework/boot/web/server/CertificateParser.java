/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.web.server;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Base64Utils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.ResourceUtils;

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

	private CertificateParser() {
	}

	/**
	 * Load certificates from the specified resource.
	 * @param path the certificate to parse
	 * @return the parsed certificates
	 */
	static X509Certificate[] parse(String path) {
		CertificateFactory factory = getCertificateFactory();
		List<X509Certificate> certificates = new ArrayList<>();
		readCertificates(path, factory, certificates::add);
		return certificates.toArray(new X509Certificate[0]);
	}

	private static CertificateFactory getCertificateFactory() {
		try {
			return CertificateFactory.getInstance("X.509");
		}
		catch (CertificateException ex) {
			throw new IllegalStateException("Unable to get X.509 certificate factory", ex);
		}
	}

	private static void readCertificates(String resource, CertificateFactory factory,
			Consumer<X509Certificate> consumer) {
		try {
			String text = readText(resource);
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
			throw new IllegalStateException("Error reading certificate from '" + resource + "' : " + ex.getMessage(),
					ex);
		}
	}

	private static String readText(String resource) throws IOException {
		URL url = ResourceUtils.getURL(resource);
		try (Reader reader = new InputStreamReader(url.openStream())) {
			return FileCopyUtils.copyToString(reader);
		}
	}

	private static byte[] decodeBase64(String content) {
		byte[] bytes = content.replaceAll("\r", "").replaceAll("\n", "").getBytes();
		return Base64Utils.decode(bytes);
	}

}
