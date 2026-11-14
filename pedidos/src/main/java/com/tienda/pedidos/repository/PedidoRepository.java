package com.tienda.pedidos.repository;

import com.tienda.pedidos.entity.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * DAO de Pedido: Spring Data JPA genera la implementacion en tiempo de ejecucion.
 * Reemplaza por completo el SQL concatenado con JdbcTemplate del codigo de partida.
 */
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByClienteEmail(String clienteEmail);
}
