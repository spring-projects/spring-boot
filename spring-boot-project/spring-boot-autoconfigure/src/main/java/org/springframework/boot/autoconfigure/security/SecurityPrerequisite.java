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

package org.springframework.boot.autoconfigure.security;

/**
 * Marker interface for beans that need to be initialized before any security
 * configuration is evaluated.
 *
 * @author Dave Syer
 * @since 1.2.1
 * @deprecated since 2.0.6 since security prerequisites are not supported in Spring Boot 2
 */
@Deprecated
public interface SecurityPrerequisite {

}
