package com.tienda.pedidos.strategy;

/**
 * Strategy: cada tipo de cliente tiene su propia regla de descuento.
 * Agregar un nuevo tipo (por ejemplo CORPORATIVO) solo requiere una nueva clase
 * anotada con @Component("NOMBRE"), sin modificar PedidoServiceImpl (OCP).
 */
public interface EstrategiaDescuento {

    double calcularDescuento(double subtotal);
}
