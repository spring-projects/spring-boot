/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.freemarker;

import java.util.Map;

import freemarker.template.Configuration;

import org.springframework.ui.freemarker.FreeMarkerConfigurationFactory;

/**
 * Callback interface that can be implemented by beans wishing to customize the FreeMarker
 * variables used as {@link Configuration#getSharedVariableNames() shared variables}
 * before it is used by an auto-configured {@link FreeMarkerConfigurationFactory}.
 *
 * @author Stephane Nicoll
 * @since 3.4.0
 */
@FunctionalInterface
public interface FreeMarkerVariablesCustomizer {

	/**
	 * Customize the {@code variables} to be set as well-known FreeMarker objects.
	 * @param variables the variables to customize
	 * @see FreeMarkerConfigurationFactory#setFreemarkerVariables(Map)
	 */
	void customizeFreeMarkerVariables(Map<String, Object> variables);

}
