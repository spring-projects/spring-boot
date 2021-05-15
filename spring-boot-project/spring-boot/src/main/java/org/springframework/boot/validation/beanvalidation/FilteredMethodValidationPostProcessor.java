/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.validation.beanvalidation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;

/**
 * Custom {@link MethodValidationPostProcessor} that applies
 * {@link MethodValidationExcludeFilter exclusion filters}.
 *
 * @author Andy Wilkinson
 * @since 2.4.0
 */
public class FilteredMethodValidationPostProcessor extends MethodValidationPostProcessor {

	private final Collection<MethodValidationExcludeFilter> excludeFilters;

	/**
	 * Creates a new {@code FilteredMethodValidationPostProcessor} that will apply the
	 * given {@code excludeFilters} when identifying beans that are eligible for method
	 * validation post-processing.
	 * @param excludeFilters filters to apply
	 */
	public FilteredMethodValidationPostProcessor(Stream<? extends MethodValidationExcludeFilter> excludeFilters) {
		this.excludeFilters = excludeFilters.collect(Collectors.toList());
	}

	/**
	 * Creates a new {@code FilteredMethodValidationPostProcessor} that will apply the
	 * given {@code excludeFilters} when identifying beans that are eligible for method
	 * validation post-processing.
	 * @param excludeFilters filters to apply
	 */
	public FilteredMethodValidationPostProcessor(Collection<? extends MethodValidationExcludeFilter> excludeFilters) {
		this.excludeFilters = new ArrayList<>(excludeFilters);
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		DefaultPointcutAdvisor advisor = (DefaultPointcutAdvisor) this.advisor;
		ClassFilter classFilter = advisor.getPointcut().getClassFilter();
		MethodMatcher methodMatcher = advisor.getPointcut().getMethodMatcher();
		advisor.setPointcut(new ComposablePointcut(classFilter, methodMatcher).intersection(this::isIncluded));
	}

	private boolean isIncluded(Class<?> candidate) {
		for (MethodValidationExcludeFilter exclusionFilter : this.excludeFilters) {
			if (exclusionFilter.isExcluded(candidate)) {
				return false;
			}
		}
		return true;
	}

}
