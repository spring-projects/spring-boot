/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.apache.coyote.http2.Http2Protocol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.web.server.Compression;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CompressionConnectorCustomizer}
 *
 * @author Rudy Adams
 */
class CompressionConnectorCustomizerTests {

	private static final int MIN_SIZE = 100;

	private final String[] mimeTypes = { "text/html", "text/xml", "text/xhtml" };

	private final String[] excludedUserAgents = { "SomeUserAgent", "AnotherUserAgent" };

	private Compression compression;

	@BeforeEach
	void setup() {
		this.compression = new Compression();
		this.compression.setEnabled(true);
		this.compression.setMinResponseSize(DataSize.ofBytes(MIN_SIZE));
		this.compression.setMimeTypes(this.mimeTypes);
		this.compression.setExcludedUserAgents(this.excludedUserAgents);
	}

	@Test
	void shouldCustomizeCompression() {
		CompressionConnectorCustomizer compressionConnectorCustomizer = new CompressionConnectorCustomizer(
				this.compression);
		Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
		Http2Protocol upgradeProtocol = new Http2Protocol();
		upgradeProtocol.setHttp11Protocol((AbstractHttp11Protocol<?>) connector.getProtocolHandler());
		connector.addUpgradeProtocol(upgradeProtocol);
		compressionConnectorCustomizer.customize(connector);
		AbstractHttp11Protocol<?> abstractHttp11Protocol = (AbstractHttp11Protocol<?>) connector.getProtocolHandler();
		compressionOn(abstractHttp11Protocol.getCompression());
		minSize(abstractHttp11Protocol.getCompressionMinSize());
		mimeType(abstractHttp11Protocol.getCompressibleMimeTypes());
		excludedUserAgents(abstractHttp11Protocol.getNoCompressionUserAgents());
	}

	private void compressionOn(String compression) {
		assertThat(compression).isEqualTo("on");
	}

	private void minSize(int minSize) {
		assertThat(minSize).isEqualTo(MIN_SIZE);
	}

	private void mimeType(String[] mimeTypes) {
		assertThat(mimeTypes).isEqualTo(this.mimeTypes);
	}

	private void excludedUserAgents(String combinedUserAgents) {
		assertThat(combinedUserAgents).isEqualTo("SomeUserAgent,AnotherUserAgent");
	}

}
