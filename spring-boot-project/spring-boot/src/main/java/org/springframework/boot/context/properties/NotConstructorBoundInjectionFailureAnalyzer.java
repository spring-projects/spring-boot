/*
 * Copyright 2012-2024 the original author or authors.
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

import java.lang.reflect.Constructor;

import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.context.properties.bind.BindMethod;
import org.springframework.boot.context.properties.bind.ConstructorBinding;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.analyzer.AbstractInjectionFailureAnalyzer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;

/**
 * An {@link AbstractInjectionFailureAnalyzer} for
 * {@link ConfigurationProperties @ConfigurationProperties} that are intended to use
 * {@link ConstructorBinding constructor binding} but did not.
 *
 * @author Andy Wilkinson
 */
class NotConstructorBoundInjectionFailureAnalyzer
		extends AbstractInjectionFailureAnalyzer<NoSuchBeanDefinitionException> implements Ordered {

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, NoSuchBeanDefinitionException cause, String description) {
		InjectionPoint injectionPoint = findInjectionPoint(rootFailure);
		if (isConstructorBindingConfigurationProperties(injectionPoint)) {
			String simpleName = injectionPoint.getMember().getDeclaringClass().getSimpleName();
			String action = "Update your configuration so that " + simpleName + " is defined via @"
					+ ConfigurationPropertiesScan.class.getSimpleName() + " or @"
					+ EnableConfigurationProperties.class.getSimpleName() + ".";
			return new FailureAnalysis(
					simpleName + " is annotated with @" + ConstructorBinding.class.getSimpleName()
							+ " but it is defined as a regular bean which caused dependency injection to fail.",
					action, cause);
		}
		return null;
	}

	private boolean isConstructorBindingConfigurationProperties(InjectionPoint injectionPoint) {
		return injectionPoint != null && injectionPoint.getMember() instanceof Constructor<?> constructor
				&& isConstructorBindingConfigurationProperties(constructor);
	}

	private boolean isConstructorBindingConfigurationProperties(Constructor<?> constructor) {
		Class<?> declaringClass = constructor.getDeclaringClass();
		BindMethod bindMethod = ConfigurationPropertiesBean.deduceBindMethod(declaringClass);
		return MergedAnnotations.from(declaringClass, SearchStrategy.TYPE_HIERARCHY)
			.isPresent(ConfigurationProperties.class) && bindMethod == BindMethod.VALUE_OBJECT;
	}

	private InjectionPoint findInjectionPoint(Throwable failure) {
		UnsatisfiedDependencyException unsatisfiedDependencyException = findCause(failure,
				UnsatisfiedDependencyException.class);
		if (unsatisfiedDependencyException == null) {
			return null;
		}
		return unsatisfiedDependencyException.getInjectionPoint();
	}

}
