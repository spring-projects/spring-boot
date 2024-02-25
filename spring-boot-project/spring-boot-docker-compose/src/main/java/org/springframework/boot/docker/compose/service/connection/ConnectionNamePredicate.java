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

package org.springframework.boot.docker.compose.service.connection;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.boot.docker.compose.core.ImageReference;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.util.Assert;

/**
 * {@link Predicate} that matches against connection name.
 *
 * @author Phillip Webb
 */
class ConnectionNamePredicate implements Predicate<DockerComposeConnectionSource> {

	private final Set<String> required;

	/**
     * Constructs a new ConnectionNamePredicate with the specified required connection names.
     * 
     * @param required the required connection names (must not be empty)
     * @throws IllegalArgumentException if the required array is empty
     */
    ConnectionNamePredicate(String... required) {
		Assert.notEmpty(required, "Required must not be empty");
		this.required = Arrays.stream(required).map(this::asCanonicalName).collect(Collectors.toSet());
	}

	/**
     * Tests if the given DockerComposeConnectionSource matches the required connection name.
     * 
     * @param source the DockerComposeConnectionSource to test
     * @return true if the connection name matches the required connection name, false otherwise
     */
    @Override
	public boolean test(DockerComposeConnectionSource source) {
		String actual = getActual(source.getRunningService());
		return this.required.contains(actual);
	}

	/**
     * Returns the actual connection name of the given RunningService.
     * If the service has a label "org.springframework.boot.service-connection",
     * it returns the canonical name of the label. Otherwise, it returns the name of the service image.
     *
     * @param service the RunningService for which to get the actual connection name
     * @return the actual connection name of the service
     */
    private String getActual(RunningService service) {
		String label = service.labels().get("org.springframework.boot.service-connection");
		return (label != null) ? asCanonicalName(label) : service.image().getName();
	}

	/**
     * Returns the canonical name of the given image reference name.
     *
     * @param name the name of the image reference
     * @return the canonical name of the image reference
     */
    private String asCanonicalName(String name) {
		return ImageReference.of(name).getName();
	}

}
