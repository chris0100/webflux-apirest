package com.webfluxapirest.dao;

import com.webfluxapirest.models.documents.Producto;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;


public interface ProductoRepository extends ReactiveMongoRepository<Producto,String> {

    Mono<Producto> findByNombre(String nombre);

    @Query("{ 'nombre':?0 }")
    Mono<Producto> obtenerPorNombre(String nombre);
}
