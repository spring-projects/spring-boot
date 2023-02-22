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

package org.springframework.boot.context.properties;

import java.lang.reflect.Constructor;

import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.UnsatisfiedDependencyException;
import org.springframework.boot.context.properties.ConfigurationPropertiesBean.BindMethod;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.analyzer.AbstractInjectionFailureAnalyzer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;

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
			String action = String.format("Update your configuration so that " + simpleName + " is defined via @"
					+ ConfigurationPropertiesScan.class.getSimpleName() + " or @"
					+ EnableConfigurationProperties.class.getSimpleName() + ".", simpleName);
			return new FailureAnalysis(
					simpleName + " is annotated with @" + ConstructorBinding.class.getSimpleName()
							+ " but it is defined as a regular bean which caused dependency injection to fail.",
					action, cause);
		}
		return null;
	}

	private boolean isConstructorBindingConfigurationProperties(InjectionPoint injectionPoint) {
		if (injectionPoint != null && injectionPoint.getMember() instanceof Constructor) {
			Constructor<?> constructor = (Constructor<?>) injectionPoint.getMember();
			Class<?> declaringClass = constructor.getDeclaringClass();
			MergedAnnotation<ConfigurationProperties> configurationProperties = MergedAnnotations.from(declaringClass)
				.get(ConfigurationProperties.class);
			return configurationProperties.isPresent()
					&& BindMethod.forType(constructor.getDeclaringClass()) == BindMethod.VALUE_OBJECT;
		}
		return false;
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
