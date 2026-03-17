package com.example.test.di

import android.content.Context
import com.example.test.data.ipfs.IpfsClient
import com.example.test.data.ipfs.NabuIpfsClient
import com.example.test.data.repository.IpfsRepositoryImpl
import com.example.test.domain.repository.IpfsRepository
import com.example.test.domain.usecase.FetchCidUseCase
import com.example.test.domain.usecase.PingNodeUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideIpfsClient(@ApplicationContext context: Context): IpfsClient {
        return NabuIpfsClient(
            fetchTimeout = IpfsConfig.fetchTimeout,
            pingTimeout = IpfsConfig.pingTimeout,
            pingTestCid = IpfsConfig.PING_TEST_CID,
            context = context
        )
    }

    @Provides
    @Singleton
    fun provideIpfsRepository(client: IpfsClient): IpfsRepository {
        return IpfsRepositoryImpl(client)
    }

    @Provides
    @Singleton
    fun provideFetchCidUseCase(repository: IpfsRepository): FetchCidUseCase {
        return FetchCidUseCase(repository)
    }

    @Provides
    @Singleton
    fun providePingNodeUseCase(repository: IpfsRepository): PingNodeUseCase {
        return PingNodeUseCase(repository)
    }
}
