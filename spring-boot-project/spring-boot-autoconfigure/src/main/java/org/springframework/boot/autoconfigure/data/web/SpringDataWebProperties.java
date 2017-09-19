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
