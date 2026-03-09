package com.nextgen.player.network.di

import com.nextgen.player.network.client.DlnaClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideDlnaClient(): DlnaClient = DlnaClient()
}
