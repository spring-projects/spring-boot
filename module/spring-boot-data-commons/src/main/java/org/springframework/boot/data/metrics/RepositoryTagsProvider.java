/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.metrics;

import io.micrometer.core.instrument.Tag;

import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocation;

/**
 * Provides {@link Tag Tags} for Spring Data {@link RepositoryMethodInvocation Repository
 * invocations}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@FunctionalInterface
public interface RepositoryTagsProvider {

	/**
	 * Provides tags to be associated with metrics for the given {@code invocation}.
	 * @param invocation the repository invocation
	 * @return tags to associate with metrics for the invocation
	 */
	Iterable<Tag> repositoryTags(RepositoryMethodInvocation invocation);

}
