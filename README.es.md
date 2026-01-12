# Spring Boot

[![Estado de Build](https://github.com/spring-projects/spring-boot/actions/workflows/build-and-deploy-snapshot.yml/badge.svg?branch=main)](https://github.com/spring-projects/spring-boot/actions/workflows/build-and-deploy-snapshot.yml?query=branch%3Amain)

**[English](README.adoc) | Español**

---

## ¿Qué es Spring Boot?

Spring Boot te ayuda a crear aplicaciones y servicios basados en Spring de forma súper rápida y sin complicarte la vida. 

La idea es simple: tomamos decisiones inteligentes por ti (opiniones, le dicen) para que no tengas que configurar mil cosas antes de empezar. Pero tranquilo, si necesitas cambiar algo, es fácil salirse de los defaults y hacer las cosas a tu manera.

Con Spring Boot puedes crear:
- Aplicaciones Java standalone que corren con un simple `java -jar`
- Deployments tradicionales con archivos WAR
- Scripts usando nuestra herramienta de línea de comandos

## ¿Por qué usar Spring Boot?

Nos enfocamos en:

- **Comenzar rápido**: Queremos que todos puedan empezar con Spring de la forma más fácil y rápida posible.
- **Ser práctico pero flexible**: Damos opiniones claras al inicio, pero si necesitas algo diferente, no te ponemos trabas.
- **Incluir lo esencial**: Servers embebidos, seguridad, métricas, health checks, configuración externalizada... todo lo que necesitas en producción.
- **Cero magia negra**: No generamos código por ti y tampoco te obligamos a escribir XML (gracias a Dios).

## Instalación y Primeros Pasos

La [documentación oficial](https://docs.spring.io/spring-boot) incluye:
- [Instrucciones detalladas de instalación](https://docs.spring.io/spring-boot/installing.html)
- [Guía completa de "Primeros Pasos"](https://docs.spring.io/spring-boot/tutorial/first-application/index.html)

### Ejemplo Rápido

Acá va un ejemplo súper simple de una aplicación completa en Spring Boot:

```java
import org.springframework.boot.*;
import org.springframework.boot.autocomplete.*;
import org.springframework.web.bind.annotation.*;

@RestController
@SpringBootApplication
public class Example {

    @RequestMapping("/")
    String home() {
        return "¡Hola Mundo!";
    }

    public static void main(String[] args) {
        SpringApplication.run(Example.class, args);
    }
}
```

Sí, eso es todo. Con eso ya tienes una aplicación web funcionando.

## Reportar Problemas

Usamos GitHub Issues para trackear bugs y mejoras. Si tienes una pregunta general sobre cómo usar Spring Boot, mejor pregunta en [Stack Overflow](https://stackoverflow.com) usando el tag `spring-boot`.

Si encontraste un bug, ayúdanos a solucionarlo más rápido dándonos toda la info posible. Idealmente, un proyecto pequeño que reproduzca el problema.

## Contribuir

¿Quieres contribuir? ¡Genial! Lee nuestra [guía de contribución](CONTRIBUTING.adoc) para saber cómo empezar.

Spring Boot es un proyecto de código abierto bajo la licencia Apache 2.0. Todas las contribuciones son bienvenidas, desde arreglar typos hasta implementar features nuevos.

## Licencia

Spring Boot se distribuye bajo la [Licencia Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0).

## Mantente Conectado

- 🌐 [Sitio Oficial](https://spring.io/projects/spring-boot)
- 📖 [Documentación](https://docs.spring.io/spring-boot)
- 💬 [Stack Overflow](https://stackoverflow.com/questions/tagged/spring-boot)
- 🐦 [Twitter](https://twitter.com/springboot)

---

**¿Primera vez con Spring Boot?** No te preocupes, todos empezamos así. La documentación oficial es bastante buena y la comunidad es muy activa. ¡Suerte con tu proyecto!
