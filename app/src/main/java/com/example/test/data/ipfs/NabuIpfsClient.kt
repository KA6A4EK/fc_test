package com.example.test.data.ipfs

import android.content.Context
import com.example.test.di.IpfsConfig
import com.example.test.di.IpfsHolder
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ipfs.cid.Cid
import io.libp2p.core.ConnectionClosedException
import io.libp2p.core.multiformats.Multiaddr
import io.libp2p.protocol.PingController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.peergos.Want
import kotlin.time.Duration

/**
 * IPFS client implementation using Peergos Nabu (EmbeddedIpfs).
 * Uses [IpfsHolder] for lazy async init; only [fetchCid] and [ping] are required by the app.
 */
class NabuIpfsClient(
    private val fetchTimeout: Duration = IpfsConfig.fetchTimeout,
    private val pingTimeout: Duration = IpfsConfig.pingTimeout,
    private val pingTestCid: String = IpfsConfig.PING_TEST_CID,
    @ApplicationContext private val context: Context,
    ) : IpfsClient {

    private var ipfsHolder : IpfsHolder? = null
    override suspend fun fetchCid(): String = withContext(Dispatchers.IO) {
        val ipfs = getIpfsHolder().get()
        withTimeout(fetchTimeout.inWholeMilliseconds) {
            val decoded = try {
                Cid.decode(pingTestCid)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid CID format", e)
            }
            val wants = listOf(Want(decoded))
            val blocks = ipfs.getBlocks(wants, emptySet(), true)

            val data = blocks.firstOrNull()?.block
                ?: throw IllegalStateException("Block not found for CID")

            data.decodeToString().replace(Regex("[^\\p{L}\\p{N}\\p{P}\\p{Z}]"), "")


        }
    }

    private var pingController: PingController? = null

    override suspend fun ping(): Long = withContext(Dispatchers.IO) {
        withTimeout(pingTimeout) {

            if (pingController == null) {
                val ipfs = getIpfsHolder().get()
                val host = ipfs.node

                val peerMultiaddr = Multiaddr(IpfsConfig.IPFS_MULTIADDRESS)
                val peerId = requireNotNull(peerMultiaddr.getPeerId()) {
                    "PeerId not found in multiaddr"
                }

                pingController = host
                    .newStream<PingController>(
                        listOf("/ipfs/ping/1.0.0"),
                        peerId,
                        peerMultiaddr
                    )
                    .controller
                    .await()


            }
            try {
            pingController?.ping()?.await()?: 0
            }
            catch (e : ConnectionClosedException){
                e
                pingController=null
                ipfsHolder = null
                Long.MAX_VALUE
            }
            catch (e : Exception){
                e
                pingController=null
                ipfsHolder = null
                Long.MAX_VALUE
            }

        }
    }

    private fun getIpfsHolder() : IpfsHolder  {
        if (ipfsHolder==null){
            ipfsHolder = IpfsHolder(context)
            ipfsHolder?.warmUp()
        }
        return ipfsHolder!!
    }
}