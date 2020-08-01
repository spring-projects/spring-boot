package org.test

import java.util.concurrent.atomic.AtomicLong

@Configuration(proxyBeanMethods = false)
@EnableCaching
class Sample {

	@Bean CacheManager cacheManager() {
		new ConcurrentMapCacheManager()
	}

	@Component
	static class MyClient implements CommandLineRunner {

		private final MyService myService

		@Autowired
		MyClient(MyService myService) {
			this.myService = myService
		}

		void run(String... args) {
			long counter = myService.get('someKey')
			long counter2 = myService.get('someKey')
			if (counter == counter2) {
				println 'Hello World'
			} else {
				println 'Something went wrong with the cache setup'
			}

		}
	}

	@Component
	static class MyService {

		private final AtomicLong counter = new AtomicLong()

		@Cacheable('foo')
		Long get(String id) {
			return counter.getAndIncrement()
		}

	}

}