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

package org.springframework.boot.autoconfigure.web.servlet.error;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProviders;
import org.springframework.boot.autoconfigure.web.WebProperties.Resources;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatus.Series;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

/**
 * Default {@link ErrorViewResolver} implementation that attempts to resolve error views
 * using well known conventions. Will search for templates and static assets under
 * {@code '/error'} using the {@link HttpStatus status code} and the
 * {@link HttpStatus#series() status series}.
 * <p>
 * For example, an {@code HTTP 404} will search (in the specific order):
 * <ul>
 * <li>{@code '/<templates>/error/404.<ext>'}</li>
 * <li>{@code '/<static>/error/404.html'}</li>
 * <li>{@code '/<templates>/error/4xx.<ext>'}</li>
 * <li>{@code '/<static>/error/4xx.html'}</li>
 * </ul>
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.4.0
 */
public class DefaultErrorViewResolver implements ErrorViewResolver, Ordered {

	private static final Map<Series, String> SERIES_VIEWS;

	static {
		Map<Series, String> views = new EnumMap<>(Series.class);
		views.put(Series.CLIENT_ERROR, "4xx");
		views.put(Series.SERVER_ERROR, "5xx");
		SERIES_VIEWS = Collections.unmodifiableMap(views);
	}

	private final ApplicationContext applicationContext;

	private final Resources resources;

	private final TemplateAvailabilityProviders templateAvailabilityProviders;

	private int order = Ordered.LOWEST_PRECEDENCE;

	/**
	 * Create a new {@link DefaultErrorViewResolver} instance.
	 * @param applicationContext the source application context
	 * @param resources resource properties
	 * @since 2.4.0
	 */
	public DefaultErrorViewResolver(ApplicationContext applicationContext, Resources resources) {
		Assert.notNull(applicationContext, "ApplicationContext must not be null");
		Assert.notNull(resources, "Resources must not be null");
		this.applicationContext = applicationContext;
		this.resources = resources;
		this.templateAvailabilityProviders = new TemplateAvailabilityProviders(applicationContext);
	}

	/**
	 * Constructs a new DefaultErrorViewResolver with the specified ApplicationContext,
	 * Resources, and TemplateAvailabilityProviders.
	 * @param applicationContext the ApplicationContext to be used by the resolver (must
	 * not be null)
	 * @param resourceProperties the Resources to be used by the resolver (must not be
	 * null)
	 * @param templateAvailabilityProviders the TemplateAvailabilityProviders to be used
	 * by the resolver
	 * @throws IllegalArgumentException if any of the parameters are null
	 */
	DefaultErrorViewResolver(ApplicationContext applicationContext, Resources resourceProperties,
			TemplateAvailabilityProviders templateAvailabilityProviders) {
		Assert.notNull(applicationContext, "ApplicationContext must not be null");
		Assert.notNull(resourceProperties, "Resources must not be null");
		this.applicationContext = applicationContext;
		this.resources = resourceProperties;
		this.templateAvailabilityProviders = templateAvailabilityProviders;
	}

	/**
	 * Resolves the error view for the given HTTP status and model.
	 * @param request The HttpServletRequest object.
	 * @param status The HttpStatus object representing the HTTP status.
	 * @param model A Map containing the model attributes.
	 * @return A ModelAndView object representing the resolved error view, or null if no
	 * error view is found.
	 */
	@Override
	public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status, Map<String, Object> model) {
		ModelAndView modelAndView = resolve(String.valueOf(status.value()), model);
		if (modelAndView == null && SERIES_VIEWS.containsKey(status.series())) {
			modelAndView = resolve(SERIES_VIEWS.get(status.series()), model);
		}
		return modelAndView;
	}

	/**
	 * Resolves the error view based on the given view name and model. If an error view is
	 * available for the given view name, it is returned as a ModelAndView. Otherwise, it
	 * tries to resolve the error view as a resource.
	 * @param viewName The name of the error view to resolve.
	 * @param model The model to be passed to the error view.
	 * @return A ModelAndView object representing the resolved error view, or null if no
	 * error view is available.
	 */
	private ModelAndView resolve(String viewName, Map<String, Object> model) {
		String errorViewName = "error/" + viewName;
		TemplateAvailabilityProvider provider = this.templateAvailabilityProviders.getProvider(errorViewName,
				this.applicationContext);
		if (provider != null) {
			return new ModelAndView(errorViewName, model);
		}
		return resolveResource(errorViewName, model);
	}

	/**
	 * Resolves a resource view for the given view name and model.
	 * @param viewName the name of the view to resolve
	 * @param model the model to be passed to the view
	 * @return a ModelAndView object representing the resolved resource view, or null if
	 * no resource view was found
	 */
	private ModelAndView resolveResource(String viewName, Map<String, Object> model) {
		for (String location : this.resources.getStaticLocations()) {
			try {
				Resource resource = this.applicationContext.getResource(location);
				resource = resource.createRelative(viewName + ".html");
				if (resource.exists()) {
					return new ModelAndView(new HtmlResourceView(resource), model);
				}
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		return null;
	}

	/**
	 * Returns the order value of this DefaultErrorViewResolver.
	 * @return the order value of this DefaultErrorViewResolver
	 */
	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Sets the order of the error view resolver.
	 * @param order the order to set
	 */
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * {@link View} backed by an HTML resource.
	 */
	private static class HtmlResourceView implements View {

		private final Resource resource;

		/**
		 * Constructs a new HtmlResourceView with the specified resource.
		 * @param resource the resource to be used by the view
		 */
		HtmlResourceView(Resource resource) {
			this.resource = resource;
		}

		/**
		 * Returns the content type of the HTML resource view.
		 * @return the content type of the HTML resource view
		 */
		@Override
		public String getContentType() {
			return MediaType.TEXT_HTML_VALUE;
		}

		/**
		 * Render the HTML resource view.
		 * @param model the model containing the data to be rendered
		 * @param request the current HTTP servlet request
		 * @param response the current HTTP servlet response
		 * @throws Exception if an error occurs during rendering
		 */
		@Override
		public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
				throws Exception {
			response.setContentType(getContentType());
			FileCopyUtils.copy(this.resource.getInputStream(), response.getOutputStream());
		}

	}

}
