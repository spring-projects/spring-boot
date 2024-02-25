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

package smoketest.secure.webflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * SampleSecureWebFluxApplication class.
 */
@SpringBootApplication
public class SampleSecureWebFluxApplication {

	/**
	 * The main method is the entry point of the application. It starts the Spring Boot
	 * application by running the SpringApplication.run() method.
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(SampleSecureWebFluxApplication.class);
	}

	/**
	 * Creates a router function for handling HTTP POST requests to the "/echo" endpoint.
	 * @param echoHandler the handler responsible for processing the echo request
	 * @return the router function for handling the echo request
	 */
	@Bean
	public RouterFunction<ServerResponse> monoRouterFunction(EchoHandler echoHandler) {
		return route(POST("/echo"), echoHandler::echo);
	}

}
