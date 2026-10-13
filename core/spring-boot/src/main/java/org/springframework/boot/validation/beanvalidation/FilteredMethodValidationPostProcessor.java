/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.validation.beanvalidation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.util.Assert;
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
		this.excludeFilters = excludeFilters.map(MethodValidationExcludeFilter.class::cast).toList();
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
		Assert.state(advisor != null, "'advisor' must not be null");
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
