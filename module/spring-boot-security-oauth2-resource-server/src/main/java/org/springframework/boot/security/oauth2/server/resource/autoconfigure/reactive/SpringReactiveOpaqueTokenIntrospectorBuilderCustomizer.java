/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.server.resource.autoconfigure.reactive;

import org.springframework.security.oauth2.server.resource.introspection.SpringReactiveOpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringReactiveOpaqueTokenIntrospector.Builder;

/**
 * Callback interface for the customization of the {@link Builder
 * SpringReactiveOpaqueTokenIntrospector.Builder} used to create the auto-configured
 * {@link SpringReactiveOpaqueTokenIntrospector}.
 *
 * @author Vishnutheep B
 * @since 4.1.0
 */
@FunctionalInterface
public interface SpringReactiveOpaqueTokenIntrospectorBuilderCustomizer {

	/**
	 * Customize the given {@code builder}.
	 * @param builder the {@code builder} to customize
	 */
	void customize(SpringReactiveOpaqueTokenIntrospector.Builder builder);

}
