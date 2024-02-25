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

package org.springframework.boot.docs.web.reactive.webflux.errorhandling;

import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.function.server.ServerResponse.BodyBuilder;

/**
 * MyErrorWebExceptionHandler class.
 */
@Component
public class MyErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

	/**
     * Constructs a new MyErrorWebExceptionHandler with the specified errorAttributes, webProperties,
     * applicationContext, and serverCodecConfigurer.
     * 
     * @param errorAttributes the ErrorAttributes object used to retrieve error information
     * @param webProperties the WebProperties object containing web-related properties
     * @param applicationContext the ApplicationContext object used to access application context
     * @param serverCodecConfigurer the ServerCodecConfigurer object used to configure server codecs
     */
    public MyErrorWebExceptionHandler(ErrorAttributes errorAttributes, WebProperties webProperties,
			ApplicationContext applicationContext, ServerCodecConfigurer serverCodecConfigurer) {
		super(errorAttributes, webProperties.getResources(), applicationContext);
		setMessageReaders(serverCodecConfigurer.getReaders());
		setMessageWriters(serverCodecConfigurer.getWriters());
	}

	/**
     * Returns the routing function for handling errors.
     *
     * @param errorAttributes the error attributes to be used
     * @return the router function for handling errors
     */
    @Override
	protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
		return RouterFunctions.route(this::acceptsXml, this::handleErrorAsXml);
	}

	/**
     * Checks if the given server request accepts XML media type.
     * 
     * @param request the server request to check
     * @return true if the server request accepts XML media type, false otherwise
     */
    private boolean acceptsXml(ServerRequest request) {
		return request.headers().accept().contains(MediaType.APPLICATION_XML);
	}

	/**
     * Handles the error and returns the error response as XML format.
     *
     * @param request the server request object
     * @return a Mono of ServerResponse representing the error response in XML format
     */
    public Mono<ServerResponse> handleErrorAsXml(ServerRequest request) {
		BodyBuilder builder = ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR);
		// ... additional builder calls
		return builder.build();
	}

}
