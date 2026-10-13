/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.tomcat;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.web.server.Compression;
import org.springframework.util.StringUtils;

/**
 * {@link TomcatConnectorCustomizer} that configures compression support on the given
 * Connector.
 *
 * @author Brian Clozel
 * @since 4.0.0
 */
public class CompressionConnectorCustomizer implements TomcatConnectorCustomizer {

	private final @Nullable Compression compression;

	public CompressionConnectorCustomizer(@Nullable Compression compression) {
		this.compression = compression;
	}

	@Override
	public void customize(Connector connector) {
		if (this.compression != null && this.compression.getEnabled()) {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractHttp11Protocol<?> abstractHttp11Protocol) {
				customize(abstractHttp11Protocol, this.compression);
			}
		}
	}

	private void customize(AbstractHttp11Protocol<?> protocol, Compression compression) {
		protocol.setCompression("on");
		protocol.setCompressionMinSize(getMinResponseSize(compression));
		protocol.setCompressibleMimeType(getMimeTypes(compression));
		if (compression.getExcludedUserAgents() != null) {
			protocol.setNoCompressionUserAgents(getExcludedUserAgents(compression));
		}
	}

	private int getMinResponseSize(Compression compression) {
		return (int) compression.getMinResponseSize().toBytes();
	}

	private String getMimeTypes(Compression compression) {
		return StringUtils.arrayToCommaDelimitedString(compression.getAllMimeTypes());
	}

	private String getExcludedUserAgents(Compression compression) {
		return StringUtils.arrayToCommaDelimitedString(compression.getExcludedUserAgents());
	}

}
