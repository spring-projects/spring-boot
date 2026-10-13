/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webservices.client;

import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * Callback interface that can be used to customize a {@link WebServiceTemplate}.
 *
 * @author Dmytro Nosan
 * @since 2.1.0
 */
@FunctionalInterface
public interface WebServiceTemplateCustomizer {

	/**
	 * Callback to customize a {@link WebServiceTemplate} instance.
	 * @param webServiceTemplate the template to customize
	 */
	void customize(WebServiceTemplate webServiceTemplate);

}
