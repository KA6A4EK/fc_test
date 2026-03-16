package com.example.test.di

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ipfs.multiaddr.MultiAddress
import java.io.File
import java.util.Optional
import java.util.concurrent.CompletableFuture
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.peergos.BlockRequestAuthoriser
import org.peergos.EmbeddedIpfs
import org.peergos.HostBuilder
import org.peergos.blockstore.FileBlockstore
import org.peergos.config.IdentitySection
import org.peergos.protocol.dht.RamRecordStore
import org.peergos.protocol.http.HttpProtocol

/**
 * Holds [EmbeddedIpfs] and builds it asynchronously on a background thread
 * so the main thread is not blocked at app startup.
 */
@Singleton
class IpfsHolder @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _initState = MutableStateFlow<IpfsInitState>(IpfsInitState.Loading)
    val initState: StateFlow<IpfsInitState> = _initState.asStateFlow()

    @Volatile
    private var deferred: Deferred<EmbeddedIpfs>? = null

    /** Call from Application.onCreate to start init early (non-blocking). */
    fun warmUp() {
        scope.launch { get() }
    }

    suspend fun get(): EmbeddedIpfs = getAsync().await()

    private fun getAsync(): Deferred<EmbeddedIpfs> =
        synchronized(this) {
            deferred ?: scope.async {
                runCatching { buildEmbeddedIpfs() }
                    .onSuccess { _initState.value = IpfsInitState.Ready }
                    .onFailure { _initState.value = IpfsInitState.Error(it) }
                    .getOrThrow()
            }.also { deferred = it }
        }

    private fun buildEmbeddedIpfs(): EmbeddedIpfs {
        val swarmAddresses = listOf(MultiAddress("/ip4/0.0.0.0/tcp/0"))
        val bootstrapAddresses = listOf(MultiAddress(IpfsConfig.IPFS_MULTIADDRESS))

        val builder = HostBuilder().generateIdentity()
        val identity = IdentitySection(builder.privateKey.bytes(), builder.peerId)

        val authoriser: BlockRequestAuthoriser = BlockRequestAuthoriser { _, _, _ ->
            CompletableFuture.completedFuture(true)
        }

        val httpProxyTarget: Optional<HttpProtocol.HttpRequestProcessor> = Optional.empty()

        val storeDir = File(context.filesDir, "ipfs").apply {
            if (!exists()) {
                mkdirs()
            }
        }
        val storePath = storeDir.toPath()
        val recordStore = RamRecordStore()
        val blockstore = FileBlockstore(storePath)

        return EmbeddedIpfs.build(
            recordStore,
            blockstore,
            true,
            swarmAddresses,
            bootstrapAddresses,
            identity,
            authoriser,
            httpProxyTarget,
        ).also { it.start() }
    }
}

sealed interface IpfsInitState {
    data object Loading : IpfsInitState
    data object Ready : IpfsInitState
    data class Error(val throwable: Throwable) : IpfsInitState
}
