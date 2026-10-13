/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jackson2;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.jspecify.annotations.Nullable;

/**
 * Spring Bean and Jackson {@link Module} to find and
 * {@link SimpleModule#setMixInAnnotation(Class, Class) register}
 * {@link JsonMixin @JsonMixin}-annotated classes.
 *
 * @author Guirong Hu
 * @author Stephane Nicoll
 * @since 4.0.0
 * @see JsonMixin
 * @deprecated since 4.0.0 for removal in 4.3.0 in favor of Jackson 3 and
 * {@link org.springframework.boot.jackson.JacksonMixinModule}
 */
@Deprecated(since = "4.0.0", forRemoval = true)
@SuppressWarnings("removal")
public class JsonMixinModule extends SimpleModule {

	/**
	 * Register the specified {@link JsonMixinModuleEntries entries}.
	 * @param entries the entries to register to this instance
	 * @param classLoader the classloader to use
	 */
	public void registerEntries(JsonMixinModuleEntries entries, @Nullable ClassLoader classLoader) {
		entries.doWithEntry(classLoader, this::setMixInAnnotation);
	}

}
