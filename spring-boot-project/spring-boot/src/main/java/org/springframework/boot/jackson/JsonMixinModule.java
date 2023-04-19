/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.jackson;

import java.util.Collection;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;

import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

/**
 * Spring Bean and Jackson {@link Module} to find and
 * {@link SimpleModule#setMixInAnnotation(Class, Class) register}
 * {@link JsonMixin @JsonMixin}-annotated classes.
 *
 * @author Guirong Hu
 * @author Stephane Nicoll
 * @since 2.7.0
 * @see JsonMixin
 */
public class JsonMixinModule extends SimpleModule {

	public JsonMixinModule() {
	}

	/**
	 * Create a new {@link JsonMixinModule} instance.
	 * @param context the source application context
	 * @param basePackages the packages to check for annotated classes
	 * @deprecated since 3.0.0 in favor of
	 * {@link #registerEntries(JsonMixinModuleEntries, ClassLoader)}
	 */
	@Deprecated(since = "3.0.0", forRemoval = true)
	public JsonMixinModule(ApplicationContext context, Collection<String> basePackages) {
		Assert.notNull(context, "Context must not be null");
		registerEntries(JsonMixinModuleEntries.scan(context, basePackages), context.getClassLoader());
	}

	/**
	 * Register the specified {@link JsonMixinModuleEntries entries}.
	 * @param entries the entries to register to this instance
	 * @param classLoader the classloader to use
	 */
	public void registerEntries(JsonMixinModuleEntries entries, ClassLoader classLoader) {
		entries.doWithEntry(classLoader, this::setMixInAnnotation);
	}

}
