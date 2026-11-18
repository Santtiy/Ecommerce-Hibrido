package co.edu.proyecto.orden.dominio;

import co.edu.proyecto.orden.dominio.excepciones.DominioException;

import java.util.UUID;

/** Value Object. */
public record OrdenId(UUID valor) {

    public OrdenId {
        if (valor == null) {
            throw new DominioException("El identificador de la orden no puede ser nulo");
        }
    }

    public static OrdenId nuevo() {
        return new OrdenId(UUID.randomUUID());
    }

    public static OrdenId de(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new DominioException("El identificador de la orden no puede estar vacío");
        }
        try {
            return new OrdenId(UUID.fromString(texto.trim()));
        } catch (IllegalArgumentException e) {
            throw new DominioException("Identificador de orden con formato inválido: " + texto, e);
        }
    }
}
