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

package org.springframework.boot.autoconfigure.web.reactive.error;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.template.TemplateAvailabilityProviders;
import org.springframework.boot.autoconfigure.web.WebProperties.Resources;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.error.ErrorAttributeOptions.Include;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.log.LogMessage;
import org.springframework.http.HttpLogging;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.HtmlUtils;

/**
 * Abstract base class for {@link ErrorWebExceptionHandler} implementations.
 *
 * @author Brian Clozel
 * @author Scott Frederick
 * @since 2.0.0
 * @see ErrorAttributes
 */
public abstract class AbstractErrorWebExceptionHandler implements ErrorWebExceptionHandler, InitializingBean {

	/**
	 * Currently duplicated from Spring WebFlux HttpWebHandlerAdapter.
	 */
	private static final Set<String> DISCONNECTED_CLIENT_EXCEPTIONS;

	static {
		Set<String> exceptions = new HashSet<>();
		exceptions.add("AbortedException");
		exceptions.add("ClientAbortException");
		exceptions.add("EOFException");
		exceptions.add("EofException");
		DISCONNECTED_CLIENT_EXCEPTIONS = Collections.unmodifiableSet(exceptions);
	}

	private static final Log logger = HttpLogging.forLogName(AbstractErrorWebExceptionHandler.class);

	private final ApplicationContext applicationContext;

	private final ErrorAttributes errorAttributes;

	private final Resources resources;

	private final TemplateAvailabilityProviders templateAvailabilityProviders;

	private List<HttpMessageReader<?>> messageReaders = Collections.emptyList();

	private List<HttpMessageWriter<?>> messageWriters = Collections.emptyList();

	private List<ViewResolver> viewResolvers = Collections.emptyList();

	/**
	 * Create a new {@code AbstractErrorWebExceptionHandler}.
	 * @param errorAttributes the error attributes
	 * @param resources the resources configuration properties
	 * @param applicationContext the application context
	 * @since 2.4.0
	 */
	public AbstractErrorWebExceptionHandler(ErrorAttributes errorAttributes, Resources resources,
			ApplicationContext applicationContext) {
		Assert.notNull(errorAttributes, "ErrorAttributes must not be null");
		Assert.notNull(resources, "Resources must not be null");
		Assert.notNull(applicationContext, "ApplicationContext must not be null");
		this.errorAttributes = errorAttributes;
		this.resources = resources;
		this.applicationContext = applicationContext;
		this.templateAvailabilityProviders = new TemplateAvailabilityProviders(applicationContext);
	}

	/**
	 * Configure HTTP message writers to serialize the response body with.
	 * @param messageWriters the {@link HttpMessageWriter}s to use
	 */
	public void setMessageWriters(List<HttpMessageWriter<?>> messageWriters) {
		Assert.notNull(messageWriters, "'messageWriters' must not be null");
		this.messageWriters = messageWriters;
	}

	/**
	 * Configure HTTP message readers to deserialize the request body with.
	 * @param messageReaders the {@link HttpMessageReader}s to use
	 */
	public void setMessageReaders(List<HttpMessageReader<?>> messageReaders) {
		Assert.notNull(messageReaders, "'messageReaders' must not be null");
		this.messageReaders = messageReaders;
	}

	/**
	 * Configure the {@link ViewResolver} to use for rendering views.
	 * @param viewResolvers the list of {@link ViewResolver}s to use
	 */
	public void setViewResolvers(List<ViewResolver> viewResolvers) {
		this.viewResolvers = viewResolvers;
	}

	/**
	 * Extract the error attributes from the current request, to be used to populate error
	 * views or JSON payloads.
	 * @param request the source request
	 * @param includeStackTrace whether to include the error stacktrace information
	 * @return the error attributes as a Map
	 * @deprecated since 2.3.0 for removal in 2.5.0 in favor of
	 * {@link #getErrorAttributes(ServerRequest, ErrorAttributeOptions)}
	 */
	@Deprecated
	protected Map<String, Object> getErrorAttributes(ServerRequest request, boolean includeStackTrace) {
		return getErrorAttributes(request,
				(includeStackTrace) ? ErrorAttributeOptions.of(Include.STACK_TRACE) : ErrorAttributeOptions.defaults());
	}

	/**
	 * Extract the error attributes from the current request, to be used to populate error
	 * views or JSON payloads.
	 * @param request the source request
	 * @param options options to control error attributes
	 * @return the error attributes as a Map
	 */
	protected Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
		return this.errorAttributes.getErrorAttributes(request, options);
	}

	/**
	 * Extract the original error from the current request.
	 * @param request the source request
	 * @return the error
	 */
	protected Throwable getError(ServerRequest request) {
		return this.errorAttributes.getError(request);
	}

	/**
	 * Check whether the trace attribute has been set on the given request.
	 * @param request the source request
	 * @return {@code true} if the error trace has been requested, {@code false} otherwise
	 */
	protected boolean isTraceEnabled(ServerRequest request) {
		return getBooleanParameter(request, "trace");
	}

	/**
	 * Check whether the message attribute has been set on the given request.
	 * @param request the source request
	 * @return {@code true} if the message attribute has been requested, {@code false}
	 * otherwise
	 */
	protected boolean isMessageEnabled(ServerRequest request) {
		return getBooleanParameter(request, "message");
	}

	/**
	 * Check whether the errors attribute has been set on the given request.
	 * @param request the source request
	 * @return {@code true} if the errors attribute has been requested, {@code false}
	 * otherwise
	 */
	protected boolean isBindingErrorsEnabled(ServerRequest request) {
		return getBooleanParameter(request, "errors");
	}

	private boolean getBooleanParameter(ServerRequest request, String parameterName) {
		String parameter = request.queryParam(parameterName).orElse("false");
		return !"false".equalsIgnoreCase(parameter);
	}

	/**
	 * Render the given error data as a view, using a template view if available or a
	 * static HTML file if available otherwise. This will return an empty
	 * {@code Publisher} if none of the above are available.
	 * @param viewName the view name
	 * @param responseBody the error response being built
	 * @param error the error data as a map
	 * @return a Publisher of the {@link ServerResponse}
	 */
	protected Mono<ServerResponse> renderErrorView(String viewName, ServerResponse.BodyBuilder responseBody,
			Map<String, Object> error) {
		if (isTemplateAvailable(viewName)) {
			return responseBody.render(viewName, error);
		}
		Resource resource = resolveResource(viewName);
		if (resource != null) {
			return responseBody.body(BodyInserters.fromResource(resource));
		}
		return Mono.empty();
	}

	private boolean isTemplateAvailable(String viewName) {
		return this.templateAvailabilityProviders.getProvider(viewName, this.applicationContext) != null;
	}

	private Resource resolveResource(String viewName) {
		for (String location : this.resources.getStaticLocations()) {
			try {
				Resource resource = this.applicationContext.getResource(location);
				resource = resource.createRelative(viewName + ".html");
				if (resource.exists()) {
					return resource;
				}
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		return null;
	}

	/**
	 * Render a default HTML "Whitelabel Error Page".
	 * <p>
	 * Useful when no other error view is available in the application.
	 * @param responseBody the error response being built
	 * @param error the error data as a map
	 * @return a Publisher of the {@link ServerResponse}
	 */
	protected Mono<ServerResponse> renderDefaultErrorView(ServerResponse.BodyBuilder responseBody,
			Map<String, Object> error) {
		StringBuilder builder = new StringBuilder();
		Date timestamp = (Date) error.get("timestamp");
		Object message = error.get("message");
		Object trace = error.get("trace");
		Object requestId = error.get("requestId");
		builder.append("<html><body><h1>Whitelabel Error Page</h1>")
				.append("<p>This application has no configured error view, so you are seeing this as a fallback.</p>")
				.append("<div id='created'>").append(timestamp).append("</div>").append("<div>[").append(requestId)
				.append("] There was an unexpected error (type=").append(htmlEscape(error.get("error")))
				.append(", status=").append(htmlEscape(error.get("status"))).append(").</div>");
		if (message != null) {
			builder.append("<div>").append(htmlEscape(message)).append("</div>");
		}
		if (trace != null) {
			builder.append("<div style='white-space:pre-wrap;'>").append(htmlEscape(trace)).append("</div>");
		}
		builder.append("</body></html>");
		return responseBody.bodyValue(builder.toString());
	}

	private String htmlEscape(Object input) {
		return (input != null) ? HtmlUtils.htmlEscape(input.toString()) : null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (CollectionUtils.isEmpty(this.messageWriters)) {
			throw new IllegalArgumentException("Property 'messageWriters' is required");
		}
	}

	/**
	 * Create a {@link RouterFunction} that can route and handle errors as JSON responses
	 * or HTML views.
	 * <p>
	 * If the returned {@link RouterFunction} doesn't route to a {@code HandlerFunction},
	 * the original exception is propagated in the pipeline and can be processed by other
	 * {@link org.springframework.web.server.WebExceptionHandler}s.
	 * @param errorAttributes the {@code ErrorAttributes} instance to use to extract error
	 * information
	 * @return a {@link RouterFunction} that routes and handles errors
	 */
	protected abstract RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes);

	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable throwable) {
		if (exchange.getResponse().isCommitted() || isDisconnectedClientError(throwable)) {
			return Mono.error(throwable);
		}
		this.errorAttributes.storeErrorInformation(throwable, exchange);
		ServerRequest request = ServerRequest.create(exchange, this.messageReaders);
		return getRoutingFunction(this.errorAttributes).route(request).switchIfEmpty(Mono.error(throwable))
				.flatMap((handler) -> handler.handle(request))
				.doOnNext((response) -> logError(request, response, throwable))
				.flatMap((response) -> write(exchange, response));
	}

	private boolean isDisconnectedClientError(Throwable ex) {
		return DISCONNECTED_CLIENT_EXCEPTIONS.contains(ex.getClass().getSimpleName())
				|| isDisconnectedClientErrorMessage(NestedExceptionUtils.getMostSpecificCause(ex).getMessage());
	}

	private boolean isDisconnectedClientErrorMessage(String message) {
		message = (message != null) ? message.toLowerCase() : "";
		return (message.contains("broken pipe") || message.contains("connection reset by peer"));
	}

	/**
	 * Logs the {@code throwable} error for the given {@code request} and {@code response}
	 * exchange. The default implementation logs all errors at debug level. Additionally,
	 * any internal server error (500) is logged at error level.
	 * @param request the request that was being handled
	 * @param response the response that was being sent
	 * @param throwable the error to be logged
	 * @since 2.2.0
	 */
	protected void logError(ServerRequest request, ServerResponse response, Throwable throwable) {
		if (logger.isDebugEnabled()) {
			logger.debug(request.exchange().getLogPrefix() + formatError(throwable, request));
		}
		if (HttpStatus.resolve(response.rawStatusCode()) != null
				&& response.statusCode().equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
			logger.error(LogMessage.of(() -> String.format("%s 500 Server Error for %s",
					request.exchange().getLogPrefix(), formatRequest(request))), throwable);
		}
	}

	private String formatError(Throwable ex, ServerRequest request) {
		String reason = ex.getClass().getSimpleName() + ": " + ex.getMessage();
		return "Resolved [" + reason + "] for HTTP " + request.methodName() + " " + request.path();
	}

	private String formatRequest(ServerRequest request) {
		String rawQuery = request.uri().getRawQuery();
		String query = StringUtils.hasText(rawQuery) ? "?" + rawQuery : "";
		return "HTTP " + request.methodName() + " \"" + request.path() + query + "\"";
	}

	private Mono<? extends Void> write(ServerWebExchange exchange, ServerResponse response) {
		// force content-type since writeTo won't overwrite response header values
		exchange.getResponse().getHeaders().setContentType(response.headers().getContentType());
		return response.writeTo(exchange, new ResponseContext());
	}

	private class ResponseContext implements ServerResponse.Context {

		@Override
		public List<HttpMessageWriter<?>> messageWriters() {
			return AbstractErrorWebExceptionHandler.this.messageWriters;
		}

		@Override
		public List<ViewResolver> viewResolvers() {
			return AbstractErrorWebExceptionHandler.this.viewResolvers;
		}

	}

}
