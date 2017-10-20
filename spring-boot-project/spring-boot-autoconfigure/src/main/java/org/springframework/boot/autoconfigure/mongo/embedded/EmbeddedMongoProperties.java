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

package org.springframework.boot.autoconfigure.mongo.embedded;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import de.flapdoodle.embed.mongo.distribution.Feature;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Embedded Mongo.
 *
 * @author Andy Wilkinson
 * @author Yogesh Lonkar
 * @author Adrien Colson
 * @since 1.3.0
 */
@ConfigurationProperties(prefix = "spring.mongodb.embedded")
public class EmbeddedMongoProperties {

	/**
	 * Version of Mongo to use.
	 */
	private String version = "3.2.2";

	private final Storage storage = new Storage();

	private final CmdOptions cmdOptions = new CmdOptions();

	/**
	 * Comma-separated list of features to enable.
	 */
	private Set<Feature> features = new HashSet<>(
			Collections.singletonList(Feature.SYNC_DELAY));

	public String getVersion() {
		return this.version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Set<Feature> getFeatures() {
		return this.features;
	}

	public void setFeatures(Set<Feature> features) {
		this.features = features;
	}

	public Storage getStorage() {
		return this.storage;
	}

	public CmdOptions getCmdOptions() {
		return this.cmdOptions;
	}

	public static class Storage {

		/**
		 * Maximum size of the oplog in megabytes.
		 */
		private Integer oplogSize;

		/**
		 * Name of the replica set.
		 */
		private String replSetName;

		/**
		 * Directory used for data storage.
		 */
		private String databaseDir;

		public Integer getOplogSize() {
			return this.oplogSize;
		}

		public void setOplogSize(Integer oplogSize) {
			this.oplogSize = oplogSize;
		}

		public String getReplSetName() {
			return this.replSetName;
		}

		public void setReplSetName(String replSetName) {
			this.replSetName = replSetName;
		}

		public String getDatabaseDir() {
			return this.databaseDir;
		}

		public void setDatabaseDir(String databaseDir) {
			this.databaseDir = databaseDir;
		}

	}

	public static class CmdOptions {

		private Integer syncDelay;

		private String storageEngine;

		private Boolean isVerbose;

		private Boolean useNoPrealloc;

		private Boolean useSmallFiles;

		private Boolean useNoJournal;

		private Boolean enableTextSearch;

		private Boolean auth;

		private Boolean master;

		public Integer getSyncDelay() {
			return this.syncDelay;
		}

		public void setSyncDelay(final Integer syncDelay) {
			this.syncDelay = syncDelay;
		}

		public String getStorageEngine() {
			return this.storageEngine;
		}

		public void setStorageEngine(final String storageEngine) {
			this.storageEngine = storageEngine;
		}

		public Boolean isVerbose() {
			return this.isVerbose;
		}

		public void setVerbose(final Boolean isVerbose) {
			this.isVerbose = isVerbose;
		}

		public Boolean isUseNoPrealloc() {
			return this.useNoPrealloc;
		}

		public void setUseNoPrealloc(final Boolean useNoPrealloc) {
			this.useNoPrealloc = useNoPrealloc;
		}

		public Boolean isUseSmallFiles() {
			return this.useSmallFiles;
		}

		public void setUseSmallFiles(final Boolean useSmallFiles) {
			this.useSmallFiles = useSmallFiles;
		}

		public Boolean isUseNoJournal() {
			return this.useNoJournal;
		}

		public void setUseNoJournal(final Boolean useNoJournal) {
			this.useNoJournal = useNoJournal;
		}

		public Boolean isEnableTextSearch() {
			return this.enableTextSearch;
		}

		public void setEnableTextSearch(final Boolean enableTextSearch) {
			this.enableTextSearch = enableTextSearch;
		}

		public Boolean isAuth() {
			return this.auth;
		}

		public void setAuth(final Boolean auth) {
			this.auth = auth;
		}

		public Boolean isMaster() {
			return this.master;
		}

		public void setMaster(final Boolean master) {
			this.master = master;
		}

	}
}
