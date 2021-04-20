/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.springbootfeatures.springapplication.availability;

import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class LocalCacheVerifier {

	private final ApplicationEventPublisher eventPublisher;

	public LocalCacheVerifier(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	public void checkLocalCache() {
		try {
			// ...
		}
		catch (CacheCompletelyBrokenException ex) {
			AvailabilityChangeEvent.publish(this.eventPublisher, ex, LivenessState.BROKEN);
		}
	}

}
// @chomp:file

class CacheCompletelyBrokenException extends RuntimeException {

}
