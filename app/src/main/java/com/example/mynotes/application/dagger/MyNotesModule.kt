package com.example.mynotes.application.dagger

import androidx.room.Room
import com.example.mynotes.application.MyNotesApplication
import com.example.mynotes.database.AppDatabase
import com.example.mynotes.presenter.MyNotesPresenter
import dagger.Module
import dagger.Provides
import javax.inject.Singleton


@Module
class MyNotesModule(val myNotesApplication: MyNotesApplication) {
    @Provides
    @Singleton
    fun databaseManager(): AppDatabase {
        return Room.databaseBuilder(
            this.myNotesApplication,
            AppDatabase::class.java, "notes-database"
        ).fallbackToDestructiveMigration().build()
    }
    @Provides
    @Singleton
    fun myNotesApplicationManager() = myNotesApplication

    @Provides
    @Singleton
    fun notasPresenterManager() : MyNotesPresenter{
        return MyNotesPresenter(this.myNotesApplication)
    }

}