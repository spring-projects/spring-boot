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

package org.springframework.boot.autoconfigure.data.rest;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;
import org.springframework.http.MediaType;

/**
 * Configuration properties for Spring Data REST.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.data.rest")
public class RepositoryRestProperties {

	/**
	 * Base path to be used by Spring Data REST to expose repository resources.
	 */
	private String basePath;

	/**
	 * Default size of pages.
	 */
	private Integer defaultPageSize;

	/**
	 * Maximum size of pages.
	 */
	private Integer maxPageSize;

	/**
	 * Name of the URL query string parameter that indicates what page to return.
	 */
	private String pageParamName;

	/**
	 * Name of the URL query string parameter that indicates how many results to return at
	 * once.
	 */
	private String limitParamName;

	/**
	 * Name of the URL query string parameter that indicates what direction to sort
	 * results.
	 */
	private String sortParamName;

	/**
	 * Strategy to use to determine which repositories get exposed.
	 */
	private RepositoryDetectionStrategies detectionStrategy = RepositoryDetectionStrategies.DEFAULT;

	/**
	 * Content type to use as a default when none is specified.
	 */
	private MediaType defaultMediaType;

	/**
	 * Return a response body after creating an entity.
	 */
	private Boolean returnBodyOnCreate;

	/**
	 * Return a response body after updating an entity.
	 */
	private Boolean returnBodyOnUpdate;

	/**
	 * Enable enum value translation via the Spring Data REST default resource bundle.
	 * Will use the fully qualified enum name as key.
	 */
	private Boolean enableEnumTranslation;

	public String getBasePath() {
		return this.basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public Integer getDefaultPageSize() {
		return this.defaultPageSize;
	}

	public void setDefaultPageSize(Integer defaultPageSize) {
		this.defaultPageSize = defaultPageSize;
	}

	public Integer getMaxPageSize() {
		return this.maxPageSize;
	}

	public void setMaxPageSize(Integer maxPageSize) {
		this.maxPageSize = maxPageSize;
	}

	public String getPageParamName() {
		return this.pageParamName;
	}

	public void setPageParamName(String pageParamName) {
		this.pageParamName = pageParamName;
	}

	public String getLimitParamName() {
		return this.limitParamName;
	}

	public void setLimitParamName(String limitParamName) {
		this.limitParamName = limitParamName;
	}

	public String getSortParamName() {
		return this.sortParamName;
	}

	public void setSortParamName(String sortParamName) {
		this.sortParamName = sortParamName;
	}

	public RepositoryDetectionStrategies getDetectionStrategy() {
		return this.detectionStrategy;
	}

	public void setDetectionStrategy(RepositoryDetectionStrategies detectionStrategy) {
		this.detectionStrategy = detectionStrategy;
	}

	public MediaType getDefaultMediaType() {
		return this.defaultMediaType;
	}

	public void setDefaultMediaType(MediaType defaultMediaType) {
		this.defaultMediaType = defaultMediaType;
	}

	public Boolean getReturnBodyOnCreate() {
		return this.returnBodyOnCreate;
	}

	public void setReturnBodyOnCreate(Boolean returnBodyOnCreate) {
		this.returnBodyOnCreate = returnBodyOnCreate;
	}

	public Boolean getReturnBodyOnUpdate() {
		return this.returnBodyOnUpdate;
	}

	public void setReturnBodyOnUpdate(Boolean returnBodyOnUpdate) {
		this.returnBodyOnUpdate = returnBodyOnUpdate;
	}

	public Boolean getEnableEnumTranslation() {
		return this.enableEnumTranslation;
	}

	public void setEnableEnumTranslation(Boolean enableEnumTranslation) {
		this.enableEnumTranslation = enableEnumTranslation;
	}

	public void applyTo(RepositoryRestConfiguration configuration) {
		if (this.basePath != null) {
			configuration.setBasePath(this.basePath);
		}
		if (this.defaultPageSize != null) {
			configuration.setDefaultPageSize(this.defaultPageSize);
		}
		if (this.maxPageSize != null) {
			configuration.setMaxPageSize(this.maxPageSize);
		}
		if (this.pageParamName != null) {
			configuration.setPageParamName(this.pageParamName);
		}
		if (this.limitParamName != null) {
			configuration.setLimitParamName(this.limitParamName);
		}
		if (this.sortParamName != null) {
			configuration.setSortParamName(this.sortParamName);
		}
		if (this.detectionStrategy != null) {
			configuration.setRepositoryDetectionStrategy(this.detectionStrategy);
		}
		if (this.defaultMediaType != null) {
			configuration.setDefaultMediaType(this.defaultMediaType);
		}
		if (this.returnBodyOnCreate != null) {
			configuration.setReturnBodyOnCreate(this.returnBodyOnCreate);
		}
		if (this.returnBodyOnUpdate != null) {
			configuration.setReturnBodyOnUpdate(this.returnBodyOnUpdate);
		}
		if (this.enableEnumTranslation != null) {
			configuration.setEnableEnumTranslation(this.enableEnumTranslation);
		}
	}

}
