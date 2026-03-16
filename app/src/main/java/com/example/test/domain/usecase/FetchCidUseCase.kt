package com.example.test.domain.usecase

import com.example.test.domain.repository.IpfsRepository

class FetchCidUseCase(
    private val repository: IpfsRepository,
) {

    suspend operator fun invoke(cid: String): String {
        return repository.fetchCid(cid)
    }
}

