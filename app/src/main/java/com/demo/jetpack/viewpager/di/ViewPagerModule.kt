package com.demo.jetpack.viewpager.di

import com.demo.jetpack.viewpager.ViewPagerAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
object ViewPager2Module {

    @ActivityScoped
    @Provides
    fun provideViewPagerAdapter(): ViewPagerAdapter {
        return ViewPagerAdapter()
    }
}
