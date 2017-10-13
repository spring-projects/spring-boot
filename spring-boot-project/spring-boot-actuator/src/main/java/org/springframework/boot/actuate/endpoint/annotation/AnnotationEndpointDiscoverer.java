/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.boot.actuate.endpoint.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.EndpointFilter;
import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.reflect.OperationMethodInvokerAdvisor;
import org.springframework.boot.actuate.endpoint.reflect.ParameterMapper;
import org.springframework.context.ApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.style.ToStringCreator;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * A base {@link EndpointDiscoverer} implementation that discovers
 * {@link Endpoint @Endpoint} beans and {@link EndpointExtension @EndpointExtension} beans
 * in an application context.
 *
 * @param <K> the type of the operation key
 * @param <T> the type of the operation
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Phillip Webb
 * @since 2.0.0
 */
public abstract class AnnotationEndpointDiscoverer<K, T extends Operation>
		implements EndpointDiscoverer<T> {

	private final ApplicationContext applicationContext;

	private final Function<T, K> operationKeyFactory;

	private final OperationsFactory<T> operationsFactory;

	private final List<EndpointFilter<T>> filters;

	/**
	 * Create a new {@link AnnotationEndpointDiscoverer} instance.
	 * @param applicationContext the application context
	 * @param operationFactory a factory used to create operations
	 * @param operationKeyFactory a factory used to create a key for an operation
	 * @param parameterMapper the {@link ParameterMapper} used to convert arguments when
	 * an operation is invoked
	 * @param invokerAdvisors advisors used to add additional invoker advise
	 * @param filters filters that must match for an endpoint to be exposed
	 */
	protected AnnotationEndpointDiscoverer(ApplicationContext applicationContext,
			OperationFactory<T> operationFactory, Function<T, K> operationKeyFactory,
			ParameterMapper parameterMapper,
			Collection<? extends OperationMethodInvokerAdvisor> invokerAdvisors,
			Collection<? extends EndpointFilter<T>> filters) {
		Assert.notNull(applicationContext, "Application Context must not be null");
		Assert.notNull(operationFactory, "Operation Factory must not be null");
		Assert.notNull(operationKeyFactory, "Operation Key Factory must not be null");
		Assert.notNull(parameterMapper, "Parameter Mapper must not be null");
		this.applicationContext = applicationContext;
		this.operationKeyFactory = operationKeyFactory;
		this.operationsFactory = new OperationsFactory<>(operationFactory,
				parameterMapper, invokerAdvisors);
		this.filters = (filters == null ? Collections.emptyList()
				: new ArrayList<>(filters));
	}

	@Override
	public final Collection<EndpointInfo<T>> discoverEndpoints() {
		Class<T> operationType = getOperationType();
		Map<Class<?>, DiscoveredEndpoint> endpoints = getEndpoints(operationType);
		Map<Class<?>, DiscoveredExtension> extensions = getExtensions(operationType,
				endpoints);
		Collection<DiscoveredEndpoint> exposed = mergeExposed(endpoints, extensions);
		verify(exposed);
		return exposed.stream().map(DiscoveredEndpoint::getInfo)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	/**
	 * Return the operation type being discovered. By default this method will resolve the
	 * class generic "{@code <T>}".
	 * @return the operation type
	 */
	@SuppressWarnings("unchecked")
	protected Class<T> getOperationType() {
		return (Class<T>) ResolvableType
				.forClass(AnnotationEndpointDiscoverer.class, getClass())
				.resolveGeneric(1);
	}

	private Map<Class<?>, DiscoveredEndpoint> getEndpoints(Class<T> operationType) {
		Map<Class<?>, DiscoveredEndpoint> endpoints = new LinkedHashMap<>();
		Map<String, DiscoveredEndpoint> endpointsById = new LinkedHashMap<>();
		String[] beanNames = BeanFactoryUtils.beanNamesForAnnotationIncludingAncestors(
				this.applicationContext, Endpoint.class);
		for (String beanName : beanNames) {
			addEndpoint(endpoints, endpointsById, beanName);
		}
		return endpoints;
	}

	private void addEndpoint(Map<Class<?>, DiscoveredEndpoint> endpoints,
			Map<String, DiscoveredEndpoint> endpointsById, String beanName) {
		Class<?> endpointType = this.applicationContext.getType(beanName);
		Object target = this.applicationContext.getBean(beanName);
		DiscoveredEndpoint endpoint = createEndpoint(target, endpointType);
		String id = endpoint.getInfo().getId();
		DiscoveredEndpoint previous = endpointsById.putIfAbsent(id, endpoint);
		Assert.state(previous == null, () -> "Found two endpoints with the id '" + id
				+ "': " + endpoint + " and " + previous);
		endpoints.put(endpointType, endpoint);
	}

	private DiscoveredEndpoint createEndpoint(Object target, Class<?> endpointType) {
		AnnotationAttributes annotationAttributes = AnnotatedElementUtils
				.findMergedAnnotationAttributes(endpointType, Endpoint.class, true, true);
		String id = annotationAttributes.getString("id");
		Assert.state(StringUtils.hasText(id),
				"No @Endpoint id attribute specified for " + endpointType.getName());
		boolean enabledByDefault = (Boolean) annotationAttributes.get("enableByDefault");
		Collection<T> operations = this.operationsFactory
				.createOperations(id, target, endpointType).values();
		EndpointInfo<T> endpointInfo = new EndpointInfo<>(id, enabledByDefault,
				operations);
		boolean exposed = isEndpointExposed(endpointType, endpointInfo);
		return new DiscoveredEndpoint(endpointType, endpointInfo, exposed);
	}

	private Map<Class<?>, DiscoveredExtension> getExtensions(Class<T> operationType,
			Map<Class<?>, DiscoveredEndpoint> endpoints) {
		Map<Class<?>, DiscoveredExtension> extensions = new LinkedHashMap<>();
		String[] beanNames = BeanFactoryUtils.beanNamesForAnnotationIncludingAncestors(
				this.applicationContext, EndpointExtension.class);
		for (String beanName : beanNames) {
			addExtension(endpoints, extensions, beanName);
		}
		return extensions;
	}

	private void addExtension(Map<Class<?>, DiscoveredEndpoint> endpoints,
			Map<Class<?>, DiscoveredExtension> extensions, String beanName) {
		Class<?> extensionType = this.applicationContext.getType(beanName);
		Class<?> endpointType = getEndpointType(extensionType);
		DiscoveredEndpoint endpoint = getExtendingEndpoint(endpoints, extensionType,
				endpointType);
		if (isExtensionExposed(extensionType, endpoint.getInfo())) {
			Assert.state(endpoint.isExposed() || isEndpointFiltered(endpoint.getInfo()),
					() -> "Invalid extension " + extensionType.getName() + "': endpoint '"
							+ endpointType.getName()
							+ "' does not support such extension");
			Object target = this.applicationContext.getBean(beanName);
			Map<Method, T> operations = this.operationsFactory
					.createOperations(endpoint.getInfo().getId(), target, extensionType);
			DiscoveredExtension extension = new DiscoveredExtension(extensionType,
					operations.values());
			DiscoveredExtension previous = extensions.putIfAbsent(endpointType,
					extension);
			Assert.state(previous == null,
					() -> "Found two extensions for the same endpoint '"
							+ endpointType.getName() + "': "
							+ extension.getExtensionType().getName() + " and "
							+ previous.getExtensionType().getName());
		}
	}

	private Class<?> getEndpointType(Class<?> extensionType) {
		AnnotationAttributes attributes = AnnotatedElementUtils
				.getMergedAnnotationAttributes(extensionType, EndpointExtension.class);
		Class<?> endpointType = attributes.getClass("endpoint");
		Assert.state(!endpointType.equals(Void.class), () -> "Extension "
				+ endpointType.getName() + " does not specify an endpoint");
		return endpointType;
	}

	private DiscoveredEndpoint getExtendingEndpoint(
			Map<Class<?>, DiscoveredEndpoint> endpoints, Class<?> extensionType,
			Class<?> endpointType) {
		DiscoveredEndpoint endpoint = endpoints.get(endpointType);
		Assert.state(endpoint != null,
				() -> "Invalid extension '" + extensionType.getName()
						+ "': no endpoint found with type '" + endpointType.getName()
						+ "'");
		return endpoint;
	}

	private boolean isEndpointExposed(Class<?> endpointType,
			EndpointInfo<T> endpointInfo) {
		if (isEndpointFiltered(endpointInfo)) {
			return false;
		}
		AnnotationAttributes annotationAttributes = AnnotatedElementUtils
				.getMergedAnnotationAttributes(endpointType, FilteredEndpoint.class);
		if (annotationAttributes == null) {
			return true;
		}
		Class<?> filterClass = annotationAttributes.getClass("value");
		return isFilterMatch(filterClass, endpointInfo);
	}

	private boolean isEndpointFiltered(EndpointInfo<T> endpointInfo) {
		for (EndpointFilter<T> filter : this.filters) {
			if (!isFilterMatch(filter, endpointInfo)) {
				return true;
			}
		}
		return false;
	}

	private boolean isExtensionExposed(Class<?> extensionType,
			EndpointInfo<T> endpointInfo) {
		AnnotationAttributes annotationAttributes = AnnotatedElementUtils
				.getMergedAnnotationAttributes(extensionType, EndpointExtension.class);
		Class<?> filterClass = annotationAttributes.getClass("filter");
		return isFilterMatch(filterClass, endpointInfo);
	}

	@SuppressWarnings("unchecked")
	private boolean isFilterMatch(Class<?> filterClass, EndpointInfo<T> endpointInfo) {
		Class<?> generic = ResolvableType.forClass(EndpointFilter.class, filterClass)
				.resolveGeneric(0);
		if (generic == null || generic.isAssignableFrom(getOperationType())) {
			EndpointFilter<T> filter = (EndpointFilter<T>) BeanUtils
					.instantiateClass(filterClass);
			return isFilterMatch(filter, endpointInfo);
		}
		return false;
	}

	private boolean isFilterMatch(EndpointFilter<T> filter,
			EndpointInfo<T> endpointInfo) {
		try {
			return filter.match(endpointInfo, this);
		}
		catch (ClassCastException ex) {
			String msg = ex.getMessage();
			if (msg == null || msg.startsWith(endpointInfo.getClass().getName())) {
				// Possibly a lambda-defined listener which we could not resolve the
				// generic event type for
				Log logger = LogFactory.getLog(getClass());
				if (logger.isDebugEnabled()) {
					logger.debug("Non-matching info type for lister: " + filter, ex);
				}
				return false;
			}
			else {
				throw ex;
			}
		}

	}

	private Collection<DiscoveredEndpoint> mergeExposed(
			Map<Class<?>, DiscoveredEndpoint> endpoints,
			Map<Class<?>, DiscoveredExtension> extensions) {
		List<DiscoveredEndpoint> result = new ArrayList<>();
		endpoints.forEach((endpointClass, endpoint) -> {
			if (endpoint.isExposed()) {
				DiscoveredExtension extension = extensions.remove(endpointClass);
				result.add(endpoint.merge(extension));
			}
		});
		return result;
	}

	/**
	 * Allows subclasses to verify that the descriptors are correctly configured.
	 * @param exposedEndpoints the discovered endpoints to verify before exposing
	 */
	protected void verify(Collection<DiscoveredEndpoint> exposedEndpoints) {
	}

	/**
	 * A discovered endpoint (which may not be valid and might not ultimately be exposed).
	 */
	protected final class DiscoveredEndpoint {

		private final EndpointInfo<T> info;

		private final boolean exposed;

		private final Map<OperationKey, List<T>> operations;

		private DiscoveredEndpoint(Class<?> type, EndpointInfo<T> info, boolean exposed) {
			Assert.notNull(info, "Info must not be null");
			this.info = info;
			this.exposed = exposed;
			this.operations = indexEndpointOperations(type, info);
		}

		private Map<OperationKey, List<T>> indexEndpointOperations(Class<?> endpointType,
				EndpointInfo<T> info) {
			return Collections.unmodifiableMap(
					indexOperations(info.getId(), endpointType, info.getOperations()));
		}

		private DiscoveredEndpoint(EndpointInfo<T> info, boolean exposed,
				Map<OperationKey, List<T>> operations) {
			Assert.notNull(info, "Info must not be null");
			this.info = info;
			this.exposed = exposed;
			this.operations = operations;
		}

		/**
		 * Return the {@link EndpointInfo} for the discovered endpoint.
		 * @return the endpoint info
		 */
		public EndpointInfo<T> getInfo() {
			return this.info;
		}

		/**
		 * Return {@code true} if the endpoint is exposed.
		 * @return if the is exposed
		 */
		private boolean isExposed() {
			return this.exposed;
		}

		/**
		 * Return all operation that were discovered. These might be different to the ones
		 * that are in {@link #getInfo()}.
		 * @return the endpoint operations
		 */
		public Map<OperationKey, List<T>> getOperations() {
			return this.operations;
		}

		/**
		 * Find any duplicate operations.
		 * @return any duplicate operations
		 */
		public Map<OperationKey, List<T>> findDuplicateOperations() {
			return this.operations.entrySet().stream()
					.filter((entry) -> entry.getValue().size() > 1)
					.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (u, v) -> v,
							LinkedHashMap::new));
		}

		private DiscoveredEndpoint merge(DiscoveredExtension extension) {
			if (extension == null) {
				return this;
			}
			Map<OperationKey, List<T>> operations = mergeOperations(extension);
			EndpointInfo<T> info = new EndpointInfo<>(this.info.getId(),
					this.info.isEnableByDefault(), flatten(operations).values());
			return new DiscoveredEndpoint(info, this.exposed, operations);
		}

		private Map<OperationKey, List<T>> mergeOperations(
				DiscoveredExtension extension) {
			MultiValueMap<OperationKey, T> operations = new LinkedMultiValueMap<>(
					this.operations);
			operations.addAll(indexOperations(getInfo().getId(),
					extension.getExtensionType(), extension.getOperations()));
			return Collections.unmodifiableMap(operations);
		}

		private Map<K, T> flatten(Map<OperationKey, List<T>> operations) {
			Map<K, T> flattened = new LinkedHashMap<>();
			operations.forEach((operationKey, value) -> flattened
					.put(operationKey.getKey(), getLastValue(value)));
			return Collections.unmodifiableMap(flattened);
		}

		private T getLastValue(List<T> value) {
			return value.get(value.size() - 1);
		}

		private MultiValueMap<OperationKey, T> indexOperations(String endpointId,
				Class<?> target, Collection<T> operations) {
			LinkedMultiValueMap<OperationKey, T> result = new LinkedMultiValueMap<>();
			operations.forEach((operation) -> {
				K key = getOperationKey(operation);
				result.add(new OperationKey(endpointId, target, key), operation);
			});
			return result;
		}

		private K getOperationKey(T operation) {
			return AnnotationEndpointDiscoverer.this.operationKeyFactory.apply(operation);
		}

		@Override
		public String toString() {
			return getInfo().toString();
		}

	}

	/**
	 * A discovered extension.
	 */
	protected final class DiscoveredExtension {

		private final Class<?> extensionType;

		private final Collection<T> operations;

		private DiscoveredExtension(Class<?> extensionType, Collection<T> operations) {
			this.extensionType = extensionType;
			this.operations = operations;
		}

		public Class<?> getExtensionType() {
			return this.extensionType;
		}

		public Collection<T> getOperations() {
			return this.operations;
		}

		@Override
		public String toString() {
			return this.extensionType.getName();
		}

	}

	/**
	 * Define the key of an operation in the context of an operation's implementation.
	 */
	protected final class OperationKey {

		private final String endpointId;

		private final Class<?> target;

		private final K key;

		public OperationKey(String endpointId, Class<?> target, K key) {
			this.endpointId = endpointId;
			this.target = target;
			this.key = key;
		}

		public K getKey() {
			return this.key;
		}

		@Override
		@SuppressWarnings("unchecked")
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			OperationKey other = (OperationKey) o;
			Boolean result = true;
			result = result && this.endpointId.equals(other.endpointId);
			result = result && this.target.equals(other.target);
			result = result && this.key.equals(other.key);
			return result;
		}

		@Override
		public int hashCode() {
			int result = this.endpointId.hashCode();
			result = 31 * result + this.target.hashCode();
			result = 31 * result + this.key.hashCode();
			return result;
		}

		@Override
		public String toString() {
			return new ToStringCreator(this).append("endpointId", this.endpointId)
					.append("target", this.target).append("key", this.key).toString();
		}

	}

}
