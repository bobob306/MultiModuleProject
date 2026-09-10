package com.bsdevs.forms.di

import com.bsdevs.babycare.core.domain.BabyCareRepository
import com.bsdevs.coffeescreen.data.CoffeeRepository
import com.bsdevs.forms.impl.FormDeleterImpl
import com.bsdevs.forms.impl.FormPrefillerImpl
import com.bsdevs.forms.impl.FormSubmitterImpl
import com.bsdevs.data.repository.FormDeleter
import com.bsdevs.data.repository.FormPrefiller
import com.bsdevs.data.repository.FormSubmitter
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
    fun provideFormSubmitterImpl(
        coffeeRepository: CoffeeRepository,
        babyCareRepository: BabyCareRepository,
    ): FormSubmitterImpl = FormSubmitterImpl(coffeeRepository, babyCareRepository)

    @Provides
    @Singleton
    fun provideFormSubmitter(router: FormSubmitterImpl): FormSubmitter = router

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
