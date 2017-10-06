/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.cli.compiler.maven;

import java.io.File;
import java.lang.reflect.Field;

import org.apache.maven.settings.Settings;
import org.apache.maven.settings.building.DefaultSettingsBuilderFactory;
import org.apache.maven.settings.building.DefaultSettingsBuildingRequest;
import org.apache.maven.settings.building.SettingsBuildingException;
import org.apache.maven.settings.building.SettingsBuildingRequest;
import org.apache.maven.settings.crypto.DefaultSettingsDecrypter;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;

import org.springframework.boot.cli.util.Log;

/**
 * {@code MavenSettingsReader} reads settings from a user's Maven settings.xml file,
 * decrypting them if necessary using settings-security.xml.
 *
 * @author Andy Wilkinson
 * @since 1.3.0
 */
public class MavenSettingsReader {

	private final String homeDir;

	public MavenSettingsReader() {
		this(System.getProperty("user.home"));
	}

	public MavenSettingsReader(String homeDir) {
		this.homeDir = homeDir;
	}

	public MavenSettings readSettings() {
		Settings settings = loadSettings();
		SettingsDecryptionResult decrypted = decryptSettings(settings);
		if (!decrypted.getProblems().isEmpty()) {
			Log.error(
					"Maven settings decryption failed. Some Maven repositories may be inaccessible");
			// Continue - the encrypted credentials may not be used
		}
		return new MavenSettings(settings, decrypted);
	}

	private Settings loadSettings() {
		File settingsFile = new File(this.homeDir, ".m2/settings.xml");
		SettingsBuildingRequest request = new DefaultSettingsBuildingRequest();
		request.setUserSettingsFile(settingsFile);
		request.setSystemProperties(System.getProperties());
		try {
			return new DefaultSettingsBuilderFactory().newInstance().build(request)
					.getEffectiveSettings();
		}
		catch (SettingsBuildingException ex) {
			throw new IllegalStateException(
					"Failed to build settings from " + settingsFile, ex);
		}
	}

	private SettingsDecryptionResult decryptSettings(Settings settings) {
		DefaultSettingsDecryptionRequest request = new DefaultSettingsDecryptionRequest(
				settings);

		return createSettingsDecrypter().decrypt(request);
	}

	private SettingsDecrypter createSettingsDecrypter() {
		SettingsDecrypter settingsDecrypter = new DefaultSettingsDecrypter();
		setField(DefaultSettingsDecrypter.class, "securityDispatcher", settingsDecrypter,
				new SpringBootSecDispatcher());
		return settingsDecrypter;
	}

	private void setField(Class<?> sourceClass, String fieldName, Object target,
			Object value) {
		try {
			Field field = sourceClass.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
		}
		catch (Exception ex) {
			throw new IllegalStateException(
					"Failed to set field '" + fieldName + "' on '" + target + "'", ex);
		}
	}

	private class SpringBootSecDispatcher extends DefaultSecDispatcher {

		private static final String SECURITY_XML = ".m2/settings-security.xml";

		SpringBootSecDispatcher() {
			File file = new File(MavenSettingsReader.this.homeDir, SECURITY_XML);
			this._configurationFile = file.getAbsolutePath();
			try {
				this._cipher = new DefaultPlexusCipher();
			}
			catch (PlexusCipherException e) {
				throw new IllegalStateException(e);
			}
		}

	}

}
