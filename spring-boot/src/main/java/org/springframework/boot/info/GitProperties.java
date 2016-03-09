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

package org.springframework.boot.info;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

/**
 * Provide git-related information such as commit id and time.
 *
 * @author Stephane Nicoll
 * @since 1.4.0
 */
public class GitProperties extends InfoProperties {

	public GitProperties(Properties entries) {
		super(processEntries(entries));
	}

	/**
	 * Return the name of the branch or {@code null}.
	 * @return the branch
	 */
	public String getBranch() {
		return get("branch");
	}

	/**
	 * Return the full id of the commit or {@code null}.
	 * @return the full commit id
	 */
	public String getCommitId() {
		return get("commit.id");
	}

	/**
	 * Return the abbreviated id of the commit oir {@code null}.
	 * @return the short commit id
	 */
	public String getShortCommitId() {
		String commitId = getCommitId();
		return commitId == null ? null
				: (commitId.length() > 7 ? commitId.substring(0, 7) : commitId);
	}

	/**
	 * Return the timestamp of the commit, possibly as epoch time in millisecond, or {@code null}.
	 * @return the commit time
	 * @see Date#getTime()
	 */
	public String getCommitTime() {
		return get("commit.time");
	}

	private static Properties processEntries(Properties properties) {
		coerceDate(properties, "commit.time");
		coerceDate(properties, "build.time");
		return properties;
	}

	private static void coerceDate(Properties properties, String key) {
		String value = properties.getProperty(key);
		if (value != null) {
			properties.setProperty(key, coerceToEpoch(value));
		}
	}

	/**
	 * Attempt to convert the specified value to epoch time. Git properties
	 * information are known to be specified either as epoch time in seconds
	 * or using a specific date format.
	 * @param s the value to coerce to
	 * @return the epoch time in milliseconds or the original value if it couldn't be
	 * converted
	 */
	private static String coerceToEpoch(String s) {
		Long epoch = parseEpochSecond(s);
		if (epoch != null) {
			return String.valueOf(epoch);
		}
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.US);
		try {
			return String.valueOf(format.parse(s).getTime());
		}
		catch (ParseException ex) {
			return s;
		}
	}

	private static Long parseEpochSecond(String s) {
		try {
			return Long.parseLong(s) * 1000;
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

}
