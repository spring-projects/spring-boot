/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webmvc.actuate.endpoint.web;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.actuate.endpoint.web.EndpointMapping;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.ExposableWebEndpoint;
import org.springframework.boot.actuate.endpoint.web.WebOperation;
import org.springframework.boot.actuate.endpoint.web.WebOperationRequestPredicate;
import org.springframework.boot.health.actuate.endpoint.AdditionalHealthEndpointPath;
import org.springframework.boot.health.actuate.endpoint.HealthEndpointGroup;
import org.springframework.web.servlet.HandlerMapping;

/**
 * A custom {@link HandlerMapping} that allows health groups to be mapped to an additional
 * path.
 *
 * @author Madhura Bhave
 * @since 4.0.0
 */
public class AdditionalHealthEndpointPathsWebMvcHandlerMapping extends AbstractWebMvcEndpointHandlerMapping {

	private final @Nullable ExposableWebEndpoint healthEndpoint;

	private final Set<HealthEndpointGroup> groups;

	public AdditionalHealthEndpointPathsWebMvcHandlerMapping(@Nullable ExposableWebEndpoint healthEndpoint,
			Set<HealthEndpointGroup> groups) {
		super(new EndpointMapping(""), asList(healthEndpoint), new EndpointMediaTypes(), false);
		this.healthEndpoint = healthEndpoint;
		this.groups = groups;
	}

	private static Collection<ExposableWebEndpoint> asList(@Nullable ExposableWebEndpoint healthEndpoint) {
		return (healthEndpoint != null) ? Collections.singletonList(healthEndpoint) : Collections.emptyList();
	}

	@Override
	protected void initHandlerMethods() {
		if (this.healthEndpoint == null) {
			return;
		}
		for (WebOperation operation : this.healthEndpoint.getOperations()) {
			WebOperationRequestPredicate predicate = operation.getRequestPredicate();
			String matchAllRemainingPathSegmentsVariable = predicate.getMatchAllRemainingPathSegmentsVariable();
			if (matchAllRemainingPathSegmentsVariable != null) {
				for (HealthEndpointGroup group : this.groups) {
					AdditionalHealthEndpointPath additionalPath = group.getAdditionalPath();
					if (additionalPath != null) {
						registerMapping(this.healthEndpoint, predicate, operation, additionalPath.getValue());
					}
				}
			}
		}
	}

	@Override
	protected LinksHandler getLinksHandler() {
		return (request, response) -> null;
	}

}
