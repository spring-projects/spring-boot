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

package org.springframework.boot.autoconfigure.session;

import java.util.Collections;
import java.util.List;

import org.springframework.session.SessionRepository;
import org.springframework.util.ObjectUtils;

/**
 * Exception thrown when multiple {@link SessionRepository} implementations are available
 * with no way to know which implementation should be used.
 *
 * @author Stephane Nicoll
 * @since 2.0.0
 */
public class NonUniqueSessionRepositoryException extends RuntimeException {

	private final List<Class<?>> availableCandidates;

	public NonUniqueSessionRepositoryException(List<Class<?>> availableCandidates) {
		super("Multiple session repository candidates are available, set the "
				+ "'spring.session.store-type' property accordingly");
		this.availableCandidates = (!ObjectUtils.isEmpty(availableCandidates) ? availableCandidates
				: Collections.emptyList());
	}

	public List<Class<?>> getAvailableCandidates() {
		return this.availableCandidates;
	}

}
