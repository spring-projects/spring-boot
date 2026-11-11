/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.endpoint.web;

import org.springframework.boot.actuate.endpoint.ExposableEndpoint;

/**
 * Information describing an endpoint that can be exposed over the web.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
public interface ExposableWebEndpoint extends ExposableEndpoint<WebOperation>, PathMappedEndpoint {

}
