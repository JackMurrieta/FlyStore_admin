package Entity.converter;

import Entity.EstadoVenta;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EstadoVentaConverter implements AttributeConverter<EstadoVenta, String> {

    @Override
    public String convertToDatabaseColumn(EstadoVenta estadoVenta) {
        if (estadoVenta == null) {
            return null;
        }
        return estadoVenta.getValor();
    }

    @Override
    public EstadoVenta convertToEntityAttribute(String valor) {
        if (valor == null) {
            return null;
        }
        return EstadoVenta.fromValor(valor);
    }
}
