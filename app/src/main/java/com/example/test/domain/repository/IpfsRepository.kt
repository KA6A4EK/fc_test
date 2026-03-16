package com.example.test.domain.repository

interface IpfsRepository {

    suspend fun fetchCid(): String

    suspend fun ping(): Long
}

