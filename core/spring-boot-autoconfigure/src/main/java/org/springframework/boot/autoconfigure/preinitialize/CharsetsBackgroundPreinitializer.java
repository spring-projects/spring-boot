/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.preinitialize;

import java.nio.charset.StandardCharsets;

/**
 * {@link BackgroundPreinitializer} for commonly used charsets.
 *
 * @author Phillip Webb
 */
final class CharsetsBackgroundPreinitializer implements BackgroundPreinitializer {

	@Override
	public void preinitialize() throws Exception {
		StandardCharsets.UTF_8.name();
	}

}
