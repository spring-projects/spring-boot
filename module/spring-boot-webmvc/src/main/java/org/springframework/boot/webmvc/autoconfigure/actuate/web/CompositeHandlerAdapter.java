/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.autoconfigure.actuate.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;

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

	private @Nullable List<HandlerAdapter> adapters;

	CompositeHandlerAdapter(ListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public boolean supports(Object handler) {
		return getAdapter(handler).isPresent();
	}

	@Override
	public @Nullable ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		Optional<HandlerAdapter> adapter = getAdapter(handler);
		if (adapter.isPresent()) {
			return adapter.get().handle(request, response, handler);
		}
		return null;
	}

	private Optional<HandlerAdapter> getAdapter(Object handler) {
		if (this.adapters == null) {
			this.adapters = extractAdapters();
		}
		return this.adapters.stream().filter((a) -> a.supports(handler)).findFirst();
	}

	private List<HandlerAdapter> extractAdapters() {
		List<HandlerAdapter> list = new ArrayList<>(this.beanFactory.getBeansOfType(HandlerAdapter.class).values());
		list.remove(this);
		AnnotationAwareOrderComparator.sort(list);
		return list;
	}

}
