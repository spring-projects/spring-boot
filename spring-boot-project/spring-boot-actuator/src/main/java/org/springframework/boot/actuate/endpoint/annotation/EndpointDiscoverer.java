/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.annotation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointId;
import org.springframework.boot.actuate.endpoint.EndpointsSupplier;
import org.springframework.boot.actuate.endpoint.ExposableEndpoint;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvoker;
import org.springframework.boot.actuate.endpoint.invoke.OperationInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.invoke.ParameterValueMapper;
import org.springframework.boot.util.LambdaSafe;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * A Base for {@link EndpointsSupplier} implementations that discover
 * {@link Endpoint @Endpoint} beans and {@link EndpointExtension @EndpointExtension} beans
 * in an application context.
 *
 * @param <E> the endpoint type
 * @param <O> the operation type
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
public abstract class EndpointDiscoverer<E extends ExposableEndpoint<O>, O extends Operation>
		implements EndpointsSupplier<E> {

	private final ApplicationContext applicationContext;

	private final Collection<EndpointFilter<E>> filters;

	private final DiscoveredOperationsFactory<O> operationsFactory;

	private final Map<EndpointBean, E> filterEndpoints = new ConcurrentHashMap<>();

	private volatile Collection<E> endpoints;

	/**
	 * Create a new {@link EndpointDiscoverer} instance.
	 * @param applicationContext the source application context
	 * @param parameterValueMapper the parameter value mapper
	 * @param invokerAdvisors invoker advisors to apply
	 * @param filters filters to apply
	 */
	public EndpointDiscoverer(ApplicationContext applicationContext, ParameterValueMapper parameterValueMapper,
			Collection<OperationInvokerAdvisor> invokerAdvisors, Collection<EndpointFilter<E>> filters) {
		Assert.notNull(applicationContext, "ApplicationContext must not be null");
		Assert.notNull(parameterValueMapper, "ParameterValueMapper must not be null");
		Assert.notNull(invokerAdvisors, "InvokerAdvisors must not be null");
		Assert.notNull(filters, "Filters must not be null");
		this.applicationContext = applicationContext;
		this.filters = Collections.unmodifiableCollection(filters);
		this.operationsFactory = getOperationsFactory(parameterValueMapper, invokerAdvisors);
	}

	/**
	 * Returns a new instance of DiscoveredOperationsFactory with the given
	 * parameterValueMapper and invokerAdvisors.
	 * @param parameterValueMapper the ParameterValueMapper to be used by the
	 * DiscoveredOperationsFactory
	 * @param invokerAdvisors the collection of OperationInvokerAdvisor to be used by the
	 * DiscoveredOperationsFactory
	 * @return a new instance of DiscoveredOperationsFactory
	 */
	private DiscoveredOperationsFactory<O> getOperationsFactory(ParameterValueMapper parameterValueMapper,
			Collection<OperationInvokerAdvisor> invokerAdvisors) {
		return new DiscoveredOperationsFactory<>(parameterValueMapper, invokerAdvisors) {

			@Override
			protected O createOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod,
					OperationInvoker invoker) {
				return EndpointDiscoverer.this.createOperation(endpointId, operationMethod, invoker);
			}

		};
	}

	/**
	 * Returns a collection of endpoints.
	 *
	 * If the endpoints have not been discovered yet, this method will call the
	 * {@code discoverEndpoints()} method to discover and initialize the endpoints.
	 * @return a collection of endpoints
	 */
	@Override
	public final Collection<E> getEndpoints() {
		if (this.endpoints == null) {
			this.endpoints = discoverEndpoints();
		}
		return this.endpoints;
	}

	/**
	 * Discovers the endpoints by creating endpoint beans, adding extension beans, and
	 * converting them to endpoints.
	 * @return the collection of discovered endpoints
	 */
	private Collection<E> discoverEndpoints() {
		Collection<EndpointBean> endpointBeans = createEndpointBeans();
		addExtensionBeans(endpointBeans);
		return convertToEndpoints(endpointBeans);
	}

	/**
	 * Creates a collection of EndpointBeans.
	 * @return the collection of EndpointBeans
	 */
	private Collection<EndpointBean> createEndpointBeans() {
		Map<EndpointId, EndpointBean> byId = new LinkedHashMap<>();
		String[] beanNames = BeanFactoryUtils.beanNamesForAnnotationIncludingAncestors(this.applicationContext,
				Endpoint.class);
		for (String beanName : beanNames) {
			if (!ScopedProxyUtils.isScopedTarget(beanName)) {
				EndpointBean endpointBean = createEndpointBean(beanName);
				EndpointBean previous = byId.putIfAbsent(endpointBean.getId(), endpointBean);
				Assert.state(previous == null, () -> "Found two endpoints with the id '" + endpointBean.getId() + "': '"
						+ endpointBean.getBeanName() + "' and '" + previous.getBeanName() + "'");
			}
		}
		return byId.values();
	}

	/**
	 * Creates an EndpointBean object for the specified bean name.
	 * @param beanName the name of the bean
	 * @return the created EndpointBean object
	 */
	private EndpointBean createEndpointBean(String beanName) {
		Class<?> beanType = ClassUtils.getUserClass(this.applicationContext.getType(beanName, false));
		Supplier<Object> beanSupplier = () -> this.applicationContext.getBean(beanName);
		return new EndpointBean(this.applicationContext.getEnvironment(), beanName, beanType, beanSupplier);
	}

	/**
	 * Adds extension beans to the collection of endpoint beans.
	 * @param endpointBeans the collection of endpoint beans
	 */
	private void addExtensionBeans(Collection<EndpointBean> endpointBeans) {
		Map<EndpointId, EndpointBean> byId = endpointBeans.stream()
			.collect(Collectors.toMap(EndpointBean::getId, Function.identity()));
		String[] beanNames = BeanFactoryUtils.beanNamesForAnnotationIncludingAncestors(this.applicationContext,
				EndpointExtension.class);
		for (String beanName : beanNames) {
			ExtensionBean extensionBean = createExtensionBean(beanName);
			EndpointBean endpointBean = byId.get(extensionBean.getEndpointId());
			Assert.state(endpointBean != null, () -> ("Invalid extension '" + extensionBean.getBeanName()
					+ "': no endpoint found with id '" + extensionBean.getEndpointId() + "'"));
			addExtensionBean(endpointBean, extensionBean);
		}
	}

	/**
	 * Creates an ExtensionBean object for the given bean name.
	 * @param beanName the name of the bean
	 * @return the created ExtensionBean object
	 */
	private ExtensionBean createExtensionBean(String beanName) {
		Class<?> beanType = ClassUtils.getUserClass(this.applicationContext.getType(beanName));
		Supplier<Object> beanSupplier = () -> this.applicationContext.getBean(beanName);
		return new ExtensionBean(this.applicationContext.getEnvironment(), beanName, beanType, beanSupplier);
	}

	/**
	 * Adds an extension bean to the given endpoint bean if the extension is exposed by
	 * the endpoint.
	 * @param endpointBean the endpoint bean to which the extension bean will be added
	 * @param extensionBean the extension bean to be added
	 * @throws IllegalStateException if the endpoint bean does not support the extension
	 * bean
	 */
	private void addExtensionBean(EndpointBean endpointBean, ExtensionBean extensionBean) {
		if (isExtensionExposed(endpointBean, extensionBean)) {
			Assert.state(isEndpointExposed(endpointBean) || isEndpointFiltered(endpointBean),
					() -> "Endpoint bean '" + endpointBean.getBeanName() + "' cannot support the extension bean '"
							+ extensionBean.getBeanName() + "'");
			endpointBean.addExtension(extensionBean);
		}
	}

	/**
	 * Converts a collection of EndpointBean objects to a collection of endpoints.
	 * @param endpointBeans the collection of EndpointBean objects to convert
	 * @return a collection of endpoints
	 */
	private Collection<E> convertToEndpoints(Collection<EndpointBean> endpointBeans) {
		Set<E> endpoints = new LinkedHashSet<>();
		for (EndpointBean endpointBean : endpointBeans) {
			if (isEndpointExposed(endpointBean)) {
				endpoints.add(convertToEndpoint(endpointBean));
			}
		}
		return Collections.unmodifiableSet(endpoints);
	}

	/**
	 * Converts an EndpointBean to an Endpoint.
	 * @param endpointBean the EndpointBean to convert
	 * @return the converted Endpoint
	 * @throws IllegalStateException if multiple extensions are found for the endpoint
	 * bean
	 */
	private E convertToEndpoint(EndpointBean endpointBean) {
		MultiValueMap<OperationKey, O> indexed = new LinkedMultiValueMap<>();
		EndpointId id = endpointBean.getId();
		addOperations(indexed, id, endpointBean.getBean(), false);
		if (endpointBean.getExtensions().size() > 1) {
			String extensionBeans = endpointBean.getExtensions()
				.stream()
				.map(ExtensionBean::getBeanName)
				.collect(Collectors.joining(", "));
			throw new IllegalStateException("Found multiple extensions for the endpoint bean "
					+ endpointBean.getBeanName() + " (" + extensionBeans + ")");
		}
		for (ExtensionBean extensionBean : endpointBean.getExtensions()) {
			addOperations(indexed, id, extensionBean.getBean(), true);
		}
		assertNoDuplicateOperations(endpointBean, indexed);
		List<O> operations = indexed.values().stream().map(this::getLast).filter(Objects::nonNull).toList();
		return createEndpoint(endpointBean.getBean(), id, endpointBean.isEnabledByDefault(), operations);
	}

	/**
	 * Adds operations to the indexed map based on the provided endpoint ID, target
	 * object, and replaceLast flag.
	 * @param indexed the MultiValueMap used to store the indexed operations
	 * @param id the endpoint ID
	 * @param target the target object
	 * @param replaceLast flag indicating whether to replace the last operation with the
	 * new one
	 */
	private void addOperations(MultiValueMap<OperationKey, O> indexed, EndpointId id, Object target,
			boolean replaceLast) {
		Set<OperationKey> replacedLast = new HashSet<>();
		Collection<O> operations = this.operationsFactory.createOperations(id, target);
		for (O operation : operations) {
			OperationKey key = createOperationKey(operation);
			O last = getLast(indexed.get(key));
			if (replaceLast && replacedLast.add(key) && last != null) {
				indexed.get(key).remove(last);
			}
			indexed.add(key, operation);
		}
	}

	/**
	 * Returns the last element of the given list.
	 * @param list the list from which to retrieve the last element
	 * @param <T> the type of elements in the list
	 * @return the last element of the list, or null if the list is empty
	 */
	private <T> T getLast(List<T> list) {
		return CollectionUtils.isEmpty(list) ? null : list.get(list.size() - 1);
	}

	/**
	 * Asserts that there are no duplicate operations in the given indexed map for the
	 * specified endpoint bean.
	 * @param endpointBean The endpoint bean to check for duplicate operations.
	 * @param indexed The indexed map containing the operations.
	 * @throws IllegalStateException If duplicate operations are found, with a message
	 * indicating the duplicates and the endpoint bean.
	 */
	private void assertNoDuplicateOperations(EndpointBean endpointBean, MultiValueMap<OperationKey, O> indexed) {
		List<OperationKey> duplicates = indexed.entrySet()
			.stream()
			.filter((entry) -> entry.getValue().size() > 1)
			.map(Map.Entry::getKey)
			.toList();
		if (!duplicates.isEmpty()) {
			Set<ExtensionBean> extensions = endpointBean.getExtensions();
			String extensionBeanNames = extensions.stream()
				.map(ExtensionBean::getBeanName)
				.collect(Collectors.joining(", "));
			throw new IllegalStateException("Unable to map duplicate endpoint operations: " + duplicates + " to "
					+ endpointBean.getBeanName() + (extensions.isEmpty() ? "" : " (" + extensionBeanNames + ")"));
		}
	}

	/**
	 * Checks if the extension is exposed based on the provided endpoint and extension
	 * information.
	 * @param endpointBean the endpoint bean
	 * @param extensionBean the extension bean
	 * @return true if the extension is exposed, false otherwise
	 */
	private boolean isExtensionExposed(EndpointBean endpointBean, ExtensionBean extensionBean) {
		return isFilterMatch(extensionBean.getFilter(), endpointBean)
				&& isExtensionTypeExposed(extensionBean.getBeanType());
	}

	/**
	 * Determine if an extension bean should be exposed. Subclasses can override this
	 * method to provide additional logic.
	 * @param extensionBeanType the extension bean type
	 * @return {@code true} if the extension is exposed
	 */
	protected boolean isExtensionTypeExposed(Class<?> extensionBeanType) {
		return true;
	}

	/**
	 * Checks if an endpoint is exposed.
	 * @param endpointBean the endpoint bean to check
	 * @return {@code true} if the endpoint is exposed, {@code false} otherwise
	 */
	private boolean isEndpointExposed(EndpointBean endpointBean) {
		return isFilterMatch(endpointBean.getFilter(), endpointBean) && !isEndpointFiltered(endpointBean)
				&& isEndpointTypeExposed(endpointBean.getBeanType());
	}

	/**
	 * Determine if an endpoint bean should be exposed. Subclasses can override this
	 * method to provide additional logic.
	 * @param beanType the endpoint bean type
	 * @return {@code true} if the endpoint is exposed
	 */
	protected boolean isEndpointTypeExposed(Class<?> beanType) {
		return true;
	}

	/**
	 * Checks if the given endpoint is filtered based on the provided filters.
	 * @param endpointBean the endpoint to be checked
	 * @return true if the endpoint is filtered, false otherwise
	 */
	private boolean isEndpointFiltered(EndpointBean endpointBean) {
		for (EndpointFilter<E> filter : this.filters) {
			if (!isFilterMatch(filter, endpointBean)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if the given filter matches the provided endpoint bean.
	 * @param filter the filter to be matched
	 * @param endpointBean the endpoint bean to be checked against the filter
	 * @return true if the filter matches the endpoint bean, false otherwise
	 */
	@SuppressWarnings("unchecked")
	private boolean isFilterMatch(Class<?> filter, EndpointBean endpointBean) {
		if (!isEndpointTypeExposed(endpointBean.getBeanType())) {
			return false;
		}
		if (filter == null) {
			return true;
		}
		E endpoint = getFilterEndpoint(endpointBean);
		Class<?> generic = ResolvableType.forClass(EndpointFilter.class, filter).resolveGeneric(0);
		if (generic == null || generic.isInstance(endpoint)) {
			EndpointFilter<E> instance = (EndpointFilter<E>) BeanUtils.instantiateClass(filter);
			return isFilterMatch(instance, endpoint);
		}
		return false;
	}

	/**
	 * Checks if the given filter matches the provided endpoint bean.
	 * @param filter the endpoint filter to be matched
	 * @param endpointBean the endpoint bean to be checked against the filter
	 * @return true if the filter matches the endpoint bean, false otherwise
	 */
	private boolean isFilterMatch(EndpointFilter<E> filter, EndpointBean endpointBean) {
		return isFilterMatch(filter, getFilterEndpoint(endpointBean));
	}

	/**
	 * Checks if the given endpoint matches the provided filter.
	 * @param filter the filter to apply
	 * @param endpoint the endpoint to check against the filter
	 * @return {@code true} if the endpoint matches the filter, {@code false} otherwise
	 */
	@SuppressWarnings("unchecked")
	private boolean isFilterMatch(EndpointFilter<E> filter, E endpoint) {
		return LambdaSafe.callback(EndpointFilter.class, filter, endpoint)
			.withLogger(EndpointDiscoverer.class)
			.invokeAnd((f) -> f.match(endpoint))
			.get();
	}

	/**
	 * Retrieves the filter endpoint for the given endpoint bean.
	 * @param endpointBean the endpoint bean to retrieve the filter endpoint for
	 * @return the filter endpoint for the given endpoint bean
	 */
	private E getFilterEndpoint(EndpointBean endpointBean) {
		E endpoint = this.filterEndpoints.get(endpointBean);
		if (endpoint == null) {
			endpoint = createEndpoint(endpointBean.getBean(), endpointBean.getId(), endpointBean.isEnabledByDefault(),
					Collections.emptySet());
			this.filterEndpoints.put(endpointBean, endpoint);
		}
		return endpoint;
	}

	/**
	 * Returns the endpoint type of the EndpointDiscoverer.
	 * @return the endpoint type of the EndpointDiscoverer
	 */
	@SuppressWarnings("unchecked")
	protected Class<? extends E> getEndpointType() {
		return (Class<? extends E>) ResolvableType.forClass(EndpointDiscoverer.class, getClass()).resolveGeneric(0);
	}

	/**
	 * Factory method called to create the {@link ExposableEndpoint endpoint}.
	 * @param endpointBean the source endpoint bean
	 * @param id the ID of the endpoint
	 * @param enabledByDefault if the endpoint is enabled by default
	 * @param operations the endpoint operations
	 * @return a created endpoint (a {@link DiscoveredEndpoint} is recommended)
	 */
	protected abstract E createEndpoint(Object endpointBean, EndpointId id, boolean enabledByDefault,
			Collection<O> operations);

	/**
	 * Factory method to create an {@link Operation endpoint operation}.
	 * @param endpointId the endpoint id
	 * @param operationMethod the operation method
	 * @param invoker the invoker to use
	 * @return a created operation
	 */
	protected abstract O createOperation(EndpointId endpointId, DiscoveredOperationMethod operationMethod,
			OperationInvoker invoker);

	/**
	 * Create an {@link OperationKey} for the given operation.
	 * @param operation the source operation
	 * @return the operation key
	 */
	protected abstract OperationKey createOperationKey(O operation);

	/**
	 * A key generated for an {@link Operation} based on specific criteria from the actual
	 * operation implementation.
	 */
	protected static final class OperationKey {

		private final Object key;

		private final Supplier<String> description;

		/**
		 * Create a new {@link OperationKey} instance.
		 * @param key the underlying key for the operation
		 * @param description a human-readable description of the key
		 */
		public OperationKey(Object key, Supplier<String> description) {
			Assert.notNull(key, "Key must not be null");
			Assert.notNull(description, "Description must not be null");
			this.key = key;
			this.description = description;
		}

		/**
		 * Compares this OperationKey object to the specified object. The result is true
		 * if and only if the argument is not null and is an OperationKey object that
		 * represents the same key as this object.
		 * @param obj the object to compare this OperationKey against
		 * @return true if the given object represents an OperationKey equivalent to this
		 * key, false otherwise
		 */
		@Override
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			return this.key.equals(((OperationKey) obj).key);
		}

		/**
		 * Returns the hash code value for this OperationKey object.
		 * @return the hash code value for this OperationKey object
		 */
		@Override
		public int hashCode() {
			return this.key.hashCode();
		}

		/**
		 * Returns a string representation of the OperationKey object.
		 * @return the description of the OperationKey object
		 */
		@Override
		public String toString() {
			return this.description.get();
		}

	}

	/**
	 * Information about an {@link Endpoint @Endpoint} bean.
	 */
	private static class EndpointBean {

		private final String beanName;

		private final Class<?> beanType;

		private final Supplier<Object> beanSupplier;

		private final EndpointId id;

		private final boolean enabledByDefault;

		private final Class<?> filter;

		private final Set<ExtensionBean> extensions = new LinkedHashSet<>();

		/**
		 * Constructs a new EndpointBean object.
		 * @param environment the environment in which the endpoint is defined
		 * @param beanName the name of the bean
		 * @param beanType the type of the bean
		 * @param beanSupplier the supplier function for creating the bean
		 * @throws IllegalArgumentException if no @Endpoint id attribute is specified for
		 * the bean type
		 */
		EndpointBean(Environment environment, String beanName, Class<?> beanType, Supplier<Object> beanSupplier) {
			MergedAnnotation<Endpoint> annotation = MergedAnnotations.from(beanType, SearchStrategy.TYPE_HIERARCHY)
				.get(Endpoint.class);
			String id = annotation.getString("id");
			Assert.state(StringUtils.hasText(id),
					() -> "No @Endpoint id attribute specified for " + beanType.getName());
			this.beanName = beanName;
			this.beanType = beanType;
			this.beanSupplier = beanSupplier;
			this.id = EndpointId.of(environment, id);
			this.enabledByDefault = annotation.getBoolean("enableByDefault");
			this.filter = getFilter(beanType);
		}

		/**
		 * Adds an extension to the list of extensions.
		 * @param extensionBean the extension to be added
		 */
		void addExtension(ExtensionBean extensionBean) {
			this.extensions.add(extensionBean);
		}

		/**
		 * Returns the set of ExtensionBeans associated with this EndpointBean.
		 * @return the set of ExtensionBeans
		 */
		Set<ExtensionBean> getExtensions() {
			return this.extensions;
		}

		/**
		 * Retrieves the filter class for the given type.
		 * @param type the class for which the filter class needs to be retrieved
		 * @return the filter class for the given type, or null if not found
		 */
		private Class<?> getFilter(Class<?> type) {
			return MergedAnnotations.from(type, SearchStrategy.TYPE_HIERARCHY)
				.get(FilteredEndpoint.class)
				.getValue(MergedAnnotation.VALUE, Class.class)
				.orElse(null);
		}

		/**
		 * Returns the name of the bean.
		 * @return the name of the bean
		 */
		String getBeanName() {
			return this.beanName;
		}

		/**
		 * Returns the type of the bean.
		 * @return the type of the bean
		 */
		Class<?> getBeanType() {
			return this.beanType;
		}

		/**
		 * Retrieves the bean object from the bean supplier.
		 * @return the bean object
		 */
		Object getBean() {
			return this.beanSupplier.get();
		}

		/**
		 * Returns the ID of the endpoint.
		 * @return the ID of the endpoint
		 */
		EndpointId getId() {
			return this.id;
		}

		/**
		 * Returns a boolean value indicating whether the endpoint is enabled by default.
		 * @return true if the endpoint is enabled by default, false otherwise
		 */
		boolean isEnabledByDefault() {
			return this.enabledByDefault;
		}

		/**
		 * Returns the filter class associated with this EndpointBean.
		 * @return the filter class
		 */
		Class<?> getFilter() {
			return this.filter;
		}

	}

	/**
	 * Information about an {@link EndpointExtension @EndpointExtension} bean.
	 */
	private static class ExtensionBean {

		private final String beanName;

		private final Class<?> beanType;

		private final Supplier<Object> beanSupplier;

		private final EndpointId endpointId;

		private final Class<?> filter;

		/**
		 * Constructs a new ExtensionBean object.
		 * @param environment the environment in which the bean is being created
		 * @param beanName the name of the bean
		 * @param beanType the type of the bean
		 * @param beanSupplier the supplier function to create the bean
		 * @throws IllegalStateException if the extension does not specify an endpoint
		 */
		ExtensionBean(Environment environment, String beanName, Class<?> beanType, Supplier<Object> beanSupplier) {
			this.beanName = beanName;
			this.beanType = beanType;
			this.beanSupplier = beanSupplier;
			MergedAnnotation<EndpointExtension> extensionAnnotation = MergedAnnotations
				.from(beanType, SearchStrategy.TYPE_HIERARCHY)
				.get(EndpointExtension.class);
			Class<?> endpointType = extensionAnnotation.getClass("endpoint");
			MergedAnnotation<Endpoint> endpointAnnotation = MergedAnnotations
				.from(endpointType, SearchStrategy.TYPE_HIERARCHY)
				.get(Endpoint.class);
			Assert.state(endpointAnnotation.isPresent(),
					() -> "Extension " + endpointType.getName() + " does not specify an endpoint");
			this.endpointId = EndpointId.of(environment, endpointAnnotation.getString("id"));
			this.filter = extensionAnnotation.getClass("filter");
		}

		/**
		 * Returns the name of the bean.
		 * @return the name of the bean
		 */
		String getBeanName() {
			return this.beanName;
		}

		/**
		 * Returns the type of the bean.
		 * @return the type of the bean
		 */
		Class<?> getBeanType() {
			return this.beanType;
		}

		/**
		 * Retrieves the bean object.
		 * @return the bean object
		 */
		Object getBean() {
			return this.beanSupplier.get();
		}

		/**
		 * Returns the endpoint ID of the ExtensionBean.
		 * @return the endpoint ID of the ExtensionBean
		 */
		EndpointId getEndpointId() {
			return this.endpointId;
		}

		/**
		 * Returns the filter class associated with this ExtensionBean.
		 * @return the filter class
		 */
		Class<?> getFilter() {
			return this.filter;
		}

	}

}
