package com.flystore.modulos.catalogo.aplicacion.mappers;

import com.flystore.contratos.catalogo.*;
import com.flystore.persistencia.entidades.*;
import java.util.List;
import org.springframework.stereotype.Component;
@Component
public class CatalogoMapper {
    public FiltrosDisponiblesDTO aFiltrosDisponiblesDTO(List<Categoria> categorias, List<Subcategoria> subcategorias, List<Marca> marcas) {
        return new FiltrosDisponiblesDTO(categorias.stream().map(c -> new CatalogoItemDTO(c.getId().toString(), c.getNombre())).toList(),
                subcategorias.stream().map(s -> new CatalogoItemDTO(s.getId().toString(), s.getNombre())).toList(),
                marcas.stream().map(m -> new CatalogoItemDTO(m.getId().toString(), m.getNombre())).toList());
    }
    public TallaDTO aTallaDTO(Talla talla) { return new TallaDTO(talla.getId().toString(), talla.getEtiqueta()); }
}
