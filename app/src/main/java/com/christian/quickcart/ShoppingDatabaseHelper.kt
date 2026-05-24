package com.christian.quickcart

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite helper that stores QuickCart shopping list items on the device.
 */
class ShoppingDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    /**
     * Creates the shopping item table when the database is first opened.
     */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_SHOPPING_ITEMS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_QUANTITY TEXT NOT NULL,
                $COLUMN_CATEGORY TEXT NOT NULL,
                $COLUMN_PRIORITY TEXT NOT NULL,
                $COLUMN_NOTES TEXT NOT NULL DEFAULT '',
                $COLUMN_IS_BOUGHT INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    /**
     * Rebuilds the database if the schema version changes during development.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SHOPPING_ITEMS")
        onCreate(db)
    }

    /**
     * Adds starter rows so the app demonstrates stored data before the add screen exists.
     */
    fun seedSampleItemsIfEmpty() {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_SHOPPING_ITEMS", null)

        cursor.use {
            if (it.moveToFirst() && it.getInt(0) == 0) {
                insertShoppingItem("Milk", "2 bottles", "Dairy", "High", "Check expiry date")
                insertShoppingItem("Bread", "1 loaf", "Bakery", "Medium", "")
                insertShoppingItem("Apples", "6 pieces", "Fruit", "Low", "Any variety is fine")
                insertShoppingItem("Pasta", "2 packs", "Pantry", "Medium", "")
                insertShoppingItem("Tomatoes", "4 cans", "Pantry", "High", "For sauce")
            }
        }
    }

    /**
     * Inserts one shopping item into the local SQLite database.
     */
    fun insertShoppingItem(
        name: String,
        quantity: String,
        category: String,
        priority: String,
        notes: String
    ): Long {
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_QUANTITY, quantity)
            put(COLUMN_CATEGORY, category)
            put(COLUMN_PRIORITY, priority)
            put(COLUMN_NOTES, notes)
            put(COLUMN_IS_BOUGHT, 0)
        }

        return writableDatabase.insert(TABLE_SHOPPING_ITEMS, null, values)
    }

    /**
     * Reads all saved shopping items in the order they were added.
     */
    fun getAllShoppingItems(): List<ShoppingItem> {
        val items = mutableListOf<ShoppingItem>()
        val cursor = readableDatabase.query(
            TABLE_SHOPPING_ITEMS,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_ID ASC"
        )

        cursor.use {
            while (it.moveToNext()) {
                items.add(
                    ShoppingItem(
                        id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                        quantity = it.getString(it.getColumnIndexOrThrow(COLUMN_QUANTITY)),
                        category = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                        priority = it.getString(it.getColumnIndexOrThrow(COLUMN_PRIORITY)),
                        notes = it.getString(it.getColumnIndexOrThrow(COLUMN_NOTES)),
                        isBought = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_BOUGHT)) == 1
                    )
                )
            }
        }

        return items
    }

    /**
     * Reads one shopping item by id so the edit form can be pre-filled.
     */
    fun getShoppingItemById(itemId: Long): ShoppingItem? {
        val cursor = readableDatabase.query(
            TABLE_SHOPPING_ITEMS,
            null,
            "$COLUMN_ID = ?",
            arrayOf(itemId.toString()),
            null,
            null,
            null
        )

        cursor.use {
            return if (it.moveToFirst()) {
                ShoppingItem(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                    quantity = it.getString(it.getColumnIndexOrThrow(COLUMN_QUANTITY)),
                    category = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                    priority = it.getString(it.getColumnIndexOrThrow(COLUMN_PRIORITY)),
                    notes = it.getString(it.getColumnIndexOrThrow(COLUMN_NOTES)),
                    isBought = it.getInt(it.getColumnIndexOrThrow(COLUMN_IS_BOUGHT)) == 1
                )
            } else {
                null
            }
        }
    }

    /**
     * Updates the editable details for an existing shopping item.
     */
    fun updateShoppingItem(
        itemId: Long,
        name: String,
        quantity: String,
        category: String,
        priority: String,
        notes: String
    ) {
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_QUANTITY, quantity)
            put(COLUMN_CATEGORY, category)
            put(COLUMN_PRIORITY, priority)
            put(COLUMN_NOTES, notes)
        }

        writableDatabase.update(
            TABLE_SHOPPING_ITEMS,
            values,
            "$COLUMN_ID = ?",
            arrayOf(itemId.toString())
        )
    }

    /**
     * Deletes a shopping item from the local database.
     */
    fun deleteShoppingItem(itemId: Long) {
        writableDatabase.delete(
            TABLE_SHOPPING_ITEMS,
            "$COLUMN_ID = ?",
            arrayOf(itemId.toString())
        )
    }

    /**
     * Saves whether a shopping item has been bought.
     */
    fun updateBoughtStatus(itemId: Long, isBought: Boolean) {
        val values = ContentValues().apply {
            put(COLUMN_IS_BOUGHT, if (isBought) 1 else 0)
        }

        writableDatabase.update(
            TABLE_SHOPPING_ITEMS,
            values,
            "$COLUMN_ID = ?",
            arrayOf(itemId.toString())
        )
    }

    companion object {
        private const val DATABASE_NAME = "quickcart.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_SHOPPING_ITEMS = "shopping_items"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_QUANTITY = "quantity"
        private const val COLUMN_CATEGORY = "category"
        private const val COLUMN_PRIORITY = "priority"
        private const val COLUMN_NOTES = "notes"
        private const val COLUMN_IS_BOUGHT = "is_bought"
    }
}
