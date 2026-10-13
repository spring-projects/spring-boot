/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.autoconfigure.actuate.web;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Composite {@link HandlerMapping}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 */
class CompositeHandlerMapping implements HandlerMapping {

	@Autowired
	private ListableBeanFactory beanFactory;

	private @Nullable List<HandlerMapping> mappings;

	@Override
	public @Nullable HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		for (HandlerMapping mapping : getMappings()) {
			HandlerExecutionChain handler = mapping.getHandler(request);
			if (handler != null) {
				return handler;
			}
		}
		return null;
	}

	@Override
	public boolean usesPathPatterns() {
		for (HandlerMapping mapping : getMappings()) {
			if (mapping.usesPathPatterns()) {
				return true;
			}
		}
		return false;
	}

	private List<HandlerMapping> getMappings() {
		if (this.mappings == null) {
			this.mappings = extractMappings();
		}
		return this.mappings;
	}

	private List<HandlerMapping> extractMappings() {
		List<HandlerMapping> list = new ArrayList<>(this.beanFactory.getBeansOfType(HandlerMapping.class).values());
		list.remove(this);
		AnnotationAwareOrderComparator.sort(list);
		return list;
	}

}
