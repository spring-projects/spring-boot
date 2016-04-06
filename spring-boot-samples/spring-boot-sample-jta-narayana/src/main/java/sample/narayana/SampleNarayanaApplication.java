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

package sample.narayana;

import java.io.Closeable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class SampleNarayanaApplication {

	public static void main(String[] args) throws Exception {
		ApplicationContext context = SpringApplication
				.run(SampleNarayanaApplication.class, args);
		AccountService service = context.getBean(AccountService.class);
		AccountRepository repository = context.getBean(AccountRepository.class);
		service.createAccountAndNotify("josh");
		System.out.println("Count is " + repository.count());
		try {
			// Using username "error" will cause service to throw SampleRuntimeException
			service.createAccountAndNotify("error");
		}
		catch (SampleRuntimeException ex) {
			// Log message to let test case know that exception was thrown
			System.out.println(ex.getMessage());
		}
		System.out.println("Count is " + repository.count());
		((Closeable) context).close();
	}

}
