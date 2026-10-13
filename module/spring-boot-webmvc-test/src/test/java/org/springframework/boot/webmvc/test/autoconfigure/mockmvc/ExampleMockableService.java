/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.test.autoconfigure.mockmvc;

import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.stereotype.Service;

/**
 * Example mockable {@link Service @Service} used with {@link WebMvcTest @WebMvcTest}
 * tests.
 *
 * @author Phillip Webb
 */
@Service
public class ExampleMockableService {

	public ExampleMockableService() {
		throw new IllegalStateException("Should not be called");
	}

}
