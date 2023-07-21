/*
 * Copyright 2012-2022 the original author or authors.
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

import org.springframework.restdocs.restassured.RestAssuredRestDocumentationConfigurer;

/**
 * A customizer for {@link RestAssuredRestDocumentationConfigurer}. If a
 * {@code RestDocsRestAssuredConfigurationCustomizer} bean is found in the application
 * context it will be {@link #customize called} to customize the
 * {@code RestAssuredRestDocumentationConfigurer} before it is applied. Intended for use
 * only when the attributes on {@link AutoConfigureRestDocs @AutoConfigureRestDocs} do not
 * provide sufficient customization.
 *
 * @author Eddú Meléndez
 * @since 2.0.0
 */
@FunctionalInterface
public interface RestDocsRestAssuredConfigurationCustomizer {

	/**
	 * Customize the given {@code configurer}.
	 * @param configurer the configurer
	 */
	void customize(RestAssuredRestDocumentationConfigurer configurer);

}
