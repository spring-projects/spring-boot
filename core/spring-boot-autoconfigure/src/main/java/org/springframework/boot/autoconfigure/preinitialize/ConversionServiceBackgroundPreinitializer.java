/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.preinitialize;

import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;

/**
 * {@link BackgroundPreinitializer} for Spring's {@link ConversionService}.
 *
 * @author Phillip Webb
 */
final class ConversionServiceBackgroundPreinitializer implements BackgroundPreinitializer {

	@Override
	public void preinitialize() throws Exception {
		new DefaultFormattingConversionService();
	}

}
