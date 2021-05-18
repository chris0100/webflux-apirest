package com.webfluxapirest;

import com.webfluxapirest.models.documents.Categoria;
import com.webfluxapirest.models.documents.Producto;
import com.webfluxapirest.services.ProductoService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
class SpringBootWebfluxApirestApplicationTests {

    @Autowired
    private WebTestClient client;

    @Autowired
    private ProductoService productoServiceObj;

    @Test
    void listarTest() {
        client.get()
                .uri("/api/v2/productos")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBodyList(Producto.class)
                .consumeWith(response -> {
                    List<Producto> productos = response.getResponseBody();
                    assert productos != null;

                    productos.forEach(p -> {
                        System.out.println(p.getNombre());
                    });
                    assertThat(productos.size()>0).isTrue();
                });
                //.hasSize(4) replace with consumewith;
    }


    @Test
    void verTest() {

        Mono<Producto> producto = productoServiceObj.findByNombre("Radio Stereo");

        client.get()
                .uri("/api/v2/productos/{id}", Collections.singletonMap("id", Objects.requireNonNull(producto.block()).getId()))
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody(Producto.class)
                .consumeWith(response -> {
                    Producto p = response.getResponseBody();
                    Assertions.assertThat(p.getId()).isNotEmpty();
                    Assertions.assertThat(p.getId().length()>0).isTrue();
                    Assertions.assertThat(p.getNombre()).isEqualTo("Radio Stereo");
                });
    }

    @Test
    public void crearTest(){
        Categoria categoria = productoServiceObj.findCategoriaByNombre("Muebles").block();
        Producto producto = new Producto("Mesa Comedor", 100.00, categoria);
        client.post().uri("/api/v2/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(producto), Producto.class)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.id").isNotEmpty()
                .jsonPath("$.nombre").isEqualTo("Mesa Comedor")
                .jsonPath("$.categoria.nombre").isEqualTo("Muebles");
    }


    @Test
    public void editarTest() {
        Producto producto = productoServiceObj.findByNombre("Audifonos").block();
        Categoria categoria = productoServiceObj.findCategoriaByNombre("Muebles").block();

        Producto productoEditado = new Producto("parlantes", 250.36, categoria);

        client.put().uri("/api/v2/productos/{id}", Collections.singletonMap("id", producto.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Mono.just(productoEditado), Producto.class)
                .exchange()
                .expectStatus()
                .isCreated()
                .expectHeader()
                .contentType(MediaType.APPLICATION_JSON)
                .expectBody(Producto.class)
                .consumeWith(response -> {
                    Producto p = response.getResponseBody();
                    Assertions.assertThat(p.getId()).isNotEmpty();
                    Assertions.assertThat(p.getNombre()).isEqualTo("parlantes");
                    Assertions.assertThat(p.getCategoria().getNombre()).isEqualTo("Muebles");
                });
    }


    @Test
    public void eliminarTest(){
        Producto producto = productoServiceObj.findByNombre("Radio Stereo").block();
        client.delete().uri("/api/v2/productos/{id}", Collections.singletonMap("id", producto.getId()))
                .exchange()
                .expectStatus()
                .isNoContent()
                .expectBody()
                .isEmpty();

        client.get().uri("/api/v2/productos/{id}", Collections.singletonMap("id", producto.getId()))
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody()
                .isEmpty();
    }


}
