/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * An update event used to provide log updates.
 *
 * @author Phillip Webb
 * @since 2.3.0
 */
public class LogUpdateEvent extends UpdateEvent {

	private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[;\\d]*m");

	private static final Pattern TRAILING_NEW_LINE_PATTERN = Pattern.compile("\\n$");

	private final StreamType streamType;

	private final byte[] payload;

	private final String string;

	/**
	 * Logs an update event with the specified stream type and payload.
	 * @param streamType the type of the stream
	 * @param payload the payload data as a byte array
	 */
	LogUpdateEvent(StreamType streamType, byte[] payload) {
		this.streamType = streamType;
		this.payload = payload;
		String string = new String(payload, StandardCharsets.UTF_8);
		string = ANSI_PATTERN.matcher(string).replaceAll("");
		string = TRAILING_NEW_LINE_PATTERN.matcher(string).replaceAll("");
		this.string = string;
	}

	/**
	 * Prints the LogUpdateEvent object based on the stream type. If the stream type is
	 * STD_OUT, the object is printed to the standard output stream. If the stream type is
	 * STD_ERR, the object is printed to the standard error stream.
	 */
	public void print() {
		switch (this.streamType) {
			case STD_OUT -> System.out.println(this);
			case STD_ERR -> System.err.println(this);
		}
	}

	/**
	 * Returns the stream type of the LogUpdateEvent.
	 * @return the stream type of the LogUpdateEvent
	 */
	public StreamType getStreamType() {
		return this.streamType;
	}

	/**
	 * Returns the payload of the LogUpdateEvent.
	 * @return the payload as a byte array
	 */
	public byte[] getPayload() {
		return this.payload;
	}

	/**
	 * Returns a string representation of the LogUpdateEvent object.
	 * @return the string representation of the LogUpdateEvent object
	 */
	@Override
	public String toString() {
		return this.string;
	}

	/**
	 * Reads all log update events from the given input stream and passes them to the
	 * consumer.
	 * @param inputStream the input stream to read the log update events from
	 * @param consumer the consumer to accept the log update events
	 * @throws IOException if an I/O error occurs while reading the input stream
	 */
	static void readAll(InputStream inputStream, Consumer<LogUpdateEvent> consumer) throws IOException {
		try {
			LogUpdateEvent event;
			while ((event = LogUpdateEvent.read(inputStream)) != null) {
				consumer.accept(event);
			}
		}
		catch (IllegalStateException ex) {
			byte[] message = ex.getMessage().getBytes(StandardCharsets.UTF_8);
			consumer.accept(new LogUpdateEvent(StreamType.STD_ERR, message));
			StreamUtils.drain(inputStream);
		}
		finally {
			inputStream.close();
		}
	}

	/**
	 * Reads the LogUpdateEvent from the given InputStream.
	 * @param inputStream the InputStream to read from
	 * @return the LogUpdateEvent read from the InputStream, or null if the header is null
	 * @throws IOException if an I/O error occurs while reading from the InputStream
	 */
	private static LogUpdateEvent read(InputStream inputStream) throws IOException {
		byte[] header = read(inputStream, 8);
		if (header == null) {
			return null;
		}
		StreamType streamType = StreamType.forId(header[0]);
		long size = 0;
		for (int i = 0; i < 4; i++) {
			size = (size << 8) + (header[i + 4] & 0xff);
		}
		byte[] payload = read(inputStream, size);
		return new LogUpdateEvent(streamType, payload);
	}

	/**
	 * Reads data from an InputStream and returns it as a byte array.
	 * @param inputStream the InputStream to read from
	 * @param size the size of the data to read
	 * @return the byte array containing the read data, or null if the end of the stream
	 * is reached
	 * @throws IOException if an I/O error occurs while reading from the stream
	 */
	private static byte[] read(InputStream inputStream, long size) throws IOException {
		byte[] data = new byte[(int) size];
		int offset = 0;
		do {
			int amountRead = inputStream.read(data, offset, data.length - offset);
			if (amountRead == -1) {
				return null;
			}
			offset += amountRead;
		}
		while (offset < data.length);
		return data;
	}

	/**
	 * Stream types supported by the event.
	 */
	public enum StreamType {

		/**
		 * Input from {@code stdin}.
		 */
		STD_IN,

		/**
		 * Output to {@code stdout}.
		 */
		STD_OUT,

		/**
		 * Output to {@code stderr}.
		 */
		STD_ERR;

		/**
		 * Returns the StreamType corresponding to the given id.
		 * @param id the id of the StreamType
		 * @return the StreamType corresponding to the given id
		 * @throws IllegalArgumentException if the id is out of bounds
		 */
		static StreamType forId(byte id) {
			int upperBound = values().length;
			Assert.state(id > 0 && id < upperBound,
					() -> "Stream type is out of bounds. Must be >= 0 and < " + upperBound + ", but was " + id);
			return values()[id];
		}

	}

}
