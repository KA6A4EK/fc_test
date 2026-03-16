package com.example.test.data.repository

import com.example.test.data.ipfs.IpfsClient
import com.example.test.domain.repository.IpfsRepository

class IpfsRepositoryImpl(
    private val client: IpfsClient,
) : IpfsRepository {

    override suspend fun fetchCid(cid: String): String {
        return try {
            client.fetchCid(cid)
        } catch (e: Exception) {
            if (e is IllegalArgumentException) throw e
            throw RuntimeException("Failed to fetch CID", e)
        }
    }

    override suspend fun ping(): Long {
        return try {
            client.ping()
        } catch (e: Exception) {
            throw RuntimeException("Failed to ping node", e)
        }
    }
}
