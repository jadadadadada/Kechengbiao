package com.example.schedule.di

import com.example.schedule.data.db.CourseDao
import com.example.schedule.data.db.ScheduleDatabase
import com.example.schedule.data.repository.CourseRepository
import com.example.schedule.data.repository.CourseRepositoryImpl
import com.example.schedule.data.repository.SemesterRepository
import com.example.schedule.ui.viewmodel.ScheduleViewModel
import com.example.schedule.util.ReminderScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

    single { ScheduleDatabase.getInstance(androidContext()) }
    single<CourseDao> { get<ScheduleDatabase>().courseDao() }

    single<CourseRepository> { CourseRepositoryImpl(get()) }
    single { SemesterRepository(androidContext()) }
    single { ReminderScheduler(androidContext()) }

    viewModel { ScheduleViewModel(get(), get(), get()) }
}
