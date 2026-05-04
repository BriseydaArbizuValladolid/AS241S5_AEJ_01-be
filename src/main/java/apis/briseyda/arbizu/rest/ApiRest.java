package apis.briseyda.arbizu.rest;

import apis.briseyda.arbizu.model.ApiModel;
import apis.briseyda.arbizu.service.ApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux; // Importante para devolver listas reactivas
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/ia")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ApiRest {

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

    @GetMapping(value = "/ver-imagen-original/{id}", produces = MediaType.IMAGE_JPEG_VALUE)
    public Mono<byte[]> verImagenOriginal(@PathVariable String id) {
        return apiService.findById(id)
                .map(img -> img.getImagenOriginalBinaria() != null ? img.getImagenOriginalBinaria() : new byte[0]);
    }

    // --- MÉTODO PARA EDITAR (UPDATE) ---
    @PutMapping("/actualizar/{id}")
    public Mono<ApiModel> actualizar(@PathVariable String id, @RequestBody ApiModel data) {
        return apiService.update(id, data);
    }

    // --- MÉTODO PARA ELIMINADO LÓGICO (DELETE) ---
    // Usamos DELETE porque es la semántica de la acción,
    // pero el Service se encargará de que solo cambie el estado.
    @DeleteMapping("/eliminar/{id}")
    public Mono<ApiModel> deleteLogico(@PathVariable String id) {
        return apiService.deleteLogico(id);
    }
}