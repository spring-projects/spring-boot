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

package sample.data.jdbc.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import sample.data.jdbc.domain.User;
import sample.data.jdbc.domain.UserRepository;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith (MockitoJUnitRunner.class)
public class UserServiceImplTests {

	@Mock
	UserRepository userRepository;

	private UserServiceImpl userService;

	private static final int USER_ID = 1;

	@Before
	public void setUp() {
		userService = new UserServiceImpl(userRepository);
	}

	@Test
	public void testGetUser() {
		userService.getUser(USER_ID);
		verify(userRepository, times(1)).getUser(USER_ID);
		verifyNoMoreInteractions(userRepository);

	}

	@Test
	public void testGetUsers() {
		userService.getUsers();
		verify(userRepository, times(1)).getUsers();
		verifyNoMoreInteractions(userRepository);
	}

	@Test
	public void testCreateUser() {
		User user = new User();
		user.setFirstName("Benjamin");
		user.setLastName("Sisko");
		when(userRepository.insertUser(any(User.class))).thenReturn(101);
		userService.createUser(user);
		verify(userRepository, times(1)).insertUser(user);
		ArgumentCaptor<Integer> argumentCaptor = ArgumentCaptor.forClass(Integer.class);
		verify(userRepository, times(1)).getUser(argumentCaptor.capture());
		assertTrue(101 == argumentCaptor.getValue());
		verifyNoMoreInteractions(userRepository);

	}


}
