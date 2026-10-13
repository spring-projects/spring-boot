/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.web.mappings;

import java.util.List;

import org.springframework.context.ApplicationContext;

/**
 * A {@link MappingDescriptionProvider} provides a {@link List} of mapping descriptions
 * through implementation-specific introspection of an application context.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public interface MappingDescriptionProvider {

	/**
	 * Returns the name of the mappings described by this provider.
	 * @return the name of the mappings
	 */
	String getMappingName();

	/**
	 * Produce the descriptions of the mappings identified by this provider in the given
	 * {@code context}.
	 * @param context the application context to introspect
	 * @return the mapping descriptions
	 */
	Object describeMappings(ApplicationContext context);

}
