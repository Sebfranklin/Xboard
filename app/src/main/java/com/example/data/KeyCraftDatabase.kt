package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        LearnedWordEntity::class,
        BigramEntity::class,
        SnippetEntity::class,
        ClipboardItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KeyCraftDatabase : RoomDatabase() {
    abstract fun keyCraftDao(): KeyCraftDao

    companion object {
        @Volatile
        private var INSTANCE: KeyCraftDatabase? = null

        fun getInstance(context: Context): KeyCraftDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): KeyCraftDatabase {
            return Room.databaseBuilder(
                appContext,
                KeyCraftDatabase::class.java,
                "keycraft_local.db"
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val dao = getInstance(appContext).keyCraftDao()
                            dao.insertSnippets(starterSnippets)
                            dao.upsertClipboardItem(
                                ClipboardItemEntity(
                                    content = "KeyCraft runs 100% offline with zero AI models.",
                                    isPinned = true
                                )
                            )
                        }
                    }
                })
                .build()
        }

        val starterSnippets = listOf(
            SnippetEntity(
                trigger = ";omw",
                expansion = "On my way!",
                category = "Quick Reply"
            ),
            SnippetEntity(
                trigger = ";ty",
                expansion = "Thank you so much!",
                category = "Quick Reply"
            ),
            SnippetEntity(
                trigger = ";brb",
                expansion = "Be right back in a few minutes.",
                category = "Quick Reply"
            ),
            SnippetEntity(
                trigger = ";meet",
                expansion = "Let's sync up tomorrow morning.",
                category = "Work"
            ),
            SnippetEntity(
                trigger = ";date",
                expansion = "{date}",
                category = "Macro"
            ),
            SnippetEntity(
                trigger = ";time",
                expansion = "{time}",
                category = "Macro"
            ),
            SnippetEntity(
                trigger = ";shrug",
                expansion = "¯\\_(ツ)_/¯",
                category = "Fun"
            )
        )
    }
}
