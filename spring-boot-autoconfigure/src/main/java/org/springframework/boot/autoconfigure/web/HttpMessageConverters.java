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

package org.springframework.boot.autoconfigure.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.AbstractXmlHttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * Bean used to manage the {@link HttpMessageConverter}s used in a Spring Boot
 * application. Provides a convenient way to add and merge additional
 * {@link HttpMessageConverter}s to a web application.
 * <p>
 * An instance of this bean can be registered with specific
 * {@link #HttpMessageConverters(HttpMessageConverter...) additional converters} if
 * needed, otherwise default converters will be used.
 * <p>
 * NOTE: The default converters used are the same as standard Spring MVC (see
 * {@link WebMvcConfigurationSupport#getMessageConverters} with some slight re-ordering to
 * put XML converters at the back of the list.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @see #HttpMessageConverters(HttpMessageConverter...)
 * @see #HttpMessageConverters(Collection)
 * @see #getConverters()
 */
public class HttpMessageConverters implements Iterable<HttpMessageConverter<?>> {

	private final List<HttpMessageConverter<?>> converters;

	/**
	 * Create a new {@link HttpMessageConverters} instance with the specified additional
	 * converters.
	 * @param additionalConverters additional converters to be added. New converters will
	 * be added to the front of the list, overrides will replace existing items without
	 * changing the order. The {@link #getConverters()} methods can be used for further
	 * converter manipulation.
	 */
	public HttpMessageConverters(HttpMessageConverter<?>... additionalConverters) {
		this(Arrays.asList(additionalConverters));
	}

	/**
	 * Create a new {@link HttpMessageConverters} instance with the specified additional
	 * converters.
	 * @param additionalConverters additional converters to be added. Items are added just
	 * before any default converter of the same type (or at the front of the list if no
	 * default converter is found) The {@link #getConverters()} methods can be used for
	 * further converter manipulation.
	 */
	public HttpMessageConverters(Collection<HttpMessageConverter<?>> additionalConverters) {
		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		List<HttpMessageConverter<?>> processing = new ArrayList<HttpMessageConverter<?>>(
				additionalConverters);
		for (HttpMessageConverter<?> defaultConverter : getDefaultConverters()) {
			Iterator<HttpMessageConverter<?>> iterator = processing.iterator();
			while (iterator.hasNext()) {
				HttpMessageConverter<?> candidate = iterator.next();
				if (ClassUtils.isAssignableValue(defaultConverter.getClass(), candidate)) {
					converters.add(candidate);
					iterator.remove();
				}
			}
			converters.add(defaultConverter);
		}
		converters.addAll(0, processing);
		this.converters = Collections.unmodifiableList(converters);
	}

	private List<HttpMessageConverter<?>> getDefaultConverters() {
		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		if (ClassUtils.isPresent("org.springframework.web.servlet.config.annotation."
				+ "WebMvcConfigurationSupport", null)) {
			converters.addAll(new WebMvcConfigurationSupport() {
				public List<HttpMessageConverter<?>> defaultMessageConverters() {
					return super.getMessageConverters();
				}
			}.defaultMessageConverters());
		}
		else {
			converters.addAll(new RestTemplate().getMessageConverters());
		}
		reorderXmlConvertersToEnd(converters);
		return converters;
	}

	private void reorderXmlConvertersToEnd(List<HttpMessageConverter<?>> converters) {
		List<HttpMessageConverter<?>> xml = new ArrayList<HttpMessageConverter<?>>();
		for (Iterator<HttpMessageConverter<?>> iterator = converters.iterator(); iterator
				.hasNext();) {
			HttpMessageConverter<?> converter = iterator.next();
			if (converter instanceof AbstractXmlHttpMessageConverter) {
				xml.add(converter);
				iterator.remove();
			}
		}
		converters.addAll(xml);
	}

	@Override
	public Iterator<HttpMessageConverter<?>> iterator() {
		return getConverters().iterator();
	}

	/**
	 * Return a mutable list of the converters in the order that they will be registered.
	 * Values in the list cannot be modified once the bean has been initialized.
	 * @return the converters
	 */
	public List<HttpMessageConverter<?>> getConverters() {
		return this.converters;
	}

}
