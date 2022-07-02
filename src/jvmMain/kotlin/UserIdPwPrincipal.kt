package ru.mipt.npm.nica.emd

data class UserIdPwPrincipal(val name: String, val pw: String) : io.ktor.server.auth.Principal {
}
