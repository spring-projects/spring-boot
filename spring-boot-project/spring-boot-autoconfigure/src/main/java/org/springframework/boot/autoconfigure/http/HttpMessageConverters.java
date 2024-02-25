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

package org.springframework.boot.autoconfigure.http;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.AbstractXmlHttpMessageConverter;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
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
 * {@link WebMvcConfigurationSupport}) with some slight re-ordering to put XML converters
 * at the back of the list.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 2.0.0
 * @see #HttpMessageConverters(HttpMessageConverter...)
 * @see #HttpMessageConverters(Collection)
 * @see #getConverters()
 */
public class HttpMessageConverters implements Iterable<HttpMessageConverter<?>> {

	private static final List<Class<?>> NON_REPLACING_CONVERTERS;

	static {
		List<Class<?>> nonReplacingConverters = new ArrayList<>();
		addClassIfExists(nonReplacingConverters,
				"org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter");
		NON_REPLACING_CONVERTERS = Collections.unmodifiableList(nonReplacingConverters);
	}

	private static final Map<Class<?>, Class<?>> EQUIVALENT_CONVERTERS;

	static {
		Map<Class<?>, Class<?>> equivalentConverters = new HashMap<>();
		putIfExists(equivalentConverters, "org.springframework.http.converter.json.MappingJackson2HttpMessageConverter",
				"org.springframework.http.converter.json.GsonHttpMessageConverter");
		EQUIVALENT_CONVERTERS = Collections.unmodifiableMap(equivalentConverters);
	}

	private final List<HttpMessageConverter<?>> converters;

	/**
	 * Create a new {@link HttpMessageConverters} instance with the specified additional
	 * converters.
	 * @param additionalConverters additional converters to be added. Items are added just
	 * before any default converter of the same type (or at the front of the list if no
	 * default converter is found). The {@link #postProcessConverters(List)} method can be
	 * used for further converter manipulation.
	 */
	public HttpMessageConverters(HttpMessageConverter<?>... additionalConverters) {
		this(Arrays.asList(additionalConverters));
	}

	/**
	 * Create a new {@link HttpMessageConverters} instance with the specified additional
	 * converters.
	 * @param additionalConverters additional converters to be added. Items are added just
	 * before any default converter of the same type (or at the front of the list if no
	 * default converter is found). The {@link #postProcessConverters(List)} method can be
	 * used for further converter manipulation.
	 */
	public HttpMessageConverters(Collection<HttpMessageConverter<?>> additionalConverters) {
		this(true, additionalConverters);
	}

	/**
	 * Create a new {@link HttpMessageConverters} instance with the specified converters.
	 * @param addDefaultConverters if default converters should be added
	 * @param converters converters to be added. Items are added just before any default
	 * converter of the same type (or at the front of the list if no default converter is
	 * found). The {@link #postProcessConverters(List)} method can be used for further
	 * converter manipulation.
	 */
	public HttpMessageConverters(boolean addDefaultConverters, Collection<HttpMessageConverter<?>> converters) {
		List<HttpMessageConverter<?>> combined = getCombinedConverters(converters,
				addDefaultConverters ? getDefaultConverters() : Collections.emptyList());
		combined = postProcessConverters(combined);
		this.converters = Collections.unmodifiableList(combined);
	}

	/**
     * Combines the given collection of converters with the default converters and returns the combined list.
     * 
     * @param converters the collection of converters to be combined
     * @param defaultConverters the default converters to be combined with the given converters
     * @return the combined list of converters
     */
    private List<HttpMessageConverter<?>> getCombinedConverters(Collection<HttpMessageConverter<?>> converters,
			List<HttpMessageConverter<?>> defaultConverters) {
		List<HttpMessageConverter<?>> combined = new ArrayList<>();
		List<HttpMessageConverter<?>> processing = new ArrayList<>(converters);
		for (HttpMessageConverter<?> defaultConverter : defaultConverters) {
			Iterator<HttpMessageConverter<?>> iterator = processing.iterator();
			while (iterator.hasNext()) {
				HttpMessageConverter<?> candidate = iterator.next();
				if (isReplacement(defaultConverter, candidate)) {
					combined.add(candidate);
					iterator.remove();
				}
			}
			combined.add(defaultConverter);
			if (defaultConverter instanceof AllEncompassingFormHttpMessageConverter allEncompassingConverter) {
				configurePartConverters(allEncompassingConverter, converters);
			}
		}
		combined.addAll(0, processing);
		return combined;
	}

	/**
     * Checks if the given candidate HttpMessageConverter can replace the defaultConverter.
     * 
     * @param defaultConverter the default HttpMessageConverter
     * @param candidate the candidate HttpMessageConverter
     * @return true if the candidate can replace the defaultConverter, false otherwise
     */
    private boolean isReplacement(HttpMessageConverter<?> defaultConverter, HttpMessageConverter<?> candidate) {
		for (Class<?> nonReplacingConverter : NON_REPLACING_CONVERTERS) {
			if (nonReplacingConverter.isInstance(candidate)) {
				return false;
			}
		}
		Class<?> converterClass = defaultConverter.getClass();
		if (ClassUtils.isAssignableValue(converterClass, candidate)) {
			return true;
		}
		Class<?> equivalentClass = EQUIVALENT_CONVERTERS.get(converterClass);
		return equivalentClass != null && ClassUtils.isAssignableValue(equivalentClass, candidate);
	}

	/**
     * Configures the part converters for the given form converter and collection of converters.
     * 
     * @param formConverter The form converter to configure the part converters for.
     * @param converters The collection of converters to combine with the part converters.
     */
    private void configurePartConverters(AllEncompassingFormHttpMessageConverter formConverter,
			Collection<HttpMessageConverter<?>> converters) {
		List<HttpMessageConverter<?>> partConverters = formConverter.getPartConverters();
		List<HttpMessageConverter<?>> combinedConverters = getCombinedConverters(converters, partConverters);
		combinedConverters = postProcessPartConverters(combinedConverters);
		formConverter.setPartConverters(combinedConverters);
	}

	/**
	 * Method that can be used to post-process the {@link HttpMessageConverter} list
	 * before it is used.
	 * @param converters a mutable list of the converters that will be used.
	 * @return the final converts list to use
	 */
	protected List<HttpMessageConverter<?>> postProcessConverters(List<HttpMessageConverter<?>> converters) {
		return converters;
	}

	/**
	 * Method that can be used to post-process the {@link HttpMessageConverter} list
	 * before it is used to configure the part converters of
	 * {@link AllEncompassingFormHttpMessageConverter}.
	 * @param converters a mutable list of the converters that will be used.
	 * @return the final converts list to use
	 * @since 1.3.0
	 */
	protected List<HttpMessageConverter<?>> postProcessPartConverters(List<HttpMessageConverter<?>> converters) {
		return converters;
	}

	/**
     * Returns the default list of HTTP message converters.
     * 
     * @return the list of HTTP message converters
     */
    private List<HttpMessageConverter<?>> getDefaultConverters() {
		List<HttpMessageConverter<?>> converters = new ArrayList<>();
		if (ClassUtils.isPresent("org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport",
				null)) {
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

	/**
     * Reorders the XML converters to the end of the list of converters.
     * 
     * @param converters the list of HTTP message converters
     */
    private void reorderXmlConvertersToEnd(List<HttpMessageConverter<?>> converters) {
		List<HttpMessageConverter<?>> xml = new ArrayList<>();
		for (Iterator<HttpMessageConverter<?>> iterator = converters.iterator(); iterator.hasNext();) {
			HttpMessageConverter<?> converter = iterator.next();
			if ((converter instanceof AbstractXmlHttpMessageConverter)
					|| (converter instanceof MappingJackson2XmlHttpMessageConverter)) {
				xml.add(converter);
				iterator.remove();
			}
		}
		converters.addAll(xml);
	}

	/**
     * Returns an iterator over the HttpMessageConverters in this class.
     *
     * @return an iterator over the HttpMessageConverters in this class
     */
    @Override
	public Iterator<HttpMessageConverter<?>> iterator() {
		return getConverters().iterator();
	}

	/**
	 * Return an immutable list of the converters in the order that they will be
	 * registered.
	 * @return the converters
	 */
	public List<HttpMessageConverter<?>> getConverters() {
		return this.converters;
	}

	/**
     * Adds a class to the given list if it exists.
     * 
     * @param list the list to add the class to
     * @param className the name of the class to add
     */
    private static void addClassIfExists(List<Class<?>> list, String className) {
		try {
			list.add(Class.forName(className));
		}
		catch (ClassNotFoundException | NoClassDefFoundError ex) {
			// Ignore
		}
	}

	/**
     * Puts the key-value pair into the given map if both the key and value classes exist.
     * 
     * @param map              the map to put the key-value pair into
     * @param keyClassName     the fully qualified name of the key class
     * @param valueClassName   the fully qualified name of the value class
     */
    private static void putIfExists(Map<Class<?>, Class<?>> map, String keyClassName, String valueClassName) {
		try {
			map.put(Class.forName(keyClassName), Class.forName(valueClassName));
		}
		catch (ClassNotFoundException | NoClassDefFoundError ex) {
			// Ignore
		}
	}

}
