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

package org.springframework.boot.webmvc.autoconfigure;

import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * An {@link AbstractFailureAnalyzer} that provides guidance when Spring MVC is configured
 * in an application context without a servlet context.
 *
 * @author xoruddl
 */
class NoServletContextFailureAnalyzer extends AbstractFailureAnalyzer<IllegalStateException> {

	private static final String MESSAGE = "No ServletContext set";

	private final ListableBeanFactory beanFactory;

	NoServletContextFailureAnalyzer(BeanFactory beanFactory) {
		Assert.isInstanceOf(ListableBeanFactory.class, beanFactory);
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@Override
	protected @Nullable FailureAnalysis analyze(Throwable rootFailure, IllegalStateException cause) {
		if (!MESSAGE.equals(cause.getMessage()) || !isResourceHandlerMappingFailure(rootFailure)) {
			return null;
		}
		List<String> configurationClasses = findEnableWebMvcConfigurationClasses();
		if (configurationClasses.isEmpty()) {
			return null;
		}
		return new FailureAnalysis(
				"@EnableWebMvc was found on the following configuration class%s:%n%n%s%n%nSpring MVC infrastructure "
					.formatted((configurationClasses.size() != 1) ? "es" : "", format(configurationClasses))
						+ "requires a ServletContext, but the application context did not have one.",
				"Remove @EnableWebMvc from the configuration used by the test. If Spring MVC is required, use a web "
						+ "test configuration such as @SpringBootTest or @WebMvcTest.",
				cause);
	}

	private boolean isResourceHandlerMappingFailure(Throwable failure) {
		while (failure != null) {
			if (failure instanceof BeanCreationException beanCreationException
					&& "resourceHandlerMapping".equals(beanCreationException.getBeanName())) {
				return true;
			}
			failure = failure.getCause();
		}
		return false;
	}

	private List<String> findEnableWebMvcConfigurationClasses() {
		return Arrays.stream(this.beanFactory.getBeanNamesForAnnotation(EnableWebMvc.class))
			.map(this::getBeanTypeName)
			.sorted()
			.toList();
	}

	private String getBeanTypeName(String beanName) {
		Class<?> beanType = this.beanFactory.getType(beanName, false);
		return (beanType != null) ? ClassUtils.getUserClass(beanType).getName() : beanName;
	}

	private String format(List<String> configurationClasses) {
		return "\t- " + String.join(System.lineSeparator() + "\t- ", configurationClasses);
	}

}
