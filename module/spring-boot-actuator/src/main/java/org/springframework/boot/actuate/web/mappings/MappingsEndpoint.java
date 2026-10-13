/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.web.mappings;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.OperationResponseBody;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.ApplicationContext;

/**
 * {@link Endpoint @Endpoint} to expose HTTP request mappings.
 *
 * @author Andy Wilkinson
 * @author Lee JiWon
 * @since 2.0.0
 */
@Endpoint(id = "mappings")
public class MappingsEndpoint {

	private final Collection<MappingDescriptionProvider> descriptionProviders;

	private final ApplicationContext context;

	public MappingsEndpoint(Collection<MappingDescriptionProvider> descriptionProviders, ApplicationContext context) {
		this.descriptionProviders = descriptionProviders;
		this.context = context;
	}

	@ReadOperation
	public ApplicationMappingsDescriptor mappings() {
		ApplicationContext target = this.context;
		Map<@Nullable String, ContextMappingsDescriptor> contextMappings = new HashMap<>();
		while (target != null) {
			contextMappings.put(target.getId(), mappingsForContext(target));
			target = target.getParent();
		}
		return new ApplicationMappingsDescriptor(contextMappings);
	}

	private ContextMappingsDescriptor mappingsForContext(ApplicationContext applicationContext) {
		Map<String, Object> mappings = new HashMap<>();
		this.descriptionProviders.forEach(
				(provider) -> mappings.put(provider.getMappingName(), provider.describeMappings(applicationContext)));
		ApplicationContext parent = applicationContext.getParent();
		return new ContextMappingsDescriptor(mappings, (parent != null) ? parent.getId() : null);
	}

	/**
	 * Description of an application's request mappings.
	 */
	public static final class ApplicationMappingsDescriptor implements OperationResponseBody {

		private final Map<@Nullable String, ContextMappingsDescriptor> contextMappings;

		private ApplicationMappingsDescriptor(Map<@Nullable String, ContextMappingsDescriptor> contextMappings) {
			this.contextMappings = contextMappings;
		}

		public Map<@Nullable String, ContextMappingsDescriptor> getContexts() {
			return this.contextMappings;
		}

	}

	/**
	 * Description of an application context's request mappings.
	 */
	public static final class ContextMappingsDescriptor {

		private final Map<String, Object> mappings;

		private final @Nullable String parentId;

		private ContextMappingsDescriptor(Map<String, Object> mappings, @Nullable String parentId) {
			this.mappings = mappings;
			this.parentId = parentId;
		}

		public @Nullable String getParentId() {
			return this.parentId;
		}

		public Map<String, Object> getMappings() {
			return this.mappings;
		}

	}

}
