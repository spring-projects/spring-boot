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

package org.springframework.boot.devtools.restart.server;

import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.Assert;

/**
 * A HTTP server that can be used to upload updated {@link ClassLoaderFiles} and trigger
 * restarts.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see RestartServer
 */
public class HttpRestartServer {

	private static final Log logger = LogFactory.getLog(HttpRestartServer.class);

	private final RestartServer server;

	/**
	 * Create a new {@link HttpRestartServer} instance.
	 * @param sourceFolderUrlFilter the source filter used to link remote folder to the
	 * local classpath
	 */
	public HttpRestartServer(SourceFolderUrlFilter sourceFolderUrlFilter) {
		Assert.notNull(sourceFolderUrlFilter, "SourceFolderUrlFilter must not be null");
		this.server = new RestartServer(sourceFolderUrlFilter);
	}

	/**
	 * Create a new {@link HttpRestartServer} instance.
	 * @param restartServer the underlying restart server
	 */
	public HttpRestartServer(RestartServer restartServer) {
		Assert.notNull(restartServer, "RestartServer must not be null");
		this.server = restartServer;
	}

	/**
	 * Handle a server request.
	 * @param request the request
	 * @param response the response
	 * @throws IOException in case of I/O errors
	 */
	public void handle(ServerHttpRequest request, ServerHttpResponse response)
			throws IOException {
		try {
			Assert.state(request.getHeaders().getContentLength() > 0, "No content");
			ObjectInputStream objectInputStream = new ObjectInputStream(
					request.getBody());
			ClassLoaderFiles files = (ClassLoaderFiles) objectInputStream.readObject();
			objectInputStream.close();
			this.server.updateAndRestart(files);
			response.setStatusCode(HttpStatus.OK);
		}
		catch (Exception ex) {
			logger.warn("Unable to handler restart server HTTP request", ex);
			response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

}
