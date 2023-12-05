package com.example.mynotes.application

import android.app.Application
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import androidx.room.Room
import com.example.mynotes.application.dagger.DaggerMyNotesComponent
import com.example.mynotes.application.dagger.MyNotesComponent
import com.example.mynotes.application.dagger.MyNotesModule
import com.example.mynotes.database.AppDatabase

class MyNotesApplication : MultiDexApplication() {

    private lateinit var application : MyNotesApplication

    private  lateinit var myNotesComponet : MyNotesComponent

    override fun onCreate() {
        super.onCreate()
        MultiDex.install(this)
        this.application= this

        myNotesComponet = DaggerMyNotesComponent.builder().myNotesModule(MyNotesModule(this)).build()
        myNotesComponet.inject(this)

        /*myNotesComponent = DaggerMyNotesComponent.builder().myNotesModule(MyNotesModule(this)).build()
        myNotesComponent.inject(this)*/
    }
    fun getMyNotesComponent():MyNotesComponent{
        return this.myNotesComponet
    }

}

