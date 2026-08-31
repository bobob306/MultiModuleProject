package com.bsdevs.multimoduleproject.forms

import com.bsdevs.babycare.domain.BabyCareRepository
import com.bsdevs.coffeescreen.data.CoffeeRepository
import com.bsdevs.network.repository.FormDeleter
import com.bsdevs.network.repository.FormPrefiller
import com.bsdevs.network.repository.FormSubmitter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FormSubmitModule {

    @Provides
    @Singleton
    fun provideFormSubmitRouter(
        coffeeRepository: CoffeeRepository,
        babyCareRepository: BabyCareRepository,
    ): FormSubmitRouter = FormSubmitRouter(coffeeRepository, babyCareRepository)

    @Provides
    @Singleton
    fun provideFormSubmitter(router: FormSubmitRouter): FormSubmitter = router

    @Provides
    @Singleton
    fun provideFormPrefiller(
        coffeeRepository: CoffeeRepository,
        babyCareRepository: BabyCareRepository,
    ): FormPrefiller = FormPrefillerImpl(coffeeRepository, babyCareRepository)

    @Provides
    @Singleton
    fun provideFormDeleter(
        babyCareRepository: BabyCareRepository,
    ): FormDeleter = FormDeleterImpl(babyCareRepository)
}
