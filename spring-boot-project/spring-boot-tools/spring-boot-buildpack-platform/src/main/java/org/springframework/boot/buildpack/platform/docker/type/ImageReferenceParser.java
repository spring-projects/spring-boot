/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.buildpack.platform.docker.type;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parser for Docker image references in the form
 * {@code [domainHost:port/][path/]name[:tag][@digest]}.
 *
 * @author Scott Frederick
 * @see <a href=
 * "https://github.com/docker/distribution/blob/master/reference/reference.go">Docker
 * grammar reference</a>
 * @see <a href=
 * "https://github.com/docker/distribution/blob/master/reference/regexp.go">Docker grammar
 * implementation</a>
 * @see <a href=
 * "https://stackoverflow.com/questions/37861791/how-are-docker-image-names-parsed">How
 * are Docker image names parsed?</a>
 */
final class ImageReferenceParser {

	private static final String DOMAIN_SEGMENT_REGEX = "(?:[a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9])";

	private static final String DOMAIN_PORT_REGEX = "[0-9]+";

	private static final String DOMAIN_REGEX = oneOf(
			groupOf(DOMAIN_SEGMENT_REGEX, repeating("[.]", DOMAIN_SEGMENT_REGEX)),
			groupOf(DOMAIN_SEGMENT_REGEX, "[:]", DOMAIN_PORT_REGEX),
			groupOf(DOMAIN_SEGMENT_REGEX, repeating("[.]", DOMAIN_SEGMENT_REGEX), "[:]", DOMAIN_PORT_REGEX),
			"localhost");

	private static final String NAME_CHARS_REGEX = "[a-z0-9]+";

	private static final String NAME_SEPARATOR_REGEX = "(?:[._]|__|[-]*)";

	private static final String NAME_SEGMENT_REGEX = groupOf(NAME_CHARS_REGEX,
			optional(repeating(NAME_SEPARATOR_REGEX, NAME_CHARS_REGEX)));

	private static final String NAME_PATH_REGEX = groupOf(NAME_SEGMENT_REGEX,
			optional(repeating("[/]", NAME_SEGMENT_REGEX)));

	private static final String DIGEST_ALGORITHM_SEGMENT_REGEX = "[A-Za-z][A-Za-z0-9]*";

	private static final String DIGEST_ALGORITHM_SEPARATOR_REGEX = "[-_+.]";

	private static final String DIGEST_ALGORITHM_REGEX = groupOf(DIGEST_ALGORITHM_SEGMENT_REGEX,
			optional(repeating(DIGEST_ALGORITHM_SEPARATOR_REGEX, DIGEST_ALGORITHM_SEGMENT_REGEX)));

	private static final String DIGEST_VALUE_REGEX = "[0-9A-Fa-f]{32,}";

	private static final String DIGEST_REGEX = groupOf(DIGEST_ALGORITHM_REGEX, "[:]", DIGEST_VALUE_REGEX);

	private static final String TAG_REGEX = "[\\w][\\w.-]{0,127}";

	private static final String DOMAIN_CAPTURE_GROUP = "domain";

	private static final String NAME_CAPTURE_GROUP = "name";

	private static final String TAG_CAPTURE_GROUP = "tag";

	private static final String DIGEST_CAPTURE_GROUP = "digest";

	private static final Pattern REFERENCE_REGEX_PATTERN = patternOf(anchored(
			optional(captureOf(DOMAIN_CAPTURE_GROUP, DOMAIN_REGEX), "[/]"),
			captureOf(NAME_CAPTURE_GROUP, NAME_PATH_REGEX), optional("[:]", captureOf(TAG_CAPTURE_GROUP, TAG_REGEX)),
			optional("[@]", captureOf(DIGEST_CAPTURE_GROUP, DIGEST_REGEX))));

	private final String domain;

	private final String name;

	private final String tag;

	private final String digest;

	private ImageReferenceParser(String domain, String name, String tag, String digest) {
		this.domain = domain;
		this.name = name;
		this.tag = tag;
		this.digest = digest;
	}

	String getDomain() {
		return this.domain;
	}

	String getName() {
		return this.name;
	}

	String getTag() {
		return this.tag;
	}

	String getDigest() {
		return this.digest;
	}

	static ImageReferenceParser of(String reference) {
		Matcher matcher = REFERENCE_REGEX_PATTERN.matcher(reference);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Unable to parse image reference \"" + reference + "\". "
					+ "Image reference must be in the form \"[domainHost:port/][path/]name[:tag][@digest]\", "
					+ "with \"path\" and \"name\" containing only [a-z0-9][.][_][-]");
		}
		return new ImageReferenceParser(matcher.group(DOMAIN_CAPTURE_GROUP), matcher.group(NAME_CAPTURE_GROUP),
				matcher.group(TAG_CAPTURE_GROUP), matcher.group(DIGEST_CAPTURE_GROUP));
	}

	private static Pattern patternOf(String... expressions) {
		return Pattern.compile(join(expressions));
	}

	private static String groupOf(String... expressions) {
		return "(?:" + join(expressions) + ')';
	}

	private static String captureOf(String groupName, String... expressions) {
		return "(?<" + groupName + ">" + join(expressions) + ')';
	}

	private static String oneOf(String... expressions) {
		return groupOf(String.join("|", expressions));
	}

	private static String optional(String... expressions) {
		return groupOf(join(expressions)) + '?';
	}

	private static String repeating(String... expressions) {
		return groupOf(join(expressions)) + '+';
	}

	private static String anchored(String... expressions) {
		return '^' + join(expressions) + '$';
	}

	private static String join(String... expressions) {
		return String.join("", expressions);
	}

}
