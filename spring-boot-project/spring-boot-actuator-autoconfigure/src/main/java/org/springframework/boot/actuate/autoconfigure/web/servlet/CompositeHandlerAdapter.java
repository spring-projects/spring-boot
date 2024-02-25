/*
 * Copyright 2012-2022 the original author or authors.
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

	/**
     * Constructs a new CompositeHandlerAdapter with the specified ListableBeanFactory.
     *
     * @param beanFactory the ListableBeanFactory to be used by the CompositeHandlerAdapter
     */
    CompositeHandlerAdapter(ListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	/**
     * Determines if the given handler is supported by this CompositeHandlerAdapter.
     * 
     * @param handler the handler to check for support
     * @return true if the handler is supported, false otherwise
     */
    @Override
	public boolean supports(Object handler) {
		return getAdapter(handler).isPresent();
	}

	/**
     * Handles the incoming HTTP request.
     * 
     * @param request  the HttpServletRequest object representing the incoming request
     * @param response the HttpServletResponse object representing the response to be sent
     * @param handler  the Object representing the handler for the request
     * @return a ModelAndView object representing the view and model for the response
     * @throws Exception if an error occurs while handling the request
     */
    @Override
	public ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		Optional<HandlerAdapter> adapter = getAdapter(handler);
		if (adapter.isPresent()) {
			return adapter.get().handle(request, response, handler);
		}
		return null;
	}

	/**
     * Returns the last modified timestamp for the given HttpServletRequest and handler.
     * 
     * @param request the HttpServletRequest object
     * @param handler the handler object
     * @return the last modified timestamp, or 0L if not available
     * 
     * @deprecated This method has been deprecated since version 2.4.9 and may be removed in a future release. 
     *             It is recommended to use an alternative method instead.
     * 
     * @SuppressWarnings("deprecation") This annotation suppresses deprecation warnings for this method.
     */
    @Override
	@Deprecated(since = "2.4.9", forRemoval = false)
	@SuppressWarnings("deprecation")
	public long getLastModified(HttpServletRequest request, Object handler) {
		Optional<HandlerAdapter> adapter = getAdapter(handler);
		return adapter.map((handlerAdapter) -> handlerAdapter.getLastModified(request, handler)).orElse(0L);
	}

	/**
     * Retrieves the appropriate HandlerAdapter for the given handler object.
     * 
     * @param handler the handler object for which the adapter is to be retrieved
     * @return an Optional containing the HandlerAdapter if found, or an empty Optional if not found
     */
    private Optional<HandlerAdapter> getAdapter(Object handler) {
		if (this.adapters == null) {
			this.adapters = extractAdapters();
		}
		return this.adapters.stream().filter((a) -> a.supports(handler)).findFirst();
	}

	/**
     * Extracts the list of HandlerAdapters from the bean factory and sorts them based on their order.
     * 
     * @return the sorted list of HandlerAdapters
     */
    private List<HandlerAdapter> extractAdapters() {
		List<HandlerAdapter> list = new ArrayList<>(this.beanFactory.getBeansOfType(HandlerAdapter.class).values());
		list.remove(this);
		AnnotationAwareOrderComparator.sort(list);
		return list;
	}

}
