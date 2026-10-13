/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webflux.test.autoconfigure;

/**
 * Example POJO used with {@link WebFluxTest @WebFluxTest} tests.
 *
 * @author Andy Wilkinson
 */
public class ExamplePojo {

	private final String alpha;

	private final String bravo;

	public ExamplePojo(String alpha, String bravo) {
		this.alpha = alpha;
		this.bravo = bravo;
	}

	public String getAlpha() {
		return this.alpha;
	}

	public String getBravo() {
		return this.bravo;
	}

}
