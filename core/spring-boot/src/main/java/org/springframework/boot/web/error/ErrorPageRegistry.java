/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.error;

/**
 * Interface for a registry that holds {@link ErrorPage ErrorPages}.
 *
 * @author Phillip Webb
 * @since 4.0.0
 */
@FunctionalInterface
public interface ErrorPageRegistry {

	/**
	 * Adds error pages that will be used when handling exceptions.
	 * @param errorPages the error pages
	 */
	void addErrorPages(ErrorPage... errorPages);

}
