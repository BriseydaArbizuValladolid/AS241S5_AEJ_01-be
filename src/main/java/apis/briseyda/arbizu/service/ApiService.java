package apis.briseyda.arbizu.service;

import org.springframework.http.codec.multipart.FilePart;

import apis.briseyda.arbizu.model.ApiModel;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface ApiService {
    Mono<byte[]> removerFondoConArchivo(FilePart filePart);
    Mono<ApiModel> convertirAnime(String url);

    // --- LISTAR ---
    Flux<ApiModel> findAll();
    Mono<ApiModel> findById(String id);

    // --- EDITAR ---
    Mono<ApiModel> update(String id, ApiModel apiModel);

    // --- ELIMINADO LOGICO ---
    Mono<ApiModel> deleteLogico(String id);

}
