/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.web.embedded.tomcat;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;

import org.springframework.boot.web.server.Compression;
import org.springframework.util.StringUtils;

/**
 * {@link TomcatConnectorCustomizer} that configures compression support on the given
 * Connector.
 *
 * @author Brian Clozel
 */
class CompressionConnectorCustomizer implements TomcatConnectorCustomizer {

	private final Compression compression;

	/**
     * Constructs a new CompressionConnectorCustomizer with the specified Compression object.
     * 
     * @param compression the Compression object to be used for customization
     */
    CompressionConnectorCustomizer(Compression compression) {
		this.compression = compression;
	}

	/**
     * Customizes the given Connector by enabling compression if the compression is enabled.
     * 
     * @param connector the Connector to be customized
     */
    @Override
	public void customize(Connector connector) {
		if (this.compression != null && this.compression.getEnabled()) {
			ProtocolHandler handler = connector.getProtocolHandler();
			if (handler instanceof AbstractHttp11Protocol) {
				customize((AbstractHttp11Protocol<?>) handler);
			}
		}
	}

	/**
     * Customizes the given AbstractHttp11Protocol with compression settings.
     * 
     * @param protocol the AbstractHttp11Protocol to be customized
     */
    private void customize(AbstractHttp11Protocol<?> protocol) {
		Compression compression = this.compression;
		protocol.setCompression("on");
		protocol.setCompressionMinSize(getMinResponseSize(compression));
		protocol.setCompressibleMimeType(getMimeTypes(compression));
		if (this.compression.getExcludedUserAgents() != null) {
			protocol.setNoCompressionUserAgents(getExcludedUserAgents());
		}
	}

	/**
     * Returns the minimum response size in bytes for the given compression type.
     * 
     * @param compression the compression type
     * @return the minimum response size in bytes
     */
    private int getMinResponseSize(Compression compression) {
		return (int) compression.getMinResponseSize().toBytes();
	}

	/**
     * Returns a comma-delimited string of MIME types supported by the given compression.
     *
     * @param compression the compression object
     * @return a comma-delimited string of MIME types
     */
    private String getMimeTypes(Compression compression) {
		return StringUtils.arrayToCommaDelimitedString(compression.getMimeTypes());
	}

	/**
     * Returns a comma-delimited string of excluded user agents.
     * 
     * @return a comma-delimited string of excluded user agents
     */
    private String getExcludedUserAgents() {
		return StringUtils.arrayToCommaDelimitedString(this.compression.getExcludedUserAgents());
	}

}
