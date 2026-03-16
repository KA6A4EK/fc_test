package com.example.test.domain.usecase

import com.example.test.domain.repository.IpfsRepository

class PingNodeUseCase(
    private val repository: IpfsRepository,
) {

    suspend operator fun invoke(): Long {
        return repository.ping()
    }
}

