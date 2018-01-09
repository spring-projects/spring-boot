/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.restdocs;

import org.springframework.restdocs.webtestclient.WebTestClientRestDocumentationConfigurer;

/**
 * A customizer for {@link WebTestClientRestDocumentationConfigurer}. If a
 * {@code RestDocsWebTestClientConfigurationCustomizer} bean is found in the application
 * context it will be {@link #customize called} to customize the
 * {@code WebTestClientRestDocumentationConfigurer} before it is applied. Intended for use
 * only when the attributes on {@link AutoConfigureRestDocs} do not provide sufficient
 * customization.
 *
 * @author Roman Zaynetdinov
 * @since 2.0.0
 */
@FunctionalInterface
public interface RestDocsWebTestClientConfigurationCustomizer {

	/**
	 * Customize the given {@code configurer}.
	 * @param configurer the configurer
	 */
	void customize(WebTestClientRestDocumentationConfigurer configurer);

}
