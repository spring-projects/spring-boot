/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.configurationmetadata.changelog;

/**
 * The type of a difference in the metadata.
 *
 * @author Andy Wilkinson
 */
enum DifferenceType {

	/**
	 * The entry has been added.
	 */
	ADDED,

	/**
	 * The entry has been made deprecated. It may or may not still exist in the previous
	 * version.
	 */
	DEPRECATED,

	/**
	 * The entry has been deleted.
	 */
	DELETED,

	/**
	 * The entry's default value has changed. The entry exists in both the previous and
	 * current versions, but its default value is different.
	 */
	DEFAULT_CHANGED

}
