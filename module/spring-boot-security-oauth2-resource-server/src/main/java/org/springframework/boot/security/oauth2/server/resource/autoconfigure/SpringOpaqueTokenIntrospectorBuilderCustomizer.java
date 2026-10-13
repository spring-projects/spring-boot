/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.security.oauth2.server.resource.autoconfigure;

import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector.Builder;

/**
 * Callback interface for the customization of the {@link Builder
 * SpringOpaqueTokenIntrospector.Builder} used to create the auto-configured
 * {@link SpringOpaqueTokenIntrospector}.
 *
 * @author Vishnutheep B
 * @since 4.1.0
 */
@FunctionalInterface
public interface SpringOpaqueTokenIntrospectorBuilderCustomizer {

	/**
	 * Customize the given {@code builder}.
	 * @param builder the {@code builder} to customize
	 */
	void customize(SpringOpaqueTokenIntrospector.Builder builder);

}
