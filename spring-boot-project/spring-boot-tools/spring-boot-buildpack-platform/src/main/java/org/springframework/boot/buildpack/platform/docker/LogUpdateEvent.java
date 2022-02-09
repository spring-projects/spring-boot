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

	LogUpdateEvent(StreamType streamType, byte[] payload) {
		this.streamType = streamType;
		this.payload = payload;
		String string = new String(payload, StandardCharsets.UTF_8);
		string = ANSI_PATTERN.matcher(string).replaceAll("");
		string = TRAILING_NEW_LINE_PATTERN.matcher(string).replaceAll("");
		this.string = string;
	}

	public void print() {
		switch (this.streamType) {
		case STD_OUT:
			System.out.println(this);
			return;
		case STD_ERR:
			System.err.println(this);
			return;
		}
	}

	public StreamType getStreamType() {
		return this.streamType;
	}

	public byte[] getPayload() {
		return this.payload;
	}

	@Override
	public String toString() {
		return this.string;
	}

	static void readAll(InputStream inputStream, Consumer<LogUpdateEvent> consumer) throws IOException {
		try {
			LogUpdateEvent event;
			do {
				try {
					event = LogUpdateEvent.read(inputStream);
					if (event != null) {
						consumer.accept(event);
					}
				}
				catch (IllegalStateException e) {
					// If parsing the event failed, convert exception into a stdErr event
					event = new LogUpdateEvent(StreamType.STD_ERR, e.getMessage().getBytes(StandardCharsets.UTF_8));
					consumer.accept(event);
				}
			}
			while (event != null);
		}
		finally {
			inputStream.close();
		}
	}

	private static LogUpdateEvent read(InputStream inputStream) throws IOException {
		byte[] header = read(inputStream, 8);
		if (header == null) {
			return null;
		}

		// First byte denotes stream type. 0 = stdin, 1 = stdout, 2 = stderr
		byte streamTypeId = header[0];
		boolean illegalStreamType = false;
		if (streamTypeId < 0 || streamTypeId >= StreamType.values().length) {
			// Don't throw exception right here, otherwise the stream is not advanced
			// correctly, and subsequent reads start from a wrong position
			illegalStreamType = true;
		}

		long size = 0;
		for (int i = 0; i < 4; i++) {
			size = (size << 8) + (header[i + 4] & 0xff);
		}
		byte[] payload = read(inputStream, size);

		if (illegalStreamType) {
			// At this point the stream has been advanved correctly and it is safe to
			// throw exceptions
			throw new IllegalStateException("Stream type is out of bounds. Must be >= 0 and < "
					+ StreamType.values().length + ", but was " + streamTypeId);
		}

		StreamType streamType = StreamType.values()[streamTypeId];
		return new LogUpdateEvent(streamType, payload);
	}

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
		STD_ERR

	}

}
