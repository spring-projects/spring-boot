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

package sample.atomikos;

import javax.transaction.Transactional;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AccountService {

	private final JmsTemplate jmsTemplate;

	private final AccountRepository accountRepository;

	public AccountService(JmsTemplate jmsTemplate, AccountRepository accountRepository) {
		this.jmsTemplate = jmsTemplate;
		this.accountRepository = accountRepository;
	}

	public void createAccountAndNotify(String username) {
		this.jmsTemplate.convertAndSend("accounts", username);
		this.accountRepository.save(new Account(username));
		if ("error".equals(username)) {
			throw new RuntimeException("Simulated error");
		}
	}

}
