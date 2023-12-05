package com.example.mynotes.application.dagger

import com.example.mynotes.application.MyNotesApplication
import com.example.mynotes.database.AppDatabase
import com.example.mynotes.presenter.MyNotesPresenter
import com.example.mynotes.view.MainActivity
import dagger.Component
import javax.inject.Singleton


@Singleton
@Component(modules = [MyNotesModule::class])
interface MyNotesComponent {
    fun inject(myNotesApplication: MyNotesApplication)
    fun inject(appDatabase: AppDatabase)
    fun AppDatabase() : AppDatabase
    fun inject(mainActivity: MainActivity)
    fun inject(myNotesPresenter: MyNotesPresenter)

}