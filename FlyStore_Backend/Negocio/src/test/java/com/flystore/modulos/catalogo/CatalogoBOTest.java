package com.flystore.modulos.catalogo;

import com.flystore.modulos.catalogo.aplicacion.CatalogoBO;
import com.flystore.modulos.catalogo.aplicacion.mappers.CatalogoMapper;
import com.flystore.persistencia.entidades.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CatalogoBOTest {
    @Test void filtrosYTablasDeTallasRespetanCategoriaYContratoString() {
        var categoria = new Categoria(); categoria.setId(1L); categoria.setNombre("Gorras");
        var sub = new Subcategoria(); sub.setId(2L); sub.setNombre("Planos");
        var marca = new Marca(); marca.setId(3L); marca.setNombre("Fly");
        var bo = new CatalogoBO(() -> List.of(categoria), () -> List.of(sub), () -> List.of(marca), id -> {
            assertEquals(1L, id); return List.of();
        }, new CatalogoMapper());
        assertEquals("1", bo.obtenerFiltrosDisponibles().categorias().getFirst().id());
        assertEquals("2", bo.obtenerFiltrosDisponibles().subcategorias().getFirst().id());
        assertEquals("3", bo.obtenerFiltrosDisponibles().marcas().getFirst().id());
        assertEquals(List.of(), bo.obtenerTallasPorCategoria("1"));
    }
}
