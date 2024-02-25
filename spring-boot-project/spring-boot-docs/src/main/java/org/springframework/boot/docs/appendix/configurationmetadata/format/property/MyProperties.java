/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.docs.appendix.configurationmetadata.format.property;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.DeprecatedConfigurationProperty;

/**
 * MyProperties class.
 */
@ConfigurationProperties("my.app")
public class MyProperties {

	private String name;

	/**
     * Returns the name of the MyProperties object.
     *
     * @return the name of the MyProperties object
     */
    public String getName() {
		return this.name;
	}

	/**
     * Sets the name of the property.
     * 
     * @param name the name to be set
     */
    public void setName(String name) {
		this.name = name;
	}

	/**
     * Retrieves the target value.
     * 
     * @return the target value
     * 
     * @deprecated This method is deprecated. Use {@link #getAppName()} instead.
     *             The replacement property is {@code my.app.name}.
     */
    @Deprecated
	@DeprecatedConfigurationProperty(replacement = "my.app.name")
	public String getTarget() {
		return this.name;
	}

	/**
     * Sets the target for this MyProperties object.
     * 
     * @param target the target to be set
     * 
     * @deprecated This method is deprecated and should not be used. Use a different method instead.
     */
    @Deprecated
	public void setTarget(String target) {
		this.name = target;
	}

}
