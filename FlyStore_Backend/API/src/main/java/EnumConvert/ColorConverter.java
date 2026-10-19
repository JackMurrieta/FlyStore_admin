package EnumConvert;

import Entity.Color;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convertidor JPA para Color
 * Convierte entre el enum Java y el valor String en la BD
 * @author Usuario
 */
@Converter(autoApply = false)
public class ColorConverter implements AttributeConverter<Color, String> {

    @Override
    public String convertToDatabaseColumn(Color color) {
        if (color == null) {
            return null;
        }
        return color.getValor();
    }

    @Override
    public Color convertToEntityAttribute(String valor) {
        if (valor == null) {
            return null;
        }
        return Color.fromValor(valor);
    }
}
