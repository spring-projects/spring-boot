/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.batch.autoconfigure;

import org.springframework.core.convert.support.ConfigurableConversionService;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link ConfigurableConversionService} to fine-tune its auto-configuration. The
 * conversion service is used by the Spring Batch infrastructure.
 *
 * @author Claudio Nave
 * @since 4.0.0
 */
@FunctionalInterface
public interface BatchConversionServiceCustomizer {

	/**
	 * Customize the {@link ConfigurableConversionService}.
	 * @param configurableConversionService the ConfigurableConversionService to customize
	 */
	void customize(ConfigurableConversionService configurableConversionService);

}
