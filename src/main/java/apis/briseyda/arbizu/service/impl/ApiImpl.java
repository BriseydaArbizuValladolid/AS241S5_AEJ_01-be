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
            img.setUrlResultado("/api/v1/ia/ver-imagen/" + img.getId());

            if ("BACKGROUND_REMOVER".equals(img.getTipoServicio())) {
                img.setUrlOriginal("/api/v1/ia/ver-imagen-original/" + img.getId());
            }

            img.setImagenBinaria(null);
            img.setImagenOriginalBinaria(null);
            if (img.getActivo() == null) {
            img.setActivo(true);
        }
            img.setActivo(true);
            return img;
        });
    }

    public Mono<ApiModel> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public Mono<ApiModel> save(ApiModel apiModel) {
        return repository.save(apiModel);
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
                            .bodyToMono(byte[].class)
                            .flatMap(bytesProcesados -> {
                                ApiModel registro = new ApiModel();
                                registro.setTipoServicio("BACKGROUND_REMOVER");
                                // Aquí guardamos los bytes para que existan en la BD
                                registro.setImagenBinaria(bytesProcesados);
                                registro.setImagenOriginalBinaria(bytesOriginales);
                                registro.setActivo(true);
                                System.out.println("ID: " + registro.getId() + " - ACTIVO: " + registro.getActivo());

                                // Guardamos y retornamos los bytes para que la web no de error
                                return repository.save(registro)
                                        .map(guardado -> {
                                            guardado.setActivo(true);
                                            return bytesProcesados; 
                                        })
                                        .thenReturn(bytesProcesados);
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
                    String urlAnimada = "";
                    if (res.containsKey("url"))
                        urlAnimada = res.get("url").toString();
                    else if (res.containsKey("output_url"))
                        urlAnimada = res.get("output_url").toString();
                    // ... (puedes dejar tus otros checks de llaves aquí)

                    final String urlFinal = urlAnimada;

                    // Si no hay URL, guardamos el error, si hay, DESCARGAMOS
                    if (urlFinal.isEmpty() || urlFinal.equals("No se pudo generar la imagen")) {
                        ApiModel errorImg = new ApiModel();
                        errorImg.setUrlOriginal(urlOriginal);
                        errorImg.setUrlResultado("Error");
                        errorImg.setTipoServicio("PHOTO_TO_ANIME");
                        errorImg.setActivo(true);
                        return repository.save(errorImg);
                    }

                    // NUEVA LÓGICA: Descargamos la imagen de la IA y la guardamos en el binario de
                    // Mongo
                    return descargarImagenComoBytes(urlFinal)
                            .flatMap(bytes -> {
                                ApiModel img = new ApiModel();
                                img.setUrlOriginal(urlOriginal);
                                img.setTipoServicio("PHOTO_TO_ANIME");
                                img.setImagenBinaria(bytes); // Guardamos la foto real en la BD
                                return repository.save(img);
                            });
                });
    }

    @Override
    public Mono<ApiModel> update(String id, ApiModel apiModel) {
        return repository.findById(id)
                .flatMap(existing -> {
                    existing.setTipoServicio(apiModel.getTipoServicio());
                    // actualiza otros campos si es necesario
                    return repository.save(existing);
                });
    }

    @Override
    public Mono<ApiModel> deleteLogico(String id) {
        return repository.findById(id)
                .flatMap(item -> {
                    item.setActivo(false);
                    return repository.save(item);
                });
    }

    private Mono<byte[]> descargarImagenComoBytes(String urlImagen) {
        return webClient.get()
                .uri(urlImagen)
                .retrieve()
                .bodyToMono(byte[].class); // Descarga la imagen y la vuelve bytes
    }
}