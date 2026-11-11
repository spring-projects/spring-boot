/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging;

import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Factory that can be used to create multiple {@link DeferredLog} instances that will
 * switch over when appropriate.
 *
 * @author Phillip Webb
 * @since 2.4.0
 * @see DeferredLogs
 */
@FunctionalInterface
public interface DeferredLogFactory {

	/**
	 * Create a new {@link DeferredLog} for the given destination.
	 * @param destination the ultimate log destination
	 * @return a deferred log instance that will switch to the destination when
	 * appropriate.
	 */
	default Log getLog(Class<?> destination) {
		return getLog(() -> LogFactory.getLog(destination));
	}

	/**
	 * Create a new {@link DeferredLog} for the given destination.
	 * @param destination the ultimate log destination
	 * @return a deferred log instance that will switch to the destination when
	 * appropriate.
	 */
	default Log getLog(Log destination) {
		return getLog(() -> destination);
	}

	/**
	 * Create a new {@link DeferredLog} for the given destination.
	 * @param destination the ultimate log destination
	 * @return a deferred log instance that will switch to the destination when
	 * appropriate.
	 */
	Log getLog(Supplier<Log> destination);

}
