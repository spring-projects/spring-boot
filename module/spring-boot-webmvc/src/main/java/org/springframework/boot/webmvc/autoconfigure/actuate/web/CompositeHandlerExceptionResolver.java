/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.autoconfigure.actuate.web;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

/**
 * Composite {@link HandlerExceptionResolver}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Guirong Hu
 */
class CompositeHandlerExceptionResolver implements HandlerExceptionResolver {

	@Autowired
	private ListableBeanFactory beanFactory;

	private volatile @Nullable List<HandlerExceptionResolver> resolvers;

	@Override
	public @Nullable ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response,
			@Nullable Object handler, Exception ex) {
		for (HandlerExceptionResolver resolver : getResolvers()) {
			ModelAndView resolved = resolver.resolveException(request, response, handler, ex);
			if (resolved != null) {
				return resolved;
			}
		}
		return null;
	}

	private List<HandlerExceptionResolver> getResolvers() {
		List<HandlerExceptionResolver> resolvers = this.resolvers;
		if (resolvers == null) {
			resolvers = new ArrayList<>();
			collectResolverBeans(resolvers, this.beanFactory);
			resolvers.remove(this);
			AnnotationAwareOrderComparator.sort(resolvers);
			if (resolvers.isEmpty()) {
				resolvers.add(new DefaultErrorAttributes());
				resolvers.add(new DefaultHandlerExceptionResolver());
			}
			this.resolvers = resolvers;
		}
		return resolvers;
	}

	private void collectResolverBeans(List<HandlerExceptionResolver> resolvers, @Nullable BeanFactory beanFactory) {
		if (beanFactory instanceof ListableBeanFactory listableBeanFactory) {
			resolvers.addAll(listableBeanFactory.getBeansOfType(HandlerExceptionResolver.class).values());
		}
		if (beanFactory instanceof HierarchicalBeanFactory hierarchicalBeanFactory) {
			collectResolverBeans(resolvers, hierarchicalBeanFactory.getParentBeanFactory());
		}
	}

}
