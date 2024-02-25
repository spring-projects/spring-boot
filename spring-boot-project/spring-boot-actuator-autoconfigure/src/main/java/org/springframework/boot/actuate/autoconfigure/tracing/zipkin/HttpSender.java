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

package org.springframework.boot.actuate.autoconfigure.tracing.zipkin;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import zipkin2.Call;
import zipkin2.CheckResult;
import zipkin2.codec.Encoding;
import zipkin2.reporter.BytesMessageEncoder;
import zipkin2.reporter.ClosedSenderException;
import zipkin2.reporter.Sender;

import org.springframework.http.HttpHeaders;
import org.springframework.util.unit.DataSize;

/**
 * A Zipkin {@link Sender} that uses an HTTP client to send JSON spans. Supports automatic
 * compression with gzip.
 *
 * @author Moritz Halbritter
 * @author Stefan Bratanov
 */
abstract class HttpSender extends Sender {

	private static final DataSize MESSAGE_MAX_SIZE = DataSize.ofKilobytes(512);

	private volatile boolean closed;

	/**
	 * Returns the encoding type used by the HttpSender.
	 * @return The encoding type, which is JSON.
	 */
	@Override
	public Encoding encoding() {
		return Encoding.JSON;
	}

	/**
	 * Returns the maximum number of bytes allowed for a message.
	 * @return the maximum number of bytes allowed for a message
	 */
	@Override
	public int messageMaxBytes() {
		return (int) MESSAGE_MAX_SIZE.toBytes();
	}

	/**
	 * Calculates the size of the message in bytes based on the encoded spans.
	 * @param encodedSpans the list of encoded spans
	 * @return the size of the message in bytes
	 */
	@Override
	public int messageSizeInBytes(List<byte[]> encodedSpans) {
		return encoding().listSizeInBytes(encodedSpans);
	}

	/**
	 * Calculates the size of the message in bytes based on the encoded size in bytes.
	 * @param encodedSizeInBytes the size of the message after encoding in bytes
	 * @return the size of the message in bytes
	 */
	@Override
	public int messageSizeInBytes(int encodedSizeInBytes) {
		return encoding().listSizeInBytes(encodedSizeInBytes);
	}

	/**
	 * This method is used to check the status of the HttpSender. It sends an empty list
	 * of spans and returns the result of the check.
	 * @return CheckResult - The result of the check. If the check is successful, it
	 * returns CheckResult.OK. If an IOException or RuntimeException occurs during the
	 * check, it returns a failed CheckResult with the exception.
	 */
	@Override
	public CheckResult check() {
		try {
			sendSpans(Collections.emptyList()).execute();
			return CheckResult.OK;
		}
		catch (IOException | RuntimeException ex) {
			return CheckResult.failed(ex);
		}
	}

	/**
	 * Closes the HttpSender.
	 * @throws IOException if an I/O error occurs while closing the HttpSender.
	 */
	@Override
	public void close() throws IOException {
		this.closed = true;
	}

	/**
	 * The returned {@link HttpPostCall} will send span(s) as a POST to a zipkin endpoint
	 * when executed.
	 * @param batchedEncodedSpans list of encoded spans as a byte array
	 * @return an instance of a Zipkin {@link Call} which can be executed
	 */
	protected abstract HttpPostCall sendSpans(byte[] batchedEncodedSpans);

	/**
	 * Sends a list of encoded spans.
	 * @param encodedSpans the list of encoded spans to be sent
	 * @return a Call object representing the asynchronous request
	 * @throws ClosedSenderException if the sender is closed
	 */
	@Override
	public Call<Void> sendSpans(List<byte[]> encodedSpans) {
		if (this.closed) {
			throw new ClosedSenderException();
		}
		return sendSpans(BytesMessageEncoder.JSON.encode(encodedSpans));
	}

	/**
	 * HttpPostCall class.
	 */
	abstract static class HttpPostCall extends Call.Base<Void> {

		/**
		 * Only use gzip compression on data which is bigger than this in bytes.
		 */
		private static final DataSize COMPRESSION_THRESHOLD = DataSize.ofKilobytes(1);

		private final byte[] body;

		/**
		 * Sends an HTTP POST request with the specified body.
		 * @param body the body of the request as a byte array
		 */
		HttpPostCall(byte[] body) {
			this.body = body;
		}

		/**
		 * Returns the body of the HTTP POST call.
		 * @return the body of the HTTP POST call as a byte array
		 */
		protected byte[] getBody() {
			if (needsCompression()) {
				return compress(this.body);
			}
			return this.body;
		}

		/**
		 * Returns the uncompressed body of the HTTP POST call.
		 * @return the uncompressed body as a byte array
		 */
		protected byte[] getUncompressedBody() {
			return this.body;
		}

		/**
		 * Returns the default HttpHeaders for the HttpPostCall class.
		 * @return the default HttpHeaders
		 */
		protected HttpHeaders getDefaultHeaders() {
			HttpHeaders headers = new HttpHeaders();
			headers.set("b3", "0");
			headers.set("Content-Type", "application/json");
			if (needsCompression()) {
				headers.set("Content-Encoding", "gzip");
			}
			return headers;
		}

		/**
		 * Checks if the body of the HTTP post call needs compression.
		 * @return true if the body length is greater than the compression threshold,
		 * false otherwise.
		 */
		private boolean needsCompression() {
			return this.body.length > COMPRESSION_THRESHOLD.toBytes();
		}

		/**
		 * Compresses the given byte array using GZIP compression.
		 * @param input the byte array to be compressed
		 * @return the compressed byte array
		 * @throws UncheckedIOException if an I/O error occurs during compression
		 */
		private byte[] compress(byte[] input) {
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			try (GZIPOutputStream gzip = new GZIPOutputStream(result)) {
				gzip.write(input);
			}
			catch (IOException ex) {
				throw new UncheckedIOException(ex);
			}
			return result.toByteArray();
		}

	}

}
