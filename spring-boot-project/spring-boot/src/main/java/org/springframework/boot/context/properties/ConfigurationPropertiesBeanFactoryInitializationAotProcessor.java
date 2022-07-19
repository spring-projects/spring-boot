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

package org.springframework.boot.context.properties;

import java.util.ArrayList;
import java.util.List;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.support.RuntimeHintsUtils;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * {@link BeanFactoryInitializationAotProcessor} that contributes runtime hints for
 * configuration properties-annotated beans.
 *
 * @author Stephane Nicoll
 * @author Christoph Strobl
 * @author Sebastien Deleuze
 * @author Andy Wilkinson
 */
class ConfigurationPropertiesBeanFactoryInitializationAotProcessor implements BeanFactoryInitializationAotProcessor {

	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		String[] beanNames = beanFactory.getBeanNamesForAnnotation(ConfigurationProperties.class);
		List<Class<?>> types = new ArrayList<>();
		for (String beanName : beanNames) {
			Class<?> beanType = beanFactory.getType(beanName, false);
			if (beanType != null) {
				types.add(ClassUtils.getUserClass(beanType));
			}
		}
		if (!CollectionUtils.isEmpty(types)) {
			return new ConfigurationPropertiesReflectionHintsContribution(types);
		}
		return null;
	}

	private static final class ConfigurationPropertiesReflectionHintsContribution
			implements BeanFactoryInitializationAotContribution {

		private final Iterable<Class<?>> types;

		private ConfigurationPropertiesReflectionHintsContribution(Iterable<Class<?>> types) {
			this.types = types;
		}

		@Override
		public void applyTo(GenerationContext generationContext,
				BeanFactoryInitializationCode beanFactoryInitializationCode) {
			RuntimeHintsUtils.registerAnnotation(generationContext.getRuntimeHints(), ConfigurationProperties.class);
			for (Class<?> type : this.types) {
				ConfigurationPropertiesReflectionHintsProcessor.processConfigurationProperties(type,
						generationContext.getRuntimeHints().reflection());
			}
		}

	}

}
