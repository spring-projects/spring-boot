/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.cli.command.init;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.boot.cli.util.Log;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

/**
 * Invokes the initializr service over HTTP.
 *
 * @author Stephane Nicoll
 * @since 1.2.0
 */
class InitializrServiceHttpInvoker {

	private final CloseableHttpClient httpClient;

	/**
	 * Create a new instance with the given {@link CloseableHttpClient http client}.
	 */
	InitializrServiceHttpInvoker(CloseableHttpClient httpClient) {
		this.httpClient = httpClient;
	}

	/**
	 * Generate a project based on the specified {@link ProjectGenerationRequest}
	 * @return an entity defining the project
	 */
	ProjectGenerationResponse generate(ProjectGenerationRequest request)
			throws IOException {
		Log.info("Using service at " + request.getServiceUrl());
		InitializrServiceMetadata metadata = loadMetadata(request.getServiceUrl());
		URI url = request.generateUrl(metadata);
		CloseableHttpResponse httpResponse = executeProjectGenerationRequest(url);

		HttpEntity httpEntity = httpResponse.getEntity();
		if (httpEntity == null) {
			throw new ProjectGenerationException(
					"No content received from server using '" + url + "'");
		}
		if (httpResponse.getStatusLine().getStatusCode() != 200) {
			throw buildProjectGenerationException(request.getServiceUrl(), httpResponse);
		}
		return createResponse(httpResponse, httpEntity);
	}

	/**
	 * Load the {@link InitializrServiceMetadata} at the specified url.
	 */
	InitializrServiceMetadata loadMetadata(String serviceUrl) throws IOException {
		CloseableHttpResponse httpResponse = executeInitializrMetadataRetrieval(serviceUrl);
		if (httpResponse.getEntity() == null) {
			throw new ProjectGenerationException(
					"No content received from server using '" + serviceUrl + "'");
		}
		if (httpResponse.getStatusLine().getStatusCode() != 200) {
			throw buildProjectGenerationException(serviceUrl, httpResponse);
		}
		try {
			HttpEntity httpEntity = httpResponse.getEntity();
			JSONObject root = getContentAsJson(getContent(httpEntity),
					getContentType(httpEntity));
			return new InitializrServiceMetadata(root);
		}
		catch (JSONException e) {
			throw new ProjectGenerationException("Invalid content received from server ("
					+ e.getMessage() + ")");
		}
	}

	private ProjectGenerationResponse createResponse(CloseableHttpResponse httpResponse,
			HttpEntity httpEntity) throws IOException {
		ProjectGenerationResponse response = new ProjectGenerationResponse();
		ContentType contentType = ContentType.getOrDefault(httpEntity);
		response.setContentType(contentType);

		InputStream in = httpEntity.getContent();
		try {
			response.setContent(StreamUtils.copyToByteArray(in));
		}
		finally {
			in.close();
		}

		String detectedFileName = extractFileName(httpResponse
				.getFirstHeader("Content-Disposition"));
		if (detectedFileName != null) {
			response.setFileName(detectedFileName);
		}
		return response;
	}

	/**
	 * Request the creation of the project using the specified url
	 */
	private CloseableHttpResponse executeProjectGenerationRequest(URI url) {
		try {
			HttpGet get = new HttpGet(url);
			return this.httpClient.execute(get);
		}
		catch (IOException e) {
			throw new ProjectGenerationException("Failed to invoke server at '" + url
					+ "' (" + e.getMessage() + ")");
		}
	}

	/**
	 * Retrieves the metadata of the service at the specified url
	 */
	private CloseableHttpResponse executeInitializrMetadataRetrieval(String serviceUrl) {
		try {
			HttpGet get = new HttpGet(serviceUrl);
			get.setHeader(new BasicHeader(HttpHeaders.ACCEPT, "application/json"));
			return this.httpClient.execute(get);
		}
		catch (IOException e) {
			throw new ProjectGenerationException(
					"Failed to retrieve metadata from service at '" + serviceUrl + "' ("
							+ e.getMessage() + ")");
		}
	}

	private byte[] getContent(HttpEntity httpEntity) throws IOException {
		InputStream in = httpEntity.getContent();
		try {
			return StreamUtils.copyToByteArray(in);
		}
		finally {
			in.close();
		}
	}

	private ContentType getContentType(HttpEntity httpEntity) {
		return ContentType.getOrDefault(httpEntity);
	}

	private JSONObject getContentAsJson(byte[] content, ContentType contentType) {
		Charset charset = contentType.getCharset() != null ? contentType.getCharset()
				: Charset.forName("UTF-8");
		String data = new String(content, charset);
		return new JSONObject(data);
	}

	private ProjectGenerationException buildProjectGenerationException(String url,
			CloseableHttpResponse httpResponse) {
		StringBuilder sb = new StringBuilder("Project generation failed using '");
		sb.append(url).append("' - service returned ")
				.append(httpResponse.getStatusLine().getReasonPhrase());
		String error = extractMessage(httpResponse.getEntity());
		if (StringUtils.hasText(error)) {
			sb.append(": '").append(error).append("'");
		}
		else {
			sb.append(" (unexpected ")
					.append(httpResponse.getStatusLine().getStatusCode())
					.append(" error)");
		}
		throw new ProjectGenerationException(sb.toString());
	}

	private String extractMessage(HttpEntity entity) {
		if (entity == null) {
			return null;
		}
		try {
			JSONObject error = getContentAsJson(getContent(entity),
					getContentType(entity));
			if (error.has("message")) {
				return error.getString("message");
			}
			return null;
		}
		catch (Exception e) {
			return null;
		}
	}

	private static String extractFileName(Header h) {
		if (h == null) {
			return null;
		}
		String value = h.getValue();
		String prefix = "filename=\"";
		int start = value.indexOf(prefix);
		if (start != -1) {
			value = value.substring(start + prefix.length(), value.length());
			int end = value.indexOf("\"");
			if (end != -1) {
				return value.substring(0, end);
			}
		}
		return null;
	}

}
