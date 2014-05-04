/*
 * Copyright 2010-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.data.gemfire.domain;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.gemfire.mapping.Region;
import org.springframework.util.ObjectUtils;

/**
 * The Gemstone class is an abstract data type modeling a Gemstone, such as a diamond or a
 * ruby.
 * 
 * @author John Blum
 */
@Region("Gemstones")
public class Gemstone implements Serializable {

	@Id
	private Long id;

	private String name;

	public Gemstone() {
	}

	public Gemstone(final Long id) {
		this.id = id;
	}

	public Gemstone(final Long id, final String name) {
		this.id = id;
		this.name = name;
	}

	public Long getId() {
		return this.id;
	}

	public void setId(final Long id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}

		if (!(obj instanceof Gemstone)) {
			return false;
		}

		Gemstone that = (Gemstone) obj;

		return ObjectUtils.nullSafeEquals(this.getName(), that.getName());
	}

	@Override
	public int hashCode() {
		int hashValue = 17;
		hashValue = 37 * hashValue + ObjectUtils.nullSafeHashCode(getName());
		return hashValue;
	}

	@Override
	public String toString() {
		return String.format("{ @type = %1$s, id = %2$d, name = %3$s }", getClass()
				.getName(), getId(), getName());
	}

}
