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

package org.springframework.boot.data.rest.autoconfigure;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.core.mapping.RepositoryDetectionStrategy.RepositoryDetectionStrategies;
import org.springframework.http.MediaType;

/**
 * Configuration properties for Spring Data REST.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@ConfigurationProperties("spring.data.rest")
public class RepositoryRestProperties {

	/**
	 * Base path to be used by Spring Data REST to expose repository resources.
	 */
	private @Nullable String basePath;

	/**
	 * Default size of pages.
	 */
	private @Nullable Integer defaultPageSize;

	/**
	 * Maximum size of pages.
	 */
	private @Nullable Integer maxPageSize;

	/**
	 * Name of the URL query string parameter that indicates what page to return.
	 */
	private @Nullable String pageParamName;

	/**
	 * Name of the URL query string parameter that indicates how many results to return at
	 * once.
	 */
	private @Nullable String limitParamName;

	/**
	 * Name of the URL query string parameter that indicates what direction to sort
	 * results.
	 */
	private @Nullable String sortParamName;

	/**
	 * Strategy to use to determine which repositories get exposed.
	 */
	private RepositoryDetectionStrategies detectionStrategy = RepositoryDetectionStrategies.DEFAULT;

	/**
	 * Content type to use as a default when none is specified.
	 */
	private @Nullable MediaType defaultMediaType;

	/**
	 * Whether to return a response body after creating an entity.
	 */
	private @Nullable Boolean returnBodyOnCreate;

	/**
	 * Whether to return a response body after updating an entity.
	 */
	private @Nullable Boolean returnBodyOnUpdate;

	/**
	 * Whether to enable enum value translation through the Spring Data REST default
	 * resource bundle.
	 */
	private @Nullable Boolean enableEnumTranslation;

	public @Nullable String getBasePath() {
		return this.basePath;
	}

	public void setBasePath(@Nullable String basePath) {
		this.basePath = basePath;
	}

	public @Nullable Integer getDefaultPageSize() {
		return this.defaultPageSize;
	}

	public void setDefaultPageSize(@Nullable Integer defaultPageSize) {
		this.defaultPageSize = defaultPageSize;
	}

	public @Nullable Integer getMaxPageSize() {
		return this.maxPageSize;
	}

	public void setMaxPageSize(@Nullable Integer maxPageSize) {
		this.maxPageSize = maxPageSize;
	}

	public @Nullable String getPageParamName() {
		return this.pageParamName;
	}

	public void setPageParamName(@Nullable String pageParamName) {
		this.pageParamName = pageParamName;
	}

	public @Nullable String getLimitParamName() {
		return this.limitParamName;
	}

	public void setLimitParamName(@Nullable String limitParamName) {
		this.limitParamName = limitParamName;
	}

	public @Nullable String getSortParamName() {
		return this.sortParamName;
	}

	public void setSortParamName(@Nullable String sortParamName) {
		this.sortParamName = sortParamName;
	}

	public RepositoryDetectionStrategies getDetectionStrategy() {
		return this.detectionStrategy;
	}

	public void setDetectionStrategy(RepositoryDetectionStrategies detectionStrategy) {
		this.detectionStrategy = detectionStrategy;
	}

	public @Nullable MediaType getDefaultMediaType() {
		return this.defaultMediaType;
	}

	public void setDefaultMediaType(@Nullable MediaType defaultMediaType) {
		this.defaultMediaType = defaultMediaType;
	}

	public @Nullable Boolean getReturnBodyOnCreate() {
		return this.returnBodyOnCreate;
	}

	public void setReturnBodyOnCreate(@Nullable Boolean returnBodyOnCreate) {
		this.returnBodyOnCreate = returnBodyOnCreate;
	}

	public @Nullable Boolean getReturnBodyOnUpdate() {
		return this.returnBodyOnUpdate;
	}

	public void setReturnBodyOnUpdate(@Nullable Boolean returnBodyOnUpdate) {
		this.returnBodyOnUpdate = returnBodyOnUpdate;
	}

	public @Nullable Boolean getEnableEnumTranslation() {
		return this.enableEnumTranslation;
	}

	public void setEnableEnumTranslation(@Nullable Boolean enableEnumTranslation) {
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
