package com.example.test.data.ipfs

interface IpfsClient {

    suspend fun fetchCid(cid: String): String

    suspend fun ping(): Long


}

