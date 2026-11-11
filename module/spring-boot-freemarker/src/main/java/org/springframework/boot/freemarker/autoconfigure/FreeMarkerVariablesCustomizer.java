/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.freemarker.autoconfigure;

import java.util.Map;

import freemarker.template.Configuration;

import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;

/**
 * Callback interface that can be implemented by beans wishing to customize the FreeMarker
 * {@link Configuration#getSharedVariableNames() shared variables} to fine-tune their
 * auto-configuration before they are used by an auto-configured
 * {@link FreeMarkerConfigurationFactory}.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@FunctionalInterface
public interface FreeMarkerVariablesCustomizer {

	/**
	 * Customize the {@code variables} to be set as well-known FreeMarker objects.
	 * @param variables the variables to customize
	 * @see FreeMarkerConfigurationFactory#setFreemarkerVariables(Map)
	 */
	void customizeFreeMarkerVariables(Map<String, Object> variables);

}
