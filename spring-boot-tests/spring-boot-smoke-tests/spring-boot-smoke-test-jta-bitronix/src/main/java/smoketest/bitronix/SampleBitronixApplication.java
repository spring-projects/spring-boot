/*
 * Copyright 2012-2019 the original author or authors.
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

package smoketest.bitronix;

import java.io.Closeable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class SampleBitronixApplication {

	public static void main(String[] args) throws Exception {
		ApplicationContext context = SpringApplication.run(SampleBitronixApplication.class, args);
		AccountService service = context.getBean(AccountService.class);
		AccountRepository repository = context.getBean(AccountRepository.class);
		service.createAccountAndNotify("josh");
		System.out.println("Count is " + repository.count());
		try {
			service.createAccountAndNotify("error");
		}
		catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		System.out.println("Count is " + repository.count());
		Thread.sleep(100);
		((Closeable) context).close();
	}

}
