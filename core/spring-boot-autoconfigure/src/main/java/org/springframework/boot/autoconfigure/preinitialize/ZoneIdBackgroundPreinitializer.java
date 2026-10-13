/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.autoconfigure.preinitialize;

import java.time.ZoneId;

/**
 * {@link BackgroundPreinitializer} for {@link ZoneId}.
 *
 * @author Phillip Webb
 */
final class ZoneIdBackgroundPreinitializer implements BackgroundPreinitializer {

	@Override
	public void preinitialize() throws Exception {
		ZoneId.systemDefault();
	}

}
