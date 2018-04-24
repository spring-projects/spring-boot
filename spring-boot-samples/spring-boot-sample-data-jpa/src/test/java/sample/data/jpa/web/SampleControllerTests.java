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

package sample.data.jpa.web;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import sample.data.jpa.domain.City;
import sample.data.jpa.service.CitySearchCriteria;
import sample.data.jpa.service.CityService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test for {@link SampleController}.
 *
 * Verifies that both {@link org.springframework.data.web.PageableArgumentResolver}
 * and {@link org.springframework.data.web.SortArgumentResolver} are registered.
 *
 * @author Alejandro Gomez
 */
@RunWith(SpringRunner.class)
@WebMvcTest(SampleController.class)
public class SampleControllerTests {

	@Autowired
	private MockMvc mvc;

	@MockBean
	private CityService cityService;

	@After
	public void tearDown() {
		verifyNoMoreInteractions(this.cityService);
	}

	@Test
	public void testPaged() throws Exception {
		when(this.cityService.findCities(any(CitySearchCriteria.class), any(Pageable.class)))
				.thenAnswer(invocation -> new PageImpl<>(Collections.singletonList(new City("Buenos Aires", "Argentina")),
						invocation.getArgument(1), 1));

		this.mvc.perform(get("/paged").param("name", "Buenos Aires")
				.param("page", "0").param("size", "10").param("sort", "name,desc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.first", is(true)))
				.andExpect(jsonPath("$.last", is(true)))
				.andExpect(jsonPath("$.totalElements", is(1)))
				.andExpect(jsonPath("$.numberOfElements", is(1)))
				.andExpect(jsonPath("$.size", is(10)))
				.andExpect(jsonPath("$.number", is(0)))
				.andExpect(jsonPath("$.totalPages", is(1)))
				.andExpect(jsonPath("$.sort.sorted", is(true)))
				.andExpect(jsonPath("$.sort.unsorted", is(false)))
				.andExpect(jsonPath("$.pageable.sort.sorted", is(true)))
				.andExpect(jsonPath("$.pageable.sort.unsorted", is(false)))
				.andExpect(jsonPath("$.pageable.pageNumber", is(0)))
				.andExpect(jsonPath("$.pageable.pageSize", is(10)))
				.andExpect(jsonPath("$.pageable.offset", is(0)))
				.andExpect(jsonPath("$.pageable.paged", is(true)))
				.andExpect(jsonPath("$.pageable.unpaged", is(false)))
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].name", is("Buenos Aires")))
				.andExpect(jsonPath("$.content[0].country", is("Argentina")));

		ArgumentCaptor<CitySearchCriteria> criteria = ArgumentCaptor.forClass(CitySearchCriteria.class);
		verify(this.cityService).findCities(criteria.capture(), eq(PageRequest.of(0, 10, Sort.Direction.DESC, "name")));
		assertThat(criteria.getValue().getName()).isEqualTo("Buenos Aires");
	}

}
