/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.tracing.brave.autoconfigure.zipkin;

import java.io.IOException;
import java.util.List;

import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.Encoding;

class NoopSender extends BytesMessageSender.Base {

	NoopSender(Encoding encoding) {
		super(encoding);
	}

	@Override
	public int messageMaxBytes() {
		return 1024;
	}

	@Override
	public void send(List<byte[]> encodedSpans) {
	}

	@Override
	public void close() throws IOException {
	}

}
