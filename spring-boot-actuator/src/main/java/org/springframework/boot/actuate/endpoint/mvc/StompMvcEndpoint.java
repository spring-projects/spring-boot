/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.mvc;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.actuate.endpoint.Endpoint;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.WebMvcStompWebSocketEndpointRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolWebSocketHandler;

/**
 * {@link MvcEndpoint} to expose stomp.
 *
 * @author Vladimir Tsanev
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "endpoint.stomp", ignoreUnknownFields = false)
@HypermediaDisabled
public class StompMvcEndpoint implements MvcEndpoint, InitializingBean, DisposableBean {

	/**
	 * Endpoint URL path.
	 */
	private String path = "/stomp";

	/**
	 * Enable security on the endpoint.
	 */
	private boolean sensitive = true;

	/**
	 * Enable sockJs fallback options.
	 */
	private boolean sockJs = false;

	/**
	 * Configure allowed 'Origin' header values.
	 */
	private String allowedOrigins = "*";

	private final String contextPath;

	private final WebSocketHandler webSocketHandler;

	private final StompSubProtocolHandler stompHandler;

    private HttpRequestHandler handler;

    private ThreadPoolTaskScheduler scheduler;

	public StompMvcEndpoint(String contextPath,
			WebSocketHandler webSocketHandler) {
		super();
		this.contextPath = contextPath;
		this.webSocketHandler = webSocketHandler;
		this.stompHandler = new StompSubProtocolHandler();
	}

	@Override
	public String getPath() {
		return this.path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	@Override
	public boolean isSensitive() {
		return this.sensitive;
	}

	public void setSensitive(boolean sensitive) {
		this.sensitive = sensitive;
	}

	public boolean isSockJs() {
		return sockJs;
	}

	public void setSockJs(boolean sockJs) {
		this.sockJs = sockJs;
	}

	public String getAllowedOrigins() {
		return allowedOrigins;
	}

	public void setAllowedOrigins(String allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Class<? extends Endpoint> getEndpointType() {
		return null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		SubProtocolWebSocketHandler subProtocolWebSocketHandler = unwrapSubProtocolWebSocketHandler(webSocketHandler);
		subProtocolWebSocketHandler.addProtocolHandler(stompHandler);
		WebMvcStompWebSocketEndpointRegistration registration = new WebMvcStompWebSocketEndpointRegistration(
				new String[] { path }, webSocketHandler, null);

		registration.setAllowedOrigins(StringUtils
				.commaDelimitedListToStringArray(allowedOrigins));
		if (sockJs) {
		    scheduler = new ThreadPoolTaskScheduler();
	        scheduler.setThreadNamePrefix("StompEndpointBroker-");
	        scheduler.setPoolSize(Runtime.getRuntime().availableProcessors());
	        scheduler.setRemoveOnCancelPolicy(true);
	        scheduler.afterPropertiesSet();
			registration.withSockJS().setTaskScheduler(scheduler);
		}
		this.handler = registration.getMappings().keySet().iterator().next();
	}

	@Override
	public void destroy() {
	    if (this.scheduler != null) {
	        this.scheduler.destroy();
	        this.scheduler = null;
	    }
	}

	private static SubProtocolWebSocketHandler unwrapSubProtocolWebSocketHandler(
			WebSocketHandler wsHandler) {
		WebSocketHandler actual = WebSocketHandlerDecorator.unwrap(wsHandler);
		Assert.isInstanceOf(SubProtocolWebSocketHandler.class, actual,
				"No SubProtocolWebSocketHandler in " + wsHandler);
		return (SubProtocolWebSocketHandler) actual;
	}

	@RequestMapping("/**")
	public ModelAndView handle(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		if (sockJs) {
			fixSockJsPath(request);
		}
		handler.handleRequest(request, response);
		return null;
	}

	private void fixSockJsPath(HttpServletRequest request) {
		String pathWithinHandlerMapping = (String) request
				.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		if (pathWithinHandlerMapping != null) {
			String prefix = this.contextPath + this.path;
			if (pathWithinHandlerMapping.startsWith(prefix)) {
				pathWithinHandlerMapping = pathWithinHandlerMapping.substring(prefix
						.length());
			}
			request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE,
					pathWithinHandlerMapping);
		}
	}

}
