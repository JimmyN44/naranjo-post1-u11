package com.tienda.pedidos.service;

import com.tienda.pedidos.dto.PedidoRequestDTO;
import com.tienda.pedidos.dto.PedidoResponseDTO;

// Interfaz - abstraccion que cumple DIP: el controller depende de ella, no de la implementacion
public interface PedidoService {

    PedidoResponseDTO crear(PedidoRequestDTO dto);

    PedidoResponseDTO buscarPorId(Long id);
}
