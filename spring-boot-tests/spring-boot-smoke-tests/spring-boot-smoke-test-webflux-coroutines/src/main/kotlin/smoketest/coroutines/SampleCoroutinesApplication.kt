package smoketest.coroutines

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SampleCoroutinesApplication
fun main(args: Array<String>) {
	runApplication<SampleCoroutinesApplication>(*args)

}
