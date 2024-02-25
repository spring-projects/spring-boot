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

package org.springframework.boot.devtools.remote.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.devtools.classpath.ClassPathChangedEvent;
import org.springframework.boot.devtools.filewatch.ChangedFile;
import org.springframework.boot.devtools.filewatch.ChangedFiles;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFile.Kind;
import org.springframework.boot.devtools.restart.classloader.ClassLoaderFiles;
import org.springframework.context.ApplicationListener;
import org.springframework.core.log.LogMessage;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * Listens and pushes any classpath updates to a remote endpoint.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 1.3.0
 */
public class ClassPathChangeUploader implements ApplicationListener<ClassPathChangedEvent> {

	private static final Map<ChangedFile.Type, ClassLoaderFile.Kind> TYPE_MAPPINGS;

	static {
		Map<ChangedFile.Type, ClassLoaderFile.Kind> map = new EnumMap<>(ChangedFile.Type.class);
		map.put(ChangedFile.Type.ADD, ClassLoaderFile.Kind.ADDED);
		map.put(ChangedFile.Type.DELETE, ClassLoaderFile.Kind.DELETED);
		map.put(ChangedFile.Type.MODIFY, ClassLoaderFile.Kind.MODIFIED);
		TYPE_MAPPINGS = Collections.unmodifiableMap(map);
	}

	private static final Log logger = LogFactory.getLog(ClassPathChangeUploader.class);

	private final URI uri;

	private final ClientHttpRequestFactory requestFactory;

	/**
     * Constructs a new ClassPathChangeUploader with the specified URL and request factory.
     * 
     * @param url the URL to upload the file to
     * @param requestFactory the request factory to use for creating HTTP requests
     * @throws IllegalArgumentException if the URL is empty or malformed
     */
    public ClassPathChangeUploader(String url, ClientHttpRequestFactory requestFactory) {
		Assert.hasLength(url, "URL must not be empty");
		Assert.notNull(requestFactory, "RequestFactory must not be null");
		try {
			this.uri = new URL(url).toURI();
		}
		catch (URISyntaxException | MalformedURLException ex) {
			throw new IllegalArgumentException("Malformed URL '" + url + "'");
		}
		this.requestFactory = requestFactory;
	}

	/**
     * This method is called when a ClassPathChangedEvent is triggered.
     * It retrieves the ClassLoaderFiles from the event, serializes them into bytes,
     * and performs an upload of the bytes.
     * 
     * @param event The ClassPathChangedEvent that triggered this method
     * @throws IllegalStateException if an IOException occurs during the process
     */
    @Override
	public void onApplicationEvent(ClassPathChangedEvent event) {
		try {
			ClassLoaderFiles classLoaderFiles = getClassLoaderFiles(event);
			byte[] bytes = serialize(classLoaderFiles);
			performUpload(bytes, event);
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

	/**
     * Performs the upload of the given byte array to the specified URI.
     * 
     * @param bytes the byte array to be uploaded
     * @param event the ClassPathChangedEvent associated with the upload
     * @throws IOException if an I/O error occurs during the upload
     */
    private void performUpload(byte[] bytes, ClassPathChangedEvent event) throws IOException {
		try {
			while (true) {
				try {
					ClientHttpRequest request = this.requestFactory.createRequest(this.uri, HttpMethod.POST);
					HttpHeaders headers = request.getHeaders();
					headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
					headers.setContentLength(bytes.length);
					FileCopyUtils.copy(bytes, request.getBody());
					logUpload(event);
					try (ClientHttpResponse response = request.execute()) {
						HttpStatusCode statusCode = response.getStatusCode();
						Assert.state(statusCode == HttpStatus.OK,
								() -> "Unexpected " + statusCode + " response uploading class files");
					}
					return;
				}
				catch (SocketException ex) {
					logger.warn(LogMessage.format(
							"A failure occurred when uploading to %s. Upload will be retried in 2 seconds", this.uri));
					logger.debug("Upload failure", ex);
					Thread.sleep(2000);
				}
			}
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ex);
		}
	}

	/**
     * Logs the upload of a ClassPathChangedEvent.
     * 
     * @param event the ClassPathChangedEvent to be logged
     */
    private void logUpload(ClassPathChangedEvent event) {
		logger.info(LogMessage.format("Uploading %s", event.overview()));
	}

	/**
     * Serializes the given ClassLoaderFiles object into a byte array.
     * 
     * @param classLoaderFiles the ClassLoaderFiles object to be serialized
     * @return the byte array representation of the serialized object
     * @throws IOException if an I/O error occurs during serialization
     */
    private byte[] serialize(ClassLoaderFiles classLoaderFiles) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
		objectOutputStream.writeObject(classLoaderFiles);
		objectOutputStream.close();
		return outputStream.toByteArray();
	}

	/**
     * Retrieves the ClassLoaderFiles object containing the changed files from the given ClassPathChangedEvent.
     * 
     * @param event the ClassPathChangedEvent representing the changes in the classpath
     * @return the ClassLoaderFiles object containing the changed files
     * @throws IOException if an I/O error occurs while retrieving the files
     */
    private ClassLoaderFiles getClassLoaderFiles(ClassPathChangedEvent event) throws IOException {
		ClassLoaderFiles files = new ClassLoaderFiles();
		for (ChangedFiles changedFiles : event.getChangeSet()) {
			String sourceDirectory = changedFiles.getSourceDirectory().getAbsolutePath();
			for (ChangedFile changedFile : changedFiles) {
				files.addFile(sourceDirectory, changedFile.getRelativeName(), asClassLoaderFile(changedFile));
			}
		}
		return files;
	}

	/**
     * Converts a ChangedFile object to a ClassLoaderFile object.
     * 
     * @param changedFile the ChangedFile object to be converted
     * @return a ClassLoaderFile object representing the converted ChangedFile
     * @throws IOException if an I/O error occurs while reading the file
     */
    private ClassLoaderFile asClassLoaderFile(ChangedFile changedFile) throws IOException {
		ClassLoaderFile.Kind kind = TYPE_MAPPINGS.get(changedFile.getType());
		byte[] bytes = (kind != Kind.DELETED) ? FileCopyUtils.copyToByteArray(changedFile.getFile()) : null;
		long lastModified = (kind != Kind.DELETED) ? changedFile.getFile().lastModified() : System.currentTimeMillis();
		return new ClassLoaderFile(kind, lastModified, bytes);
	}

}
