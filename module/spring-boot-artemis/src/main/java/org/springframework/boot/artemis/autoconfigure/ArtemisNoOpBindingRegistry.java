/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.artemis.autoconfigure;

import org.apache.activemq.artemis.spi.core.naming.BindingRegistry;
import org.jspecify.annotations.Nullable;

/**
 * A no-op implementation of the {@link BindingRegistry}.
 *
 * @author Eddú Meléndez
 * @author Stephane Nicoll
 * @since 4.0.0
 */
public class ArtemisNoOpBindingRegistry implements BindingRegistry {

	@Override
	public @Nullable Object lookup(String s) {
		return null;
	}

	@Override
	public boolean bind(String s, Object o) {
		return false;
	}

	@Override
	public void unbind(String s) {
	}

	@Override
	public void close() {
	}

}
