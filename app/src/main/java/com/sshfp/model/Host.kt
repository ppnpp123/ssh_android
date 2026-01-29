package com.sshfp.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * SSH主机配置
 */
@Entity(tableName = "hosts")
data class Host(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var name: String = "",
    var address: String = "",
    var port: Int = 22,
    var username: String = "",
    var authMethod: AuthMethod = AuthMethod.PASSWORD,
    var encryptedPassword: String = "",
    var privateKeyPath: String = "",
    var encryptedPassphrase: String = "",
    var initialDirectory: String = "/home/${username}",
    var createdAt: Long = System.currentTimeMillis(),
    var lastConnectedAt: Long = 0,
    var sortOrder: Int = 0
) {
    enum class AuthMethod {
        PASSWORD,
        KEY
    }

    fun displayName(): String = name.ifEmpty { "$username@$address:$port" }
}
