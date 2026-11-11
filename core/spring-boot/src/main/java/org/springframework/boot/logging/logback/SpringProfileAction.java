/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import ch.qos.logback.core.joran.action.BaseModelAction;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.model.Model;
import org.xml.sax.Attributes;

/**
 * Logback {@link BaseModelAction} for {@code <springProfile>} tags. Allows a section of a
 * Logback configuration to only be enabled when a specific profile is active.
 *
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Andy Wilkinson
 * @see SpringProfileModel
 * @see SpringProfileModelHandler
 */
class SpringProfileAction extends BaseModelAction {

	@Override
	protected Model buildCurrentModel(SaxEventInterpretationContext interpretationContext, String name,
			Attributes attributes) {
		SpringProfileModel model = new SpringProfileModel();
		model.setName(attributes.getValue(NAME_ATTRIBUTE));
		return model;
	}

}
