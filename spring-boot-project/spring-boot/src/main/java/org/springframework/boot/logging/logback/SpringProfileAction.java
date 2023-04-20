/*
 * Copyright 2012-2023 the original author or authors.
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

import ch.qos.logback.core.joran.action.BaseModelAction;
import ch.qos.logback.core.joran.spi.SaxEventInterpretationContext;
import ch.qos.logback.core.model.Model;
import org.xml.sax.Attributes;

/**
 * Logback {@link BaseModelAction} for {@code <springProperty>} tags. Allows a section of
 * a Logback configuration to only be enabled when a specific profile is active.
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
