/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.classpath;

import org.springframework.boot.devtools.filewatch.ChangedFile;

/**
 * Strategy interface used to determine when a changed classpath file should trigger a
 * full application restart. For example, static web resources might not require a full
 * restart whereas class files would.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see PatternClassPathRestartStrategy
 */
@FunctionalInterface
public interface ClassPathRestartStrategy {

	/**
	 * Return true if a full restart is required.
	 * @param file the changed file
	 * @return {@code true} if a full restart is required
	 */
	boolean isRestartRequired(ChangedFile file);

}
