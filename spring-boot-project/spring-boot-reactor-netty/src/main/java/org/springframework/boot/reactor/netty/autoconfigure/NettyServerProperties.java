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

package org.springframework.boot.reactor.netty.autoconfigure;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Netty server properties.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Ivan Sopov
 * @author Marcos Barbero
 * @author Eddú Meléndez
 * @author Quinten De Swaef
 * @author Venil Noronha
 * @author Aurélien Leboulanger
 * @author Brian Clozel
 * @author Olivier Lamy
 * @author Chentao Qu
 * @author Artsiom Yudovin
 * @author Andrew McGhie
 * @author Rafiullah Hamedy
 * @author Dirk Deyne
 * @author HaiTao Zhang
 * @author Victor Mandujano
 * @author Chris Bono
 * @author Parviz Rozikov
 * @author Florian Storz
 * @author Michael Weidmann
 * @author Lasse Wulff
 * @since 4.0.0
 */
@ConfigurationProperties("server.netty")
public class NettyServerProperties {

	/**
	 * Connection timeout of the Netty channel.
	 */
	private Duration connectionTimeout;

	/**
	 * Maximum content length of an H2C upgrade request.
	 */
	private DataSize h2cMaxContentLength = DataSize.ofBytes(0);

	/**
	 * Initial buffer size for HTTP request decoding.
	 */
	private DataSize initialBufferSize = DataSize.ofBytes(128);

	/**
	 * Maximum length that can be decoded for an HTTP request's initial line.
	 */
	private DataSize maxInitialLineLength = DataSize.ofKilobytes(4);

	/**
	 * Maximum number of requests that can be made per connection. By default, a
	 * connection serves unlimited number of requests.
	 */
	private Integer maxKeepAliveRequests;

	/**
	 * Whether to validate headers when decoding requests.
	 */
	private boolean validateHeaders = true;

	/**
	 * Idle timeout of the Netty channel. When not specified, an infinite timeout is used.
	 */
	private Duration idleTimeout;

	public Duration getConnectionTimeout() {
		return this.connectionTimeout;
	}

	public void setConnectionTimeout(Duration connectionTimeout) {
		this.connectionTimeout = connectionTimeout;
	}

	public DataSize getH2cMaxContentLength() {
		return this.h2cMaxContentLength;
	}

	public void setH2cMaxContentLength(DataSize h2cMaxContentLength) {
		this.h2cMaxContentLength = h2cMaxContentLength;
	}

	public DataSize getInitialBufferSize() {
		return this.initialBufferSize;
	}

	public void setInitialBufferSize(DataSize initialBufferSize) {
		this.initialBufferSize = initialBufferSize;
	}

	public DataSize getMaxInitialLineLength() {
		return this.maxInitialLineLength;
	}

	public void setMaxInitialLineLength(DataSize maxInitialLineLength) {
		this.maxInitialLineLength = maxInitialLineLength;
	}

	public Integer getMaxKeepAliveRequests() {
		return this.maxKeepAliveRequests;
	}

	public void setMaxKeepAliveRequests(Integer maxKeepAliveRequests) {
		this.maxKeepAliveRequests = maxKeepAliveRequests;
	}

	public boolean isValidateHeaders() {
		return this.validateHeaders;
	}

	public void setValidateHeaders(boolean validateHeaders) {
		this.validateHeaders = validateHeaders;
	}

	public Duration getIdleTimeout() {
		return this.idleTimeout;
	}

	public void setIdleTimeout(Duration idleTimeout) {
		this.idleTimeout = idleTimeout;
	}

}
