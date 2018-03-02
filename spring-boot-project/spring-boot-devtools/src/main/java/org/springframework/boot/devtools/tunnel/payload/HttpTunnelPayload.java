/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.devtools.tunnel.payload;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Encapsulates a payload data sent via a HTTP tunnel.
 *
 * @author Phillip Webb
 * @since 1.3.0
 */
public class HttpTunnelPayload {

	private static final String SEQ_HEADER = "x-seq";

	private static final int BUFFER_SIZE = 1024 * 100;

	protected static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

	private static final Log logger = LogFactory.getLog(HttpTunnelPayload.class);

	private final long sequence;

	private final ByteBuffer data;

	/**
	 * Create a new {@link HttpTunnelPayload} instance.
	 * @param sequence the sequence number of the payload
	 * @param data the payload data
	 */
	public HttpTunnelPayload(long sequence, ByteBuffer data) {
		Assert.isTrue(sequence > 0, "Sequence must be positive");
		Assert.notNull(data, "Data must not be null");
		this.sequence = sequence;
		this.data = data;
	}

	/**
	 * Return the sequence number of the payload.
	 * @return the sequence
	 */
	public long getSequence() {
		return this.sequence;
	}

	/**
	 * Assign this payload to the given {@link HttpOutputMessage}.
	 * @param message the message to assign this payload to
	 * @throws IOException in case of I/O errors
	 */
	public void assignTo(HttpOutputMessage message) throws IOException {
		Assert.notNull(message, "Message must not be null");
		HttpHeaders headers = message.getHeaders();
		headers.setContentLength(this.data.remaining());
		headers.add(SEQ_HEADER, Long.toString(getSequence()));
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		WritableByteChannel body = Channels.newChannel(message.getBody());
		while (this.data.hasRemaining()) {
			body.write(this.data);
		}
		body.close();
	}

	/**
	 * Write the content of this payload to the given target channel.
	 * @param channel the channel to write to
	 * @throws IOException in case of I/O errors
	 */
	public void writeTo(WritableByteChannel channel) throws IOException {
		Assert.notNull(channel, "Channel must not be null");
		while (this.data.hasRemaining()) {
			channel.write(this.data);
		}
	}

	/**
	 * Return the {@link HttpTunnelPayload} for the given message or {@code null} if there
	 * is no payload.
	 * @param message the HTTP message
	 * @return the payload or {@code null}
	 * @throws IOException in case of I/O errors
	 */
	public static HttpTunnelPayload get(HttpInputMessage message) throws IOException {
		long length = message.getHeaders().getContentLength();
		if (length <= 0) {
			return null;
		}
		String seqHeader = message.getHeaders().getFirst(SEQ_HEADER);
		Assert.state(StringUtils.hasLength(seqHeader), "Missing sequence header");
		ReadableByteChannel body = Channels.newChannel(message.getBody());
		ByteBuffer payload = ByteBuffer.allocate((int) length);
		while (payload.hasRemaining()) {
			body.read(payload);
		}
		body.close();
		payload.flip();
		return new HttpTunnelPayload(Long.valueOf(seqHeader), payload);
	}

	/**
	 * Return the payload data for the given source {@link ReadableByteChannel} or null if
	 * the channel timed out whilst reading.
	 * @param channel the source channel
	 * @return payload data or {@code null}
	 * @throws IOException in case of I/O errors
	 */
	public static ByteBuffer getPayloadData(ReadableByteChannel channel)
			throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		try {
			int amountRead = channel.read(buffer);
			Assert.state(amountRead != -1, "Target server connection closed");
			buffer.flip();
			return buffer;
		}
		catch (InterruptedIOException ex) {
			return null;
		}
	}

	/**
	 * Log incoming payload information at trace level to aid diagnostics.
	 */
	public void logIncoming() {
		log("< ");
	}

	/**
	 * Log incoming payload information at trace level to aid diagnostics.
	 */
	public void logOutgoing() {
		log("> ");
	}

	private void log(String prefix) {
		if (logger.isTraceEnabled()) {
			logger.trace(prefix + toHexString());
		}
	}

	/**
	 * Return the payload as a hexadecimal string.
	 * @return the payload as a hex string
	 */
	public String toHexString() {
		byte[] bytes = this.data.array();
		char[] hex = new char[this.data.remaining() * 2];
		for (int i = this.data.position(); i < this.data.remaining(); i++) {
			int b = bytes[i] & 0xFF;
			hex[i * 2] = HEX_CHARS[b >>> 4];
			hex[i * 2 + 1] = HEX_CHARS[b & 0x0F];
		}
		return new String(hex);
	}

}
