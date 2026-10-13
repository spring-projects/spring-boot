/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.validation.autoconfigure;

import jakarta.validation.Configuration;
import jakarta.validation.Validation;

import org.springframework.boot.autoconfigure.preinitialize.BackgroundPreinitializer;

/**
 * {@link BackgroundPreinitializer} for jakarta.validation.
 *
 * @author Phillip Webb
 */
final class JakartaValidationBackgroundPreinitializer implements BackgroundPreinitializer {

	@Override
	public void preinitialize() throws Exception {
		Configuration<?> configuration = Validation.byDefaultProvider().configure();
		configuration.buildValidatorFactory().getValidator();
	}

}
