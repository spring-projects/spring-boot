/*
 * Copyright 2012-present the original author or authors.
 */

package org.test;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SampleRunner {

	@Async
	public void run() {

	}
}

