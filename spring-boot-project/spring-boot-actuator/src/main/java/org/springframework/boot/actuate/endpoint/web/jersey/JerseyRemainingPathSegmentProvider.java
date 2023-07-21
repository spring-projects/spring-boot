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

package org.springframework.boot.actuate.endpoint.web.jersey;

import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Strategy interface used to provide the remaining path segments for a Jersey actuator
 * endpoint.
 *
 * @author Madhura Bhave
 */
interface JerseyRemainingPathSegmentProvider {

	String get(ContainerRequestContext requestContext, String matchAllRemainingPathSegmentsVariable);

}
