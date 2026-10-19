package EnumConvert;

import Entity.Talla;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convertidor JPA para Talla
 * Convierte entre el enum Java y el valor String en la BD
 * @author Usuario
 */
@Converter(autoApply = false)
public class TallaConverter implements AttributeConverter<Talla, String> {

    @Override
    public String convertToDatabaseColumn(Talla talla) {
        if (talla == null) {
            return null;
        }
        return talla.getValor();
    }

    @Override
    public Talla convertToEntityAttribute(String valor) {
        if (valor == null) {
            return null;
        }
        return Talla.fromValor(valor);
    }
}
