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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@Service
public class ApiImpl implements ApiService {

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;
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

                    if (res.containsKey("data") && res.get("data") instanceof Map) {
                        Map<?, ?> data = (Map<?, ?>) res.get("data");
                        if (data.containsKey("image_url")) {
                            urlAnimada = data.get("image_url").toString();
                        }
                    } else if (res.containsKey("image_url")) {
                        urlAnimada = res.get("image_url").toString();
                    } else if (res.containsKey("url")) {
                        urlAnimada = res.get("url").toString();
                    } else if (res.containsKey("output_url")) {
                        urlAnimada = res.get("output_url").toString();
                    }

                    final String urlFinal = urlAnimada;

                    // Manejo del error si la API externa no devolvió ninguna ruta válida
                    if (urlFinal == null || urlFinal.isEmpty() || urlFinal.equals("No se pudo generar la imagen")) {
                        ApiModel errorImg = new ApiModel();
                        errorImg.setUrlOriginal(urlOriginal);
                        errorImg.setUrlResultado("Error");
                        errorImg.setTipoServicio("PHOTO_TO_ANIME");
                        errorImg.setActivo(true);
                        return repository.save(errorImg);
                    }

                    // Descargamos la imagen generada por la IA y la metemos a MongoDB
                    return descargarImagenComoBytes(urlFinal)
                            .flatMap(bytes -> {
                                ApiModel img = new ApiModel();
                                img.setUrlOriginal(urlOriginal);
                                img.setTipoServicio("PHOTO_TO_ANIME");
                                img.setImagenBinaria(bytes); // Los bytes de anime ya están listos
                                img.setActivo(true);
                                return repository.save(img);
                            })
                            // Si la descarga falla por seguridad de la URL de la IA, guardamos la URL como
                            // texto alternativo
                            .onErrorResume(err -> {
                                ApiModel fallbackImg = new ApiModel();
                                fallbackImg.setUrlOriginal(urlOriginal);
                                fallbackImg.setUrlResultado(urlFinal);
                                fallbackImg.setTipoServicio("PHOTO_TO_ANIME");
                                fallbackImg.setActivo(true);
                                return repository.save(fallbackImg);
                            });
                });
    }

    @Override
    public Mono<ApiModel> update(String id, ApiModel apiModel) {
        return repository.findById(id)
                .flatMap(existing -> {
                    // CORRECCIÓN: Ahora actualiza TODOS los campos importantes que mande el
                    // frontend
                    if (apiModel.getTipoServicio() != null)
                        existing.setTipoServicio(apiModel.getTipoServicio());
                    if (apiModel.getActivo() != null)
                        existing.setActivo(apiModel.getActivo());
                    if (apiModel.getUrlResultado() != null)
                        existing.setUrlResultado(apiModel.getUrlResultado());

                    System.out.println("====== [BACKEND] Editando registro ID " + id + " (Activo: "
                            + existing.getActivo() + ") ======");
                    return repository.save(existing);
                })
                // Forzamos la persistencia en el flujo reactivo antes de responder
                .flatMap(itemGuardado -> repository.findById(itemGuardado.getId()));
    }

@Override
public Mono<ApiModel> deleteLogico(String id) {
    // 1. Creamos la condición de búsqueda por ID
    Query query = new Query(Criteria.where("id").is(id));

    // 2. Definimos qué campo queremos modificar textualmente en MongoDB
    Update update = new Update().set("activo", false);

    // 3. Ejecutamos el update directo en la base de datos y luego recuperamos el objeto actualizado
    return mongoTemplate.updateFirst(query, update, ApiModel.class)
            .flatMap(updateResult -> {
                System.out.println("Documentos modificados en MongoDB: " + updateResult.getModifiedCount());
                return repository.findById(id); // Devolvemos el registro real desde la BD para Postman
            });
}

public Mono<ApiModel> restoreRecord(String id) {
    Query query = new Query(Criteria.where("id").is(id));
    Update update = new Update().set("activo", true);

    return mongoTemplate.updateFirst(query, update, ApiModel.class)
            .flatMap(updateResult -> repository.findById(id));
}

    private Mono<byte[]> descargarImagenComoBytes(String urlImagen) {
        return webClient.get()
                .uri(urlImagen)
                .retrieve()
                .bodyToMono(byte[].class); // Descarga la imagen y la vuelve bytes
    }
}