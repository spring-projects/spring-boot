/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging.logback;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.spi.PropertyContainer;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.util.OptionHelper;
import org.jspecify.annotations.Nullable;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;

/**
 * Logback {@link ModelHandlerBase model handler} to support {@code <springProfile>} tags.
 *
 * @author Phillip Webb
 * @author Eddú Meléndez
 * @author Andy Wilkinson
 * @see SpringProfileModel
 * @see SpringProfileAction
 */
class SpringProfileModelHandler extends ModelHandlerBase {

	private final @Nullable Environment environment;

	SpringProfileModelHandler(Context context, @Nullable Environment environment) {
		super(context);
		this.environment = environment;
	}

	@Override
	public void handle(ModelInterpretationContext intercon, Model model) throws ModelHandlerException {
		SpringProfileModel profileModel = (SpringProfileModel) model;
		if (!acceptsProfiles(this.environment, profileModel.getName(), intercon, this.context)) {
			model.deepMarkAsSkipped();
		}
	}

	/**
	 * Return whether the given environment accepts the given comma-separated profile
	 * names after variable substitution.
	 * @param environment the environment or {@code null}
	 * @param names the comma-separated profile names
	 * @param propertyContainer the property container used for variable substitution
	 * @param context the context used for variable substitution
	 * @return whether the profiles are accepted
	 */
	static boolean acceptsProfiles(@Nullable Environment environment, @Nullable String names,
			PropertyContainer propertyContainer, Context context) {
		if (environment == null) {
			return false;
		}
		String[] profileNames = trimArrayElements(StringUtils.commaDelimitedListToStringArray(names));
		if (profileNames.length == 0) {
			return false;
		}
		for (int i = 0; i < profileNames.length; i++) {
			try {
				profileNames[i] = OptionHelper.substVars(profileNames[i], propertyContainer, context);
			}
			catch (ScanException ex) {
				throw new RuntimeException(ex);
			}
		}
		return environment.acceptsProfiles(Profiles.of(profileNames));
	}

	// The array has no nulls in it, but StringUtils.trimArrayElements return
	// @Nullable String[]
	@SuppressWarnings("NullAway")
	private static String[] trimArrayElements(String[] array) {
		return StringUtils.trimArrayElements(array);
	}

}
