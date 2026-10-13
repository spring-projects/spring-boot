/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.rsocket;

import reactor.core.publisher.Mono;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
public class ProjectController {

	@MessageMapping("find.project.{name}")
	public Mono<Project> findProject(@DestinationVariable String name) {
		return Mono.just(new Project(name));
	}

}
