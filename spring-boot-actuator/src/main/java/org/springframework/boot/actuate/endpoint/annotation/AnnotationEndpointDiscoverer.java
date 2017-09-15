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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.boot.actuate.endpoint.DefaultEnablement;
import org.springframework.boot.actuate.endpoint.EndpointDiscoverer;
import org.springframework.boot.actuate.endpoint.EndpointExposure;
import org.springframework.boot.actuate.endpoint.EndpointInfo;
import org.springframework.boot.actuate.endpoint.Operation;
import org.springframework.boot.actuate.endpoint.OperationType;
import org.springframework.boot.actuate.endpoint.cache.CachingConfiguration;
import org.springframework.boot.actuate.endpoint.cache.CachingConfigurationFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.ObjectUtils;

/**
 * A base {@link EndpointDiscoverer} implementation that discovers {@link Endpoint} beans
 * in an application context.
 *
 * @param <T> the type of the operation
 * @param <K> the type of the operation key
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public abstract class AnnotationEndpointDiscoverer<T extends Operation, K>
		implements EndpointDiscoverer<T> {

	private final ApplicationContext applicationContext;

	private final EndpointOperationFactory<T> operationFactory;

	private final Function<T, K> operationKeyFactory;

	private final CachingConfigurationFactory cachingConfigurationFactory;

	protected AnnotationEndpointDiscoverer(ApplicationContext applicationContext,
			EndpointOperationFactory<T> operationFactory,
			Function<T, K> operationKeyFactory,
			CachingConfigurationFactory cachingConfigurationFactory) {
		this.applicationContext = applicationContext;
		this.operationFactory = operationFactory;
		this.operationKeyFactory = operationKeyFactory;
		this.cachingConfigurationFactory = cachingConfigurationFactory;
	}

	/**
	 * Perform endpoint discovery, including discovery and merging of extensions.
	 * @param extensionType the annotation type of the extension
	 * @param exposure the {@link EndpointExposure} that should be considered
	 * @return the list of {@link EndpointInfo EndpointInfos} that describes the
	 * discovered endpoints matching the specified {@link EndpointExposure}
	 */
	protected Collection<EndpointInfoDescriptor<T, K>> discoverEndpoints(
			Class<? extends Annotation> extensionType, EndpointExposure exposure) {
		Map<Class<?>, EndpointInfo<T>> endpoints = discoverEndpoints(exposure);
		Map<Class<?>, EndpointExtensionInfo<T>> extensions = discoverExtensions(endpoints,
				extensionType, exposure);
		Collection<EndpointInfoDescriptor<T, K>> result = new ArrayList<>();
		endpoints.forEach((endpointClass, endpointInfo) -> {
			EndpointExtensionInfo<T> extension = extensions.remove(endpointClass);
			result.add(createDescriptor(endpointClass, endpointInfo, extension));
		});
		return result;
	}

	private Map<Class<?>, EndpointInfo<T>> discoverEndpoints(EndpointExposure exposure) {
		String[] beanNames = BeanFactoryUtils.beanNamesForAnnotationIncludingAncestors(
				this.applicationContext, Endpoint.class);
		Map<Class<?>, EndpointInfo<T>> endpoints = new LinkedHashMap<>();
		Map<String, EndpointInfo<T>> endpointsById = new LinkedHashMap<>();
		for (String beanName : beanNames) {
			Class<?> beanType = this.applicationContext.getType(beanName);
			AnnotationAttributes attributes = AnnotatedElementUtils
					.findMergedAnnotationAttributes(beanType, Endpoint.class, true, true);
			if (isExposedOver(attributes, exposure)) {
				EndpointInfo<T> info = createEndpointInfo(beanName, beanType, attributes);
				EndpointInfo<T> previous = endpointsById.putIfAbsent(info.getId(), info);
				Assert.state(previous == null, () -> "Found two endpoints with the id '"
						+ info.getId() + "': " + info + " and " + previous);
				endpoints.put(beanType, info);
			}
		}
		return endpoints;
	}

	private EndpointInfo<T> createEndpointInfo(String beanName, Class<?> beanType,
			AnnotationAttributes attributes) {
		String id = attributes.getString("id");
		DefaultEnablement defaultEnablement = (DefaultEnablement) attributes
				.get("defaultEnablement");
		Map<Method, T> operations = discoverOperations(id, beanName, beanType);
		return new EndpointInfo<>(id, defaultEnablement, operations.values());
	}

	private Map<Class<?>, EndpointExtensionInfo<T>> discoverExtensions(
			Map<Class<?>, EndpointInfo<T>> endpoints,
			Class<? extends Annotation> extensionType, EndpointExposure exposure) {
		if (extensionType == null) {
			return Collections.emptyMap();
		}
		String[] beanNames = BeanFactoryUtils.beanNamesForAnnotationIncludingAncestors(
				this.applicationContext, extensionType);
		Map<Class<?>, EndpointExtensionInfo<T>> extensions = new HashMap<>();
		for (String beanName : beanNames) {
			Class<?> beanType = this.applicationContext.getType(beanName);
			Class<?> endpointType = getEndpointType(extensionType, beanType);
			AnnotationAttributes endpointAttributes = AnnotatedElementUtils
					.getMergedAnnotationAttributes(endpointType, Endpoint.class);
			Assert.state(isExposedOver(endpointAttributes, exposure),
					"Invalid extension " + beanType.getName() + "': endpoint '"
							+ endpointType.getName()
							+ "' does not support such extension");
			EndpointInfo<T> info = getEndpointInfo(endpoints, beanType, endpointType);
			Map<Method, T> operations = discoverOperations(info.getId(), beanName,
					beanType);
			EndpointExtensionInfo<T> extension = new EndpointExtensionInfo<>(beanType,
					operations.values());
			EndpointExtensionInfo<T> previous = extensions.putIfAbsent(endpointType,
					extension);
			Assert.state(previous == null,
					() -> "Found two extensions for the same endpoint '"
							+ endpointType.getName() + "': "
							+ extension.getExtensionType().getName() + " and "
							+ previous.getExtensionType().getName());
		}
		return extensions;

	}

	private EndpointInfo<T> getEndpointInfo(Map<Class<?>, EndpointInfo<T>> endpoints,
			Class<?> beanType, Class<?> endpointClass) {
		EndpointInfo<T> endpoint = endpoints.get(endpointClass);
		Assert.state(endpoint != null, "Invalid extension '" + beanType.getName()
				+ "': no endpoint found with type '" + endpointClass.getName() + "'");
		return endpoint;
	}

	private Class<?> getEndpointType(Class<? extends Annotation> extensionType,
			Class<?> beanType) {
		AnnotationAttributes attributes = AnnotatedElementUtils
				.getMergedAnnotationAttributes(beanType, extensionType);
		return (Class<?>) attributes.get("endpoint");
	}

	private EndpointInfoDescriptor<T, K> createDescriptor(Class<?> type,
			EndpointInfo<T> info, EndpointExtensionInfo<T> extension) {
		Map<OperationKey<K>, List<T>> operations = indexOperations(info.getId(), type,
				info.getOperations());
		if (extension != null) {
			operations.putAll(indexOperations(info.getId(), extension.getExtensionType(),
					extension.getOperations()));
			return new EndpointInfoDescriptor<>(mergeEndpoint(info, extension),
					operations);
		}
		return new EndpointInfoDescriptor<>(info, operations);
	}

	private EndpointInfo<T> mergeEndpoint(EndpointInfo<T> endpoint,
			EndpointExtensionInfo<T> extension) {
		Map<K, T> operations = new HashMap<>();
		Consumer<T> consumer = (operation) -> operations
				.put(this.operationKeyFactory.apply(operation), operation);
		endpoint.getOperations().forEach(consumer);
		extension.getOperations().forEach(consumer);
		return new EndpointInfo<>(endpoint.getId(), endpoint.getDefaultEnablement(),
				operations.values());
	}

	private Map<OperationKey<K>, List<T>> indexOperations(String endpointId,
			Class<?> target, Collection<T> operations) {
		LinkedMultiValueMap<OperationKey<K>, T> result = new LinkedMultiValueMap<>();
		operations.forEach((operation) -> {
			K key = this.operationKeyFactory.apply(operation);
			result.add(new OperationKey<>(endpointId, target, key), operation);
		});
		return result;
	}

	private boolean isExposedOver(AnnotationAttributes attributes,
			EndpointExposure exposure) {
		if (exposure == null) {
			return true;
		}
		EndpointExposure[] supported = (EndpointExposure[]) attributes.get("exposure");
		return ObjectUtils.isEmpty(supported)
				|| ObjectUtils.containsElement(supported, exposure);
	}

	private Map<Method, T> discoverOperations(String id, String name, Class<?> type) {
		return MethodIntrospector.selectMethods(type,
				(MethodIntrospector.MetadataLookup<T>) (
						method) -> createOperationIfPossible(id, name, method));
	}

	private T createOperationIfPossible(String endpointId, String beanName,
			Method method) {
		T operation = createReadOperationIfPossible(endpointId, beanName, method);
		if (operation != null) {
			return operation;
		}
		operation = createWriteOperationIfPossible(endpointId, beanName, method);
		if (operation != null) {
			return operation;
		}
		return createDeleteOperationIfPossible(endpointId, beanName, method);
	}

	private T createReadOperationIfPossible(String endpointId, String beanName,
			Method method) {
		return createOperationIfPossible(endpointId, beanName, method,
				ReadOperation.class, OperationType.READ);
	}

	private T createWriteOperationIfPossible(String endpointId, String beanName,
			Method method) {
		return createOperationIfPossible(endpointId, beanName, method,
				WriteOperation.class, OperationType.WRITE);
	}

	private T createDeleteOperationIfPossible(String endpointId, String beanName,
			Method method) {
		return createOperationIfPossible(endpointId, beanName, method,
				DeleteOperation.class, OperationType.DELETE);
	}

	private T createOperationIfPossible(String endpointId, String beanName, Method method,
			Class<? extends Annotation> operationAnnotation,
			OperationType operationType) {
		AnnotationAttributes operationAttributes = AnnotatedElementUtils
				.getMergedAnnotationAttributes(method, operationAnnotation);
		if (operationAttributes == null) {
			return null;
		}
		CachingConfiguration cachingConfiguration = this.cachingConfigurationFactory
				.getCachingConfiguration(endpointId);
		return this.operationFactory.createOperation(endpointId, operationAttributes,
				this.applicationContext.getBean(beanName), method, operationType,
				determineTimeToLive(cachingConfiguration, operationType, method));
	}

	private long determineTimeToLive(CachingConfiguration cachingConfiguration,
			OperationType operationType, Method method) {
		if (cachingConfiguration != null && cachingConfiguration.getTimeToLive() > 0
				&& operationType == OperationType.READ
				&& method.getParameters().length == 0) {
			return cachingConfiguration.getTimeToLive();
		}
		return 0;
	}

	/**
	 * An {@code EndpointOperationFactory} creates an {@link Operation} for an operation
	 * on an endpoint.
	 *
	 * @param <T> the {@link Operation} type
	 */
	@FunctionalInterface
	protected interface EndpointOperationFactory<T extends Operation> {

		/**
		 * Creates an {@code EndpointOperation} for an operation on an endpoint.
		 * @param endpointId the id of the endpoint
		 * @param operationAttributes the annotation attributes for the operation
		 * @param target the target that implements the operation
		 * @param operationMethod the method on the bean that implements the operation
		 * @param operationType the type of the operation
		 * @param timeToLive the caching period in milliseconds
		 * @return the operation info that describes the operation
		 */
		T createOperation(String endpointId, AnnotationAttributes operationAttributes,
				Object target, Method operationMethod, OperationType operationType,
				long timeToLive);

	}

	/**
	 * Describes a tech-specific extension of an endpoint.
	 * @param <T> the type of the operation
	 */
	private static final class EndpointExtensionInfo<T extends Operation> {

		private final Class<?> extensionType;

		private final Collection<T> operations;

		private EndpointExtensionInfo(Class<?> extensionType, Collection<T> operations) {
			this.extensionType = extensionType;
			this.operations = operations;
		}

		private Class<?> getExtensionType() {
			return this.extensionType;
		}

		private Collection<T> getOperations() {
			return this.operations;
		}

	}

	/**
	 * Describes an {@link EndpointInfo endpoint} and whether or not it is valid.
	 *
	 * @param <T> the type of the operation
	 * @param <K> the type of the operation key
	 */
	protected static class EndpointInfoDescriptor<T extends Operation, K> {

		private final EndpointInfo<T> endpointInfo;

		private final Map<OperationKey<K>, List<T>> operations;

		protected EndpointInfoDescriptor(EndpointInfo<T> endpointInfo,
				Map<OperationKey<K>, List<T>> operations) {
			this.endpointInfo = endpointInfo;
			this.operations = operations;
		}

		public EndpointInfo<T> getEndpointInfo() {
			return this.endpointInfo;
		}

		public Map<OperationKey<K>, List<T>> findDuplicateOperations() {
			Map<OperationKey<K>, List<T>> duplicateOperations = new HashMap<>();
			this.operations.forEach((k, list) -> {
				if (list.size() > 1) {
					duplicateOperations.put(k, list);
				}
			});
			return duplicateOperations;
		}

	}

	/**
	 * Define the key of an operation in the context of an operation's implementation.
	 *
	 * @param <K> the type of the key
	 */
	protected static final class OperationKey<K> {

		private final String endpointId;

		private final Class<?> endpointType;

		private final K key;

		public OperationKey(String endpointId, Class<?> endpointType, K key) {
			this.endpointId = endpointId;
			this.endpointType = endpointType;
			this.key = key;
		}

		@Override
		public boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			OperationKey<?> other = (OperationKey<?>) o;
			Boolean result = true;
			result = result && this.endpointId.equals(other.endpointId);
			result = result && this.endpointType.equals(other.endpointType);
			result = result && this.key.equals(other.key);
			return result;
		}

		@Override
		public int hashCode() {
			int result = this.endpointId.hashCode();
			result = 31 * result + this.endpointType.hashCode();
			result = 31 * result + this.key.hashCode();
			return result;
		}

	}

}
