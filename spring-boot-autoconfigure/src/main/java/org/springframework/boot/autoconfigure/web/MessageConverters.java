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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.xml.AbstractXmlHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * Convenient utility for adding and merging additional {@link HttpMessageConverter} in an
 * application context. It also modifies the default converters a bit (putting XML
 * converters at the back of the list if they are present).
 * 
 * @author Dave Syer
 */
public class MessageConverters {

	private List<HttpMessageConverter<?>> defaults;

	private List<HttpMessageConverter<?>> overrides;

	private Object lock = new Object();

	private List<HttpMessageConverter<?>> converters;

	public MessageConverters() {
		this(Collections.<HttpMessageConverter<?>> emptyList());
	}

	public MessageConverters(Collection<HttpMessageConverter<?>> overrides) {
		this.overrides = new ArrayList<HttpMessageConverter<?>>(overrides);
	}

	public List<HttpMessageConverter<?>> getMessageConverters() {
		if (this.converters == null) {
			synchronized (this.lock) {
				if (this.converters == null) {
					this.converters = new ArrayList<HttpMessageConverter<?>>();
					getDefaultMessageConverters(); // ensure they are available
					for (HttpMessageConverter<?> fallback : this.defaults) {
						boolean overridden = false;
						for (HttpMessageConverter<?> converter : this.overrides) {
							if (fallback.getClass()
									.isAssignableFrom(converter.getClass())) {
								if (!this.converters.contains(converter)) {
									this.converters.add(converter);
									overridden = true;
								}
							}
						}
						if (!overridden) {
							this.converters.add(fallback);
						}
					}
				}
			}
		}
		return this.converters;
	}

	public List<HttpMessageConverter<?>> getDefaultMessageConverters() {
		if (this.defaults == null) {
			synchronized (this.lock) {
				if (this.defaults == null) {
					this.defaults = new ArrayList<HttpMessageConverter<?>>();
					this.defaults.addAll(new WebMvcConfigurationSupport() {
						public List<HttpMessageConverter<?>> defaultMessageConverters() {
							return super.getMessageConverters();
						}
					}.defaultMessageConverters());
					List<HttpMessageConverter<?>> xmls = new ArrayList<HttpMessageConverter<?>>();
					for (HttpMessageConverter<?> converter : this.defaults) {
						// Shift XML converters to the back of the list so they only get
						// used if nothing else works...
						if (converter instanceof AbstractXmlHttpMessageConverter) {
							xmls.add(converter);
						}
					}
					this.defaults.removeAll(xmls);
					this.defaults.addAll(xmls);
				}
			}
		}
		return Collections.unmodifiableList(this.defaults);
	}

}