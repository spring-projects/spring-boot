/*
 * Copyright 2012-present the original author or authors.
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
