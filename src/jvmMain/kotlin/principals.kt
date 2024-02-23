package ru.mipt.npm.nica.ems

import io.ktor.server.auth.Principal

data class UserRoles(
    val isReader: Boolean,
    val isWriter: Boolean,
    val isAdmin: Boolean
)

interface WithRoles: Principal {
    val roles: UserRoles
}
data class UserIdPwPrincipal(val name: String, val pw: String, override val roles: UserRoles) : Principal, WithRoles

data class UserIdPwGroupsPrincipal(val name: String, val pw: String, val groups: List<String>, override val roles: UserRoles): Principal, WithRoles

data class TokenGroupsPrincipal(val token: String, val groups: List<String>, override val roles: UserRoles): Principal, WithRoles

