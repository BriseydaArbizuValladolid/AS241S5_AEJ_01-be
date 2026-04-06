package apis.briseyda.arbizu.rest;

import apis.briseyda.arbizu.model.ApiModel;
import apis.briseyda.arbizu.service.ApiService;
import lombok.RequiredArgsConstructor; // Agrega esta dependencia en tu pom.xml si no la tienes
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/ia")
@RequiredArgsConstructor // Esto genera el constructor automáticamente
@CrossOrigin(origins = "*")
public class ApiRest {

    private final ApiService apiService;

    @PostMapping(value = "/remover-fondo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.IMAGE_PNG_VALUE)
    public Mono<byte[]> remover(@RequestPart("file") FilePart file) {
        return apiService.removerFondoConArchivo(file);
    }

    @PostMapping("/convertir-anime")
    public Mono<ApiModel> anime(@RequestParam("url") String url) {
        return apiService.convertirAnime(url);
    }
}