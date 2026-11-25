package com.wanyamajohn.musicplayer.di

import com.wanyamajohn.musicplayer.data.repository.MediaRepositoryImpl
import com.wanyamajohn.musicplayer.domain.repository.MediaRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindMediaRepository(impl: MediaRepositoryImpl): MediaRepository
}
