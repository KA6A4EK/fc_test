package com.example.test.data.ipfs

import com.example.test.di.IpfsConfig
import com.example.test.di.IpfsHolder
import io.ipfs.cid.Cid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.peergos.Want
import kotlin.time.Duration
import kotlin.time.measureTime

/**
 * IPFS client implementation using Peergos Nabu (EmbeddedIpfs).
 * Uses [IpfsHolder] for lazy async init; only [fetchCid] and [ping] are required by the app.
 */
class NabuIpfsClient(
    private val ipfsHolder: IpfsHolder,
    private val fetchTimeout: Duration = IpfsConfig.fetchTimeout,
    private val pingTimeout: Duration = IpfsConfig.pingTimeout,
    private val pingTestCid: String = IpfsConfig.PING_TEST_CID,
) : IpfsClient {

    override suspend fun fetchCid(cid: String): String = withContext(Dispatchers.IO) {
        val ipfs = ipfsHolder.get()
        withTimeout(fetchTimeout.inWholeMilliseconds) {
            val decoded = try {
                Cid.decode(cid)
            } catch (e: Exception) {
                throw IllegalArgumentException("Invalid CID format", e)
            }
            val wants = listOf(Want(decoded))
            val blocks = ipfs.getBlocks(wants, emptySet(), true)
            val data = blocks.firstOrNull()?.block
                ?: throw IllegalStateException("Block not found for CID")
            data.toString(Charsets.UTF_8)
        }
    }

    override suspend fun ping(): Long = withContext(Dispatchers.IO) {
        val ipfs = ipfsHolder.get()
        val testCid = Cid.decode(pingTestCid)
        val elapsed = measureTime {
            withTimeout(pingTimeout.inWholeMilliseconds) {
                val wants = listOf(Want(testCid))
                ipfs.getBlocks(wants, emptySet(), true)
            }
        }
        elapsed.inWholeMilliseconds
    }
}
