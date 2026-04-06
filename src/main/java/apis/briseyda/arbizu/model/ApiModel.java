package apis.briseyda.arbizu.model;

import lombok.Data;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "apisprueba")


public class ApiModel {
    @Id
    private String id;
    private String urlOriginal;
    private String urlResultado;
    private String tipoServicio; // Ejemplo: "BACKGROUND_REMOVER" o "PHOTO_TO_ANIME"
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
