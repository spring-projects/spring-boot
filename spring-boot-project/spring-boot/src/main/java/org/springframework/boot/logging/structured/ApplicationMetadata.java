/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.logging.structured;

/**
 * Metadata about the application.
 *
 * @param pid the process ID of the application
 * @param name the application name
 * @param version the version of the application
 * @param environment the name of the environment the application is running in
 * @param nodeName the name of the node the application is running on
 * @author Moritz Halbritter
 * @since 3.4.0
 */
public record ApplicationMetadata(Long pid, String name, String version, String environment, String nodeName) {

}
