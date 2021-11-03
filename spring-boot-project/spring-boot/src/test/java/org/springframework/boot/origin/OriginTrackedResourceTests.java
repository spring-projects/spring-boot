/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.origin;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.origin.OriginTrackedResource.OriginTrackedWritableResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link OriginTrackedResource}.
 *
 * @author Phillip Webb
 */
class OriginTrackedResourceTests {

	private Origin origin;

	private WritableResource resource;

	private OriginTrackedWritableResource tracked;

	@BeforeEach
	void setup() {
		this.origin = MockOrigin.of("test");
		this.resource = mock(WritableResource.class);
		this.tracked = OriginTrackedResource.of(this.resource, this.origin);
	}

	@Test
	void getInputStreamDelegatesToResource() throws IOException {
		this.tracked.getInputStream();
		verify(this.resource).getInputStream();
	}

	@Test
	void existsDelegatesToResource() {
		this.tracked.exists();
		verify(this.resource).exists();
	}

	@Test
	void isReadableDelegatesToResource() {
		this.tracked.isReadable();
		verify(this.resource).isReadable();
	}

	@Test
	void isOpenDelegatesToResource() {
		this.tracked.isOpen();
		verify(this.resource).isOpen();
	}

	@Test
	void isFileDelegatesToResource() {
		this.tracked.isFile();
		verify(this.resource).isFile();
	}

	@Test
	void getURLDelegatesToResource() throws IOException {
		this.tracked.getURL();
		verify(this.resource).getURL();
	}

	@Test
	void getURIDelegatesToResource() throws IOException {
		this.tracked.getURI();
		verify(this.resource).getURI();
	}

	@Test
	void getFileDelegatesToResource() throws IOException {
		this.tracked.getFile();
		verify(this.resource).getFile();
	}

	@Test
	void readableChannelDelegatesToResource() throws IOException {
		this.tracked.readableChannel();
		verify(this.resource).readableChannel();
	}

	@Test
	void contentLengthDelegatesToResource() throws IOException {
		this.tracked.contentLength();
		verify(this.resource).contentLength();
	}

	@Test
	void lastModifiedDelegatesToResource() throws IOException {
		this.tracked.lastModified();
		verify(this.resource).lastModified();
	}

	@Test
	void createRelativeDelegatesToResource() throws IOException {
		this.tracked.createRelative("path");
		verify(this.resource).createRelative("path");
	}

	@Test
	void getFilenameDelegatesToResource() {
		this.tracked.getFilename();
		verify(this.resource).getFilename();
	}

	@Test
	void getDescriptionDelegatesToResource() {
		this.tracked.getDescription();
		verify(this.resource).getDescription();
	}

	@Test
	void getOutputStreamDelegatesToResource() throws IOException {
		this.tracked.getOutputStream();
		verify(this.resource).getOutputStream();
	}

	@Test
	void toStringDelegatesToResource() {
		Resource resource = new ClassPathResource("test");
		Resource tracked = OriginTrackedResource.of(resource, this.origin);
		assertThat(tracked).hasToString(resource.toString());
	}

	@Test
	void getOriginReturnsOrigin() {
		assertThat(this.tracked.getOrigin()).isEqualTo(this.origin);
	}

	@Test
	void getResourceReturnsResource() {
		assertThat(this.tracked.getResource()).isEqualTo(this.resource);
	}

	@Test
	void equalsAndHashCode() {
		Origin o1 = MockOrigin.of("o1");
		Origin o2 = MockOrigin.of("o2");
		Resource r1 = mock(Resource.class);
		Resource r2 = mock(Resource.class);
		OriginTrackedResource r1o1a = OriginTrackedResource.of(r1, o1);
		OriginTrackedResource r1o1b = OriginTrackedResource.of(r1, o1);
		OriginTrackedResource r1o2 = OriginTrackedResource.of(r1, o2);
		OriginTrackedResource r2o1 = OriginTrackedResource.of(r2, o1);
		OriginTrackedResource r2o2 = OriginTrackedResource.of(r2, o2);
		assertThat(r1o1a).isEqualTo(r1o1a).isEqualTo(r1o1a).isNotEqualTo(r1o2).isNotEqualTo(r2o1).isNotEqualTo(r2o2);
		assertThat(r1o1a.hashCode()).isEqualTo(r1o1b.hashCode());
	}

	@Test
	void ofReturnsOriginTrackedResource() {
		Resource resource = mock(Resource.class);
		Resource tracked = OriginTrackedResource.of(resource, this.origin);
		assertThat(tracked).isExactlyInstanceOf(OriginTrackedResource.class);
	}

	@Test
	void ofWhenWritableReturnsOriginTrackedWritableResource() {
		Resource resource = mock(WritableResource.class);
		Resource tracked = OriginTrackedResource.of(resource, this.origin);
		assertThat(tracked).isInstanceOf(WritableResource.class)
				.isExactlyInstanceOf(OriginTrackedWritableResource.class);
	}

}
