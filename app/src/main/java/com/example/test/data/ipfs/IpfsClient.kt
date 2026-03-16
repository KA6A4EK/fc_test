package com.example.test.data.ipfs

interface IpfsClient {

    suspend fun fetchCid(): String

    suspend fun ping(): Long


}

