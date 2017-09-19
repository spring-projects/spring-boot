/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.autoconfigure.session;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * A {@link AbstractFailureAnalyzer} for {@link NonUniqueSessionRepositoryException}.
 *
 * @author Stephane Nicoll
 */
class NonUniqueSessionRepositoryFailureAnalyzer
		extends AbstractFailureAnalyzer<NonUniqueSessionRepositoryException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure,
			NonUniqueSessionRepositoryException cause) {
		StringBuilder message = new StringBuilder();
		message.append(String.format("Multiple Spring Session store implementations are "
				+ "available on the classpath:%n"));
		for (Class<?> candidate : cause.getAvailableCandidates()) {
			message.append(String.format("    - %s%n", candidate.getName()));
		}
		StringBuilder action = new StringBuilder();
		action.append(String.format("Consider any of the following:%n"));
		action.append(String.format("    - Define the `spring.session.store-type` "
				+ "property to the store you want to use%n"));
		action.append(String.format("    - Review your classpath and remove the unwanted "
				+ "store implementation(s)%n"));
		return new FailureAnalysis(message.toString(), action.toString(), cause);
	}

}
