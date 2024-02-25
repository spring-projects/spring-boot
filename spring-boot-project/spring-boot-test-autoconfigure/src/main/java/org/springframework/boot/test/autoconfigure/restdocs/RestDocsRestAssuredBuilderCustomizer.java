/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.restdocs;

import io.restassured.specification.RequestSpecification;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.util.StringUtils;

/**
 * A customizer that configures Spring REST Docs with REST Assured.
 *
 * @author Eddú Meléndez
 */
class RestDocsRestAssuredBuilderCustomizer implements InitializingBean {

	private final RestDocsProperties properties;

	private final RequestSpecification delegate;

	/**
     * Constructs a new RestDocsRestAssuredBuilderCustomizer with the specified properties and delegate.
     *
     * @param properties the properties for configuring the RestDocsRestAssuredBuilderCustomizer
     * @param delegate the delegate RequestSpecification to be used by the RestDocsRestAssuredBuilderCustomizer
     */
    RestDocsRestAssuredBuilderCustomizer(RestDocsProperties properties, RequestSpecification delegate) {
		this.properties = properties;
		this.delegate = delegate;
	}

	/**
     * This method is called after all the properties have been set and is used to configure the RestDocsRestAssuredBuilderCustomizer.
     * It sets the base URI and port for the RestDocsRestAssuredBuilderCustomizer based on the properties provided.
     * 
     * @throws Exception if an error occurs while setting the properties
     */
    @Override
	public void afterPropertiesSet() throws Exception {
		PropertyMapper map = PropertyMapper.get();
		String host = this.properties.getUriHost();
		map.from(this.properties::getUriScheme)
			.when((scheme) -> StringUtils.hasText(scheme) && StringUtils.hasText(host))
			.to((scheme) -> this.delegate.baseUri(scheme + "://" + host));
		map.from(this.properties::getUriPort).whenNonNull().to(this.delegate::port);
	}

}
