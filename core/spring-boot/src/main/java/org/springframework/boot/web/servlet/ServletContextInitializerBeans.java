/*
 * Copyright 2012-present the original author or authors.
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
import java.util.EnumSet;
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
import jakarta.servlet.annotation.WebInitParam;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.annotation.Order;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

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
 * @author Moritz Halbritter
 * @author Daeho Kwon
 * @author Dmytro Danilenkov
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

	private void addServletContextInitializerBeans(ListableBeanFactory beanFactory) {
		for (Class<? extends ServletContextInitializer> initializerType : this.initializerTypes) {
			for (Entry<String, ? extends ServletContextInitializer> initializerBean : getOrderedBeansOfType(beanFactory,
					initializerType)) {
				addServletContextInitializerBean(initializerBean.getKey(), initializerBean.getValue(), beanFactory);
			}
		}
	}

	private void addServletContextInitializerBean(String beanName, ServletContextInitializer initializer,
			ListableBeanFactory beanFactory) {
		if (initializer instanceof ServletRegistrationBean<?> servletRegistrationBean) {
			Servlet source = servletRegistrationBean.getServlet();
			addServletContextInitializerBean(Servlet.class, beanName, servletRegistrationBean, beanFactory, source);
		}
		else if (initializer instanceof FilterRegistrationBean<?> filterRegistrationBean) {
			Filter source = filterRegistrationBean.getFilter();
			addServletContextInitializerBean(Filter.class, beanName, filterRegistrationBean, beanFactory, source);
		}
		else if (initializer instanceof DelegatingFilterProxyRegistrationBean registrationBean) {
			String source = registrationBean.getTargetBeanName();
			addServletContextInitializerBean(Filter.class, beanName, registrationBean, beanFactory, source);
		}
		else if (initializer instanceof ServletListenerRegistrationBean<?> registrationBean) {
			EventListener source = registrationBean.getListener();
			addServletContextInitializerBean(EventListener.class, beanName, registrationBean, beanFactory, source);
		}
		else {
			addServletContextInitializerBean(ServletContextInitializer.class, beanName, initializer, beanFactory,
					initializer);
		}
	}

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

	private String getResourceDescription(String beanName, ListableBeanFactory beanFactory) {
		if (beanFactory instanceof BeanDefinitionRegistry registry) {
			return registry.getBeanDefinition(beanName).getResourceDescription();
		}
		return "unknown";
	}

	@SuppressWarnings("unchecked")
	protected void addAdaptableBeans(ListableBeanFactory beanFactory) {
		MultipartConfigElement multipartConfig = getMultipartConfig(beanFactory);
		addAsRegistrationBean(beanFactory, Servlet.class,
				new ServletRegistrationBeanAdapter(multipartConfig, beanFactory));
		addAsRegistrationBean(beanFactory, Filter.class, new FilterRegistrationBeanAdapter(beanFactory));
		for (Class<?> listenerType : ServletListenerRegistrationBean.getSupportedTypes()) {
			addAsRegistrationBean(beanFactory, EventListener.class, (Class<EventListener>) listenerType,
					new ServletListenerRegistrationBeanAdapter());
		}
	}

	private MultipartConfigElement getMultipartConfig(ListableBeanFactory beanFactory) {
		List<Entry<String, MultipartConfigElement>> beans = getOrderedBeansOfType(beanFactory,
				MultipartConfigElement.class);
		return beans.isEmpty() ? null : beans.get(0).getValue();
	}

	protected <T> void addAsRegistrationBean(ListableBeanFactory beanFactory, Class<T> type,
			RegistrationBeanAdapter<T> adapter) {
		addAsRegistrationBean(beanFactory, type, type, adapter);
	}

	private <T, B extends T> void addAsRegistrationBean(ListableBeanFactory beanFactory, Class<T> type,
			Class<B> beanType, RegistrationBeanAdapter<T> adapter) {
		List<Map.Entry<String, B>> entries = getOrderedBeansOfType(beanFactory, beanType, this.seen);
		for (Entry<String, B> entry : entries) {
			String beanName = entry.getKey();
			B bean = entry.getValue();
			if (this.seen.add(type, bean)) {
				// One that we haven't already seen
				RegistrationBean registration = adapter.createRegistrationBean(beanName, bean, entries.size());
				Integer order = findOrder(bean);
				if (order != null) {
					registration.setOrder(order);
				}
				this.initializers.add(type, registration);
				if (logger.isTraceEnabled()) {
					logger.trace("Created " + type.getSimpleName() + " initializer for bean '" + beanName + "'; order="
							+ order + ", resource=" + getResourceDescription(beanName, beanFactory));
				}
			}
		}
	}

	private int getOrder(Object value) {
		Integer order = findOrder(value);
		return (order != null) ? order : Ordered.LOWEST_PRECEDENCE;
	}

	private Integer findOrder(Object value) {
		return new AnnotationAwareOrderComparator() {

			@Override
			public Integer findOrder(Object obj) {
				return super.findOrder(obj);
			}

		}.findOrder(value);
	}

	private <T> List<Entry<String, T>> getOrderedBeansOfType(ListableBeanFactory beanFactory, Class<T> type) {
		return getOrderedBeansOfType(beanFactory, type, Seen.empty());
	}

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

	private void logMappings(MultiValueMap<Class<?>, ServletContextInitializer> initializers) {
		if (logger.isDebugEnabled()) {
			logMappings("filters", initializers, Filter.class, FilterRegistrationBean.class);
			logMappings("servlets", initializers, Servlet.class, ServletRegistrationBean.class);
		}
	}

	private void logMappings(String name, MultiValueMap<Class<?>, ServletContextInitializer> initializers,
			Class<?> type, Class<? extends RegistrationBean> registrationType) {
		List<ServletContextInitializer> registrations = new ArrayList<>();
		registrations.addAll(initializers.getOrDefault(registrationType, Collections.emptyList()));
		registrations.addAll(initializers.getOrDefault(type, Collections.emptyList()));
		String info = registrations.stream().map(Object::toString).collect(Collectors.joining(", "));
		logger.debug("Mapping " + name + ": " + info);
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
	 * {@link ServletContextInitializer}).
	 *
	 * @param <T> the type of the Bean to adapt
	 */
	@FunctionalInterface
	protected interface RegistrationBeanAdapter<T> {

		RegistrationBean createRegistrationBean(String beanName, T source, int totalNumberOfSourceBeans);

	}

	/**
	 * {@link RegistrationBeanAdapter} for {@link Servlet} beans.
	 */
	private static class ServletRegistrationBeanAdapter implements RegistrationBeanAdapter<Servlet> {

		private final MultipartConfigElement multipartConfig;

		private final ListableBeanFactory beanFactory;

		ServletRegistrationBeanAdapter(MultipartConfigElement multipartConfig, ListableBeanFactory beanFactory) {
			this.multipartConfig = multipartConfig;
			this.beanFactory = beanFactory;
		}

		@Override
		public RegistrationBean createRegistrationBean(String beanName, Servlet source, int totalNumberOfSourceBeans) {
			String url = (totalNumberOfSourceBeans != 1) ? "/" + beanName + "/" : "/";
			if (beanName.equals(DISPATCHER_SERVLET_NAME)) {
				url = "/"; // always map the main dispatcherServlet to "/"
			}
			ServletRegistrationBean<Servlet> bean = new ServletRegistrationBean<>(source, url);
			bean.setName(beanName);
			bean.setMultipartConfig(this.multipartConfig);
			ServletRegistration registrationAnnotation = this.beanFactory.findAnnotationOnBean(beanName,
					ServletRegistration.class);
			if (registrationAnnotation != null) {
				Order orderAnnotation = this.beanFactory.findAnnotationOnBean(beanName, Order.class);
				Assert.notNull(orderAnnotation, "'orderAnnotation' must not be null");
				configureFromAnnotation(bean, registrationAnnotation, orderAnnotation);
			}
			return bean;
		}

		private void configureFromAnnotation(ServletRegistrationBean<Servlet> bean, ServletRegistration registration,
				Order order) {
			bean.setEnabled(registration.enabled());
			bean.setOrder(order.value());
			if (StringUtils.hasText(registration.name())) {
				bean.setName(registration.name());
			}
			bean.setAsyncSupported(registration.asyncSupported());
			bean.setIgnoreRegistrationFailure(registration.ignoreRegistrationFailure());
			bean.setLoadOnStartup(registration.loadOnStartup());
			bean.setUrlMappings(Arrays.asList(registration.urlMappings()));
			for (WebInitParam param : registration.initParameters()) {
				bean.addInitParameter(param.name(), param.value());
			}
			bean.setMultipartConfig(new MultipartConfigElement(registration.multipartConfig()));
		}

	}

	/**
	 * {@link RegistrationBeanAdapter} for {@link Filter} beans.
	 */
	private static class FilterRegistrationBeanAdapter implements RegistrationBeanAdapter<Filter> {

		private final ListableBeanFactory beanFactory;

		FilterRegistrationBeanAdapter(ListableBeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		@Override
		public RegistrationBean createRegistrationBean(String beanName, Filter source, int totalNumberOfSourceBeans) {
			FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>(source);
			bean.setName(beanName);
			FilterRegistration registrationAnnotation = this.beanFactory.findAnnotationOnBean(beanName,
					FilterRegistration.class);
			if (registrationAnnotation != null) {
				Order orderAnnotation = this.beanFactory.findAnnotationOnBean(beanName, Order.class);
				Assert.notNull(orderAnnotation, "'orderAnnotation' must not be null");
				configureFromAnnotation(bean, registrationAnnotation, orderAnnotation);
			}
			return bean;
		}

		private void configureFromAnnotation(FilterRegistrationBean<Filter> bean, FilterRegistration registration,
				Order order) {
			bean.setEnabled(registration.enabled());
			bean.setOrder(order.value());
			if (StringUtils.hasText(registration.name())) {
				bean.setName(registration.name());
			}
			bean.setAsyncSupported(registration.asyncSupported());
			if (registration.dispatcherTypes().length > 0) {
				bean.setDispatcherTypes(EnumSet.copyOf(Arrays.asList(registration.dispatcherTypes())));
			}
			bean.setIgnoreRegistrationFailure(registration.ignoreRegistrationFailure());
			bean.setMatchAfter(registration.matchAfter());
			bean.setServletNames(Arrays.asList(registration.servletNames()));
			bean.setUrlPatterns(Arrays.asList(registration.urlPatterns()));
			for (WebInitParam param : registration.initParameters()) {
				bean.addInitParameter(param.name(), param.value());
			}
			this.beanFactory.getBeanProvider(ServletRegistrationBean.class).forEach((servletRegistrationBean) -> {
				for (Class<?> servletClass : registration.servletClasses()) {
					if (servletClass.isInstance(servletRegistrationBean.getServlet())) {
						bean.addServletRegistrationBeans(servletRegistrationBean);
					}
				}
			});
		}

	}

	/**
	 * {@link RegistrationBeanAdapter} for certain {@link EventListener} beans.
	 */
	private static final class ServletListenerRegistrationBeanAdapter
			implements RegistrationBeanAdapter<EventListener> {

		@Override
		public RegistrationBean createRegistrationBean(String beanName, EventListener source,
				int totalNumberOfSourceBeans) {
			return new ServletListenerRegistrationBean<>(source);
		}

	}

	/**
	 * Tracks seen initializers.
	 */
	private static final class Seen {

		private final Map<Class<?>, Set<Object>> seen = new HashMap<>();

		boolean add(Class<?> type, Object object) {
			if (contains(type, object)) {
				return false;
			}
			return this.seen.computeIfAbsent(type, (ignore) -> new HashSet<>()).add(object);
		}

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

		static Seen empty() {
			return new Seen();
		}

	}

}
