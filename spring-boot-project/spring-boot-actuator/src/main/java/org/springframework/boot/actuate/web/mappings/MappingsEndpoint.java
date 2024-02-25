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

package org.springframework.boot.actuate.web.mappings;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.ApplicationContext;

/**
 * {@link Endpoint @Endpoint} to expose HTTP request mappings.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@Endpoint(id = "mappings")
public class MappingsEndpoint {

	private final Collection<MappingDescriptionProvider> descriptionProviders;

	private final ApplicationContext context;

	/**
	 * Constructs a new MappingsEndpoint with the specified collection of
	 * MappingDescriptionProviders and ApplicationContext.
	 * @param descriptionProviders the collection of MappingDescriptionProviders to be
	 * used by this MappingsEndpoint
	 * @param context the ApplicationContext to be used by this MappingsEndpoint
	 */
	public MappingsEndpoint(Collection<MappingDescriptionProvider> descriptionProviders, ApplicationContext context) {
		this.descriptionProviders = descriptionProviders;
		this.context = context;
	}

	/**
	 * Retrieves the mappings for the application.
	 * @return The application mappings descriptor containing the mappings for each
	 * context.
	 */
	@ReadOperation
	public ApplicationMappingsDescriptor mappings() {
		ApplicationContext target = this.context;
		Map<String, ContextMappingsDescriptor> contextMappings = new HashMap<>();
		while (target != null) {
			contextMappings.put(target.getId(), mappingsForContext(target));
			target = target.getParent();
		}
		return new ApplicationMappingsDescriptor(contextMappings);
	}

	/**
	 * Returns the mappings for the given application context.
	 * @param applicationContext the application context for which to retrieve the
	 * mappings
	 * @return the context mappings descriptor containing the mappings and the parent
	 * context ID if applicable
	 */
	private ContextMappingsDescriptor mappingsForContext(ApplicationContext applicationContext) {
		Map<String, Object> mappings = new HashMap<>();
		this.descriptionProviders.forEach(
				(provider) -> mappings.put(provider.getMappingName(), provider.describeMappings(applicationContext)));
		return new ContextMappingsDescriptor(mappings,
				(applicationContext.getParent() != null) ? applicationContext.getId() : null);
	}

	/**
	 * Description of an application's request mappings.
	 */
	public static final class ApplicationMappingsDescriptor implements OperationResponseBody {

		private final Map<String, ContextMappingsDescriptor> contextMappings;

		/**
		 * Constructs a new ApplicationMappingsDescriptor with the specified context
		 * mappings.
		 * @param contextMappings the map of context mappings
		 */
		private ApplicationMappingsDescriptor(Map<String, ContextMappingsDescriptor> contextMappings) {
			this.contextMappings = contextMappings;
		}

		/**
		 * Returns the map of contexts and their corresponding ContextMappingsDescriptor
		 * objects.
		 * @return the map of contexts and their corresponding ContextMappingsDescriptor
		 * objects
		 */
		public Map<String, ContextMappingsDescriptor> getContexts() {
			return this.contextMappings;
		}

	}

	/**
	 * Description of an application context's request mappings.
	 */
	public static final class ContextMappingsDescriptor {

		private final Map<String, Object> mappings;

		private final String parentId;

		/**
		 * Constructs a new ContextMappingsDescriptor with the specified mappings and
		 * parentId.
		 * @param mappings the mappings to be set for the descriptor
		 * @param parentId the parentId to be set for the descriptor
		 */
		private ContextMappingsDescriptor(Map<String, Object> mappings, String parentId) {
			this.mappings = mappings;
			this.parentId = parentId;
		}

		/**
		 * Returns the parent ID of the ContextMappingsDescriptor.
		 * @return the parent ID of the ContextMappingsDescriptor
		 */
		public String getParentId() {
			return this.parentId;
		}

		/**
		 * Returns the mappings stored in the ContextMappingsDescriptor object.
		 * @return a Map containing the mappings, where the keys are of type String and
		 * the values are of type Object.
		 */
		public Map<String, Object> getMappings() {
			return this.mappings;
		}

	}

}
