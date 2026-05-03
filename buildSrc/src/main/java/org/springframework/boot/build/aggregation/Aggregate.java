/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.build.aggregation;

import org.gradle.api.Named;
import org.gradle.api.attributes.Category;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;

/**
 * An aggregate.
 *
 * @author Andy Wilkinson
 */
public interface Aggregate extends Named {

	/**
	 * The {@link Category} used to select the variant that's included in the aggregate.
	 * @return the category
	 */
	Property<String> getCategory();

	/**
	 * The {@link Usage} used to select the variant that's included in the aggregate.
	 * @return the usage
	 */
	Property<String> getUsage();

	/**
	 * The aggregated files.
	 * @return the aggregated files
	 */
	ConfigurableFileCollection getFiles();

}
