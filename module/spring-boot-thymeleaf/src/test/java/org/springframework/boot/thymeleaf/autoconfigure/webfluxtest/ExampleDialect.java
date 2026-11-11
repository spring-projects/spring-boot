/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.thymeleaf.autoconfigure.webfluxtest;

import org.thymeleaf.dialect.IDialect;

import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.stereotype.Component;

/**
 * {@link IDialect} used to test their inclusion in {@link WebFluxTest @WebFluxTest}s.
 *
 * @author Andy Wilkinson
 */
@Component
class ExampleDialect implements IDialect {

	@Override
	public String getName() {
		return "example";
	}

}
