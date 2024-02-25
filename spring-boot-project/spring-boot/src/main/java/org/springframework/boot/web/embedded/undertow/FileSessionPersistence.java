/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.web.embedded.undertow;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import io.undertow.servlet.UndertowServletLogger;
import io.undertow.servlet.api.SessionPersistenceManager;

import org.springframework.core.ConfigurableObjectInputStream;

/**
 * {@link SessionPersistenceManager} that stores session information in a file.
 *
 * @author Phillip Webb
 * @author Peter Leibiger
 * @author Raja Kolli
 */
class FileSessionPersistence implements SessionPersistenceManager {

	private final File dir;

	/**
	 * Constructs a new FileSessionPersistence object with the specified directory.
	 * @param dir the directory where the session persistence files will be stored
	 */
	FileSessionPersistence(File dir) {
		this.dir = dir;
	}

	/**
	 * Persists the sessions for a given deployment.
	 * @param deploymentName the name of the deployment
	 * @param sessionData a map containing the session data to be persisted
	 */
	@Override
	public void persistSessions(String deploymentName, Map<String, PersistentSession> sessionData) {
		try {
			save(sessionData, getSessionFile(deploymentName));
		}
		catch (Exception ex) {
			UndertowServletLogger.ROOT_LOGGER.failedToPersistSessions(ex);
		}
	}

	/**
	 * Saves the session data to a file.
	 * @param sessionData the map containing the session data to be saved
	 * @param file the file to save the session data to
	 * @throws IOException if an I/O error occurs while saving the session data
	 */
	private void save(Map<String, PersistentSession> sessionData, File file) throws IOException {
		try (ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(file))) {
			save(sessionData, stream);
		}
	}

	/**
	 * Saves the session data to an ObjectOutputStream.
	 * @param sessionData the map containing the session data
	 * @param stream the ObjectOutputStream to save the session data to
	 * @throws IOException if an I/O error occurs while saving the session data
	 */
	private void save(Map<String, PersistentSession> sessionData, ObjectOutputStream stream) throws IOException {
		Map<String, Serializable> session = new LinkedHashMap<>();
		sessionData.forEach((key, value) -> session.put(key, new SerializablePersistentSession(value)));
		stream.writeObject(session);
	}

	/**
	 * Loads the session attributes from a file for a given deployment name and class
	 * loader.
	 * @param deploymentName the name of the deployment
	 * @param classLoader the class loader to use for loading the session attributes
	 * @return a map of session attributes loaded from the file, or null if the file does
	 * not exist or an error occurs
	 */
	@Override
	public Map<String, PersistentSession> loadSessionAttributes(String deploymentName, final ClassLoader classLoader) {
		try {
			File file = getSessionFile(deploymentName);
			if (file.exists()) {
				return load(file, classLoader);
			}
		}
		catch (Exception ex) {
			UndertowServletLogger.ROOT_LOGGER.failedtoLoadPersistentSessions(ex);
		}
		return null;
	}

	/**
	 * Loads a map of persistent sessions from a file using the specified class loader.
	 * @param file the file to load the sessions from
	 * @param classLoader the class loader to use for deserialization
	 * @return a map of persistent sessions loaded from the file
	 * @throws IOException if an I/O error occurs while reading the file
	 * @throws ClassNotFoundException if the class of a serialized object cannot be found
	 */
	private Map<String, PersistentSession> load(File file, ClassLoader classLoader)
			throws IOException, ClassNotFoundException {
		try (ObjectInputStream stream = new ConfigurableObjectInputStream(new FileInputStream(file), classLoader)) {
			return load(stream);
		}
	}

	/**
	 * Loads a map of persistent sessions from an ObjectInputStream.
	 * @param stream the ObjectInputStream to read the sessions from
	 * @return a map of persistent sessions, filtered by expiration time
	 * @throws ClassNotFoundException if the class of a serialized object cannot be found
	 * @throws IOException if an I/O error occurs while reading the sessions
	 */
	private Map<String, PersistentSession> load(ObjectInputStream stream) throws ClassNotFoundException, IOException {
		Map<String, SerializablePersistentSession> session = readSession(stream);
		long time = System.currentTimeMillis();
		Map<String, PersistentSession> result = new LinkedHashMap<>();
		session.forEach((key, value) -> {
			PersistentSession entrySession = value.getPersistentSession();
			if (entrySession.getExpiration().getTime() > time) {
				result.put(key, entrySession);
			}
		});
		return result;
	}

	/**
	 * Reads a session from an ObjectInputStream and returns it as a Map of String keys to
	 * SerializablePersistentSession values.
	 * @param stream the ObjectInputStream to read the session from
	 * @return a Map of String keys to SerializablePersistentSession values representing
	 * the session
	 * @throws ClassNotFoundException if the class of a serialized object could not be
	 * found
	 * @throws IOException if an I/O error occurs while reading the session
	 */
	@SuppressWarnings("unchecked")
	private Map<String, SerializablePersistentSession> readSession(ObjectInputStream stream)
			throws ClassNotFoundException, IOException {
		return ((Map<String, SerializablePersistentSession>) stream.readObject());
	}

	/**
	 * Returns the session file for the specified deployment name. If the directory does
	 * not exist, it creates the directory.
	 * @param deploymentName the name of the deployment
	 * @return the session file for the specified deployment name
	 */
	private File getSessionFile(String deploymentName) {
		if (!this.dir.exists()) {
			this.dir.mkdirs();
		}
		return new File(this.dir, deploymentName + ".session");
	}

	/**
	 * Clears the session file for the specified deployment.
	 * @param deploymentName the name of the deployment
	 */
	@Override
	public void clear(String deploymentName) {
		getSessionFile(deploymentName).delete();
	}

	/**
	 * Session data in a serializable form.
	 */
	static class SerializablePersistentSession implements Serializable {

		private static final long serialVersionUID = 0L;

		private final Date expiration;

		private final Map<String, Object> sessionData;

		/**
		 * Creates a new instance of SerializablePersistentSession by copying the
		 * properties of the given PersistentSession.
		 * @param session the PersistentSession to be serialized
		 */
		SerializablePersistentSession(PersistentSession session) {
			this.expiration = session.getExpiration();
			this.sessionData = new LinkedHashMap<>(session.getSessionData());
		}

		/**
		 * Returns a new instance of PersistentSession with the specified expiration and
		 * session data.
		 * @return a new instance of PersistentSession
		 */
		PersistentSession getPersistentSession() {
			return new PersistentSession(this.expiration, this.sessionData);
		}

	}

}
