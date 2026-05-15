package mx.itson.potrobus.entities


data class RutaConParadas(
    val id_ruta: Int,
    val nombre: String?,
    val descripcion: String?,
    val origen: String?,
    val destino: String?,
    val paradas: List<Parada>?
)