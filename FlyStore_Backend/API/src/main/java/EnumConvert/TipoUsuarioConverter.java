package EnumConvert;

import Entity.TipoUsuario;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class TipoUsuarioConverter implements AttributeConverter<TipoUsuario, String> {

    @Override
    public String convertToDatabaseColumn(TipoUsuario tipoUsuario) {
        if (tipoUsuario == null) {
            return null;
        }
        return tipoUsuario.getValor();
    }

    @Override
    public TipoUsuario convertToEntityAttribute(String valor) {
        if (valor == null) {
            return null;
        }
        return TipoUsuario.fromValor(valor);
    }
}
