package com.webfluxapirest.dao;

import com.webfluxapirest.models.documents.Categoria;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface CategoriaRepository extends ReactiveMongoRepository<Categoria,String> {

    public Mono<Categoria> findByNombre(String nombre);
}
