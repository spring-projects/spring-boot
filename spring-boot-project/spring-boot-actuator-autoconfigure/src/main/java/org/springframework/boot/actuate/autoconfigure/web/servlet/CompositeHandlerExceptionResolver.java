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

package org.springframework.boot.actuate.autoconfigure.web.servlet;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
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

	private volatile List<HandlerExceptionResolver> resolvers;

	/**
	 * Resolves the exception thrown during the handling of a request.
	 * @param request the HttpServletRequest object representing the current request
	 * @param response the HttpServletResponse object representing the current response
	 * @param handler the Object representing the handler for the request
	 * @param ex the Exception object representing the exception thrown during request
	 * handling
	 * @return a ModelAndView object representing the resolved exception, or null if no
	 * resolution is found
	 */
	@Override
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) {
		for (HandlerExceptionResolver resolver : getResolvers()) {
			ModelAndView resolved = resolver.resolveException(request, response, handler, ex);
			if (resolved != null) {
				return resolved;
			}
		}
		return null;
	}

	/**
	 * Retrieves the list of HandlerExceptionResolvers.
	 * @return The list of HandlerExceptionResolvers.
	 */
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

	/**
	 * Collects all the resolver beans of type HandlerExceptionResolver from the given
	 * bean factory and its parent bean factories recursively.
	 * @param resolvers the list to store the collected resolver beans
	 * @param beanFactory the bean factory to collect the resolver beans from
	 */
	private void collectResolverBeans(List<HandlerExceptionResolver> resolvers, BeanFactory beanFactory) {
		if (beanFactory instanceof ListableBeanFactory listableBeanFactory) {
			resolvers.addAll(listableBeanFactory.getBeansOfType(HandlerExceptionResolver.class).values());
		}
		if (beanFactory instanceof HierarchicalBeanFactory hierarchicalBeanFactory) {
			collectResolverBeans(resolvers, hierarchicalBeanFactory.getParentBeanFactory());
		}
	}

}
