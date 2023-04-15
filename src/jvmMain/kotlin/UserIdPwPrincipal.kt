package ru.mipt.npm.nica.ems

data class UserIdPwPrincipal(val name: String, val pw: String) : io.ktor.server.auth.Principal {
}
