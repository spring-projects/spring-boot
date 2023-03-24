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

package org.springframework.boot.devtools.livereload;

import java.io.IOException;
import java.io.OutputStream;

import org.springframework.util.Assert;

/**
 * A limited implementation of a WebSocket Frame used to carry LiveReload data.
 *
 * @author Phillip Webb
 */
class Frame {

	private static final byte[] NO_BYTES = new byte[0];

	private final Type type;

	private final byte[] payload;

	/**
	 * Create a new {@link Type#TEXT text} {@link Frame} instance with the specified
	 * payload.
	 * @param payload the text payload
	 */
	Frame(String payload) {
		Assert.notNull(payload, "Payload must not be null");
		this.type = Type.TEXT;
		this.payload = payload.getBytes();
	}

	Frame(Type type) {
		Assert.notNull(type, "Type must not be null");
		this.type = type;
		this.payload = NO_BYTES;
	}

	Frame(Type type, byte[] payload) {
		this.type = type;
		this.payload = payload;
	}

	Type getType() {
		return this.type;
	}

	byte[] getPayload() {
		return this.payload;
	}

	@Override
	public String toString() {
		return new String(this.payload);
	}

	void write(OutputStream outputStream) throws IOException {
		outputStream.write(0x80 | this.type.code);
		if (this.payload.length < 126) {
			outputStream.write(this.payload.length & 0x7F);
		}
		else {
			outputStream.write(0x7E);
			outputStream.write(this.payload.length >> 8 & 0xFF);
			outputStream.write(this.payload.length & 0xFF);
		}
		outputStream.write(this.payload);
		outputStream.flush();
	}

	static Frame read(ConnectionInputStream inputStream) throws IOException {
		int firstByte = inputStream.checkedRead();
		Assert.state((firstByte & 0x80) != 0, "Fragmented frames are not supported");
		int maskAndLength = inputStream.checkedRead();
		boolean hasMask = (maskAndLength & 0x80) != 0;
		int length = (maskAndLength & 0x7F);
		Assert.state(length != 127, "Large frames are not supported");
		if (length == 126) {
			length = ((inputStream.checkedRead()) << 8 | inputStream.checkedRead());
		}
		byte[] mask = new byte[4];
		if (hasMask) {
			inputStream.readFully(mask, 0, mask.length);
		}
		byte[] payload = new byte[length];
		inputStream.readFully(payload, 0, length);
		if (hasMask) {
			for (int i = 0; i < payload.length; i++) {
				payload[i] ^= mask[i % 4];
			}
		}
		return new Frame(Type.forCode(firstByte & 0x0F), payload);
	}

	/**
	 * Frame types.
	 */
	enum Type {

		/**
		 * Continuation frame.
		 */
		CONTINUATION(0x00),

		/**
		 * Text frame.
		 */
		TEXT(0x01),

		/**
		 * Binary frame.
		 */
		BINARY(0x02),

		/**
		 * Close frame.
		 */
		CLOSE(0x08),

		/**
		 * Ping frame.
		 */
		PING(0x09),

		/**
		 * Pong frame.
		 */
		PONG(0x0A);

		private final int code;

		Type(int code) {
			this.code = code;
		}

		static Type forCode(int code) {
			for (Type type : values()) {
				if (type.code == code) {
					return type;
				}
			}
			throw new IllegalStateException("Unknown code " + code);
		}

	}

}
