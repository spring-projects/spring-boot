/*
 * Copyright 2012-2017 the original author or authors.
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

	public Pageable getPageable() {
		return this.pageable;
	}

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
		 * Default page size.
		 */
		private int defaultPageSize = 20;

		/**
		 * Configures a general prefix to be prepended to the page number and page size parameters.
		 */
		private String prefix;

		/**
		 * Configures the delimiter to be used between the qualifier and the actual page number and size properties.
		 */
		private String qualifierDelimiter;

		/**
		 * Configures the maximum page size to be accepted.
		 */
		private int maxPageSize = 2000;

		/**
		 * Whether to expose and assume 1-based page number indexes in the request parameters.
		 * Defaults to {@literal false}, meaning a page number of 0 in the request equals the first page.
		 * If this is set to {@literal true}, a page number of 1 in the request will be considered the first page.
		 */
		private boolean oneIndexedParameters = false;

		public String getPageParameter() {
			return this.pageParameter;
		}

		public void setPageParameter(String pageParameter) {
			this.pageParameter = pageParameter;
		}

		public String getSizeParameter() {
			return this.sizeParameter;
		}

		public void setSizeParameter(String sizeParameter) {
			this.sizeParameter = sizeParameter;
		}

		public int getDefaultPageSize() {
			return this.defaultPageSize;
		}

		public void setDefaultPageSize(int defaultPageSize) {
			this.defaultPageSize = defaultPageSize;
		}

		public String getPrefix() {
			return prefix;
		}

		public void setPrefix(final String prefix) {
			this.prefix = prefix;
		}

		public String getQualifierDelimiter() {
			return qualifierDelimiter;
		}

		public void setQualifierDelimiter(final String qualifierDelimiter) {
			this.qualifierDelimiter = qualifierDelimiter;
		}

		public int getMaxPageSize() {
			return maxPageSize;
		}

		public void setMaxPageSize(final int maxPageSize) {
			this.maxPageSize = maxPageSize;
		}

		public boolean isOneIndexedParameters() {
			return this.oneIndexedParameters;
		}

		public void setOneIndexedParameters(final boolean oneIndexedParameters) {
			this.oneIndexedParameters = oneIndexedParameters;
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

		public String getSortParameter() {
			return this.sortParameter;
		}

		public void setSortParameter(String sortParameter) {
			this.sortParameter = sortParameter;
		}

	}

}
