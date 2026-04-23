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
import reactor.core.publisher.Flux; // Importado para el listado
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBufferUtils;

@Service
public class ApiImpl implements ApiService {

    private final WebClient webClient;
    private final ApiRepository repository;

    @Value("${api.rapidapi.key}") // Coincide con tu application.yml
    private String apiKey;

    public ApiImpl(WebClient webClient, ApiRepository repository) {
        this.webClient = webClient;
        this.repository = repository;
    }

    // --- MÉTODO PARA LISTAR ---
    @Override
    public Flux<ApiModel> findAll() {
        return repository.findAll().map(img -> {
            // 1. SOLO si el servicio es de remover fondo, aplicamos la URL corta
            if ("BACKGROUND_REMOVER".equals(img.getTipoServicio())) {
                String urlCorta = "/api/v1/ia/ver-imagen/" + img.getId();
                img.setUrlResultado(urlCorta);

                // Limpiamos el binario para que no pese en el JSON
                img.setImagenBinaria(null);
            }

            // 2. Si es PHOTO_TO_ANIME, no tocamos nada.
            // Se enviará la URL de la IA que ya está guardada en urlResultado.

            return img;
        });
    }

    public Mono<ApiModel> findById(String id) {
        return repository.findById(id);
    }

    // --- MÉTODO PARA REMOVER FONDO ---
    @Override
    public Mono<byte[]> removerFondoConArchivo(FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
                .flatMap(dataBuffer -> {
                    byte[] bytesOriginales = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytesOriginales);
                    DataBufferUtils.release(dataBuffer);

                    MultipartBodyBuilder builder = new MultipartBodyBuilder();
                    builder.part("image", bytesOriginales).filename(filePart.filename());

                    return webClient.post()
                            .uri("https://ai-background-remover.p.rapidapi.com/image/matte/v1")
                            .header("x-rapidapi-key", apiKey)
                            .header("x-rapidapi-host", "ai-background-remover.p.rapidapi.com")
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .body(BodyInserters.fromMultipartData(builder.build()))
                            .retrieve()
                            .bodyToMono(byte[].class) // La IA nos da los bytes de la imagen limpia
                            .flatMap(bytesProcesados -> {

                                // --- BLOQUE PARA GUARDAR EN MONGO ---
                                ApiModel registro = new ApiModel();
                                registro.setUrlOriginal(filePart.filename());
                                registro.setTipoServicio("BACKGROUND_REMOVER");
                                // Guardamos los bytes en el nuevo campo que creamos en el modelo
                                registro.setImagenBinaria(bytesProcesados);

                                // Guardamos en repositorio y luego devolvemos los bytes para Postman
                                return repository.save(registro)
                                        .thenReturn(bytesProcesados);
                                // ------------------------------------
                            });
                });
    }

    // --- MÉTODO PARA CONVERTIR A ANIME ---
    @Override
    public Mono<ApiModel> convertirAnime(String urlOriginal) {
        return webClient.post()
                .uri("https://phototoanime1.p.rapidapi.com/cartoonize")
                .header("x-rapidapi-key", apiKey)
                .header("x-rapidapi-host", "phototoanime1.p.rapidapi.com")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("image_url", urlOriginal))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .flatMap(res -> {
                    System.out.println("Respuesta completa de la IA: " + res);

                    String urlAnimada = "No se pudo generar la imagen";

                    if (res.containsKey("url")) {
                        urlAnimada = res.get("url").toString();
                    } else if (res.containsKey("output_url")) {
                        urlAnimada = res.get("output_url").toString();
                    } else if (res.containsKey("image_url")) {
                        urlAnimada = res.get("image_url").toString();
                    } else if (res.containsKey("data")) {
                        urlAnimada = res.get("data").toString();
                    }

                    ApiModel img = new ApiModel();
                    img.setUrlOriginal(urlOriginal);
                    img.setUrlResultado(urlAnimada);
                    img.setTipoServicio("PHOTO_TO_ANIME");

                    return repository.save(img); // Guarda en Mongo
                });
    }
}