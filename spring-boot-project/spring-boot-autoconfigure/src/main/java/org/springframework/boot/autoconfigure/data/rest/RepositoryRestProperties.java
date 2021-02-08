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

package org.springframework.boot.autoconfigure.data.rest;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
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
	 * Whether to return a response body after creating an entity.
	 */
	private Boolean returnBodyOnCreate;

	/**
	 * Whether to return a response body after updating an entity.
	 */
	private Boolean returnBodyOnUpdate;

	/**
	 * Whether to enable enum value translation through the Spring Data REST default
	 * resource bundle.
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

	public void applyTo(RepositoryRestConfiguration rest) {
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(this::getBasePath).to(rest::setBasePath);
		map.from(this::getDefaultPageSize).to(rest::setDefaultPageSize);
		map.from(this::getMaxPageSize).to(rest::setMaxPageSize);
		map.from(this::getPageParamName).to(rest::setPageParamName);
		map.from(this::getLimitParamName).to(rest::setLimitParamName);
		map.from(this::getSortParamName).to(rest::setSortParamName);
		map.from(this::getDetectionStrategy).to(rest::setRepositoryDetectionStrategy);
		map.from(this::getDefaultMediaType).to(rest::setDefaultMediaType);
		map.from(this::getReturnBodyOnCreate).to(rest::setReturnBodyOnCreate);
		map.from(this::getReturnBodyOnUpdate).to(rest::setReturnBodyOnUpdate);
		map.from(this::getEnableEnumTranslation).to(rest::setEnableEnumTranslation);
	}

}
