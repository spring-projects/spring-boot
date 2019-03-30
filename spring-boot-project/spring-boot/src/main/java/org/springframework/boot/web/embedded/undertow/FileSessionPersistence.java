/*
 * Copyright 2012-2018 the original author or authors.
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

	FileSessionPersistence(File dir) {
		this.dir = dir;
	}

	@Override
	public void persistSessions(String deploymentName,
			Map<String, PersistentSession> sessionData) {
		try {
			save(sessionData, getSessionFile(deploymentName));
		}
		catch (Exception ex) {
			UndertowServletLogger.ROOT_LOGGER.failedToPersistSessions(ex);
		}
	}

	private void save(Map<String, PersistentSession> sessionData, File file)
			throws IOException {
		try (ObjectOutputStream stream = new ObjectOutputStream(
				new FileOutputStream(file))) {
			save(sessionData, stream);
		}
	}

	private void save(Map<String, PersistentSession> sessionData,
			ObjectOutputStream stream) throws IOException {
		Map<String, Serializable> session = new LinkedHashMap<>();
		sessionData.forEach((key, value) -> session.put(key,
				new SerializablePersistentSession(value)));
		stream.writeObject(session);
	}

	@Override
	public Map<String, PersistentSession> loadSessionAttributes(String deploymentName,
			final ClassLoader classLoader) {
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

	private Map<String, PersistentSession> load(File file, ClassLoader classLoader)
			throws IOException, ClassNotFoundException {
		try (ObjectInputStream stream = new ConfigurableObjectInputStream(
				new FileInputStream(file), classLoader)) {
			return load(stream);
		}
	}

	private Map<String, PersistentSession> load(ObjectInputStream stream)
			throws ClassNotFoundException, IOException {
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

	@SuppressWarnings("unchecked")
	private Map<String, SerializablePersistentSession> readSession(
			ObjectInputStream stream) throws ClassNotFoundException, IOException {
		return ((Map<String, SerializablePersistentSession>) stream.readObject());
	}

	private File getSessionFile(String deploymentName) {
		if (!this.dir.exists()) {
			this.dir.mkdirs();
		}
		return new File(this.dir, deploymentName + ".session");
	}

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

		SerializablePersistentSession(PersistentSession session) {
			this.expiration = session.getExpiration();
			this.sessionData = new LinkedHashMap<>(session.getSessionData());
		}

		public PersistentSession getPersistentSession() {
			return new PersistentSession(this.expiration, this.sessionData);
		}

	}

}
