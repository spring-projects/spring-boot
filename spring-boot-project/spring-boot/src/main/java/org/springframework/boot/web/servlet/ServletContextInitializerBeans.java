/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.web.servlet;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.Filter;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.Servlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * A collection {@link ServletContextInitializer}s obtained from a
 * {@link ListableBeanFactory}. Includes all {@link ServletContextInitializer} beans and
 * also adapts {@link Servlet}, {@link Filter} and certain {@link EventListener} beans.
 * <p>
 * Items are sorted so that adapted beans are top ({@link Servlet}, {@link Filter} then
 * {@link EventListener}) and direct {@link ServletContextInitializer} beans are at the
 * end. Further sorting is applied within these groups using the
 * {@link AnnotationAwareOrderComparator}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Brian Clozel
 * @since 1.4.0
 */
public class ServletContextInitializerBeans extends AbstractCollection<ServletContextInitializer> {

	private static final String DISPATCHER_SERVLET_NAME = "dispatcherServlet";

	private static final Log logger = LogFactory.getLog(ServletContextInitializerBeans.class);

	/**
	 * Seen bean instances or bean names.
	 */
	private final Seen seen = new Seen();

	private final MultiValueMap<Class<?>, ServletContextInitializer> initializers;

	private final List<Class<? extends ServletContextInitializer>> initializerTypes;

	private final List<ServletContextInitializer> sortedList;

	/**
	 * Constructs a new instance of ServletContextInitializerBeans with the specified
	 * parameters.
	 * @param beanFactory the ListableBeanFactory used to retrieve beans
	 * @param initializerTypes the types of ServletContextInitializers to include
	 */
	@SafeVarargs
	@SuppressWarnings("varargs")
	public ServletContextInitializerBeans(ListableBeanFactory beanFactory,
			Class<? extends ServletContextInitializer>... initializerTypes) {
		this.initializers = new LinkedMultiValueMap<>();
		this.initializerTypes = (initializerTypes.length != 0) ? Arrays.asList(initializerTypes)
				: Collections.singletonList(ServletContextInitializer.class);
		addServletContextInitializerBeans(beanFactory);
		addAdaptableBeans(beanFactory);
		this.sortedList = this.initializers.values()
			.stream()
			.flatMap((value) -> value.stream().sorted(AnnotationAwareOrderComparator.INSTANCE))
			.toList();
		logMappings(this.initializers);
	}

	/**
	 * Adds the ServletContextInitializer beans to the given bean factory.
	 * @param beanFactory the bean factory to add the ServletContextInitializer beans to
	 */
	private void addServletContextInitializerBeans(ListableBeanFactory beanFactory) {
		for (Class<? extends ServletContextInitializer> initializerType : this.initializerTypes) {
			for (Entry<String, ? extends ServletContextInitializer> initializerBean : getOrderedBeansOfType(beanFactory,
					initializerType)) {
				addServletContextInitializerBean(initializerBean.getKey(), initializerBean.getValue(), beanFactory);
			}
		}
	}

	/**
	 * Adds a ServletContextInitializer bean to the list of beans.
	 * @param beanName the name of the bean
	 * @param initializer the ServletContextInitializer to be added
	 * @param beanFactory the ListableBeanFactory to retrieve the bean from
	 */
	private void addServletContextInitializerBean(String beanName, ServletContextInitializer initializer,
			ListableBeanFactory beanFactory) {
		if (initializer instanceof ServletRegistrationBean) {
			Servlet source = ((ServletRegistrationBean<?>) initializer).getServlet();
			addServletContextInitializerBean(Servlet.class, beanName, initializer, beanFactory, source);
		}
		else if (initializer instanceof FilterRegistrationBean) {
			Filter source = ((FilterRegistrationBean<?>) initializer).getFilter();
			addServletContextInitializerBean(Filter.class, beanName, initializer, beanFactory, source);
		}
		else if (initializer instanceof DelegatingFilterProxyRegistrationBean) {
			String source = ((DelegatingFilterProxyRegistrationBean) initializer).getTargetBeanName();
			addServletContextInitializerBean(Filter.class, beanName, initializer, beanFactory, source);
		}
		else if (initializer instanceof ServletListenerRegistrationBean) {
			EventListener source = ((ServletListenerRegistrationBean<?>) initializer).getListener();
			addServletContextInitializerBean(EventListener.class, beanName, initializer, beanFactory, source);
		}
		else {
			addServletContextInitializerBean(ServletContextInitializer.class, beanName, initializer, beanFactory,
					initializer);
		}
	}

	/**
	 * Adds a ServletContextInitializer bean to the list of initializers.
	 * @param type the type of the bean
	 * @param beanName the name of the bean
	 * @param initializer the ServletContextInitializer instance
	 * @param beanFactory the ListableBeanFactory instance
	 * @param source the underlying source object
	 */
	private void addServletContextInitializerBean(Class<?> type, String beanName, ServletContextInitializer initializer,
			ListableBeanFactory beanFactory, Object source) {
		this.initializers.add(type, initializer);
		if (source != null) {
			// Mark the underlying source as seen in case it wraps an existing bean
			this.seen.add(type, source);
		}
		if (logger.isTraceEnabled()) {
			String resourceDescription = getResourceDescription(beanName, beanFactory);
			int order = getOrder(initializer);
			logger.trace("Added existing " + type.getSimpleName() + " initializer bean '" + beanName + "'; order="
					+ order + ", resource=" + resourceDescription);
		}
	}

	/**
	 * Retrieves the resource description for a given bean name from the provided bean
	 * factory.
	 * @param beanName the name of the bean to retrieve the resource description for
	 * @param beanFactory the bean factory to retrieve the bean definition from
	 * @return the resource description of the bean, or "unknown" if the bean factory is
	 * not an instance of BeanDefinitionRegistry
	 */
	private String getResourceDescription(String beanName, ListableBeanFactory beanFactory) {
		if (beanFactory instanceof BeanDefinitionRegistry registry) {
			return registry.getBeanDefinition(beanName).getResourceDescription();
		}
		return "unknown";
	}

	/**
	 * Adds adaptable beans to the given bean factory.
	 * @param beanFactory the bean factory to add the adaptable beans to
	 */
	@SuppressWarnings("unchecked")
	protected void addAdaptableBeans(ListableBeanFactory beanFactory) {
		MultipartConfigElement multipartConfig = getMultipartConfig(beanFactory);
		addAsRegistrationBean(beanFactory, Servlet.class, new ServletRegistrationBeanAdapter(multipartConfig));
		addAsRegistrationBean(beanFactory, Filter.class, new FilterRegistrationBeanAdapter());
		for (Class<?> listenerType : ServletListenerRegistrationBean.getSupportedTypes()) {
			addAsRegistrationBean(beanFactory, EventListener.class, (Class<EventListener>) listenerType,
					new ServletListenerRegistrationBeanAdapter());
		}
	}

	/**
	 * Retrieves the MultipartConfigElement from the given ListableBeanFactory.
	 * @param beanFactory the ListableBeanFactory to retrieve the MultipartConfigElement
	 * from
	 * @return the MultipartConfigElement retrieved from the beanFactory, or null if none
	 * found
	 */
	private MultipartConfigElement getMultipartConfig(ListableBeanFactory beanFactory) {
		List<Entry<String, MultipartConfigElement>> beans = getOrderedBeansOfType(beanFactory,
				MultipartConfigElement.class);
		return beans.isEmpty() ? null : beans.get(0).getValue();
	}

	/**
	 * Adds a registration bean to the given bean factory.
	 * @param beanFactory the listable bean factory to add the registration bean to
	 * @param type the class type of the registration bean
	 * @param adapter the registration bean adapter
	 * @param <T> the type of the registration bean
	 */
	protected <T> void addAsRegistrationBean(ListableBeanFactory beanFactory, Class<T> type,
			RegistrationBeanAdapter<T> adapter) {
		addAsRegistrationBean(beanFactory, type, type, adapter);
	}

	/**
	 * Adds the beans of type {@code beanType} as registration beans to the list of
	 * initializers.
	 * @param beanFactory the bean factory to retrieve the beans from
	 * @param type the type of the registration beans
	 * @param beanType the type of the beans to be added as registration beans
	 * @param adapter the adapter to create the registration beans
	 * @param <T> the type of the registration beans
	 * @param <B> the type of the beans to be added as registration beans, which must
	 * extend {@code T}
	 */
	private <T, B extends T> void addAsRegistrationBean(ListableBeanFactory beanFactory, Class<T> type,
			Class<B> beanType, RegistrationBeanAdapter<T> adapter) {
		List<Map.Entry<String, B>> entries = getOrderedBeansOfType(beanFactory, beanType, this.seen);
		for (Entry<String, B> entry : entries) {
			String beanName = entry.getKey();
			B bean = entry.getValue();
			if (this.seen.add(type, bean)) {
				// One that we haven't already seen
				RegistrationBean registration = adapter.createRegistrationBean(beanName, bean, entries.size());
				int order = getOrder(bean);
				registration.setOrder(order);
				this.initializers.add(type, registration);
				if (logger.isTraceEnabled()) {
					logger.trace("Created " + type.getSimpleName() + " initializer for bean '" + beanName + "'; order="
							+ order + ", resource=" + getResourceDescription(beanName, beanFactory));
				}
			}
		}
	}

	/**
	 * Returns the order of the given value based on the annotation aware order
	 * comparator.
	 * @param value the value for which the order needs to be determined
	 * @return the order of the value
	 */
	private int getOrder(Object value) {
		return new AnnotationAwareOrderComparator() {
			@Override
			public int getOrder(Object obj) {
				return super.getOrder(obj);
			}
		}.getOrder(value);
	}

	/**
	 * Retrieves a list of beans of the specified type from the given bean factory,
	 * ordered by their priority.
	 * @param beanFactory the bean factory to retrieve beans from
	 * @param type the type of beans to retrieve
	 * @param <T> the type parameter for the beans
	 * @return a list of beans of the specified type, ordered by their priority
	 */
	private <T> List<Entry<String, T>> getOrderedBeansOfType(ListableBeanFactory beanFactory, Class<T> type) {
		return getOrderedBeansOfType(beanFactory, type, Seen.empty());
	}

	/**
	 * Retrieves a list of beans of the specified type from the given bean factory,
	 * ordered based on their annotation-aware order.
	 * @param beanFactory The bean factory to retrieve the beans from.
	 * @param type The type of beans to retrieve.
	 * @param seen A Seen object to keep track of already seen beans.
	 * @param <T> The type of beans to retrieve.
	 * @return A list of beans of the specified type, ordered based on their
	 * annotation-aware order.
	 */
	private <T> List<Entry<String, T>> getOrderedBeansOfType(ListableBeanFactory beanFactory, Class<T> type,
			Seen seen) {
		String[] names = beanFactory.getBeanNamesForType(type, true, false);
		Map<String, T> map = new LinkedHashMap<>();
		for (String name : names) {
			if (!seen.contains(type, name) && !ScopedProxyUtils.isScopedTarget(name)) {
				T bean = beanFactory.getBean(name, type);
				if (!seen.contains(type, bean)) {
					map.put(name, bean);
				}
			}
		}
		List<Entry<String, T>> beans = new ArrayList<>(map.entrySet());
		beans.sort((o1, o2) -> AnnotationAwareOrderComparator.INSTANCE.compare(o1.getValue(), o2.getValue()));
		return beans;
	}

	/**
	 * Logs the mappings of filters and servlets in the given {@link MultiValueMap} of
	 * initializers.
	 * @param initializers the {@link MultiValueMap} containing the initializers
	 */
	private void logMappings(MultiValueMap<Class<?>, ServletContextInitializer> initializers) {
		if (logger.isDebugEnabled()) {
			logMappings("filters", initializers, Filter.class, FilterRegistrationBean.class);
			logMappings("servlets", initializers, Servlet.class, ServletRegistrationBean.class);
		}
	}

	/**
	 * Logs the mappings for a given name, initializers, type, and registration type.
	 * @param name the name of the mapping
	 * @param initializers the initializers containing the mappings
	 * @param type the type of the mapping
	 * @param registrationType the registration type of the mapping
	 */
	private void logMappings(String name, MultiValueMap<Class<?>, ServletContextInitializer> initializers,
			Class<?> type, Class<? extends RegistrationBean> registrationType) {
		List<ServletContextInitializer> registrations = new ArrayList<>();
		registrations.addAll(initializers.getOrDefault(registrationType, Collections.emptyList()));
		registrations.addAll(initializers.getOrDefault(type, Collections.emptyList()));
		String info = registrations.stream().map(Object::toString).collect(Collectors.joining(", "));
		logger.debug("Mapping " + name + ": " + info);
	}

	/**
	 * Returns an iterator over the elements in this ServletContextInitializerBeans object
	 * in proper sequence.
	 * @return an iterator over the elements in this ServletContextInitializerBeans object
	 * in proper sequence
	 */
	@Override
	public Iterator<ServletContextInitializer> iterator() {
		return this.sortedList.iterator();
	}

	/**
	 * Returns the size of the sorted list.
	 * @return the size of the sorted list
	 */
	@Override
	public int size() {
		return this.sortedList.size();
	}

	/**
	 * Adapter to convert a given Bean type into a {@link RegistrationBean} (and hence a
	 * {@link ServletContextInitializer}).
	 *
	 * @param <T> the type of the Bean to adapt
	 */
	@FunctionalInterface
	protected interface RegistrationBeanAdapter<T> {

		RegistrationBean createRegistrationBean(String name, T source, int totalNumberOfSourceBeans);

	}

	/**
	 * {@link RegistrationBeanAdapter} for {@link Servlet} beans.
	 */
	private static class ServletRegistrationBeanAdapter implements RegistrationBeanAdapter<Servlet> {

		private final MultipartConfigElement multipartConfig;

		/**
		 * Constructs a new ServletRegistrationBeanAdapter with the specified
		 * MultipartConfigElement.
		 * @param multipartConfig the MultipartConfigElement to be set for this
		 * ServletRegistrationBeanAdapter
		 */
		ServletRegistrationBeanAdapter(MultipartConfigElement multipartConfig) {
			this.multipartConfig = multipartConfig;
		}

		/**
		 * Creates a new RegistrationBean for the given name and source Servlet.
		 * @param name the name of the Servlet
		 * @param source the source Servlet
		 * @param totalNumberOfSourceBeans the total number of source beans
		 * @return the created RegistrationBean
		 */
		@Override
		public RegistrationBean createRegistrationBean(String name, Servlet source, int totalNumberOfSourceBeans) {
			String url = (totalNumberOfSourceBeans != 1) ? "/" + name + "/" : "/";
			if (name.equals(DISPATCHER_SERVLET_NAME)) {
				url = "/"; // always map the main dispatcherServlet to "/"
			}
			ServletRegistrationBean<Servlet> bean = new ServletRegistrationBean<>(source, url);
			bean.setName(name);
			bean.setMultipartConfig(this.multipartConfig);
			return bean;
		}

	}

	/**
	 * {@link RegistrationBeanAdapter} for {@link Filter} beans.
	 */
	private static final class FilterRegistrationBeanAdapter implements RegistrationBeanAdapter<Filter> {

		/**
		 * Creates a new RegistrationBean with the specified name, source Filter, and
		 * total number of source beans.
		 * @param name The name of the RegistrationBean.
		 * @param source The source Filter for the RegistrationBean.
		 * @param totalNumberOfSourceBeans The total number of source beans.
		 * @return The created RegistrationBean.
		 */
		@Override
		public RegistrationBean createRegistrationBean(String name, Filter source, int totalNumberOfSourceBeans) {
			FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>(source);
			bean.setName(name);
			return bean;
		}

	}

	/**
	 * {@link RegistrationBeanAdapter} for certain {@link EventListener} beans.
	 */
	private static final class ServletListenerRegistrationBeanAdapter
			implements RegistrationBeanAdapter<EventListener> {

		/**
		 * Creates a new RegistrationBean with the given name, source, and total number of
		 * source beans.
		 * @param name the name of the RegistrationBean
		 * @param source the EventListener source
		 * @param totalNumberOfSourceBeans the total number of source beans
		 * @return the created RegistrationBean
		 */
		@Override
		public RegistrationBean createRegistrationBean(String name, EventListener source,
				int totalNumberOfSourceBeans) {
			return new ServletListenerRegistrationBean<>(source);
		}

	}

	/**
	 * Seen class.
	 */
	private static final class Seen {

		private final Map<Class<?>, Set<Object>> seen = new HashMap<>();

		/**
		 * Adds an object of the specified type to the set of seen objects.
		 * @param type the class type of the object
		 * @param object the object to be added
		 * @return {@code true} if the object was successfully added, {@code false} if the
		 * object is already present
		 */
		boolean add(Class<?> type, Object object) {
			if (contains(type, object)) {
				return false;
			}
			return this.seen.computeIfAbsent(type, (ignore) -> new HashSet<>()).add(object);
		}

		/**
		 * Checks if the given object is contained in the seen map of the specified type.
		 * @param type the type of the object to be checked
		 * @param object the object to be checked for containment
		 * @return true if the object is contained in the seen map, false otherwise
		 */
		boolean contains(Class<?> type, Object object) {
			if (this.seen.isEmpty()) {
				return false;
			}
			// If it has been directly seen, or the implemented ServletContextInitializer
			// has been seen already
			if (type != ServletContextInitializer.class
					&& this.seen.getOrDefault(type, Collections.emptySet()).contains(object)) {
				return true;
			}
			return this.seen.getOrDefault(ServletContextInitializer.class, Collections.emptySet()).contains(object);
		}

		/**
		 * Creates and returns an empty Seen object.
		 * @return an empty Seen object
		 */
		static Seen empty() {
			return new Seen();
		}

	}

}
