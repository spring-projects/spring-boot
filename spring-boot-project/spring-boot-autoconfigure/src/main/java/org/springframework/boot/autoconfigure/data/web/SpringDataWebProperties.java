/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Spring Data Web.
 *
 * @author Vedran Pavic
 * @since 2.0.0
 */
@ConfigurationProperties("spring.data.web")
public class SpringDataWebProperties {

	private final Pageable pageable = new Pageable();

	private final Sort sort = new Sort();

	/**
	 * Returns the Pageable object associated with this instance.
	 * @return the Pageable object
	 */
	public Pageable getPageable() {
		return this.pageable;
	}

	/**
	 * Returns the sort object associated with this SpringDataWebProperties instance.
	 * @return the sort object
	 */
	public Sort getSort() {
		return this.sort;
	}

	/**
	 * Pageable properties.
	 */
	public static class Pageable {

		/**
		 * Page index parameter name.
		 */
		private String pageParameter = "page";

		/**
		 * Page size parameter name.
		 */
		private String sizeParameter = "size";

		/**
		 * Whether to expose and assume 1-based page number indexes. Defaults to "false",
		 * meaning a page number of 0 in the request equals the first page.
		 */
		private boolean oneIndexedParameters = false;

		/**
		 * General prefix to be prepended to the page number and page size parameters.
		 */
		private String prefix = "";

		/**
		 * Delimiter to be used between the qualifier and the actual page number and size
		 * properties.
		 */
		private String qualifierDelimiter = "_";

		/**
		 * Default page size.
		 */
		private int defaultPageSize = 20;

		/**
		 * Maximum page size to be accepted.
		 */
		private int maxPageSize = 2000;

		/**
		 * Returns the value of the page parameter.
		 * @return the value of the page parameter
		 */
		public String getPageParameter() {
			return this.pageParameter;
		}

		/**
		 * Sets the page parameter for the Pageable object.
		 * @param pageParameter the page parameter to be set
		 */
		public void setPageParameter(String pageParameter) {
			this.pageParameter = pageParameter;
		}

		/**
		 * Returns the size parameter used for pagination.
		 * @return the size parameter
		 */
		public String getSizeParameter() {
			return this.sizeParameter;
		}

		/**
		 * Sets the size parameter for pagination.
		 * @param sizeParameter the size parameter to be set
		 */
		public void setSizeParameter(String sizeParameter) {
			this.sizeParameter = sizeParameter;
		}

		/**
		 * Returns a boolean value indicating whether the parameters in the Pageable class
		 * are one-indexed.
		 * @return {@code true} if the parameters are one-indexed, {@code false}
		 * otherwise.
		 */
		public boolean isOneIndexedParameters() {
			return this.oneIndexedParameters;
		}

		/**
		 * Sets the flag indicating whether the parameters for pagination should be
		 * one-indexed.
		 * @param oneIndexedParameters true if the parameters should be one-indexed, false
		 * otherwise
		 */
		public void setOneIndexedParameters(boolean oneIndexedParameters) {
			this.oneIndexedParameters = oneIndexedParameters;
		}

		/**
		 * Returns the prefix used for pagination.
		 * @return the prefix used for pagination
		 */
		public String getPrefix() {
			return this.prefix;
		}

		/**
		 * Sets the prefix for the Pageable object.
		 * @param prefix the prefix to be set
		 */
		public void setPrefix(String prefix) {
			this.prefix = prefix;
		}

		/**
		 * Returns the qualifier delimiter used in the Pageable class.
		 * @return the qualifier delimiter used in the Pageable class
		 */
		public String getQualifierDelimiter() {
			return this.qualifierDelimiter;
		}

		/**
		 * Sets the qualifier delimiter for the Pageable object.
		 * @param qualifierDelimiter the delimiter to be used for qualifying the Pageable
		 * object
		 */
		public void setQualifierDelimiter(String qualifierDelimiter) {
			this.qualifierDelimiter = qualifierDelimiter;
		}

		/**
		 * Returns the default page size.
		 * @return the default page size
		 */
		public int getDefaultPageSize() {
			return this.defaultPageSize;
		}

		/**
		 * Sets the default page size for pagination.
		 * @param defaultPageSize the default page size to be set
		 */
		public void setDefaultPageSize(int defaultPageSize) {
			this.defaultPageSize = defaultPageSize;
		}

		/**
		 * Returns the maximum page size.
		 * @return the maximum page size
		 */
		public int getMaxPageSize() {
			return this.maxPageSize;
		}

		/**
		 * Sets the maximum page size for pagination.
		 * @param maxPageSize the maximum page size to be set
		 */
		public void setMaxPageSize(int maxPageSize) {
			this.maxPageSize = maxPageSize;
		}

	}

	/**
	 * Sort properties.
	 */
	public static class Sort {

		/**
		 * Sort parameter name.
		 */
		private String sortParameter = "sort";

		/**
		 * Returns the sort parameter.
		 * @return the sort parameter
		 */
		public String getSortParameter() {
			return this.sortParameter;
		}

		/**
		 * Sets the sort parameter for sorting the data.
		 * @param sortParameter the sort parameter to be set
		 */
		public void setSortParameter(String sortParameter) {
			this.sortParameter = sortParameter;
		}

	}

}
