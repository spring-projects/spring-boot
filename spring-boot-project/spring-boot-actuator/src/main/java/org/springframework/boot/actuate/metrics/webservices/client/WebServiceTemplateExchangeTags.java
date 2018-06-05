/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.boot.actuate.metrics.webservices.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import io.micrometer.core.instrument.Tag;

import org.springframework.util.StringUtils;
import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.WebServiceIOException;
import org.springframework.ws.context.MessageContext;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.transport.WebServiceConnection;
import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.context.TransportContextHolder;

/**
 * Factory methods for creating {@link Tag Tags} related to a soap request-response
 * exchange performed by a {@link org.springframework.ws.client.core.WebServiceTemplate}.
 *
 * @author Dmytro Nosan
 * @since 2.1.0
 **/
public abstract class WebServiceTemplateExchangeTags {

	private static final Tag OK = Tag.of("status", "OK");

	private static final Tag IO_ERROR = Tag.of("status", "IO_ERROR");

	private static final Tag CLIENT_ERROR = Tag.of("status", "CLIENT_ERROR");

	private static final Tag SERVER_ERROR = Tag.of("status", "SERVER_ERROR");

	private static final Tag NO_URI = Tag.of("uri", "NONE");

	private static final Tag NO_ACTION = Tag.of("action", "NONE");

	/**
	 * Creates a {@code Tag(uri)} using {@link TransportContext#getConnection()}.
	 * @return the uri tag
	 */
	public static Tag uri() {
		String uri = getUri();
		return (!StringUtils.hasText(uri) ? NO_URI : Tag.of("uri", uri));
	}

	/**
	 * Creates a {@code Tag(action)} using {@link SoapMessage#getSoapAction()}.
	 * @param messageContext Context of the SOAP message.
	 * @return the action tag
	 */
	public static Tag action(MessageContext messageContext) {
		String action = getAction(messageContext);
		return (!StringUtils.hasText(action) ? NO_ACTION : Tag.of("action", action));
	}

	/**
	 * Creates a {@code Tag(status)} based on the exception.
	 * @param exception the exception
	 * @return the status tag
	 */
	public static Tag status(Exception exception) {
		if (exception == null) {
			return OK;
		}
		if (exception instanceof WebServiceIOException) {
			return IO_ERROR;
		}
		if (exception instanceof WebServiceClientException) {
			return CLIENT_ERROR;
		}

		return SERVER_ERROR;
	}

	private static String getUri() {
		return Optional.ofNullable(TransportContextHolder.getTransportContext())
				.map(TransportContext::getConnection)
				.map(WebServiceTemplateExchangeTags::getConnectionUri).map(URI::toString)
				.map(WebServiceTemplateExchangeTags::stripUri)
				.map(WebServiceTemplateExchangeTags::ensureLeadingSlash).orElse(null);
	}

	private static URI getConnectionUri(WebServiceConnection connection) {
		try {
			return connection.getUri();
		}
		catch (URISyntaxException ignore) {
			return null;
		}
	}

	private static String getAction(MessageContext messageContext) {
		WebServiceMessage webServiceMessage = messageContext.getRequest();
		if (webServiceMessage instanceof SoapMessage) {
			return getAction((SoapMessage) webServiceMessage);
		}
		return null;
	}

	private static String getAction(SoapMessage webServiceMessage) {
		String action = unescapeAction(webServiceMessage.getSoapAction());
		if (StringUtils.hasText(action)) {
			try {
				URI uri = new URI(action);
				if (StringUtils.hasText(uri.getHost())) {
					return ensureLeadingSlash(stripUri(uri.toString()));
				}
			}
			catch (URISyntaxException ignore) {
			}
		}
		return action;
	}

	// "action" -> action
	private static String unescapeAction(String action) {
		if (StringUtils.hasText(action)) {
			if (action.startsWith("\"")) {
				action = action.substring(1);
			}
			if (action.endsWith("\"")) {
				action = action.substring(0, action.length() - 1);
			}
		}
		return action;

	}

	private static String stripUri(String uri) {
		return uri.replaceAll("^https?://[^/]+/", "");
	}

	private static String ensureLeadingSlash(String url) {
		return (!url.startsWith("/") ? "/" + url : url);
	}

}
