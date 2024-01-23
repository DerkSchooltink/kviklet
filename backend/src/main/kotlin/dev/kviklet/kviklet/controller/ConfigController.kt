package dev.kviklet.kviklet.controller

import dev.kviklet.kviklet.security.IdentityProviderProperties
import dev.kviklet.kviklet.service.LicenseService
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.time.LocalDateTime

data class CreateConfigRequest(
    val licenseKey: String,
)

enum class OAuthProvider() {
    GOOGLE,
    KEYCLOAK,
    ;

    companion object {
        fun fromString(string: String): OAuthProvider? {
            return when (string) {
                "google" -> GOOGLE
                "keycloak" -> KEYCLOAK
                else -> null
            }
        }
    }
}

data class ConfigResponse(
    val licenseValid: Boolean,
    val validUntil: LocalDate?,
    val createdAt: LocalDateTime?,
    val allowedUsers: UInt?,
    val oAuthProvider: OAuthProvider?,
)

@RestController
@Validated
@RequestMapping("/config")
@Tag(
    name = "Controller",
    description = "Configure Kviklet as a whole",
)
class ConfigController(
    val licenseService: LicenseService,
    val IdentityProviderProperties: IdentityProviderProperties,
) {

    @GetMapping("/")
    fun getConfig(): ConfigResponse {
        val licenses = licenseService.getLicenses()
        val licensesSorted = licenses.sortedByDescending { it.validUntil }

        return ConfigResponse(
            licenseValid = licenses.any { it.isValid() },
            validUntil = licensesSorted.firstOrNull()?.validUntil,
            createdAt = licensesSorted.firstOrNull()?.createdAt,
            allowedUsers = licensesSorted.firstOrNull()?.allowedUsers,
            oAuthProvider = IdentityProviderProperties.type?.let { OAuthProvider.fromString(it) },
        )
    }

    @PostMapping("/license/")
    fun uploadLicense(@RequestParam("file") file: MultipartFile): ResponseEntity<Any> {
        // Call service to handle file
        licenseService.processLicenseFile(file)
        return ResponseEntity.ok("License file uploaded successfully")
    }
}
