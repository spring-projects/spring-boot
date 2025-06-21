/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.logging.structured;

import java.util.Set;

import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.logging.structured.StructuredLoggingJsonProperties.StackTrace;
import org.springframework.core.env.Environment;

/**
 * {@link BeanFactoryInitializationAotProcessor} that registers {@link RuntimeHints} for
 * {@link StructuredLoggingJsonProperties}.
 *
 * @author Dmytro Nosan
 * @author Yanming Zhou
 * @author Phillip Webb
 */
class StructuredLoggingJsonPropertiesBeanFactoryInitializationAotProcessor
		implements BeanFactoryInitializationAotProcessor {

	private static final String ENVIRONMENT_BEAN_NAME = "environment";

	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		Environment environment = beanFactory.getBean(ENVIRONMENT_BEAN_NAME, Environment.class);
		StructuredLoggingJsonProperties properties = StructuredLoggingJsonProperties.get(environment);
		if (properties != null) {
			Set<Class<? extends StructuredLoggingJsonMembersCustomizer<?>>> customizers = properties.customizer();
			String stackTracePrinter = getCustomStackTracePrinter(properties);
			if (stackTracePrinter != null || !customizers.isEmpty()) {
				return new AotContribution(beanFactory.getBeanClassLoader(), customizers, stackTracePrinter);
			}
		}
		return null;
	}

	private static String getCustomStackTracePrinter(StructuredLoggingJsonProperties properties) {
		StackTrace stackTrace = properties.stackTrace();
		return (stackTrace != null && stackTrace.hasCustomPrinter()) ? stackTrace.printer() : null;
	}

	private static final class AotContribution implements BeanFactoryInitializationAotContribution {

		private final ClassLoader classLoader;

		private final Set<Class<? extends StructuredLoggingJsonMembersCustomizer<?>>> customizers;

		private final String stackTracePrinter;

		private AotContribution(ClassLoader classLoader,
				Set<Class<? extends StructuredLoggingJsonMembersCustomizer<?>>> customizers, String stackTracePrinter) {
			this.classLoader = classLoader;
			this.customizers = customizers;
			this.stackTracePrinter = stackTracePrinter;
		}

		@Override
		public void applyTo(GenerationContext generationContext,
				BeanFactoryInitializationCode beanFactoryInitializationCode) {
			ReflectionHints reflection = generationContext.getRuntimeHints().reflection();
			this.customizers.forEach((customizer) -> reflection.registerType(customizer,
					MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS));
			if (this.stackTracePrinter != null) {
				reflection.registerTypeIfPresent(this.classLoader, this.stackTracePrinter,
						MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
			}
		}

	}

}
