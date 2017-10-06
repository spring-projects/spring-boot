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

package org.springframework.boot.origin;

import org.junit.Test;

import org.springframework.boot.origin.TextResourceOrigin.Location;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link TextResourceOrigin}.
 *
 * @author Phillip Webb
 */
public class TextResourceOriginTests {

	@Test
	public void createWithNullResourceShouldSetNullResource() throws Exception {
		TextResourceOrigin origin = new TextResourceOrigin(null, null);
		assertThat(origin.getResource()).isNull();
	}

	@Test
	public void createWithNullLocationShouldSetNullLocation() throws Exception {
		TextResourceOrigin origin = new TextResourceOrigin(null, null);
		assertThat(origin.getLocation()).isNull();
	}

	@Test
	public void getResourceShouldReturnResource() throws Exception {
		ClassPathResource resource = new ClassPathResource("foo.txt");
		TextResourceOrigin origin = new TextResourceOrigin(resource, null);
		assertThat(origin.getResource()).isEqualTo(resource);
	}

	@Test
	public void getLocationShouldReturnLocation() throws Exception {
		Location location = new Location(1, 2);
		TextResourceOrigin origin = new TextResourceOrigin(null, location);
		assertThat(origin.getLocation()).isEqualTo(location);

	}

	@Test
	public void getLocationLineShouldReturnLine() throws Exception {
		Location location = new Location(1, 2);
		assertThat(location.getLine()).isEqualTo(1);
	}

	@Test
	public void getLocationColumnShouldReturnColumn() throws Exception {
		Location location = new Location(1, 2);
		assertThat(location.getColumn()).isEqualTo(2);
	}

	@Test
	public void locationToStringShouldReturnNiceString() throws Exception {
		Location location = new Location(1, 2);
		assertThat(location.toString()).isEqualTo("2:3");
	}

	@Test
	public void toStringShouldReturnNiceString() throws Exception {
		ClassPathResource resource = new ClassPathResource("foo.txt");
		Location location = new Location(1, 2);
		TextResourceOrigin origin = new TextResourceOrigin(resource, location);
		assertThat(origin.toString()).isEqualTo("class path resource [foo.txt]:2:3");
	}

	@Test
	public void toStringWhenResourceIsNullShouldReturnNiceString() throws Exception {
		Location location = new Location(1, 2);
		TextResourceOrigin origin = new TextResourceOrigin(null, location);
		assertThat(origin.toString()).isEqualTo("unknown resource [?]:2:3");
	}

	@Test
	public void toStringWhenLocationIsNullShouldReturnNiceString() throws Exception {
		ClassPathResource resource = new ClassPathResource("foo.txt");
		TextResourceOrigin origin = new TextResourceOrigin(resource, null);
		assertThat(origin.toString()).isEqualTo("class path resource [foo.txt]");
	}

	@Test
	public void locationEqualsAndHashCodeShouldUseLineAndColumn() throws Exception {
		Location location1 = new Location(1, 2);
		Location location2 = new Location(1, 2);
		Location location3 = new Location(2, 2);
		assertThat(location1.hashCode()).isEqualTo(location1.hashCode());
		assertThat(location1.hashCode()).isEqualTo(location2.hashCode());
		assertThat(location1.hashCode()).isNotEqualTo(location3.hashCode());
		assertThat(location1).isEqualTo(location1);
		assertThat(location1).isEqualTo(location2);
		assertThat(location1).isNotEqualTo(location3);
	}

	@Test
	public void equalsAndHashCodeShouldResourceAndLocation() throws Exception {
		TextResourceOrigin origin1 = new TextResourceOrigin(
				new ClassPathResource("foo.txt"), new Location(1, 2));
		TextResourceOrigin origin2 = new TextResourceOrigin(
				new ClassPathResource("foo.txt"), new Location(1, 2));
		TextResourceOrigin origin3 = new TextResourceOrigin(
				new ClassPathResource("foo.txt"), new Location(2, 2));
		TextResourceOrigin origin4 = new TextResourceOrigin(
				new ClassPathResource("foo2.txt"), new Location(1, 2));
		assertThat(origin1.hashCode()).isEqualTo(origin1.hashCode());
		assertThat(origin1.hashCode()).isEqualTo(origin2.hashCode());
		assertThat(origin1.hashCode()).isNotEqualTo(origin3.hashCode());
		assertThat(origin1.hashCode()).isNotEqualTo(origin4.hashCode());
		assertThat(origin1).isEqualTo(origin1);
		assertThat(origin1).isEqualTo(origin2);
		assertThat(origin1).isNotEqualTo(origin3);
		assertThat(origin1).isNotEqualTo(origin4);
	}

}
