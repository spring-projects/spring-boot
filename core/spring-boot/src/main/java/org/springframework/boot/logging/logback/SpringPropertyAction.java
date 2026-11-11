/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import ch.qos.logback.core.joran.action.BaseModelAction;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.model.Model;
import org.xml.sax.Attributes;

/**
 * Logback {@link BaseModelAction} for {@code <springProperty>} tags. Allows Logback
 * properties to be sourced from the Spring environment.
 *
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Madhura Bhave
 * @author Andy Wilkinson
 * @see SpringPropertyModel
 * @see SpringPropertyModelHandler
 */
class SpringPropertyAction extends BaseModelAction {

	private static final String SOURCE_ATTRIBUTE = "source";

	private static final String DEFAULT_VALUE_ATTRIBUTE = "defaultValue";

	@Override
	protected Model buildCurrentModel(SaxEventInterpretationContext interpretationContext, String name,
			Attributes attributes) {
		SpringPropertyModel model = new SpringPropertyModel();
		model.setName(attributes.getValue(NAME_ATTRIBUTE));
		model.setSource(attributes.getValue(SOURCE_ATTRIBUTE));
		model.setScope(attributes.getValue(SCOPE_ATTRIBUTE));
		model.setDefaultValue(attributes.getValue(DEFAULT_VALUE_ATTRIBUTE));
		return model;
	}

}
