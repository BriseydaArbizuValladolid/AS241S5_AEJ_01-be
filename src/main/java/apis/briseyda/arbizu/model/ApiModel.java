package apis.briseyda.arbizu.model;

import lombok.Data;


import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Document(collection = "apisprueba")
@JsonInclude(JsonInclude.Include.NON_NULL) // Vuelve a estar presente
public class ApiModel {
    @Id
    private String id;
    private String urlOriginal;
    private String urlResultado;
    private byte[] imagenBinaria;
    private byte[] imagenOriginalBinaria;
    private String tipoServicio; 
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    
    @JsonProperty("activo")
    @Field("activo")
    private Boolean activo;

    // Getter manual para asegurar que Jackson lo vea siempre
    public Boolean getActivo() {
        return activo == null ? true : activo;
    }
}