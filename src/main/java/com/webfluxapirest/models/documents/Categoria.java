package com.webfluxapirest.models.documents;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "categorias")
@Data
@NoArgsConstructor
public class Categoria {

    @Id
    private String id;
    private String nombre;

    public Categoria(String nombre) {
        this.nombre = nombre;
    }
}
