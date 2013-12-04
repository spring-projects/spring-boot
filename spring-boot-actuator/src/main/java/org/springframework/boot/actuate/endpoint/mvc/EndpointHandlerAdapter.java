/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.actuate.endpoint.EndpointDisabledException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMessageConverterMethodProcessor;
import org.springframework.web.servlet.mvc.multiaction.NoSuchRequestHandlingMethodException;

import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * MVC {@link HandlerAdapter} for {@link Endpoint}s. Similar in may respects to
 * {@link AbstractMessageConverterMethodProcessor} but not tied to annotated methods.
 * 
 * @author Phillip Webb
 * 
 * @see EndpointHandlerMapping
 */
public final class EndpointHandlerAdapter implements HandlerAdapter {

	private final Log logger = LogFactory.getLog(getClass());

	private static final MediaType MEDIA_TYPE_APPLICATION = new MediaType("application");

	private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

	private List<HttpMessageConverter<?>> messageConverters;

	private List<MediaType> allSupportedMediaTypes;

	public EndpointHandlerAdapter() {
		WebMvcConfigurationSupportConventions conventions = new WebMvcConfigurationSupportConventions();
		setMessageConverters(conventions.getDefaultHttpMessageConverters());
	}

	@Override
	public boolean supports(Object handler) {
		return handler instanceof Endpoint;
	}

	@Override
	public long getLastModified(HttpServletRequest request, Object handler) {
		return -1;
	}

	@Override
	public ModelAndView handle(HttpServletRequest request, HttpServletResponse response,
			Object handler) throws Exception {
		handle(request, response, (Endpoint<?>) handler);
		return null;
	}

	@SuppressWarnings("unchecked")
	private void handle(HttpServletRequest request, HttpServletResponse response,
			Endpoint<?> endpoint) throws Exception {

		Object result = null;
		try {
			result = endpoint.invoke();
		}
		catch (EndpointDisabledException e) {
			// Disabled endpoints should get mapped to a HTTP 404
			throw new NoSuchRequestHandlingMethodException(request);
		}

		Class<?> resultClass = result.getClass();

		List<MediaType> mediaTypes = getMediaTypes(request, endpoint, resultClass);
		MediaType selectedMediaType = selectMediaType(mediaTypes);

		ServletServerHttpResponse outputMessage = new ServletServerHttpResponse(response);
		try {
			if (selectedMediaType != null) {
				selectedMediaType = selectedMediaType.removeQualityValue();
				for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
					if (messageConverter.canWrite(resultClass, selectedMediaType)) {
						((HttpMessageConverter<Object>) messageConverter).write(result,
								selectedMediaType, outputMessage);
						if (this.logger.isDebugEnabled()) {
							this.logger.debug("Written [" + result + "] as \""
									+ selectedMediaType + "\" using [" + messageConverter
									+ "]");
						}
						return;
					}
				}
			}
			throw new HttpMediaTypeNotAcceptableException(this.allSupportedMediaTypes);
		}
		finally {
			outputMessage.close();
		}
	}

	private List<MediaType> getMediaTypes(HttpServletRequest request,
			Endpoint<?> endpoint, Class<?> resultClass)
			throws HttpMediaTypeNotAcceptableException {
		List<MediaType> requested = getAcceptableMediaTypes(request);
		List<MediaType> producible = getProducibleMediaTypes(endpoint, resultClass);

		Set<MediaType> compatible = new LinkedHashSet<MediaType>();
		for (MediaType r : requested) {
			for (MediaType p : producible) {
				if (r.isCompatibleWith(p)) {
					compatible.add(getMostSpecificMediaType(r, p));
				}
			}
		}
		if (compatible.isEmpty()) {
			throw new HttpMediaTypeNotAcceptableException(producible);
		}
		List<MediaType> mediaTypes = new ArrayList<MediaType>(compatible);
		MediaType.sortBySpecificityAndQuality(mediaTypes);
		return mediaTypes;
	}

	private List<MediaType> getAcceptableMediaTypes(HttpServletRequest request)
			throws HttpMediaTypeNotAcceptableException {
		List<MediaType> mediaTypes = this.contentNegotiationManager
				.resolveMediaTypes(new ServletWebRequest(request));
		return mediaTypes.isEmpty() ? Collections.singletonList(MediaType.ALL)
				: mediaTypes;
	}

	private List<MediaType> getProducibleMediaTypes(Endpoint<?> endpoint,
			Class<?> returnValueClass) {
		MediaType[] mediaTypes = endpoint.produces();
		if (mediaTypes != null && mediaTypes.length != 0) {
			return Arrays.asList(mediaTypes);
		}

		if (this.allSupportedMediaTypes.isEmpty()) {
			return Collections.singletonList(MediaType.ALL);
		}

		List<MediaType> result = new ArrayList<MediaType>();
		for (HttpMessageConverter<?> converter : this.messageConverters) {
			if (converter.canWrite(returnValueClass, null)) {
				result.addAll(converter.getSupportedMediaTypes());
			}
		}
		return result;
	}

	private MediaType getMostSpecificMediaType(MediaType acceptType, MediaType produceType) {
		produceType = produceType.copyQualityValue(acceptType);
		return MediaType.SPECIFICITY_COMPARATOR.compare(acceptType, produceType) <= 0 ? acceptType
				: produceType;
	}

	private MediaType selectMediaType(List<MediaType> mediaTypes) {
		MediaType selectedMediaType = null;
		for (MediaType mediaType : mediaTypes) {
			if (mediaType.isConcrete()) {
				selectedMediaType = mediaType;
				break;
			}
			else if (mediaType.equals(MediaType.ALL)
					|| mediaType.equals(MEDIA_TYPE_APPLICATION)) {
				selectedMediaType = MediaType.APPLICATION_OCTET_STREAM;
				break;
			}
		}
		return selectedMediaType;
	}

	public void setContentNegotiationManager(
			ContentNegotiationManager contentNegotiationManager) {
		this.contentNegotiationManager = contentNegotiationManager;
	}

	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = messageConverters;
		Set<MediaType> allSupportedMediaTypes = new LinkedHashSet<MediaType>();
		for (HttpMessageConverter<?> messageConverter : messageConverters) {
			allSupportedMediaTypes.addAll(messageConverter.getSupportedMediaTypes());
		}
		this.allSupportedMediaTypes = new ArrayList<MediaType>(allSupportedMediaTypes);
		MediaType.sortBySpecificity(this.allSupportedMediaTypes);
	}

	/**
	 * Default conventions, taken from {@link WebMvcConfigurationSupport} with a few minor
	 * tweaks.
	 */
	private static class WebMvcConfigurationSupportConventions extends
			WebMvcConfigurationSupport {
		public List<HttpMessageConverter<?>> getDefaultHttpMessageConverters() {
			List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
			addDefaultHttpMessageConverters(converters);
			for (HttpMessageConverter<?> converter : converters) {
				if (converter instanceof MappingJackson2HttpMessageConverter) {
					MappingJackson2HttpMessageConverter jacksonConverter = (MappingJackson2HttpMessageConverter) converter;
					jacksonConverter.getObjectMapper().disable(
							SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
				}
			}
			return converters;
		}
	}
}
