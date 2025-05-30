package dev.kviklet.kviklet.controller

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import dev.kviklet.kviklet.db.ConnectionType
import dev.kviklet.kviklet.service.ConnectionService
import dev.kviklet.kviklet.service.TestConnectionResult
import dev.kviklet.kviklet.service.dto.AuthenticationDetails
import dev.kviklet.kviklet.service.dto.AuthenticationType
import dev.kviklet.kviklet.service.dto.Connection
import dev.kviklet.kviklet.service.dto.ConnectionId
import dev.kviklet.kviklet.service.dto.DatabaseProtocol
import dev.kviklet.kviklet.service.dto.DatasourceConnection
import dev.kviklet.kviklet.service.dto.DatasourceType
import dev.kviklet.kviklet.service.dto.KubernetesConnection
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "connectionType")
@JsonSubTypes(
    JsonSubTypes.Type(value = CreateDatasourceConnectionRequest::class, name = "DATASOURCE"),
    JsonSubTypes.Type(value = CreateKubernetesConnectionRequest::class, name = "KUBERNETES"),
)
sealed class ConnectionRequest

data class CreateKubernetesConnectionRequest(
    @Schema(example = "k8s-conn1")
    @field:Size(max = 255, message = "Maximum length 255")
    @field:Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Only alphanumeric and dashes (-) allowed")
    val id: String,

    @Schema(example = "My Kubernetes Connection")
    @field:Size(max = 255, message = "Maximum length 255")
    val displayName: String,

    val description: String = "",

    val reviewConfig: ReviewConfigRequest,

    val maxExecutions: Int? = null,
) : ConnectionRequest()

data class CreateDatasourceConnectionRequest(
    @Schema(example = "postgres-read-only")
    @field:Size(max = 255, message = "Maximum length 255")
    @field:Pattern(regexp = "^[a-zA-Z0-9-]+$", message = "Only alphanumeric and dashes (-) allowed")
    val id: String,

    @Schema(example = "My Postgres Db User")
    @field:Size(max = 255, message = "Maximum length 255")
    val displayName: String,

    @Schema(example = "postgres")
    val databaseName: String? = null,

    val maxExecutions: Int? = null,

    @Schema(example = "root")
    @field:Size(min = 0, max = 255, message = "Maximum length 255")
    val username: String,

    val password: String? = null,

    val description: String = "",

    val reviewConfig: ReviewConfigRequest,

    val type: DatasourceType,

    val protocol: DatabaseProtocol? = null,

    val hostname: String,
    val port: Int,
    val additionalJDBCOptions: String = "",
    val dumpsEnabled: Boolean = false,
    val authenticationType: AuthenticationType = AuthenticationType.USER_PASSWORD,
    val temporaryAccessEnabled: Boolean = true,
    val explainEnabled: Boolean = false,
    val roleArn: String? = null,
) : ConnectionRequest()

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "connectionType")
@JsonSubTypes(
    JsonSubTypes.Type(value = UpdateDatasourceConnectionRequest::class, name = "DATASOURCE"),
    JsonSubTypes.Type(value = UpdateKubernetesConnectionRequest::class, name = "KUBERNETES"),
)
sealed class UpdateConnectionRequest

data class UpdateDatasourceConnectionRequest(
    @Schema(example = "My Postgres Db User")
    @field:Size(max = 255, message = "Maximum length 255")
    val displayName: String? = null,

    val description: String? = null,

    val type: DatasourceType? = null,

    val protocol: DatabaseProtocol? = null,

    val maxExecutions: Int? = null,

    val hostname: String? = null,

    val port: Int? = null,

    @Schema(example = "postgres")
    val databaseName: String? = null,

    @Schema(example = "root")
    @field:Size(min = 0, max = 255, message = "Maximum length 255")
    val username: String? = null,

    val roleArn: String? = null,

    @Schema(example = "password")
    @field:Size(min = 0, max = 255, message = "Maximum length 255")
    val password: String? = null,

    val reviewConfig: ReviewConfigRequest? = null,

    val additionalJDBCOptions: String? = null,

    val authenticationType: AuthenticationType? = null,

    val dumpsEnabled: Boolean? = null,

    val temporaryAccessEnabled: Boolean? = null,

    val explainEnabled: Boolean? = null,
) : UpdateConnectionRequest()

data class UpdateKubernetesConnectionRequest(
    @Schema(example = "My Kubernetes Connection")
    @field:Size(max = 255, message = "Maximum length 255")
    val displayName: String? = null,

    val description: String? = null,

    val reviewConfig: ReviewConfigRequest? = null,

    val maxExecutions: Int? = null,
) : UpdateConnectionRequest()

data class ReviewConfigRequest(val numTotalRequired: Int = 0)

data class ReviewConfigResponse(val numTotalRequired: Int = 0)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "connectionType")
@JsonSubTypes(
    JsonSubTypes.Type(value = UpdateDatasourceConnectionRequest::class, name = "DATASOURCE"),
    JsonSubTypes.Type(value = UpdateKubernetesConnectionRequest::class, name = "KUBERNETES"),
)
sealed class ConnectionResponse(val connectionType: ConnectionType) {
    companion object {
        fun fromDto(connection: Connection): ConnectionResponse = when (connection) {
            is DatasourceConnection -> DatasourceConnectionResponse.fromDto(connection)
            is KubernetesConnection -> KubernetesConnectionResponse.fromDto(connection)
        }
    }
}

data class DatasourceConnectionResponse(
    val id: ConnectionId,
    val authenticationType: AuthenticationType,
    val type: DatasourceType,
    val protocol: DatabaseProtocol,
    val maxExecutions: Int?,
    val displayName: String,
    val databaseName: String?,
    val username: String,
    val hostname: String,
    val port: Int,
    val description: String,
    val reviewConfig: ReviewConfigResponse,
    val additionalJDBCOptions: String,
    val dumpsEnabled: Boolean,
    val temporaryAccessEnabled: Boolean,
    val explainEnabled: Boolean,
    val roleArn: String?,
) : ConnectionResponse(ConnectionType.DATASOURCE) {
    companion object {
        fun fromDto(datasourceConnection: DatasourceConnection) = DatasourceConnectionResponse(
            id = datasourceConnection.id,
            authenticationType = datasourceConnection.authenticationType,
            displayName = datasourceConnection.displayName,
            type = datasourceConnection.type,
            protocol = datasourceConnection.protocol,
            databaseName = datasourceConnection.databaseName,
            maxExecutions = datasourceConnection.maxExecutions,
            username = datasourceConnection.auth.username,
            hostname = datasourceConnection.hostname,
            port = datasourceConnection.port,
            description = datasourceConnection.description,
            reviewConfig = ReviewConfigResponse(
                datasourceConnection.reviewConfig.numTotalRequired,
            ),
            additionalJDBCOptions = datasourceConnection.additionalOptions,
            dumpsEnabled = datasourceConnection.dumpsEnabled,
            temporaryAccessEnabled = datasourceConnection.temporaryAccessEnabled,
            explainEnabled = datasourceConnection.explainEnabled,
            roleArn = when (datasourceConnection.auth) {
                is AuthenticationDetails.AwsIam -> datasourceConnection.auth.roleArn
                else -> null
            },
        )
    }
}

data class KubernetesConnectionResponse(
    val id: ConnectionId,
    val displayName: String,
    val description: String,
    val reviewConfig: ReviewConfigResponse,
    val maxExecutions: Int?,
) : ConnectionResponse(connectionType = ConnectionType.KUBERNETES) {
    companion object {
        fun fromDto(kubernetesConnection: KubernetesConnection) = KubernetesConnectionResponse(
            id = kubernetesConnection.id,
            displayName = kubernetesConnection.displayName,
            description = kubernetesConnection.description,
            reviewConfig = ReviewConfigResponse(
                kubernetesConnection.reviewConfig.numTotalRequired,
            ),
            maxExecutions = kubernetesConnection.maxExecutions,
        )
    }
}

data class TestConnectionResponse(val success: Boolean, val details: String, val accessibleDatabases: List<String>)

@RestController()
@Validated
@RequestMapping("/connections")
@Tag(
    name = "Datasource Connections",
)
class ConnectionController(val connectionService: ConnectionService) {

    @GetMapping("/{connectionId}")
    fun getConnection(@PathVariable connectionId: String): ConnectionResponse {
        val connection = connectionService.getDatasourceConnection(
            connectionId = ConnectionId(connectionId),
        )
        return ConnectionResponse.fromDto(connection)
    }

    @GetMapping("/")
    fun getDatasourceConnections(): List<ConnectionResponse> {
        val datasourceConnections = connectionService.listConnections()
        return datasourceConnections.map { ConnectionResponse.fromDto(it) }
    }

    private fun createDatasourceConnection(request: CreateDatasourceConnectionRequest): Connection =
        connectionService.createDatasourceConnection(
            connectionId = ConnectionId(request.id),
            displayName = request.displayName,
            databaseName = request.databaseName,
            username = request.username,
            password = request.password,
            authenticationType = request.authenticationType,
            description = request.description,
            reviewsRequired = request.reviewConfig.numTotalRequired,
            port = request.port,
            hostname = request.hostname,
            type = request.type,
            protocol = request.protocol ?: request.type.toProtocol(),
            additionalJDBCOptions = request.additionalJDBCOptions,
            maxExecutions = request.maxExecutions,
            dumpsEnabled = request.dumpsEnabled,
            temporaryAccessEnabled = request.temporaryAccessEnabled,
            explainEnabled = request.explainEnabled,
            roleArn = request.roleArn,
        )

    private fun testDatabaseConnection(request: CreateDatasourceConnectionRequest): TestConnectionResult =
        connectionService.testDatabaseConnection(
            connectionId = ConnectionId(request.id),
            displayName = request.displayName,
            databaseName = request.databaseName,
            username = request.username,
            password = request.password,
            description = request.description,
            reviewsRequired = request.reviewConfig.numTotalRequired,
            port = request.port,
            hostname = request.hostname,
            type = request.type,
            protocol = request.protocol ?: request.type.toProtocol(),
            additionalJDBCOptions = request.additionalJDBCOptions,
            maxExecutions = request.maxExecutions,
            dumpsEnabled = request.dumpsEnabled,
            authenticationType = request.authenticationType,
            temporaryAccessEnabled = request.temporaryAccessEnabled,
            explainEnabled = request.explainEnabled,
            roleArn = request.roleArn,
        )

    private fun createKubernetesConnection(request: CreateKubernetesConnectionRequest): Connection =
        connectionService.createKubernetesConnection(
            connectionId = ConnectionId(request.id),
            displayName = request.displayName,
            description = request.description,
            reviewsRequired = request.reviewConfig.numTotalRequired,
            maxExecutions = request.maxExecutions,
        )

    @PostMapping("/")
    fun createConnection(
        @Valid @RequestBody
        datasourceConnection: ConnectionRequest,
    ): ConnectionResponse {
        val connection = when (datasourceConnection) {
            is CreateDatasourceConnectionRequest -> createDatasourceConnection(datasourceConnection)
            is CreateKubernetesConnectionRequest -> createKubernetesConnection(datasourceConnection)
        }

        return ConnectionResponse.fromDto(connection)
    }

    @PostMapping("/test")
    fun testConnection(
        @Valid @RequestBody
        datasourceConnection: CreateDatasourceConnectionRequest,
    ): TestConnectionResponse {
        val result = testDatabaseConnection(datasourceConnection)
        return TestConnectionResponse(
            success = result.success,
            details = result.message,
            accessibleDatabases = result.accessibleDatabases,
        )
    }

    @DeleteMapping("/{connectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteConnection(@PathVariable connectionId: String) {
        connectionService.deleteDatasourceConnection(
            connectionId = ConnectionId(connectionId),
        )
    }

    @PatchMapping("/{connectionId}")
    fun updateConnection(
        @PathVariable connectionId: String,
        @Valid @RequestBody
        datasourceConnection: UpdateConnectionRequest,
    ): ConnectionResponse {
        val connection = connectionService.updateConnection(
            connectionId = ConnectionId(connectionId),
            request = datasourceConnection,
        )
        return ConnectionResponse.fromDto(connection)
    }
}
