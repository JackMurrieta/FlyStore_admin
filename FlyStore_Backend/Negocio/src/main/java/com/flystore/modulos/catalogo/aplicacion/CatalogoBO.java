package com.flystore.modulos.catalogo.aplicacion;

import com.flystore.contratos.catalogo.*;
import com.flystore.persistencia.dao.interfaces.*;
import com.flystore.modulos.catalogo.aplicacion.mappers.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CatalogoBO implements ICatalogoBO {

    private final ICategoriaDAO categoriaDAO;
    private final ISubcategoriaDAO subcategoriaDAO;
    private final IMarcaDAO marcaDAO;
    private final ITallaDAO tallaDAO;
    private final CatalogoMapper catalogoMapper;

    public CatalogoBO(ICategoriaDAO categoriaDAO, ISubcategoriaDAO subcategoriaDAO, IMarcaDAO marcaDAO, ITallaDAO tallaDAO, CatalogoMapper catalogoMapper) {
        this.categoriaDAO = categoriaDAO;
        this.subcategoriaDAO = subcategoriaDAO;
        this.marcaDAO = marcaDAO;
        this.tallaDAO = tallaDAO;
        this.catalogoMapper = catalogoMapper;
    }

    @Override
    public FiltrosDisponiblesDTO obtenerFiltrosDisponibles() {
        return catalogoMapper.aFiltrosDisponiblesDTO(categoriaDAO.obtenerTodas(), subcategoriaDAO.obtenerTodas(), marcaDAO.obtenerTodas());
    }

    @Override
    public List<TallaDTO> obtenerTallasPorCategoria(String idCategoria) {
        return tallaDAO.obtenerPorCategoria(ProductoMapper.id(idCategoria)).stream().map(catalogoMapper::aTallaDTO).toList();
    }
}
