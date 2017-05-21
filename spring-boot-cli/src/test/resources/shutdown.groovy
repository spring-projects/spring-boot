import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class Shutdown implements ApplicationContextAware {

    ConfigurableApplicationContext applicationContext

    @RequestMapping("/shutdown")
    String shutdown() {
        try {
            return "Shutting down server"
        } finally {
            Thread thread = new Thread(new Runnable() {
                @Override
                void run() {
                    try {
                        Thread.sleep(500L)
                    }
                    catch (InterruptedException ex) {
                        Thread.currentThread().interrupt()
                    }
                    Shutdown.this.applicationContext.close()
                }
            });
            thread.setContextClassLoader(getClass().getClassLoader())
            thread.start()
        }
    }

    @Override
    void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            this.applicationContext = (ConfigurableApplicationContext) applicationContext
        }
    }
}
