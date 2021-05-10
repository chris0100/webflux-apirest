package com.webfluxapirest.handler;

import com.webfluxapirest.models.documents.Categoria;
import com.webfluxapirest.models.documents.Producto;
import com.webfluxapirest.services.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.codec.multipart.FormFieldPart;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.net.URI;
import java.util.Date;
import java.util.UUID;

import static org.springframework.web.reactive.function.BodyInserters.fromObject;

@Component
public class ProductoHandler {

    @Autowired
    private ProductoService productoServiceObj;

    @Value("${config.uploads.path}")
    private String path;

    @Autowired
    private Validator validator;


    // listar todos los productos
    public Mono<ServerResponse> listar(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(productoServiceObj.findAll(), Producto.class);
    }

    // ver un producto por id
    public Mono<ServerResponse> ver(ServerRequest request) {
        String id = request.pathVariable("id");

        return productoServiceObj.findById(id)
                .flatMap(p -> ServerResponse
                        .ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(fromObject(p))
                        .switchIfEmpty(ServerResponse.notFound().build()));
    }


    // crear un producto
    public Mono<ServerResponse> crear(ServerRequest request) {
        Mono<Producto> productoMono = request.bodyToMono(Producto.class);

        return productoMono
                .flatMap(p -> {

                    Errors errors = new BeanPropertyBindingResult(p, Producto.class.getName());
                    validator.validate(p, errors);

                    if (errors.hasErrors()) {
                        return Flux.fromIterable(errors.getFieldErrors())
                                .map(fieldError -> "El campo " + fieldError.getField() + " " + fieldError.getDefaultMessage())
                                .collectList()
                                .flatMap(list -> ServerResponse.badRequest().body(fromObject(list)));

                    } else {
                        if (p.getCreateAt() == null) {
                            p.setCreateAt(new Date());
                        }
                        return productoServiceObj.save(p)
                                .flatMap(pdb -> ServerResponse
                                        .created(URI.create("/api/v2/producctos/".concat(pdb.getId())))
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .body(fromObject(pdb)));
                    }
                });
    }


    // editar un producto
    public Mono<ServerResponse> editar(ServerRequest request) {
        Mono<Producto> productoMono = request.bodyToMono(Producto.class);
        String id = request.pathVariable("id");

        Mono<Producto> productoDB = productoServiceObj.findById(id);

        return productoDB.zipWith(productoMono, (db, req) -> {
            db.setNombre(req.getNombre());
            db.setPrecio(req.getPrecio());
            db.setCategoria(req.getCategoria());
            return db;
        })
                .flatMap(p -> ServerResponse
                        .created(URI.create("/api/v2/producctos/".concat(p.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(productoServiceObj.save(p), Producto.class))
                .switchIfEmpty(ServerResponse.notFound().build());
    }


    // eliminar un producto
    public Mono<ServerResponse> eliminar(ServerRequest request) {
        String id = request.pathVariable("id");

        Mono<Producto> productoDB = productoServiceObj.findById(id);

        return productoDB
                .flatMap(p -> productoServiceObj.delete(p)
                        .then(ServerResponse.noContent().build()))
                .switchIfEmpty(ServerResponse.notFound().build());
    }


    // cargar una imagen, enviando el id
    public Mono<ServerResponse> upload(ServerRequest request) {
        String id = request.pathVariable("id");
        return request
                .multipartData()
                .map(multipart -> multipart.toSingleValueMap().get("file"))
                .cast(FilePart.class)
                .flatMap(file -> productoServiceObj.findById(id)
                        .flatMap(p -> {
                            p.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
                                    .replace(" ", "")
                                    .replace(":", "")
                                    .replace("\\", ""));
                            return file.transferTo(new File(path + p.getFoto()))
                                    .then(productoServiceObj.save(p));
                        }))
                .flatMap(p -> ServerResponse.created(URI.create("/api/v2/productos/".concat(p.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(fromObject(p)))
                .switchIfEmpty(ServerResponse.notFound().build());
    }


    // se crea un producto y se envia la imagen
    public Mono<ServerResponse> crearConFoto(ServerRequest request) {
        Mono<Producto> productoMono = request.multipartData()
                .map(multipart -> {
                    FormFieldPart nombre = (FormFieldPart) multipart.toSingleValueMap().get("nombre");
                    FormFieldPart precio = (FormFieldPart) multipart.toSingleValueMap().get("precio");
                    FormFieldPart categoriaId = (FormFieldPart) multipart.toSingleValueMap().get("categoria.id");
                    FormFieldPart categoriaNombre = (FormFieldPart) multipart.toSingleValueMap().get("categoria.nombre");

                    Categoria categoria = new Categoria(categoriaNombre.value());
                    categoria.setId(categoriaId.value());
                    return new Producto(nombre.value(), Double.parseDouble(precio.value()), categoria);
                });

        return request
                .multipartData()
                .map(multipart -> multipart.toSingleValueMap().get("file"))
                .cast(FilePart.class)
                .flatMap(file -> productoMono
                        .flatMap(p -> {
                            p.setFoto(UUID.randomUUID().toString() + "-" + file.filename()
                                    .replace(" ", "")
                                    .replace(":", "")
                                    .replace("\\", ""));
                            p.setCreateAt(new Date());
                            return file.transferTo(new File(path + p.getFoto()))
                                    .then(productoServiceObj.save(p));
                        }))
                .flatMap(p -> ServerResponse.created(URI.create("/api/v2/productos/".concat(p.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(fromObject(p)));
    }


}
