/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.logging.logback;

import ch.qos.logback.core.Context;
import ch.qos.logback.core.model.Model;
import ch.qos.logback.core.model.processor.ModelHandlerBase;
import ch.qos.logback.core.model.processor.ModelHandlerException;
import ch.qos.logback.core.model.processor.ModelInterpretationContext;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.util.OptionHelper;

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

	private final Environment environment;

	SpringProfileModelHandler(Context context, Environment environment) {
		super(context);
		this.environment = environment;
	}

	@Override
	public void handle(ModelInterpretationContext intercon, Model model) throws ModelHandlerException {
		SpringProfileModel profileModel = (SpringProfileModel) model;
		if (!acceptsProfiles(intercon, profileModel)) {
			model.markAsSkipped();
		}
	}

	private boolean acceptsProfiles(ModelInterpretationContext ic, SpringProfileModel model) {
		if (this.environment == null) {
			return false;
		}
		String[] profileNames = StringUtils
				.trimArrayElements(StringUtils.commaDelimitedListToStringArray(model.getName()));
		if (profileNames.length == 0) {
			return false;
		}
		for (int i = 0; i < profileNames.length; i++) {
			try {
				profileNames[i] = OptionHelper.substVars(profileNames[i], ic, this.context);
			}
			catch (ScanException ex) {
				throw new RuntimeException(ex);
			}
		}
		return this.environment.acceptsProfiles(Profiles.of(profileNames));
	}

}
