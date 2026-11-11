/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.joran.action.ActionUtil;
import ch.qos.logback.core.joran.action.ActionUtil.Scope;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.model.util.PropertyModelHandlerHelper;
import ch.qos.logback.core.util.OptionHelper;
import org.jspecify.annotations.Nullable;

import org.springframework.core.env.Environment;

/**
 * Logback {@link ModelHandlerBase model handler} to support {@code <springProperty>}
 * tags. Allows Logback properties to be sourced from the Spring environment.
 *
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Madhura Bhave
 * @author Andy Wilkinson
 * @see SpringPropertyAction
 * @see SpringPropertyModel
 */
class SpringPropertyModelHandler extends ModelHandlerBase {

	private final @Nullable Environment environment;

	SpringPropertyModelHandler(Context context, @Nullable Environment environment) {
		super(context);
		this.environment = environment;
	}

	@Override
	public void handle(ModelInterpretationContext intercon, Model model) throws ModelHandlerException {
		SpringPropertyModel propertyModel = (SpringPropertyModel) model;
		Scope scope = ActionUtil.stringToScope(propertyModel.getScope());
		String defaultValue = propertyModel.getDefaultValue();
		String source = propertyModel.getSource();
		if (OptionHelper.isNullOrEmpty(propertyModel.getName()) || OptionHelper.isNullOrEmpty(source)) {
			addError("The \"name\" and \"source\" attributes of <springProperty> must be set");
		}
		PropertyModelHandlerHelper.setProperty(intercon, propertyModel.getName(), getValue(source, defaultValue),
				scope);
	}

	private String getValue(String source, String defaultValue) {
		if (this.environment == null) {
			addWarn("No Spring Environment available to resolve " + source);
			return defaultValue;
		}
		return this.environment.getProperty(source, defaultValue);
	}

}
