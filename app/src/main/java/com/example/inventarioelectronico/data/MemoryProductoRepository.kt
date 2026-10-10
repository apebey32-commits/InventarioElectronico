
package com.example.inventarioelectronico.data

import com.example.inventarioelectronico.Producto

class MemoryProductoRepository : ProductoRepository {

    private val productos = mutableListOf(
        Producto("1", "Notebook Lenovo", "Notebook para oficina", 8, 459990.0, "TecnoChile"),
        Producto("2", "Mouse inalámbrico", "Mouse USB inalámbrico", 25, 12990.0, "ElectroSur"),
        Producto("3", "Teclado mecánico", "Teclado para computador", 12, 34990.0, "TecnoChile"),
        Producto("4", "Monitor 24 pulgadas", "Monitor LED", 6, 119990.0, "Pantallas Ltda."),
        Producto("5", "Audífonos Bluetooth", "Audífonos inalámbricos", 15, 24990.0, "AudioMax")
    )

    override suspend fun obtenerProductos(): List<Producto> {
        return productos.toList()
    }

    override suspend fun obtenerProducto(id: String): Producto? {
        return productos.find { it.id == id }
    }

    override suspend fun registrarProducto(producto: Producto) {
        productos.add(producto)
    }

    override suspend fun actualizarProducto(producto: Producto) {
        val indice = productos.indexOfFirst {
            it.id == producto.id
        }

        if (indice >= 0) {
            productos[indice] = producto
        }
    }

    override suspend fun eliminarProducto(id: String) {
        productos.removeAll { it.id == id }
    }
}
