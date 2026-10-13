/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.messaging.rsocket.requester;

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
