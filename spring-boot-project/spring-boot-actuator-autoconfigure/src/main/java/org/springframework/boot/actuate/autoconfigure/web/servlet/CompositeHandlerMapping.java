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

import javax.servlet.http.HttpServletRequest;

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

	private List<HandlerMapping> mappings;

	@Override
	public HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {
		if (this.mappings == null) {
			this.mappings = extractMappings();
		}
		for (HandlerMapping mapping : this.mappings) {
			HandlerExecutionChain handler = mapping.getHandler(request);
			if (handler != null) {
				return handler;
			}
		}
		return null;
	}

	private List<HandlerMapping> extractMappings() {
		List<HandlerMapping> list = new ArrayList<>(this.beanFactory.getBeansOfType(HandlerMapping.class).values());
		list.remove(this);
		AnnotationAwareOrderComparator.sort(list);
		return list;
	}

}
