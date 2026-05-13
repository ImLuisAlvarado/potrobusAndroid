package mx.itson.potrobus.entities

data class LoginRequest(val correo: String, val password: String)

data class LoginResponse(val access_token: String, val user: UserData)

data class UserData(val nombre: String, val correo: String, val rol: String)

data class RegisterRequest(
    val nombre: String,
    val apellido: String,
    val correo: String,
    val password: String,
    val rol: String = "estudiante"
)

data class RegisterResponse(val msg: String)