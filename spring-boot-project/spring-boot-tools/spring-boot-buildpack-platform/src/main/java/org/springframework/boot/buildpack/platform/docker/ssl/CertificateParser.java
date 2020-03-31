/*
 * Copyright 2012-2020 the original author or authors.
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Base64Utils;

/**
 * Parser for X.509 certificates in PEM format.
 *
 * @author Scott Frederick
 */
final class CertificateParser {

	private static final Pattern CERTIFICATE_PATTERN = Pattern
			.compile("-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+" + // Header
					"([a-z0-9+/=\\r\\n]+)" + // Base64 text
					"-+END\\s+.*CERTIFICATE[^-]*-+", // Footer
					Pattern.CASE_INSENSITIVE);

	private CertificateParser() {
	}

	/**
	 * Load certificates from the specified file paths.
	 * @param certPaths one or more paths to certificate files
	 * @return certificates parsed from specified file paths
	 */
	static X509Certificate[] parse(Path... certPaths) {
		List<X509Certificate> certs = new ArrayList<>();
		for (Path certFile : certPaths) {
			certs.addAll(generateCertificates(certFile));
		}
		return certs.toArray(new X509Certificate[0]);
	}

	private static List<X509Certificate> generateCertificates(Path certPath) {
		try {
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			List<X509Certificate> certs = new ArrayList<>();

			byte[] certBytes = Files.readAllBytes(certPath);
			String certString = new String(certBytes, StandardCharsets.UTF_8);

			Matcher matcher = CERTIFICATE_PATTERN.matcher(certString);

			while (matcher.find()) {
				byte[] content = decodeContent(matcher.group(1));
				ByteArrayInputStream contentStream = new ByteArrayInputStream(content);
				while (contentStream.available() > 0) {
					certs.add((X509Certificate) certificateFactory.generateCertificate(contentStream));
				}
			}

			return certs;
		}
		catch (CertificateException | IOException ex) {
			throw new IllegalStateException("Error reading certificate from file " + certPath + ": " + ex.getMessage(),
					ex);
		}
	}

	private static byte[] decodeContent(String content) {
		byte[] contentBytes = content.replaceAll("\r", "").replaceAll("\n", "").getBytes();
		return Base64Utils.decode(contentBytes);
	}

}
