package com.example.test.domain.repository

interface IpfsRepository {

    suspend fun fetchCid(cid: String): String

    suspend fun ping(): Long
}

