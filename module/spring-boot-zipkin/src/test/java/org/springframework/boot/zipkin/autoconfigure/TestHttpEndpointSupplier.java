/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.zipkin.autoconfigure;

import java.util.concurrent.atomic.AtomicInteger;

import zipkin2.reporter.HttpEndpointSupplier;

/**
 * Test {@link HttpEndpointSupplier}.
 *
 * @author Moritz Halbritter
 */
class TestHttpEndpointSupplier implements HttpEndpointSupplier {

	private final String url;

	private final AtomicInteger suffix = new AtomicInteger();

	TestHttpEndpointSupplier(String url) {
		this.url = url;
	}

	@Override
	public String get() {
		return this.url + "/" + this.suffix.incrementAndGet();
	}

	@Override
	public void close() {
	}

}
