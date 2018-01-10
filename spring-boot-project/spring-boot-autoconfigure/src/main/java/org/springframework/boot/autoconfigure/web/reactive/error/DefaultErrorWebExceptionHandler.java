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

package org.springframework.boot.autoconfigure.web.reactive.error;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ResourceProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicate;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Basic global {@link org.springframework.web.server.WebExceptionHandler}, rendering
 * {@link ErrorAttributes}.
 * <p>
 * More specific errors can be handled either using Spring WebFlux abstractions (e.g.
 * {@code @ExceptionHandler} with the annotation model) or by adding
 * {@link RouterFunction} to the chain.
 * <p>
 * This implementation will render error as HTML views if the client explicitly supports
 * that media type. It attempts to resolve error views using well known conventions. Will
 * search for templates and static assets under {@code '/error'} using the
 * {@link HttpStatus status code} and the {@link HttpStatus#series() status series}.
 * <p>
 * For example, an {@code HTTP 404} will search (in the specific order):
 * <ul>
 * <li>{@code '/<templates>/error/404.<ext>'}</li>
 * <li>{@code '/<static>/error/404.html'}</li>
 * <li>{@code '/<templates>/error/4xx.<ext>'}</li>
 * <li>{@code '/<static>/error/4xx.html'}</li>
 * <li>{@code '/<templates>/error/error'}</li>
 * <li>{@code '/<static>/error/error.html'}</li>
 * </ul>
 * <p>
 * If none found, a default "Whitelabel Error" HTML view will be rendered.
 * <p>
 * If the client doesn't support HTML, the error information will be rendered as a JSON
 * payload.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class DefaultErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

	private static final Map<HttpStatus.Series, String> SERIES_VIEWS;

	private static final Log logger = LogFactory
			.getLog(DefaultErrorWebExceptionHandler.class);

	static {
		Map<HttpStatus.Series, String> views = new HashMap<>();
		views.put(HttpStatus.Series.CLIENT_ERROR, "4xx");
		views.put(HttpStatus.Series.SERVER_ERROR, "5xx");
		SERIES_VIEWS = Collections.unmodifiableMap(views);
	}

	private final ErrorProperties errorProperties;

	/**
	 * Create a new {@code DefaultErrorWebExceptionHandler} instance.
	 * @param errorAttributes the error attributes
	 * @param resourceProperties the resources configuration properties
	 * @param errorProperties the error configuration properties
	 * @param applicationContext the current application context
	 */
	public DefaultErrorWebExceptionHandler(ErrorAttributes errorAttributes,
			ResourceProperties resourceProperties, ErrorProperties errorProperties,
			ApplicationContext applicationContext) {
		super(errorAttributes, resourceProperties, applicationContext);
		this.errorProperties = errorProperties;
	}

	@Override
	protected RouterFunction<ServerResponse> getRoutingFunction(
			ErrorAttributes errorAttributes) {
		return RouterFunctions.route(acceptsTextHtml(), this::renderErrorView)
				.andRoute(RequestPredicates.all(), this::renderErrorResponse);
	}

	/**
	 * Render the error information as an HTML view.
	 * @param request the current request
	 * @return a {@code Publisher} of the HTTP response
	 */
	protected Mono<ServerResponse> renderErrorView(ServerRequest request) {
		boolean includeStackTrace = isIncludeStackTrace(request, MediaType.TEXT_HTML);
		Map<String, Object> error = getErrorAttributes(request, includeStackTrace);
		HttpStatus errorStatus = getHttpStatus(error);
		ServerResponse.BodyBuilder response = ServerResponse.status(errorStatus)
				.contentType(MediaType.TEXT_HTML);
		return Flux
				.just("error/" + errorStatus.toString(),
						"error/" + SERIES_VIEWS.get(errorStatus.series()), "error/error")
				.flatMap((viewName) -> renderErrorView(viewName, response, error))
				.switchIfEmpty(renderDefaultErrorView(response, error)).next()
				.doOnNext((resp) -> logError(request, errorStatus));
	}

	/**
	 * Render the error information as a JSON payload.
	 * @param request the current request
	 * @return a {@code Publisher} of the HTTP response
	 */
	protected Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
		boolean includeStackTrace = isIncludeStackTrace(request, MediaType.ALL);
		Map<String, Object> error = getErrorAttributes(request, includeStackTrace);
		HttpStatus errorStatus = getHttpStatus(error);
		return ServerResponse.status(getHttpStatus(error))
				.contentType(MediaType.APPLICATION_JSON_UTF8)
				.body(BodyInserters.fromObject(error))
				.doOnNext((resp) -> logError(request, errorStatus));
	}

	/**
	 * Determine if the stacktrace attribute should be included.
	 * @param request the source request
	 * @param produces the media type produced (or {@code MediaType.ALL})
	 * @return if the stacktrace attribute should be included
	 */
	protected boolean isIncludeStackTrace(ServerRequest request, MediaType produces) {
		ErrorProperties.IncludeStacktrace include = this.errorProperties
				.getIncludeStacktrace();
		if (include == ErrorProperties.IncludeStacktrace.ALWAYS) {
			return true;
		}
		if (include == ErrorProperties.IncludeStacktrace.ON_TRACE_PARAM) {
			return isTraceEnabled(request);
		}
		return false;
	}

	/**
	 * Get the HTTP error status information from the error map.
	 * @param errorAttributes the current error information
	 * @return the error HTTP status
	 */
	protected HttpStatus getHttpStatus(Map<String, Object> errorAttributes) {
		int statusCode = (int) errorAttributes.get("status");
		return HttpStatus.valueOf(statusCode);
	}

	/**
	 * Predicate that checks whether the current request explicitly support
	 * {@code "text/html"} media type.
	 * <p>
	 * The "match-all" media type is not considered here.
	 * @return the request predicate
	 */
	protected RequestPredicate acceptsTextHtml() {
		return (serverRequest) -> {
			List<MediaType> acceptedMediaTypes = serverRequest.headers().accept();
			acceptedMediaTypes.remove(MediaType.ALL);
			MediaType.sortBySpecificityAndQuality(acceptedMediaTypes);
			return acceptedMediaTypes.stream()
					.anyMatch(MediaType.TEXT_HTML::isCompatibleWith);
		};
	}

	/**
	 * Log the original exception if handling it results in a Server Error.
	 * @param request the source request
	 * @param errorStatus the HTTP error status
	 */
	protected void logError(ServerRequest request, HttpStatus errorStatus) {
		if (errorStatus.is5xxServerError()) {
			Throwable ex = getError(request);
			logger.error("Failed to handle request [" + request.methodName() + " "
					+ request.uri() + "]", ex);
		}
	}

}
