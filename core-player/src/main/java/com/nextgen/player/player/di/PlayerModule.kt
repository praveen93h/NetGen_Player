package com.nextgen.player.player.di

import android.content.Context
import com.nextgen.player.player.PlayerEngine
import com.nextgen.player.player.audio.AudioEngine
import com.nextgen.player.player.audio.EqualizerEngine
import com.nextgen.player.player.gesture.GestureController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlayerModule {

    @Provides
    @Singleton
    fun providePlayerEngine(@ApplicationContext context: Context): PlayerEngine {
        return PlayerEngine(context)
    }

    @Provides
    @Singleton
    fun provideAudioEngine(@ApplicationContext context: Context): AudioEngine {
        return AudioEngine(context)
    }

    @Provides
    @Singleton
    fun provideGestureController(@ApplicationContext context: Context): GestureController {
        return GestureController(context)
    }

    @Provides
    @Singleton
    fun provideEqualizerEngine(): EqualizerEngine {
        return EqualizerEngine()
    }
}

