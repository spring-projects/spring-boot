/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.devtools.remote.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;

/**
 * Dispatcher used to route incoming remote server requests to a {@link Handler}. Similar
 * to {@code DispatchServlet} in Spring MVC but separate to ensure that remote support can
 * be used regardless of any web framework.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see HandlerMapper
 */
public class Dispatcher {

	private final AccessManager accessManager;

	private final List<HandlerMapper> mappers;

	public Dispatcher(AccessManager accessManager, Collection<HandlerMapper> mappers) {
		Assert.notNull(accessManager, "AccessManager must not be null");
		Assert.notNull(mappers, "Mappers must not be null");
		this.accessManager = accessManager;
		this.mappers = new ArrayList<>(mappers);
		AnnotationAwareOrderComparator.sort(this.mappers);
	}

	/**
	 * Dispatch the specified request to an appropriate {@link Handler}.
	 * @param request the request
	 * @param response the response
	 * @return {@code true} if the request was dispatched
	 * @throws IOException in case of I/O errors
	 */
	public boolean handle(ServerHttpRequest request, ServerHttpResponse response)
			throws IOException {
		for (HandlerMapper mapper : this.mappers) {
			Handler handler = mapper.getHandler(request);
			if (handler != null) {
				handle(handler, request, response);
				return true;
			}
		}
		return false;
	}

	private void handle(Handler handler, ServerHttpRequest request,
			ServerHttpResponse response) throws IOException {
		if (!this.accessManager.isAllowed(request)) {
			response.setStatusCode(HttpStatus.FORBIDDEN);
			return;
		}
		handler.handle(request, response);
	}

}
