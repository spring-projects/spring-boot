/*
 * Copyright 2012-present the original author or authors.
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
import org.jspecify.annotations.Nullable;

import org.springframework.boot.cli.json.JSONException;
import org.springframework.boot.cli.json.JSONObject;
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
	private @Nullable HttpClient http;

	InitializrService() {
	}

	InitializrService(HttpClient http) {
		this.http = http;
	}

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

	private InitializrServiceMetadata parseJsonMetadata(HttpEntity httpEntity) throws IOException {
		try {
			return new InitializrServiceMetadata(getContentAsJson(httpEntity));
		}
		catch (JSONException ex) {
			throw new ReportableException("Invalid content received from server (" + ex.getMessage() + ")", ex);
		}
	}

	private void validateResponse(ClassicHttpResponse httpResponse, String serviceUrl) {
		if (httpResponse.getEntity() == null) {
			throw new ReportableException("No content received from server '" + serviceUrl + "'");
		}
		if (httpResponse.getCode() != 200) {
			throw createException(serviceUrl, httpResponse);
		}
	}

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

	private @Nullable String extractMessage(@Nullable HttpEntity entity) {
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

	private JSONObject getContentAsJson(HttpEntity entity) throws IOException, JSONException {
		return new JSONObject(getContent(entity));
	}

	private String getContent(HttpEntity entity) throws IOException {
		ContentType contentType = ContentType.create(entity.getContentType());
		Charset charset = contentType.getCharset();
		charset = (charset != null) ? charset : StandardCharsets.UTF_8;
		byte[] content = FileCopyUtils.copyToByteArray(entity.getContent());
		return new String(content, charset);
	}

	private @Nullable String extractFileName(@Nullable Header header) {
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
