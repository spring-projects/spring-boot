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
import java.util.List;
import java.util.zip.GZIPOutputStream;

import zipkin2.Call;
import zipkin2.Callback;
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

	private static final DataSize MESSAGE_MAX_BYTES = DataSize.ofKilobytes(512);

	volatile boolean closed;

	@Override
	public Encoding encoding() {
		return Encoding.JSON;
	}

	@Override
	public int messageMaxBytes() {
		return (int) MESSAGE_MAX_BYTES.toBytes();
	}

	@Override
	public int messageSizeInBytes(List<byte[]> encodedSpans) {
		return encoding().listSizeInBytes(encodedSpans);
	}

	@Override
	public int messageSizeInBytes(int encodedSizeInBytes) {
		return encoding().listSizeInBytes(encodedSizeInBytes);
	}

	@Override
	public CheckResult check() {
		try {
			sendSpans(List.of()).execute();
			return CheckResult.OK;
		}
		catch (IOException | RuntimeException ex) {
			return CheckResult.failed(ex);
		}
	}

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

	@Override
	public Call<Void> sendSpans(List<byte[]> encodedSpans) {
		if (this.closed) {
			throw new ClosedSenderException();
		}
		return sendSpans(BytesMessageEncoder.JSON.encode(encodedSpans));
	}

	abstract static class HttpPostCall extends Call.Base<Void> {

		byte[] body;

		protected HttpPostCall(byte[] body) {
			this.body = body;
		}

		/**
		 * Only use gzip compression on data which is bigger than this in bytes.
		 */
		private static final DataSize COMPRESSION_THRESHOLD = DataSize.ofKilobytes(1);

		/**
		 * Implementations should send a POST request.
		 * @param body the body to send as bytes
		 * @param defaultHeaders the headers for the POST request. They can be further
		 * modified if necessary.
		 */
		abstract void post(byte[] body, HttpHeaders defaultHeaders);

		@Override
		protected Void doExecute() throws IOException {
			HttpHeaders headers = new HttpHeaders();
			headers.set("b3", "0");
			headers.set("Content-Type", "application/json");
			byte[] body;
			if (needsCompression()) {
				headers.set("Content-Encoding", "gzip");
				body = compress(this.body);
			}
			else {
				body = this.body;
			}
			post(body, headers);
			return null;
		}

		@Override
		protected void doEnqueue(Callback<Void> callback) {
			try {
				doExecute();
				callback.onSuccess(null);
			}
			catch (IOException | RuntimeException ex) {
				callback.onError(ex);
			}
		}

		private boolean needsCompression() {
			return this.body.length > COMPRESSION_THRESHOLD.toBytes();
		}

		private byte[] compress(byte[] input) throws IOException {
			ByteArrayOutputStream result = new ByteArrayOutputStream();
			try (GZIPOutputStream gzip = new GZIPOutputStream(result)) {
				gzip.write(input);
			}
			return result.toByteArray();
		}

	}

}
