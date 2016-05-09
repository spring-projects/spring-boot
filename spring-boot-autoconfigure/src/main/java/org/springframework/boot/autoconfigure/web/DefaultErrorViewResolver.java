/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.SpringFactoriesLoader;
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
 * @since 1.4.0
 */
public class DefaultErrorViewResolver implements ErrorViewResolver, Ordered {

	private static final Map<Series, String> SERIES_VIEWS;

	static {
		Map<Series, String> views = new HashMap<Series, String>();
		views.put(Series.CLIENT_ERROR, "4xx");
		views.put(Series.SERVER_ERROR, "5xx");
		SERIES_VIEWS = Collections.unmodifiableMap(views);
	}

	private ApplicationContext applicationContext;

	private final ResourceProperties resourceProperties;

	private final List<TemplateAvailabilityProvider> templateAvailabilityProviders;

	private int order = Ordered.LOWEST_PRECEDENCE;

	/**
	 * Create a new {@link DefaultErrorViewResolver} instance.
	 * @param applicationContext the source application context
	 * @param resourceProperties resource properties
	 */
	public DefaultErrorViewResolver(ApplicationContext applicationContext,
			ResourceProperties resourceProperties) {
		this(applicationContext, resourceProperties,
				loadTemplateAvailabilityProviders(applicationContext));
	}

	private static List<TemplateAvailabilityProvider> loadTemplateAvailabilityProviders(
			ApplicationContext applicationContext) {
		return SpringFactoriesLoader.loadFactories(TemplateAvailabilityProvider.class,
				applicationContext == null ? null : applicationContext.getClassLoader());
	}

	DefaultErrorViewResolver(ApplicationContext applicationContext,
			ResourceProperties resourceProperties,
			List<TemplateAvailabilityProvider> templateAvailabilityProviders) {
		Assert.notNull(applicationContext, "ApplicationContext must not be null");
		Assert.notNull(resourceProperties, "ResourceProperties must not be null");
		this.applicationContext = applicationContext;
		this.resourceProperties = resourceProperties;
		this.templateAvailabilityProviders = templateAvailabilityProviders;
	}

	@Override
	public ModelAndView resolveErrorView(HttpServletRequest request, HttpStatus status,
			Map<String, Object> model) {
		ModelAndView modelAndView = resolve(String.valueOf(status), model);
		if (modelAndView == null && SERIES_VIEWS.containsKey(status.series())) {
			modelAndView = resolve(SERIES_VIEWS.get(status.series()), model);
		}
		return modelAndView;
	}

	private ModelAndView resolve(String viewName, Map<String, Object> model) {
		ModelAndView modelAndView = resolveTemplate(viewName, model);
		if (modelAndView == null) {
			modelAndView = resolveResource(viewName, model);
		}
		return modelAndView;
	}

	private ModelAndView resolveTemplate(String viewName, Map<String, Object> model) {
		for (TemplateAvailabilityProvider templateAvailabilityProvider : this.templateAvailabilityProviders) {
			if (templateAvailabilityProvider.isTemplateAvailable("error/" + viewName,
					this.applicationContext.getEnvironment(),
					this.applicationContext.getClassLoader(), this.applicationContext)) {
				return new ModelAndView("error/" + viewName, model);
			}
		}
		return null;
	}

	private ModelAndView resolveResource(String viewName, Map<String, Object> model) {
		for (String location : this.resourceProperties.getStaticLocations()) {
			try {
				Resource resource = this.applicationContext.getResource(location);
				resource = resource.createRelative("error/" + viewName + ".html");
				if (resource.exists()) {
					return new ModelAndView(new HtmlResourceView(resource), model);
				}
			}
			catch (Exception ex) {
			}
		}
		return null;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * {@link View} backed by a HTML resource.
	 */
	private static class HtmlResourceView implements View {

		private Resource resource;

		HtmlResourceView(Resource resource) {
			this.resource = resource;
		}

		@Override
		public String getContentType() {
			return MediaType.TEXT_HTML_VALUE;
		}

		@Override
		public void render(Map<String, ?> model, HttpServletRequest request,
				HttpServletResponse response) throws Exception {
			FileCopyUtils.copy(this.resource.getInputStream(),
					response.getOutputStream());
		}

	}
}
