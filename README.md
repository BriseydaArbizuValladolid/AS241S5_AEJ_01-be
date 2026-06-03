# AS241S5_AEJ_01-be ..
# AI Image Transformation Services con Spring Boot

Servicios Cognitivos de Rapid API: **AI Background Remover** y **PhotoToAnime**. Estos modelos permiten procesar imágenes mediante visión artificial para eliminar fondos y transformar fotografías en arte estilo anime. El sistema consume estas APIs de forma reactiva, captura los resultados (URLs de las imágenes procesadas) y las almacena automáticamente en una base de datos **MongoDB Atlas** (NoSQL Cloud).

## 1. Cognitive Services
✅ **Rapid API - AI Background Remover**
* Eliminación automática de fondo en imágenes con precisión de IA.

✅ **Rapid API - PhotoToAnime**
* Transformación de fotografías reales a estilo ilustración anime mediante redes neuronales.

## 2. Spring Boot
* **Java:** JDK 17
* **IDE:**  Visual Studio Code
* **Maven:** Apache Maven
* **Frameworks:** Spring Boot 3.x

## 3. Maven Dependencias:
* `spring-boot-starter-webflux`
* `spring-boot-starter-data-mongodb-reactive`
* `lombok`
* `reactor-test`
* `springdoc-openapi-starter-webflux-ui`

### Dependencias Spring WebFlux + MongoDB (NoSQL)

```xml
<dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>
<dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-mongodb-reactive</artifactId>
</dependency>
<dependency>
      <groupId>io.projectreactor</groupId>
      <artifactId>reactor-test</artifactId>
      <scope>test</scope>
</dependency>
