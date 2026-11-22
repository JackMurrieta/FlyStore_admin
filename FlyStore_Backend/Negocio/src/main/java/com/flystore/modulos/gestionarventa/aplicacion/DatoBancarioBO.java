package com.flystore.modulos.gestionarventa.aplicacion;

import com.flystore.contratos.gestionarventa.DatosBancariosDTO;
import com.flystore.dominio.excepcion.NegocioException;
import com.flystore.dominio.excepcion.TipoError;
import com.flystore.modulos.gestionarventa.aplicacion.mappers.PedidoVentaMapper;
import com.flystore.persistencia.dao.interfaces.IDatoBancarioDAO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatoBancarioBO implements IDatoBancarioBO {

    private final IDatoBancarioDAO datoBancarioDAO;
    private final PedidoVentaMapper mapper;

    public DatoBancarioBO(IDatoBancarioDAO datoBancarioDAO, PedidoVentaMapper mapper) {
        this.datoBancarioDAO = datoBancarioDAO;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public DatosBancariosDTO obtenerDatosBancarios() {
        return datoBancarioDAO.obtenerDatoBancarioActivo()
                .map(mapper::aDatosBancariosDTO)
                .orElseThrow(() -> new NegocioException(TipoError.SERVICIO_NO_DISPONIBLE,
                        "La tienda todavía no tiene una cuenta para transferencias. Elige pago en efectivo o intenta más tarde."));
    }
}
