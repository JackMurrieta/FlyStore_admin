package Entity.converter;

import Entity.TipoCliente;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TipoClienteConverter implements AttributeConverter<TipoCliente, String> {

    @Override
    public String convertToDatabaseColumn(TipoCliente tipoCliente) {
        if (tipoCliente == null) {
            return null;
        }
        return tipoCliente.getValor();
    }

    @Override
    public TipoCliente convertToEntityAttribute(String valor) {
        if (valor == null) {
            return null;
        }
        return TipoCliente.fromValor(valor);
    }
}
