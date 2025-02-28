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

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;

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

	/**
	 * Register the specified {@link JsonMixinModuleEntries entries}.
	 * @param entries the entries to register to this instance
	 * @param classLoader the classloader to use
	 */
	public void registerEntries(JsonMixinModuleEntries entries, ClassLoader classLoader) {
		entries.doWithEntry(classLoader, this::setMixInAnnotation);
	}

}
