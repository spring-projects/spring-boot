/*
 * Copyright 2012-2019 the original author or authors.
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
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;

/**
 * Composite {@link HandlerAdapter}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class CompositeHandlerAdapter implements HandlerAdapter {

	private final ListableBeanFactory beanFactory;

	private List<HandlerAdapter> adapters;

	CompositeHandlerAdapter(ListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public boolean supports(Object handler) {
		return getAdapter(handler).isPresent();
	}

	@Override
	public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		Optional<HandlerAdapter> adapter = getAdapter(handler);
		if (adapter.isPresent()) {
			return adapter.get().handle(request, response, handler);
		}
		return null;
	}

	@Override
	public long getLastModified(HttpServletRequest request, Object handler) {
		Optional<HandlerAdapter> adapter = getAdapter(handler);
		if (adapter.isPresent()) {
			return adapter.get().getLastModified(request, handler);
		}
		return 0;
	}

	private Optional<HandlerAdapter> getAdapter(Object handler) {
		if (this.adapters == null) {
			this.adapters = extractAdapters();
		}
		return this.adapters.stream().filter((a) -> a.supports(handler)).findFirst();
	}

	private List<HandlerAdapter> extractAdapters() {
		List<HandlerAdapter> list = new ArrayList<>();
		list.addAll(this.beanFactory.getBeansOfType(HandlerAdapter.class).values());
		list.remove(this);
		AnnotationAwareOrderComparator.sort(list);
		return list;
	}

}
