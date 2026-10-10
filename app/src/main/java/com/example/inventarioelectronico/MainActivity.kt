
package com.example.inventarioelectronico

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.inventarioelectronico.data.MemoryProductoRepository
import com.example.inventarioelectronico.ui.theme.InventarioElectronicoTheme
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

data class Producto(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val cantidad: Int,
    val precio: Double,
    val proveedor: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            InventarioElectronicoTheme {
                AplicacionInventario()
            }
        }
    }
}

@Composable
fun AplicacionInventario() {
    // Repositorio temporal que después se podrá sustituir
    // por la implementación de Room y Firebase.
    val repositorio = remember { MemoryProductoRepository() }
    val productos = remember { mutableStateListOf<Producto>() }
    val scope = rememberCoroutineScope()

    var pantalla by rememberSaveable { mutableStateOf("inicio") }
    var productoEditandoId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(repositorio) {
        productos.clear()
        productos.addAll(repositorio.obtenerProductos())
    }

    when (pantalla) {
        "inicio" -> PantallaPrincipal(
            onVerInventario = {
                pantalla = "inventario"
            },
            onRegistrar = {
                productoEditandoId = null
                pantalla = "registro"
            }
        )

        "inventario" -> PantallaInventario(
            productos = productos,
            onVolver = {
                pantalla = "inicio"
            },
            onEditar = { producto ->
                productoEditandoId = producto.id
                pantalla = "editar"
            },
            onEliminar = { producto ->
                scope.launch {
                    repositorio.eliminarProducto(producto.id)
                    productos.removeAll {
                        it.id == producto.id
                    }
                }
            }
        )

        "registro" -> PantallaFormulario(
            titulo = "Registrar producto",
            productoInicial = null,
            onVolver = {
                pantalla = "inicio"
            },
            onGuardar = { producto ->
                val nuevoProducto = producto.copy(
                    id = UUID.randomUUID().toString()
                )

                scope.launch {
                    repositorio.registrarProducto(nuevoProducto)
                    productos.add(nuevoProducto)
                    pantalla = "inventario"
                }
            }
        )

        "editar" -> {
            val producto = productos.find {
                it.id == productoEditandoId
            }

            if (producto != null) {
                PantallaFormulario(
                    titulo = "Editar producto",
                    productoInicial = producto,
                    onVolver = {
                        pantalla = "inventario"
                    },
                    onGuardar = { actualizado ->
                        scope.launch {
                            repositorio.actualizarProducto(actualizado)

                            val indice = productos.indexOfFirst {
                                it.id == actualizado.id
                            }

                            if (indice >= 0) {
                                productos[indice] = actualizado
                            }

                            pantalla = "inventario"
                        }
                    }
                )
            } else {
                pantalla = "inventario"
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal(
    onVerInventario: () -> Unit,
    onRegistrar: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("Inventario Electrónico")
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Gestión de inventario",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                "Administra los productos electrónicos de tu empresa."
            )

            Spacer(modifier = Modifier.height(28.dp))

            Button(onClick = onVerInventario) {
                Text("Ver inventario")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = onRegistrar) {
                Text("Registrar producto")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInventario(
    productos: List<Producto>,
    onVolver: () -> Unit,
    onEditar: (Producto) -> Unit,
    onEliminar: (Producto) -> Unit
) {
    var textoBusqueda by rememberSaveable {
        mutableStateOf("")
    }

    var productoPendienteEliminar by remember {
        mutableStateOf<Producto?>(null)
    }

    val filtrados = productos.filter {
        it.nombre.contains(
            textoBusqueda.trim(),
            ignoreCase = true
        )
    }

    if (productoPendienteEliminar != null) {
        val producto = productoPendienteEliminar!!

        AlertDialog(
            onDismissRequest = {
                productoPendienteEliminar = null
            },
            title = {
                Text("Confirmar eliminación")
            },
            text = {
                Text(
                    "¿Deseas eliminar el producto " +
                            "\"${producto.nombre}\" del inventario?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onEliminar(producto)
                        productoPendienteEliminar = null
                    }
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        productoPendienteEliminar = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("Inventario de productos")
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "Productos registrados: ${productos.size}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = textoBusqueda,
                onValueChange = {
                    textoBusqueda = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Buscar producto")
                },
                placeholder = {
                    Text("Ej.: mouse, notebook...")
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Resultados: ${filtrados.size}")

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filtrados,
                    key = { it.id }
                ) { producto ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = producto.nombre,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(
                                modifier = Modifier.height(6.dp)
                            )

                            Text("Descripción: ${producto.descripcion}")
                            Text("Cantidad: ${producto.cantidad}")

                            Text(
                                "Precio: $" + String.format(
                                    Locale("es", "CL"),
                                    "%,.0f",
                                    producto.precio
                                )
                            )

                            Text("Proveedor: ${producto.proveedor}")

                            Spacer(
                                modifier = Modifier.height(10.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onEditar(producto)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Editar")
                                }

                                OutlinedButton(
                                    onClick = {
                                        productoPendienteEliminar = producto
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Eliminar")
                                }
                            }
                        }
                    }
                }

                if (filtrados.isEmpty()) {
                    item {
                        Text("No se encontraron productos.")
                    }
                }
            }

            Button(
                onClick = onVolver,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Volver al inicio")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaFormulario(
    titulo: String,
    productoInicial: Producto?,
    onVolver: () -> Unit,
    onGuardar: (Producto) -> Unit
) {
    var nombre by rememberSaveable(productoInicial?.id) {
        mutableStateOf(productoInicial?.nombre ?: "")
    }

    var descripcion by rememberSaveable(productoInicial?.id) {
        mutableStateOf(productoInicial?.descripcion ?: "")
    }

    var cantidad by rememberSaveable(productoInicial?.id) {
        mutableStateOf(
            productoInicial?.cantidad?.toString() ?: ""
        )
    }

    var precio by rememberSaveable(productoInicial?.id) {
        mutableStateOf(
            productoInicial?.precio?.toString() ?: ""
        )
    }

    var proveedor by rememberSaveable(productoInicial?.id) {
        mutableStateOf(productoInicial?.proveedor ?: "")
    }

    var error by rememberSaveable(productoInicial?.id) {
        mutableStateOf("")
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(titulo)
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Datos del producto",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = nombre,
                onValueChange = {
                    nombre = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Nombre")
                },
                singleLine = true
            )

            OutlinedTextField(
                value = descripcion,
                onValueChange = {
                    descripcion = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Descripción")
                }
            )

            OutlinedTextField(
                value = cantidad,
                onValueChange = {
                    cantidad = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Cantidad")
                },
                singleLine = true
            )

            OutlinedTextField(
                value = precio,
                onValueChange = {
                    precio = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Precio")
                },
                singleLine = true
            )

            OutlinedTextField(
                value = proveedor,
                onValueChange = {
                    proveedor = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Proveedor")
                },
                singleLine = true
            )

            if (error.isNotBlank()) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    val cantidadNumero = cantidad.toIntOrNull()

                    val precioNumero = precio.trim()
                        .replace(",", ".")
                        .toDoubleOrNull()

                    error = when {
                        nombre.isBlank() ->
                            "Ingresa el nombre del producto."

                        descripcion.isBlank() ->
                            "Ingresa la descripción."

                        cantidadNumero == null || cantidadNumero < 0 ->
                            "La cantidad debe ser un entero igual o mayor que cero."

                        precioNumero == null ||
                                !precioNumero.isFinite() ||
                                precioNumero < 0 ->
                            "Ingresa un precio válido igual o mayor que cero."

                        proveedor.isBlank() ->
                            "Ingresa el proveedor."

                        else -> ""
                    }

                    if (error.isEmpty()) {
                        onGuardar(
                            Producto(
                                id = productoInicial?.id ?: "",
                                nombre = nombre.trim(),
                                descripcion = descripcion.trim(),
                                cantidad = cantidadNumero!!,
                                precio = precioNumero!!,
                                proveedor = proveedor.trim()
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (productoInicial == null) {
                        "Guardar producto"
                    } else {
                        "Guardar cambios"
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = onVolver) {
                    Text("Cancelar")
                }
            }
        }
    }
}
