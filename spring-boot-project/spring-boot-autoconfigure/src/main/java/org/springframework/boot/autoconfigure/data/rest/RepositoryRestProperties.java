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

	/**
     * Returns the base path of the repository.
     *
     * @return the base path of the repository
     */
    public String getBasePath() {
		return this.basePath;
	}

	/**
     * Sets the base path for the repository.
     * 
     * @param basePath the new base path for the repository
     */
    public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	/**
     * Returns the default page size.
     *
     * @return the default page size
     */
    public Integer getDefaultPageSize() {
		return this.defaultPageSize;
	}

	/**
     * Sets the default page size for the repository.
     * 
     * @param defaultPageSize the default page size to be set
     */
    public void setDefaultPageSize(Integer defaultPageSize) {
		this.defaultPageSize = defaultPageSize;
	}

	/**
     * Returns the maximum page size for pagination.
     *
     * @return the maximum page size
     */
    public Integer getMaxPageSize() {
		return this.maxPageSize;
	}

	/**
     * Sets the maximum page size for the repository.
     * 
     * @param maxPageSize the maximum page size to be set
     */
    public void setMaxPageSize(Integer maxPageSize) {
		this.maxPageSize = maxPageSize;
	}

	/**
     * Returns the name of the page parameter.
     *
     * @return the name of the page parameter
     */
    public String getPageParamName() {
		return this.pageParamName;
	}

	/**
     * Sets the name of the page parameter.
     * 
     * @param pageParamName the name of the page parameter
     */
    public void setPageParamName(String pageParamName) {
		this.pageParamName = pageParamName;
	}

	/**
     * Returns the name of the limit parameter.
     *
     * @return the name of the limit parameter
     */
    public String getLimitParamName() {
		return this.limitParamName;
	}

	/**
     * Sets the name of the limit parameter.
     * 
     * @param limitParamName the name of the limit parameter
     */
    public void setLimitParamName(String limitParamName) {
		this.limitParamName = limitParamName;
	}

	/**
     * Returns the name of the sort parameter.
     *
     * @return the name of the sort parameter
     */
    public String getSortParamName() {
		return this.sortParamName;
	}

	/**
     * Sets the name of the sort parameter.
     * 
     * @param sortParamName the name of the sort parameter
     */
    public void setSortParamName(String sortParamName) {
		this.sortParamName = sortParamName;
	}

	/**
     * Returns the detection strategy used by the repository.
     *
     * @return the detection strategy used by the repository
     */
    public RepositoryDetectionStrategies getDetectionStrategy() {
		return this.detectionStrategy;
	}

	/**
     * Sets the detection strategy for the repository.
     * 
     * @param detectionStrategy the detection strategy to be set
     */
    public void setDetectionStrategy(RepositoryDetectionStrategies detectionStrategy) {
		this.detectionStrategy = detectionStrategy;
	}

	/**
     * Returns the default media type for the repository.
     * 
     * @return the default media type
     */
    public MediaType getDefaultMediaType() {
		return this.defaultMediaType;
	}

	/**
     * Sets the default media type for the repository.
     * 
     * @param defaultMediaType the default media type to be set
     */
    public void setDefaultMediaType(MediaType defaultMediaType) {
		this.defaultMediaType = defaultMediaType;
	}

	/**
     * Returns the value of the returnBodyOnCreate property.
     * 
     * @return the value of the returnBodyOnCreate property
     */
    public Boolean getReturnBodyOnCreate() {
		return this.returnBodyOnCreate;
	}

	/**
     * Sets the value indicating whether the body should be returned on create operation.
     * 
     * @param returnBodyOnCreate the value indicating whether the body should be returned on create operation
     */
    public void setReturnBodyOnCreate(Boolean returnBodyOnCreate) {
		this.returnBodyOnCreate = returnBodyOnCreate;
	}

	/**
     * Returns the value of the returnBodyOnUpdate property.
     * 
     * @return the value of the returnBodyOnUpdate property
     */
    public Boolean getReturnBodyOnUpdate() {
		return this.returnBodyOnUpdate;
	}

	/**
     * Sets the flag indicating whether the body of the updated resource should be returned.
     * 
     * @param returnBodyOnUpdate the flag indicating whether the body of the updated resource should be returned
     */
    public void setReturnBodyOnUpdate(Boolean returnBodyOnUpdate) {
		this.returnBodyOnUpdate = returnBodyOnUpdate;
	}

	/**
     * Returns the value of the enableEnumTranslation property.
     * 
     * @return the value of the enableEnumTranslation property
     */
    public Boolean getEnableEnumTranslation() {
		return this.enableEnumTranslation;
	}

	/**
     * Sets the flag to enable or disable translation of enums in the repository.
     * 
     * @param enableEnumTranslation the flag indicating whether to enable or disable enum translation
     */
    public void setEnableEnumTranslation(Boolean enableEnumTranslation) {
		this.enableEnumTranslation = enableEnumTranslation;
	}

	/**
     * Applies the configuration properties to the given RepositoryRestConfiguration.
     * 
     * @param rest the RepositoryRestConfiguration to apply the properties to
     */
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
