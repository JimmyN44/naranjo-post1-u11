package com.tienda.pedidos.strategy;

import org.springframework.stereotype.Component;

// Cliente REGULAR: sin descuento (0%)
@Component("REGULAR")
public class DescuentoRegular implements EstrategiaDescuento {

    @Override
    public double calcularDescuento(double subtotal) {
        return 0.0;
    }
}
