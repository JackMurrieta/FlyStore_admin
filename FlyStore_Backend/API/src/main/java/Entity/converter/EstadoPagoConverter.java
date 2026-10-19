package Entity.converter;

import Entity.EstadoPago;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class EstadoPagoConverter implements AttributeConverter<EstadoPago, String> {

    @Override
    public String convertToDatabaseColumn(EstadoPago estadoPago) {
        if (estadoPago == null) {
            return null;
        }
        return estadoPago.getValor();
    }

    @Override
    public EstadoPago convertToEntityAttribute(String valor) {
        if (valor == null) {
            return null;
        }
        return EstadoPago.fromValor(valor);
    }
}
