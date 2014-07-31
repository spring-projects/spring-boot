/*
 * Copyright 2012-2014 the original author or authors.
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
 *
 * @author Monica Granbois
 */

package sample.data.jdbc.web;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import sample.data.jdbc.domain.User;
import sample.data.jdbc.service.UserService;
import java.nio.charset.Charset;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@RunWith (MockitoJUnitRunner.class)
public class UserControllerTests {

	private MockMvc mockMvc;
	@Mock
	private UserService userServiceMock;

	private static final MediaType APPLICATION_JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON.getType(),
																		 MediaType.APPLICATION_JSON.getSubtype(),
																		 Charset.forName("utf8")
	);

	private static final int USER_ID = 101;


	@Before
	public void setUp() {
		UserController userController = new UserController(userServiceMock);
		this.mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
	}

	@Test
	public void testInvalidPath() throws Exception {
		mockMvc.perform(get("/junk"))
				.andExpect(status().isNotFound());
		verifyZeroInteractions(userServiceMock);

	}

	@Test
	public void testGetUser() throws Exception {
		User user = createUser(USER_ID, "James", "Kirk");
		when(userServiceMock.getUser(USER_ID)).thenReturn(user);

		mockMvc.perform(get("/users/user/" + USER_ID))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andExpect(jsonPath("$.id", is(USER_ID)))
				.andExpect(jsonPath("$.firstName", is("James")))
				.andExpect(jsonPath("$.lastName", is("Kirk")));

		ArgumentCaptor<Integer> argumentCaptor = ArgumentCaptor.forClass(Integer.class);
		verify(userServiceMock, times(1)).getUser(argumentCaptor.capture());
		assertTrue(USER_ID == argumentCaptor.getValue());
		verifyNoMoreInteractions(userServiceMock);


	}

	@Test
	public void testGetUserBadPathParameter() throws Exception {
		mockMvc.perform(get("/users/user/junk"))
				.andExpect(status().isInternalServerError());
		verifyZeroInteractions(userServiceMock);
	}

	@Test
	public void testCreateUser() throws Exception {
		String firstName = "Kathryn";
		String lastName = "Janeway";

		User createdUser = createUser(USER_ID, firstName, lastName);

		when(userServiceMock.createUser(any(User.class))).thenReturn(createdUser);

		mockMvc.perform(post("/users/user")
								.contentType(APPLICATION_JSON_UTF8)
								.content("{\"lastName\": \"Janeway\", \"firstName\":\"Kathryn\"}"))
				.andExpect(status().isCreated())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8))
				.andExpect(jsonPath("$.id", is(USER_ID)))
				.andExpect(jsonPath("$.firstName", is(firstName)))
				.andExpect(jsonPath("$.lastName", is(lastName)));


		ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
		verify(userServiceMock, times(1)).createUser(userCaptor.capture());
		assertEquals(firstName, userCaptor.getValue().getFirstName());
		assertEquals(lastName, userCaptor.getValue().getLastName());
		verifyNoMoreInteractions(userServiceMock);

	}

	@Test
	public void testGetUsers() throws Exception {
		mockMvc.perform(get("/users"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(APPLICATION_JSON_UTF8));
		verify(userServiceMock, times(1)).getUsers();
	}


	@Test
	public void testRandomException() throws Exception {
		when(userServiceMock.getUser(USER_ID)).thenThrow(new RuntimeException("BOOM!"));
		mockMvc.perform(get("/users/user/" + USER_ID))
				.andExpect(status().isInternalServerError());

	}


	private User createUser(int id, String firstName, String lastName) {
		User user = new User();
		user.setId(id);
		user.setFirstName(firstName);
		user.setLastName(lastName);
		return user;
	}

}
