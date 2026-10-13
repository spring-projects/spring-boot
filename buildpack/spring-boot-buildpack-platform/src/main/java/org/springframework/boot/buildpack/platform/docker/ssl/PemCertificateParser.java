/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.docker.ssl;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import org.springframework.lang.Contract;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Parser for X.509 certificates in PEM format.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 */
final class PemCertificateParser {

	private static final String HEADER = "-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+";

	private static final String BASE64_TEXT = "([a-z0-9+/=\\r\\n]+)";

	private static final String FOOTER = "-+END\\s+.*CERTIFICATE[^-]*-+";

	private static final Pattern PATTERN = Pattern.compile(HEADER + BASE64_TEXT + FOOTER, Pattern.CASE_INSENSITIVE);

	private PemCertificateParser() {
	}

	/**
	 * Parse certificates from the specified string.
	 * @param text the text to parse
	 * @return the parsed certificates
	 */
	@Contract("!null -> !null")
	static @Nullable List<X509Certificate> parse(@Nullable String text) {
		if (text == null) {
			return null;
		}
		CertificateFactory factory = getCertificateFactory();
		List<X509Certificate> certs = new ArrayList<>();
		readCertificates(text, factory, certs::add);
		Assert.state(!CollectionUtils.isEmpty(certs), "Missing certificates or unrecognized format");
		return List.copyOf(certs);
	}

	private static CertificateFactory getCertificateFactory() {
		try {
			return CertificateFactory.getInstance("X.509");
		}
		catch (CertificateException ex) {
			throw new IllegalStateException("Unable to get X.509 certificate factory", ex);
		}
	}

	private static void readCertificates(String text, CertificateFactory factory, Consumer<X509Certificate> consumer) {
		try {
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
		catch (CertificateException ex) {
			throw new IllegalStateException("Error reading certificate: " + ex.getMessage(), ex);
		}
	}

	private static byte[] decodeBase64(String content) {
		byte[] bytes = content.replace("\r", "").replace("\n", "").getBytes();
		return Base64.getDecoder().decode(bytes);
	}

}
