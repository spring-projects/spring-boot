/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson;

import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.module.SimpleModule;

/**
 * {@link JacksonModule} to find and {@link SimpleModule#setMixInAnnotation(Class, Class)
 * register} {@link JacksonMixin @JacksonMixin}-annotated classes.
 *
 * @author Guirong Hu
 * @author Stephane Nicoll
 * @since 4.0.0
 * @see JacksonMixin
 */
public class JacksonMixinModule extends SimpleModule {

	/**
	 * Register the specified {@link JacksonMixinModuleEntries entries}.
	 * @param entries the entries to register to this instance
	 * @param classLoader the classloader to use
	 */
	public void registerEntries(JacksonMixinModuleEntries entries, @Nullable ClassLoader classLoader) {
		entries.doWithEntry(classLoader, this::setMixInAnnotation);
	}

}
