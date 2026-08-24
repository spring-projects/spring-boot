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

package org.springframework.boot.diagnostics.analyzer;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * An {@link AbstractFailureAnalyzer} that provides guidance when Spring MVC is configured
 * in an application context without a servlet context.
 *
 * @author xoruddl taeyun1411@gmail.com
 */
class NoServletContextFailureAnalyzer extends AbstractFailureAnalyzer<IllegalStateException> {

	private static final String MESSAGE = "No ServletContext set";

	@Override
	protected @Nullable FailureAnalysis analyze(Throwable rootFailure, IllegalStateException cause) {
		if (!MESSAGE.equals(cause.getMessage()) || !isResourceHandlerMappingFailure(rootFailure)) {
			return null;
		}
		return new FailureAnalysis(
				"Spring MVC infrastructure was configured in an application context without a " + "ServletContext.",
				"Remove @EnableWebMvc from the configuration used by the test, or use "
						+ "@WebMvcTest if Spring MVC configuration is required.",
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

}
