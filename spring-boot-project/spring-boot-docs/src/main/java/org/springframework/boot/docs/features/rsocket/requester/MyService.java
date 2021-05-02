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

package org.springframework.boot.docs.features.rsocket.requester;

import reactor.core.publisher.Mono;

import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Service;

@Service
public class MyService {

	private final RSocketRequester rsocketRequester;

	public MyService(RSocketRequester.Builder rsocketRequesterBuilder) {
		this.rsocketRequester = rsocketRequesterBuilder.tcp("example.org", 9898);
	}

	public Mono<User> someRSocketCall(String name) {
		return this.rsocketRequester.route("user").data(name).retrieveMono(User.class);
	}

}
