/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.devtools.remote.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.devtools.autoconfigure.OptionalLiveReloadServer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

/**
 * {@link Runnable} that waits to trigger live reload until the remote server has
 * restarted.
 *
 * @author Phillip Webb
 */
class DelayedLiveReloadTrigger implements Runnable {

	private static final long SHUTDOWN_TIME = 1000;

	private static final long SLEEP_TIME = 500;

	private static final long TIMEOUT = 30000;

	private static final Log logger = LogFactory.getLog(DelayedLiveReloadTrigger.class);

	private final OptionalLiveReloadServer liveReloadServer;

	private final ClientHttpRequestFactory requestFactory;

	private final URI uri;

	private long shutdownTime = SHUTDOWN_TIME;

	private long sleepTime = SLEEP_TIME;

	private long timeout = TIMEOUT;

	/**
     * Constructs a new DelayedLiveReloadTrigger with the specified LiveReloadServer, RequestFactory, and URL.
     * 
     * @param liveReloadServer the LiveReloadServer to use for triggering live reload
     * @param requestFactory the RequestFactory to use for making HTTP requests
     * @param url the URL to trigger live reload on
     * @throws IllegalArgumentException if the LiveReloadServer is null, the RequestFactory is null, or the URL is empty
     */
    DelayedLiveReloadTrigger(OptionalLiveReloadServer liveReloadServer, ClientHttpRequestFactory requestFactory,
			String url) {
		Assert.notNull(liveReloadServer, "LiveReloadServer must not be null");
		Assert.notNull(requestFactory, "RequestFactory must not be null");
		Assert.hasLength(url, "URL must not be empty");
		this.liveReloadServer = liveReloadServer;
		this.requestFactory = requestFactory;
		try {
			this.uri = new URI(url);
		}
		catch (URISyntaxException ex) {
			throw new IllegalArgumentException(ex);
		}
	}

	/**
     * Sets the timings for the DelayedLiveReloadTrigger.
     * 
     * @param shutdown the time in milliseconds when the shutdown process should start
     * @param sleep the time in milliseconds to sleep before starting the shutdown process
     * @param timeout the time in milliseconds to wait for the shutdown process to complete before timing out
     */
    protected void setTimings(long shutdown, long sleep, long timeout) {
		this.shutdownTime = shutdown;
		this.sleepTime = sleep;
		this.timeout = timeout;
	}

	/**
     * This method is responsible for running the delayed live reload trigger.
     * It waits for the specified shutdown time before starting the trigger.
     * It then checks if the server is up and running within the specified timeout.
     * If the server is not up within the timeout, the method returns.
     * If the server is up, it triggers a live reload by calling the triggerReload() method of the liveReloadServer.
     * 
     * @throws InterruptedException if the thread is interrupted while sleeping
     */
    @Override
	public void run() {
		try {
			Thread.sleep(this.shutdownTime);
			long start = System.currentTimeMillis();
			while (!isUp()) {
				long runTime = System.currentTimeMillis() - start;
				if (runTime > this.timeout) {
					return;
				}
				Thread.sleep(this.sleepTime);
			}
			logger.info("Remote server has changed, triggering LiveReload");
			this.liveReloadServer.triggerReload();
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	/**
     * Checks if the server is up by sending a request and checking the response status code.
     * 
     * @return true if the server is up and returns a status code of HttpStatus.OK, false otherwise.
     */
    private boolean isUp() {
		try {
			ClientHttpRequest request = createRequest();
			try (ClientHttpResponse response = request.execute()) {
				return response.getStatusCode() == HttpStatus.OK;
			}
		}
		catch (Exception ex) {
			return false;
		}
	}

	/**
     * Creates a new ClientHttpRequest object for making a GET request.
     * 
     * @return the created ClientHttpRequest object
     * @throws IOException if an I/O error occurs while creating the request
     */
    private ClientHttpRequest createRequest() throws IOException {
		return this.requestFactory.createRequest(this.uri, HttpMethod.GET);
	}

}
