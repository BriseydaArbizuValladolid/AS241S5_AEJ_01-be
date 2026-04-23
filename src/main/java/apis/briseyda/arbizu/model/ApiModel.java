package apis.briseyda.arbizu.model;

import lombok.Data;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonInclude;

@Data
@Document(collection = "apisprueba")

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiModel {
    @Id
    private String id;
    private String urlOriginal;
    private String urlResultado;
    private byte[] imagenBinaria;
    private String tipoServicio; // Ejemplo: "BACKGROUND_REMOVER" o "PHOTO_TO_ANIME"
    private LocalDateTime fechaCreacion = LocalDateTime.now();
}
