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

	/**
	 * Constructs a new Frame object with the specified type.
	 * @param type the type of the frame (must not be null)
	 * @throws IllegalArgumentException if the type is null
	 */
	Frame(Type type) {
		Assert.notNull(type, "Type must not be null");
		this.type = type;
		this.payload = NO_BYTES;
	}

	/**
	 * Constructs a new Frame object with the specified type and payload.
	 * @param type the type of the frame
	 * @param payload the payload data of the frame
	 */
	Frame(Type type, byte[] payload) {
		this.type = type;
		this.payload = payload;
	}

	/**
	 * Returns the type of the Frame.
	 * @return the type of the Frame
	 */
	Type getType() {
		return this.type;
	}

	/**
	 * Returns the payload of the Frame as a byte array.
	 * @return the payload of the Frame as a byte array
	 */
	byte[] getPayload() {
		return this.payload;
	}

	/**
	 * Returns a string representation of the Frame object.
	 * @return a string representation of the Frame object
	 */
	@Override
	public String toString() {
		return new String(this.payload);
	}

	/**
	 * Writes the frame to the specified output stream.
	 * @param outputStream the output stream to write the frame to
	 * @throws IOException if an I/O error occurs while writing the frame
	 */
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

	/**
	 * Reads a frame from the given input stream.
	 * @param inputStream the input stream to read from
	 * @return the frame read from the input stream
	 * @throws IOException if an I/O error occurs while reading from the input stream
	 * @throws IllegalStateException if fragmented frames are encountered or if large
	 * frames are encountered
	 */
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

		/**
		 * Sets the code of the Frame.
		 * @param code the code to set
		 */
		Type(int code) {
			this.code = code;
		}

		/**
		 * Returns the Type associated with the given code.
		 * @param code the code to find the Type for
		 * @return the Type associated with the given code
		 * @throws IllegalStateException if the code does not match any Type
		 */
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
