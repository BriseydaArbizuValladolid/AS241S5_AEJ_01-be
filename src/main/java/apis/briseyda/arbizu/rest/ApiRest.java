package apis.briseyda.arbizu.rest;

import apis.briseyda.arbizu.model.ApiModel;
import apis.briseyda.arbizu.service.ApiService;
import lombok.RequiredArgsConstructor;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/api/v1/ia")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ApiRest {

    @Autowired
    private final ApiService apiService;

    // --- MÉTODOS EXISTENTES ---

    @PostMapping(value = "/remover-fondo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.IMAGE_PNG_VALUE)
    public Mono<byte[]> remover(@RequestPart("file") FilePart file) {
        return apiService.removerFondoConArchivo(file);
    }

    @PostMapping("/convertir-anime")
    public Mono<ApiModel> anime(@RequestParam("url") String url) {
        return apiService.convertirAnime(url);
    }

    // --- NUEVO MÉTODO PARA VISUALIZAR ---

    @GetMapping(value = "/listar", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<ApiModel> listarTodo() {
        return apiService.findAll(); // Este método debe estar definido en tu Service
    }

    @GetMapping(value = "/ver-imagen/{id}")
    public Mono<ResponseEntity<byte[]>> verImagen(@PathVariable String id) {
        return apiService.findById(id)
            .map(img -> {
                byte[] bytes = img.getImagenBinaria();
                
                // Si no hay imagen, devolvemos 404 de inmediato
                if (bytes == null || bytes.length == 0) {
                    return ResponseEntity.notFound().<byte[]>build();
                }

                // Si hay imagen, la enviamos con el header correcto
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_JPEG_VALUE)
                    .body(bytes);
            })
            // Si el ID ni siquiera existe en la DB, devolvemos 404
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
    @GetMapping(value = "/ver-imagen-original/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public Mono<byte[]> verImagenOriginal(@PathVariable String id) {
        return apiService.findById(id)
                .map(img -> img.getImagenOriginalBinaria() != null ? img.getImagenOriginalBinaria() : new byte[0]);
    }

    // --- MÉTODO PARA EDITAR (UPDATE) ---
    // --- EDITAR ---
    @PutMapping("/editar/{id}")
    public Mono<ApiModel> editar(@PathVariable String id, @RequestBody ApiModel registroActualizado) {
        return apiService.findById(id)
                .flatMap(itemExistente -> {
                    // Actualizamos URL
                    itemExistente.setUrlOriginal(registroActualizado.getUrlOriginal());

                    // Validación de Tipo de Servicio
                    String nuevoTipo = registroActualizado.getTipoServicio();
                    if ("BACKGROUND_REMOVER".equals(nuevoTipo) || "PHOTO_TO_ANIME".equals(nuevoTipo)) {
                        itemExistente.setTipoServicio(nuevoTipo);
                    }

                    return apiService.save(itemExistente);
                });
    }

    // --- ELIMINADO LÓGICO ---
    @PatchMapping("/eliminar/{id}")
    public Mono<ApiModel> eliminar(@PathVariable String id) {
        return apiService.findById(id)
                .flatMap(existente -> {
                    existente.setActivo(false); // Cambia de true a false
                    return apiService.save(existente);
                });
    }

    // --- RESTAURAR ---
    @PatchMapping("/restaurar/{id}")
    public Mono<ApiModel> restaurar(@PathVariable String id) {
        return apiService.findById(id)
                .flatMap(existente -> {
                    existente.setActivo(true); // Cambia de false a true
                    return apiService.save(existente);
                });
    }
}