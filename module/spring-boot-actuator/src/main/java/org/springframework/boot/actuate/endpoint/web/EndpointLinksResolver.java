/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.actuate.endpoint.ExposableEndpoint;

/**
 * A resolver for {@link Link links} to web endpoints.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public class EndpointLinksResolver {

	private static final Log logger = LogFactory.getLog(EndpointLinksResolver.class);

	private final Collection<? extends ExposableEndpoint<?>> endpoints;

	/**
	 * Creates a new {@code EndpointLinksResolver} that will resolve links to the given
	 * {@code endpoints}.
	 * @param endpoints the endpoints
	 */
	public EndpointLinksResolver(Collection<? extends ExposableEndpoint<?>> endpoints) {
		this.endpoints = endpoints;
	}

	/**
	 * Creates a new {@code EndpointLinksResolver} that will resolve links to the given
	 * {@code endpoints} that are exposed beneath the given {@code basePath}.
	 * @param endpoints the endpoints
	 * @param basePath the basePath
	 */
	public EndpointLinksResolver(Collection<? extends ExposableEndpoint<?>> endpoints, String basePath) {
		this.endpoints = endpoints;
		if (logger.isInfoEnabled()) {
			String suffix = (endpoints.size() == 1) ? "" : "s";
			logger
				.info("Exposing " + endpoints.size() + " endpoint" + suffix + " beneath base path '" + basePath + "'");
		}
	}

	/**
	 * Resolves links to the known endpoints based on a request with the given
	 * {@code requestUrl}.
	 * @param requestUrl the url of the request for the endpoint links
	 * @return the links
	 */
	public Map<String, Link> resolveLinks(String requestUrl) {
		String normalizedUrl = normalizeRequestUrl(requestUrl);
		Map<String, Link> links = new LinkedHashMap<>();
		links.put("self", new Link(normalizedUrl));
		for (ExposableEndpoint<?> endpoint : this.endpoints) {
			if (endpoint instanceof ExposableWebEndpoint exposableWebEndpoint) {
				collectLinks(links, exposableWebEndpoint, normalizedUrl);
			}
			else if (endpoint instanceof PathMappedEndpoint pathMappedEndpoint) {
				String rootPath = pathMappedEndpoint.getRootPath();
				Link link = createLink(normalizedUrl, rootPath);
				links.put(endpoint.getEndpointId().toLowerCaseString(), link);
			}
		}
		return links;
	}

	private String normalizeRequestUrl(String requestUrl) {
		if (requestUrl.endsWith("/")) {
			return requestUrl.substring(0, requestUrl.length() - 1);
		}
		return requestUrl;
	}

	private void collectLinks(Map<String, Link> links, ExposableWebEndpoint endpoint, String normalizedUrl) {
		for (WebOperation operation : endpoint.getOperations()) {
			links.put(operation.getId(), createLink(normalizedUrl, operation));
		}
	}

	private Link createLink(String requestUrl, WebOperation operation) {
		return createLink(requestUrl, operation.getRequestPredicate().getPath());
	}

	private Link createLink(String requestUrl, String path) {
		return new Link(requestUrl + (path.startsWith("/") ? path : "/" + path));
	}

}
