/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.context.embedded;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.Filter;
import javax.servlet.MultipartConfigElement;
import javax.servlet.Servlet;

import org.springframework.beans.factory.ListableBeanFactory;
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
 *
 * @author Dave Syer
 * @author Phillip Webb
 */
class ServletContextInitializerBeans extends
		AbstractCollection<ServletContextInitializer> {

	static final String DISPATCHER_SERVLET_NAME = "dispatcherServlet";

	private final Set<Object> seen = new HashSet<Object>();

	private final MultiValueMap<Class<?>, ServletContextInitializer> initializers;

	private List<ServletContextInitializer> sortedList;

	public ServletContextInitializerBeans(ListableBeanFactory beanFactory) {
		this.initializers = new LinkedMultiValueMap<Class<?>, ServletContextInitializer>();
		addServletContextInitializerBeans(beanFactory);
		addAdaptableBeans(beanFactory);
		List<ServletContextInitializer> sortedInitializers = new ArrayList<ServletContextInitializer>();
		for (Map.Entry<?, List<ServletContextInitializer>> entry : this.initializers
				.entrySet()) {
			AnnotationAwareOrderComparator.sort(entry.getValue());
			sortedInitializers.addAll(entry.getValue());
		}
		this.sortedList = Collections.unmodifiableList(sortedInitializers);
	}

	private void addServletContextInitializerBeans(ListableBeanFactory beanFactory) {
		for (Entry<String, ServletContextInitializer> initializerBean : getOrderedBeansOfType(
				beanFactory, ServletContextInitializer.class)) {
			addServletContextInitializerBean(initializerBean.getValue());
		}
	}

	private void addServletContextInitializerBean(ServletContextInitializer initializer) {
		if (initializer instanceof ServletRegistrationBean) {
			addServletContextInitializerBean(Servlet.class, initializer,
					((ServletRegistrationBean) initializer).getServlet());
		}
		else if (initializer instanceof FilterRegistrationBean) {
			addServletContextInitializerBean(Filter.class, initializer,
					((FilterRegistrationBean) initializer).getFilter());
		}
		else if (initializer instanceof ServletListenerRegistrationBean) {
			addServletContextInitializerBean(EventListener.class, initializer,
					((ServletListenerRegistrationBean<?>) initializer).getListener());
		}
		else {
			addServletContextInitializerBean(ServletContextInitializer.class,
					initializer, null);
		}
	}

	private void addServletContextInitializerBean(Class<?> type,
			ServletContextInitializer initializer, Object source) {
		this.initializers.add(type, initializer);
		if (source != null) {
			// Mark the underlying source as seen in case it wraps an existing bean
			this.seen.add(source);
		}
	}

	@SuppressWarnings("unchecked")
	private void addAdaptableBeans(ListableBeanFactory beanFactory) {
		MultipartConfigElement multipartConfig = getMultipartConfig(beanFactory);
		addAsRegistrationBean(beanFactory, Servlet.class,
				new ServletRegistrationBeanAdapter(multipartConfig));
		addAsRegistrationBean(beanFactory, Filter.class,
				new FilterRegistrationBeanAdapter());
		for (Class<?> listenerType : ServletListenerRegistrationBean.getSupportedTypes()) {
			addAsRegistrationBean(beanFactory, EventListener.class,
					(Class<EventListener>) listenerType,
					new ServletListenerRegistrationBeanAdapter());
		}
	}

	private MultipartConfigElement getMultipartConfig(ListableBeanFactory beanFactory) {
		List<Entry<String, MultipartConfigElement>> beans = getOrderedBeansOfType(
				beanFactory, MultipartConfigElement.class);
		return (beans.isEmpty() ? null : beans.get(0).getValue());
	}

	private <T> void addAsRegistrationBean(ListableBeanFactory beanFactory,
			Class<T> type, RegistrationBeanAdapter<T> adapter) {
		addAsRegistrationBean(beanFactory, type, type, adapter);
	}

	private <T, B extends T> void addAsRegistrationBean(ListableBeanFactory beanFactory,
			Class<T> type, Class<B> beanType, RegistrationBeanAdapter<T> adapter) {
		List<Map.Entry<String, B>> beans = getOrderedBeansOfType(beanFactory, beanType);
		for (Entry<String, B> bean : beans) {
			if (this.seen.add(bean.getValue())) {
				// One that we haven't already seen
				RegistrationBean registration = adapter.createRegistrationBean(
						bean.getKey(), bean.getValue(), beans.size());
				registration.setName(bean.getKey());
				registration.setOrder(getOrder(bean.getValue()));
				this.initializers.add(type, registration);
			}
		}
	}

	private int getOrder(Object value) {
		return new AnnotationAwareOrderComparator() {
			@Override
			public int getOrder(Object obj) {
				return super.getOrder(obj);
			}
		}.getOrder(value);
	}

	private <T> List<Entry<String, T>> getOrderedBeansOfType(
			ListableBeanFactory beanFactory, Class<T> type) {
		List<Entry<String, T>> beans = new ArrayList<Entry<String, T>>();
		Comparator<Entry<String, T>> comparator = new Comparator<Entry<String, T>>() {
			@Override
			public int compare(Entry<String, T> o1, Entry<String, T> o2) {
				return AnnotationAwareOrderComparator.INSTANCE.compare(o1.getValue(),
						o2.getValue());
			}
		};
		String[] names = beanFactory.getBeanNamesForType(type, true, false);
		Map<String, T> map = new LinkedHashMap<String, T>();
		for (String name : names) {
			map.put(name, beanFactory.getBean(name, type));
		}
		beans.addAll(map.entrySet());
		Collections.sort(beans, comparator);
		return beans;
	}

	@Override
	public Iterator<ServletContextInitializer> iterator() {
		return this.sortedList.iterator();
	}

	@Override
	public int size() {
		return this.sortedList.size();
	}

	/**
	 * Adapter to convert a given Bean type into a {@link RegistrationBean} (and hence a
	 * {@link ServletContextInitializer}.
	 */
	private static interface RegistrationBeanAdapter<T> {

		RegistrationBean createRegistrationBean(String name, T source,
				int totalNumberOfSourceBeans);

	}

	/**
	 * {@link RegistrationBeanAdapter} for {@link Servlet} beans.
	 */
	private static class ServletRegistrationBeanAdapter implements
			RegistrationBeanAdapter<Servlet> {

		private final MultipartConfigElement multipartConfig;

		public ServletRegistrationBeanAdapter(MultipartConfigElement multipartConfig) {
			this.multipartConfig = multipartConfig;
		}

		@Override
		public RegistrationBean createRegistrationBean(String name, Servlet source,
				int totalNumberOfSourceBeans) {
			String url = (totalNumberOfSourceBeans == 1 ? "/" : "/" + name + "/");
			if (name.equals(DISPATCHER_SERVLET_NAME)) {
				url = "/"; // always map the main dispatcherServlet to "/"
			}
			ServletRegistrationBean bean = new ServletRegistrationBean(source, url);
			bean.setMultipartConfig(this.multipartConfig);
			return bean;
		}

	}

	/**
	 * {@link RegistrationBeanAdapter} for {@link Filter} beans.
	 */
	private static class FilterRegistrationBeanAdapter implements
			RegistrationBeanAdapter<Filter> {

		@Override
		public RegistrationBean createRegistrationBean(String name, Filter source,
				int totalNumberOfSourceBeans) {
			return new FilterRegistrationBean(source);
		}

	}

	/**
	 * {@link RegistrationBeanAdapter} for certain {@link EventListener} beans.
	 */
	private static class ServletListenerRegistrationBeanAdapter implements
			RegistrationBeanAdapter<EventListener> {

		@Override
		public RegistrationBean createRegistrationBean(String name, EventListener source,
				int totalNumberOfSourceBeans) {
			return new ServletListenerRegistrationBean<EventListener>(source);
		}

	}

}
