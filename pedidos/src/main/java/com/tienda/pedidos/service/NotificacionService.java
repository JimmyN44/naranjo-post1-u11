package com.tienda.pedidos.service;

// Abstraccion de la notificacion: PedidoServiceImpl depende de ella, no de un canal concreto
public interface NotificacionService {

    void enviarConfirmacion(String clienteEmail, double total);
}
