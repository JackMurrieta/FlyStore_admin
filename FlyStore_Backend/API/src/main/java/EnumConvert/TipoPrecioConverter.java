package EnumConvert;

import Entity.TipoPrecio;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convertidor JPA para TipoPrecio
 * Convierte entre el enum Java y el valor String en la BD
 * @author Usuario
 */
@Converter(autoApply = false)
public class TipoPrecioConverter implements AttributeConverter<TipoPrecio, String> {

    @Override
    public String convertToDatabaseColumn(TipoPrecio tipoPrecio) {
        if (tipoPrecio == null) {
            return null;
        }
        return tipoPrecio.getValor();
    }

    @Override
    public TipoPrecio convertToEntityAttribute(String valor) {
        if (valor == null) {
            return null;
        }
        return TipoPrecio.fromValor(valor);
    }
}
