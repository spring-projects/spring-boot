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

package smoketest.data.couchbase;

import org.springframework.data.annotation.Id;
import org.springframework.data.couchbase.core.mapping.Document;
import org.springframework.data.couchbase.core.mapping.id.GeneratedValue;
import org.springframework.data.couchbase.core.mapping.id.GenerationStrategy;

/**
 * SampleDocument class.
 */
@Document
public class SampleDocument {

	@Id
	@GeneratedValue(strategy = GenerationStrategy.UNIQUE)
	private String id;

	private String text;

	/**
	 * Returns the ID of the object.
	 * @return the ID of the object
	 */
	public String getId() {
		return this.id;
	}

	/**
	 * Sets the ID of the object.
	 * @param id the ID to be set
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Returns the text of the SampleDocument.
	 * @return the text of the SampleDocument
	 */
	public String getText() {
		return this.text;
	}

	/**
	 * Sets the text of the SampleDocument.
	 * @param text the text to be set
	 */
	public void setText(String text) {
		this.text = text;
	}

}
