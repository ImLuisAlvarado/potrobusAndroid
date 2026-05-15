package mx.itson.potrobus.entities

class Unidad {
    constructor()
    constructor(id_unidad: Int, numero_economico: String, placa: String, modelo: String?, activo: Int) {
        this.id_unidad = id_unidad
        this.numero_economico = numero_economico
        this.placa = placa
        this.modelo = modelo
        this.activo = activo
    }

    var id_unidad: Int? = null
    var numero_economico: String? = null
    var placa: String? = null
    var modelo: String? = null
    var activo: Int? = null
}