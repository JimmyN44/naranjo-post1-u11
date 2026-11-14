package com.tienda.pedidos.strategy;

import org.springframework.stereotype.Component;

// Cliente VIP: 15% de descuento sobre el subtotal
@Component("VIP")
public class DescuentoVip implements EstrategiaDescuento {

    @Override
    public double calcularDescuento(double subtotal) {
        return subtotal * 0.15;
    }
}
