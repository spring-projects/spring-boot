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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.AbstractXmlHttpMessageConverter;
import org.springframework.util.Assert;
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
 * @see #setConverters(List)
 */
public class HttpMessageConverters implements Iterable<HttpMessageConverter<?>>,
		InitializingBean {

	private List<HttpMessageConverter<?>> converters;

	private boolean initialized;

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
	 * @param additionalConverters additional converters to be added. New converters will
	 * be added to the front of the list, overrides will replace existing items without
	 * changing the order. The {@link #getConverters()} methods can be used for further
	 * converter manipulation.
	 */
	public HttpMessageConverters(Collection<HttpMessageConverter<?>> additionalConverters) {
		this.converters = new ArrayList<HttpMessageConverter<?>>();
		List<HttpMessageConverter<?>> defaultConverters = getDefaultConverters();
		for (HttpMessageConverter<?> converter : additionalConverters) {
			int defaultConverterIndex = indexOfItemClass(defaultConverters, converter);
			if (defaultConverterIndex == -1) {
				this.converters.add(converter);
			}
			else {
				defaultConverters.set(defaultConverterIndex, converter);
			}
		}
		this.converters.addAll(defaultConverters);
	}

	private List<HttpMessageConverter<?>> getDefaultConverters() {
		List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
		converters.addAll(new WebMvcConfigurationSupport() {
			public List<HttpMessageConverter<?>> defaultMessageConverters() {
				return super.getMessageConverters();
			}
		}.defaultMessageConverters());
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

	private <E> int indexOfItemClass(List<E> list, E item) {
		Class<? extends Object> itemClass = item.getClass();
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).getClass().isAssignableFrom(itemClass)) {
				return i;
			}
		}
		return -1;
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

	/**
	 * Set the converters to use, replacing any existing values. This method can only be
	 * called before the bean has been initialized.
	 * @param converters the converters to set
	 */
	public void setConverters(List<HttpMessageConverter<?>> converters) {
		Assert.state(!this.initialized, "Unable to set converters once initialized");
		this.converters = converters;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.initialized = true;
		this.converters = Collections.unmodifiableList(this.converters);
	}
}
