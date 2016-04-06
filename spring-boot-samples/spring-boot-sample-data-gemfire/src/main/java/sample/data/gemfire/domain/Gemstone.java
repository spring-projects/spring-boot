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

package sample.data.gemfire.domain;

import java.io.Serializable;

import org.springframework.core.style.ToStringCreator;
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

	public Gemstone(Long id) {
		this.id = id;
	}

	public Gemstone(Long id, String name) {
		this.id = id;
		this.name = name;
	}

	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || !obj.getClass().equals(getClass())) {
			return false;
		}
		return ObjectUtils.nullSafeEquals(this.getName(), ((Gemstone) obj).getName());
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHashCode(getName());
	}

	@Override
	public String toString() {
		ToStringCreator creator = new ToStringCreator(this);
		creator.append("id", this.id);
		creator.append("name", this.name);
		return creator.toString();
	}

}
