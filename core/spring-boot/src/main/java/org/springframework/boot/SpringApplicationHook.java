/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot;

import org.jspecify.annotations.Nullable;

/**
 * Low-level hook that can be used to attach a {@link SpringApplicationRunListener} to a
 * {@link SpringApplication} in order to observe or modify its behavior. Hooks are managed
 * on a per-thread basis providing isolation when multiple applications are executed in
 * parallel.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.0.0
 * @see SpringApplication#withHook
 */
@FunctionalInterface
public interface SpringApplicationHook {

	/**
	 * Return the {@link SpringApplicationRunListener} that should be hooked into the
	 * given {@link SpringApplication}.
	 * @param springApplication the source {@link SpringApplication} instance
	 * @return the {@link SpringApplicationRunListener} to attach or {@code null}
	 */
	@Nullable SpringApplicationRunListener getRunListener(SpringApplication springApplication);

}
