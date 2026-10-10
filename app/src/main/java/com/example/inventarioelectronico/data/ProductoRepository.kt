
package com.example.inventarioelectronico.data

import com.example.inventarioelectronico.Producto

interface ProductoRepository {

    suspend fun obtenerProductos(): List<Producto>

    suspend fun obtenerProducto(id: String): Producto?

    suspend fun registrarProducto(producto: Producto)

    suspend fun actualizarProducto(producto: Producto)

    suspend fun eliminarProducto(id: String)
}
