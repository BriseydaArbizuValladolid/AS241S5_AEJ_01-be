package apis.briseyda.arbizu.service.impl;

import apis.briseyda.arbizu.model.ApiModel;
import apis.briseyda.arbizu.repository.ApiRepository;
import apis.briseyda.arbizu.service.ApiService;

import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBufferUtils;

@Service
public class ApiImpl implements ApiService {

    private final WebClient webClient;
    private final ApiRepository repository;

    @Value("${api.rapidapi.key}")
    private String apiKey;

    public ApiImpl(WebClient webClient, ApiRepository repository) {
        this.webClient = webClient;
        this.repository = repository;
    }

@Override
public Mono<byte[]> removerFondoConArchivo(FilePart filePart) { // Cambio a Mono<byte[]>
    return DataBufferUtils.join(filePart.content())
            .flatMap(dataBuffer -> {
                byte[] bytes = new byte[dataBuffer.readableByteCount()];
                dataBuffer.read(bytes);
                DataBufferUtils.release(dataBuffer);

                MultipartBodyBuilder builder = new MultipartBodyBuilder();
                builder.part("image", bytes).filename(filePart.filename());

                return webClient.post()
                        .uri("https://ai-background-remover.p.rapidapi.com/image/matte/v1")
                        .header("x-rapidapi-key", apiKey)
                        .header("x-rapidapi-host", "ai-background-remover.p.rapidapi.com")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(builder.build()))
                        .retrieve()
                        .bodyToMono(byte[].class); // Retornamos los bytes directamente
            });
}

@Override
public Mono<ApiModel> convertirAnime(String urlOriginal) {
    return webClient.post()
            .uri("https://phototoanime1.p.rapidapi.com/cartoonize")
            .header("x-rapidapi-key", apiKey)
            .header("x-rapidapi-host", "phototoanime1.p.rapidapi.com")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(BodyInserters.fromFormData("image_url", urlOriginal)) // Enviamos URL A
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .flatMap(res -> {
                // LOG PARA DEPURAR: Así verás en la consola qué te responde la IA exactamente
                System.out.println("Respuesta completa de la IA: " + res);

                // Intentamos sacar la URL de los campos más comunes: "url" o "output_url"
                String urlAnimada = "No se pudo generar la imagen";

                if (res.containsKey("url")) {
                    urlAnimada = res.get("url").toString();
                } else if (res.containsKey("output_url")) {
                    urlAnimada = res.get("output_url").toString();
                } else if (res.containsKey("image_url")) {
                    urlAnimada = res.get("image_url").toString();
                } else if (res.containsKey("data")) {
                    Object data = res.get("data");
                    urlAnimada = data.toString();
                }


                ApiModel img = new ApiModel();
                img.setUrlOriginal(urlOriginal);
                img.setUrlResultado(urlAnimada); // Aquí guardamos la URL B
                img.setTipoServicio("PHOTO_TO_ANIME");

                return repository.save(img); // Guardamos el par de URLs en la BD
            });
}

}