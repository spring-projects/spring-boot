/*
 * Copyright 2012-present the original author or authors.
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
