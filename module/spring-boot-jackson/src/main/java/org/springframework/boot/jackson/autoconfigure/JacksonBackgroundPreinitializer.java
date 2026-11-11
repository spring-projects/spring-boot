/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson.autoconfigure;

import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.autoconfigure.preinitialize.BackgroundPreinitializer;

/**
 * {@link BackgroundPreinitializer} for Jackson.
 *
 * @author Phillip Webb
 */
final class JacksonBackgroundPreinitializer implements BackgroundPreinitializer {

	@Override
	public void preinitialize() throws Exception {
		JsonMapper.builder().build();
	}

}
