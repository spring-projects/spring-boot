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

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.unit.DataSize;
import org.springframework.web.client.RestTemplate;

/**
 * A Zipkin {@link Sender} which uses {@link RestTemplate} for HTTP communication.
 * Supports automatic compression with gzip.
 *
 * @author Moritz Halbritter
 */
class ZipkinRestTemplateSender extends Sender {

	private static final DataSize MESSAGE_MAX_BYTES = DataSize.ofKilobytes(512);

	private final String endpoint;

	private final RestTemplate restTemplate;

	private volatile boolean closed;

	ZipkinRestTemplateSender(String endpoint, RestTemplate restTemplate) {
		this.endpoint = endpoint;
		this.restTemplate = restTemplate;
	}

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
	public Call<Void> sendSpans(List<byte[]> encodedSpans) {
		if (this.closed) {
			throw new ClosedSenderException();
		}
		return new HttpCall(this.endpoint, BytesMessageEncoder.JSON.encode(encodedSpans), this.restTemplate);
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

	private static class HttpCall extends Call.Base<Void> {

		/**
		 * Only use gzip compression on data which is bigger than this in bytes.
		 */
		private static final DataSize COMPRESSION_THRESHOLD = DataSize.ofKilobytes(1);

		private final String endpoint;

		private final byte[] body;

		private final RestTemplate restTemplate;

		HttpCall(String endpoint, byte[] body, RestTemplate restTemplate) {
			this.endpoint = endpoint;
			this.body = body;
			this.restTemplate = restTemplate;
		}

		@Override
		protected Void doExecute() throws IOException {
			HttpHeaders headers = new HttpHeaders();
			headers.set("b3", "0");
			headers.set("Content-Type", "application/json");
			byte[] body;
			if (needsCompression(this.body)) {
				headers.set("Content-Encoding", "gzip");
				body = compress(this.body);
			}
			else {
				body = this.body;
			}
			HttpEntity<byte[]> request = new HttpEntity<>(body, headers);
			this.restTemplate.exchange(this.endpoint, HttpMethod.POST, request, Void.class);
			return null;
		}

		private boolean needsCompression(byte[] body) {
			return body.length > COMPRESSION_THRESHOLD.toBytes();
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

		@Override
		public Call<Void> clone() {
			return new HttpCall(this.endpoint, this.body, this.restTemplate);
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
