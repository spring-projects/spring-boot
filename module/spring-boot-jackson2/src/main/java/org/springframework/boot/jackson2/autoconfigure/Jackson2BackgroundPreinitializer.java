/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson2.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.autoconfigure.preinitialize.BackgroundPreinitializer;

/**
 * {@link BackgroundPreinitializer} for Jackson 2.
 *
 * @author Andy Wilkinson
 * @deprecated since 4.0.0 for removal in 4.3.0 in favor of Jackson 3
 */
@Deprecated(since = "4.0.0", forRemoval = true)
final class Jackson2BackgroundPreinitializer implements BackgroundPreinitializer {

	@Override
	public void preinitialize() throws Exception {
		new ObjectMapper();
	}

}
