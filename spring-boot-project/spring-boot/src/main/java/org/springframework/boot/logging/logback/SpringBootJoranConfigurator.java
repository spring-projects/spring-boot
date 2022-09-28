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

import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.ElementSelector;
import ch.qos.logback.core.joran.spi.RuleStore;
import ch.qos.logback.core.model.processor.DefaultProcessor;

import org.springframework.boot.logging.LoggingInitializationContext;

/**
 * Extended version of the Logback {@link JoranConfigurator} that adds additional Spring
 * Boot rules.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class SpringBootJoranConfigurator extends JoranConfigurator {

	private LoggingInitializationContext initializationContext;

	SpringBootJoranConfigurator(LoggingInitializationContext initializationContext) {
		this.initializationContext = initializationContext;
	}

	@Override
	protected void addModelHandlerAssociations(DefaultProcessor defaultProcessor) {
		super.addModelHandlerAssociations(defaultProcessor);
		defaultProcessor.addHandler(SpringPropertyModel.class,
				(handlerContext, handlerMic) -> new SpringPropertyModelHandler(this.context,
						this.initializationContext.getEnvironment()));
		defaultProcessor.addHandler(SpringProfileModel.class,
				(handlerContext, handlerMic) -> new SpringProfileModelHandler(this.context,
						this.initializationContext.getEnvironment()));
	}

	@Override
	public void addElementSelectorAndActionAssociations(RuleStore ruleStore) {
		super.addElementSelectorAndActionAssociations(ruleStore);
		ruleStore.addRule(new ElementSelector("configuration/springProperty"), SpringPropertyAction::new);
		ruleStore.addRule(new ElementSelector("*/springProfile"), SpringProfileAction::new);
		ruleStore.addTransparentPathPart("springProfile");
	}

}
