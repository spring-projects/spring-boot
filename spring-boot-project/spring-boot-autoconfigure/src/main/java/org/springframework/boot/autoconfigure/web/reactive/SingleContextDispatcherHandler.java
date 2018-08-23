/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.web.reactive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.HandlerResultHandler;

/**
 * {@link DispatcherHandler} implementation that only checks for infrastructure beans in
 * the current application context (i.e. does not consider the parent context).
 *
 * @author Brian Clozel
 * @since 2.1.0
 */
public class SingleContextDispatcherHandler extends DispatcherHandler {

	@Override
	protected void initStrategies(ApplicationContext context) {
		Map<String, HandlerMapping> mappingBeans = context
				.getBeansOfType(HandlerMapping.class, true, false);
		ArrayList<HandlerMapping> mappings = new ArrayList<>(mappingBeans.values());
		AnnotationAwareOrderComparator.sort(mappings);
		this.handlerMappings = Collections.unmodifiableList(mappings);
		Map<String, HandlerAdapter> adapterBeans = context
				.getBeansOfType(HandlerAdapter.class, true, false);
		this.handlerAdapters = new ArrayList(adapterBeans.values());
		AnnotationAwareOrderComparator.sort(this.handlerAdapters);
		Map<String, HandlerResultHandler> beans = context
				.getBeansOfType(HandlerResultHandler.class, true, false);
		this.resultHandlers = new ArrayList<>(beans.values());
		AnnotationAwareOrderComparator.sort(this.resultHandlers);
	}

}
