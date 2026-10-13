/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.http.converter.autoconfigure;

import org.springframework.boot.autoconfigure.preinitialize.BackgroundPreinitializer;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.multipart.MultipartHttpMessageConverter;

/**
 * {@link BackgroundPreinitializer} Spring's {@link HttpMessageConverter} implementations.
 *
 * @author Phillip Webb
 */
final class MessageConverterBackgroundPreinitializer implements BackgroundPreinitializer {

	@Override
	public void preinitialize() throws Exception {
		new MultipartHttpMessageConverter();
	}

}
