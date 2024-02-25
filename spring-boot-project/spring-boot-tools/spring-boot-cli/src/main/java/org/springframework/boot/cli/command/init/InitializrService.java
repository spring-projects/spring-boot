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

package org.springframework.boot.cli.command.init;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequest;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.message.StatusLine;
import org.json.JSONException;
import org.json.JSONObject;

import org.springframework.boot.cli.util.Log;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * Invokes the initializr service over HTTP.
 *
 * @author Stephane Nicoll
 */
class InitializrService {

	private static final String FILENAME_HEADER_PREFIX = "filename=\"";

	/**
	 * Accept header to use to retrieve the json meta-data.
	 */
	public static final String ACCEPT_META_DATA = "application/vnd.initializr.v2.1+"
			+ "json,application/vnd.initializr.v2+json";

	/**
	 * Accept header to use to retrieve the service capabilities of the service. If the
	 * service does not offer such feature, the json meta-data are retrieved instead.
	 */
	public static final String ACCEPT_SERVICE_CAPABILITIES = "text/plain," + ACCEPT_META_DATA;

	/**
	 * Late binding HTTP client.
	 */
	private HttpClient http;

	/**
	 * Constructs a new InitializrService object.
	 */
	InitializrService() {
	}

	/**
	 * Constructs a new InitializrService with the specified HttpClient.
	 * @param http the HttpClient to be used for making HTTP requests
	 */
	InitializrService(HttpClient http) {
		this.http = http;
	}

	/**
	 * Returns the HttpClient instance. If the HttpClient instance is null, it creates a
	 * new instance using the HttpClientBuilder. The HttpClient instance is configured to
	 * use system properties.
	 * @return the HttpClient instance
	 */
	protected HttpClient getHttp() {
		if (this.http == null) {
			this.http = HttpClientBuilder.create().useSystemProperties().build();
		}
		return this.http;
	}

	/**
	 * Generate a project based on the specified {@link ProjectGenerationRequest}.
	 * @param request the generation request
	 * @return an entity defining the project
	 * @throws IOException if generation fails
	 */
	ProjectGenerationResponse generate(ProjectGenerationRequest request) throws IOException {
		Log.info("Using service at " + request.getServiceUrl());
		InitializrServiceMetadata metadata = loadMetadata(request.getServiceUrl());
		URI url = request.generateUrl(metadata);
		ClassicHttpResponse httpResponse = executeProjectGenerationRequest(url);
		HttpEntity httpEntity = httpResponse.getEntity();
		validateResponse(httpResponse, request.getServiceUrl());
		return createResponse(httpResponse, httpEntity);
	}

	/**
	 * Load the {@link InitializrServiceMetadata} at the specified url.
	 * @param serviceUrl to url of the initializer service
	 * @return the metadata describing the service
	 * @throws IOException if the service's metadata cannot be loaded
	 */
	InitializrServiceMetadata loadMetadata(String serviceUrl) throws IOException {
		ClassicHttpResponse httpResponse = executeInitializrMetadataRetrieval(serviceUrl);
		validateResponse(httpResponse, serviceUrl);
		return parseJsonMetadata(httpResponse.getEntity());
	}

	/**
	 * Loads the service capabilities of the service at the specified URL. If the service
	 * supports generating a textual representation of the capabilities, it is returned,
	 * otherwise {@link InitializrServiceMetadata} is returned.
	 * @param serviceUrl to url of the initializer service
	 * @return the service capabilities (as a String) or the
	 * {@link InitializrServiceMetadata} describing the service
	 * @throws IOException if the service capabilities cannot be loaded
	 */
	Object loadServiceCapabilities(String serviceUrl) throws IOException {
		HttpGet request = new HttpGet(serviceUrl);
		request.setHeader(new BasicHeader(HttpHeaders.ACCEPT, ACCEPT_SERVICE_CAPABILITIES));
		ClassicHttpResponse httpResponse = execute(request, URI.create(serviceUrl), "retrieve help");
		validateResponse(httpResponse, serviceUrl);
		HttpEntity httpEntity = httpResponse.getEntity();
		ContentType contentType = ContentType.create(httpEntity.getContentType());
		if (contentType.getMimeType().equals("text/plain")) {
			return getContent(httpEntity);
		}
		return parseJsonMetadata(httpEntity);
	}

	/**
	 * Parses the JSON metadata from the given HTTP entity and returns an instance of
	 * InitializrServiceMetadata.
	 * @param httpEntity the HTTP entity containing the JSON metadata
	 * @return an instance of InitializrServiceMetadata parsed from the JSON metadata
	 * @throws IOException if an I/O error occurs while reading the HTTP entity
	 * @throws ReportableException if the content received from the server is invalid
	 */
	private InitializrServiceMetadata parseJsonMetadata(HttpEntity httpEntity) throws IOException {
		try {
			return new InitializrServiceMetadata(getContentAsJson(httpEntity));
		}
		catch (JSONException ex) {
			throw new ReportableException("Invalid content received from server (" + ex.getMessage() + ")", ex);
		}
	}

	/**
	 * Validates the response received from the server.
	 * @param httpResponse The ClassicHttpResponse object representing the response
	 * received from the server.
	 * @param serviceUrl The URL of the server.
	 * @throws ReportableException If no content is received from the server.
	 * @throws Exception If the response code is not 200.
	 */
	private void validateResponse(ClassicHttpResponse httpResponse, String serviceUrl) {
		if (httpResponse.getEntity() == null) {
			throw new ReportableException("No content received from server '" + serviceUrl + "'");
		}
		if (httpResponse.getCode() != 200) {
			throw createException(serviceUrl, httpResponse);
		}
	}

	/**
	 * Creates a response object for project generation.
	 * @param httpResponse The HTTP response received from the server.
	 * @param httpEntity The HTTP entity containing the response content.
	 * @return The project generation response object.
	 * @throws IOException If an I/O error occurs while reading the response content.
	 */
	private ProjectGenerationResponse createResponse(ClassicHttpResponse httpResponse, HttpEntity httpEntity)
			throws IOException {
		ProjectGenerationResponse response = new ProjectGenerationResponse(
				ContentType.create(httpEntity.getContentType()));
		response.setContent(FileCopyUtils.copyToByteArray(httpEntity.getContent()));
		String fileName = extractFileName(httpResponse.getFirstHeader("Content-Disposition"));
		if (fileName != null) {
			response.setFileName(fileName);
		}
		return response;
	}

	/**
	 * Request the creation of the project using the specified URL.
	 * @param url the URL
	 * @return the response
	 */
	private ClassicHttpResponse executeProjectGenerationRequest(URI url) {
		return execute(new HttpGet(url), url, "generate project");
	}

	/**
	 * Retrieves the meta-data of the service at the specified URL.
	 * @param url the URL
	 * @return the response
	 */
	private ClassicHttpResponse executeInitializrMetadataRetrieval(String url) {
		HttpGet request = new HttpGet(url);
		request.setHeader(new BasicHeader(HttpHeaders.ACCEPT, ACCEPT_META_DATA));
		return execute(request, URI.create(url), "retrieve metadata");
	}

	/**
	 * Executes an HTTP request and returns the response.
	 * @param request the HTTP request to be executed
	 * @param url the URI of the service to send the request to
	 * @param description a description of the action being performed
	 * @return the HTTP response received from the service
	 * @throws ReportableException if an error occurs while executing the request
	 */
	private ClassicHttpResponse execute(HttpUriRequest request, URI url, String description) {
		try {
			HttpHost host = HttpHost.create(url);
			request.addHeader("User-Agent", "SpringBootCli/" + getClass().getPackage().getImplementationVersion());
			return getHttp().executeOpen(host, request, null);
		}
		catch (IOException ex) {
			throw new ReportableException(
					"Failed to " + description + " from service at '" + url + "' (" + ex.getMessage() + ")");
		}
	}

	/**
	 * Creates a ReportableException with the given URL and ClassicHttpResponse.
	 * @param url The URL used for the service call.
	 * @param httpResponse The ClassicHttpResponse returned by the service.
	 * @return A ReportableException with the appropriate error message.
	 * @throws ReportableException if an error occurs during the creation of the
	 * exception.
	 */
	private ReportableException createException(String url, ClassicHttpResponse httpResponse) {
		StatusLine statusLine = new StatusLine(httpResponse);
		String message = "Initializr service call failed using '" + url + "' - service returned "
				+ statusLine.getReasonPhrase();
		String error = extractMessage(httpResponse.getEntity());
		if (StringUtils.hasText(error)) {
			message += ": '" + error + "'";
		}
		else {
			int statusCode = statusLine.getStatusCode();
			message += " (unexpected " + statusCode + " error)";
		}
		throw new ReportableException(message);
	}

	/**
	 * Extracts the message from the given HttpEntity.
	 * @param entity the HttpEntity to extract the message from
	 * @return the extracted message, or null if no message is found
	 */
	private String extractMessage(HttpEntity entity) {
		if (entity != null) {
			try {
				JSONObject error = getContentAsJson(entity);
				if (error.has("message")) {
					return error.getString("message");
				}
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		return null;
	}

	/**
	 * Converts the content of the given HttpEntity object into a JSONObject.
	 * @param entity the HttpEntity object containing the content to be converted
	 * @return the content of the HttpEntity object as a JSONObject
	 * @throws IOException if an I/O error occurs while reading the content
	 * @throws JSONException if the content cannot be converted into a JSONObject
	 */
	private JSONObject getContentAsJson(HttpEntity entity) throws IOException, JSONException {
		return new JSONObject(getContent(entity));
	}

	/**
	 * Retrieves the content from the given HttpEntity.
	 * @param entity the HttpEntity from which to retrieve the content
	 * @return the content as a String
	 * @throws IOException if an I/O error occurs while retrieving the content
	 */
	private String getContent(HttpEntity entity) throws IOException {
		ContentType contentType = ContentType.create(entity.getContentType());
		Charset charset = contentType.getCharset();
		charset = (charset != null) ? charset : StandardCharsets.UTF_8;
		byte[] content = FileCopyUtils.copyToByteArray(entity.getContent());
		return new String(content, charset);
	}

	/**
	 * Extracts the file name from the given header.
	 * @param header the header containing the file name
	 * @return the extracted file name, or null if the header is null or the file name
	 * cannot be extracted
	 */
	private String extractFileName(Header header) {
		if (header != null) {
			String value = header.getValue();
			int start = value.indexOf(FILENAME_HEADER_PREFIX);
			if (start != -1) {
				value = value.substring(start + FILENAME_HEADER_PREFIX.length());
				int end = value.indexOf('\"');
				if (end != -1) {
					return value.substring(0, end);
				}
			}
		}
		return null;
	}

}
