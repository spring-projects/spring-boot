/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.configurationmetadata;

/**
 * An extension of {@link ConfigurationMetadataProperty} that provides the a reference to
 * its source.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
class ConfigurationMetadataItem extends ConfigurationMetadataProperty {

	private String sourceType;

	private String sourceMethod;

	/**
	 * The class name of the source that contributed this property. For example, if the
	 * property was from a class annotated with {@code @ConfigurationProperties} this
	 * attribute would contain the fully qualified name of that class.
	 * @return the source type
	 */
	public String getSourceType() {
		return this.sourceType;
	}

	public void setSourceType(String sourceType) {
		this.sourceType = sourceType;
	}

	/**
	 * The full name of the method (including parenthesis and argument types) that
	 * contributed this property. For example, the name of a getter in a
	 * {@code @ConfigurationProperties} annotated class.
	 * @return the source method
	 */
	public String getSourceMethod() {
		return this.sourceMethod;
	}

	public void setSourceMethod(String sourceMethod) {
		this.sourceMethod = sourceMethod;
	}

}
