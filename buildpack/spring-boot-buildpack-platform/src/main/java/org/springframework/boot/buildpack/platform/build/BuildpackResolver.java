/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.build;

import org.jspecify.annotations.Nullable;

/**
 * Strategy interface used to resolve a {@link BuildpackReference} to a {@link Buildpack}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @see BuildpackResolvers
 */
interface BuildpackResolver {

	/**
	 * Attempt to resolve the given {@link BuildpackReference}.
	 * @param context the resolver context
	 * @param reference the reference to resolve
	 * @return a resolved {@link Buildpack} instance or {@code null}
	 */
	@Nullable Buildpack resolve(BuildpackResolverContext context, BuildpackReference reference);

}
