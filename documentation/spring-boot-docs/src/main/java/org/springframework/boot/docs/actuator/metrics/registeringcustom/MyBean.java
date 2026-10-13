/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.actuator.metrics.registeringcustom;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

import org.springframework.stereotype.Component;

@Component
public class MyBean {

	private final Dictionary dictionary;

	public MyBean(MeterRegistry registry) {
		this.dictionary = Dictionary.load();
		registry.gauge("dictionary.size", Tags.empty(), this.dictionary.getWords().size());
	}

}
