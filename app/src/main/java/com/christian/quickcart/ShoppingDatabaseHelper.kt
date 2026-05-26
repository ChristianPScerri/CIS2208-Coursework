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

    private val appContext = context.applicationContext

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
                $COLUMN_AMOUNT INTEGER NOT NULL DEFAULT 1,
                $COLUMN_IMAGE_PATH TEXT NOT NULL DEFAULT '',
                $COLUMN_IS_BOUGHT INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        createPantryTable(db)
    }

    /**
     * Rebuilds the database if the schema version changes during development.
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                "ALTER TABLE $TABLE_SHOPPING_ITEMS ADD COLUMN $COLUMN_NOTES TEXT NOT NULL DEFAULT ''"
            )
        }
        if (oldVersion < 3) {
            createPantryTable(db)
        }
        if (oldVersion < 4) {
            db.execSQL(
                "ALTER TABLE $TABLE_SHOPPING_ITEMS ADD COLUMN $COLUMN_IMAGE_PATH TEXT NOT NULL DEFAULT ''"
            )
            if (oldVersion >= 3) {
                db.execSQL(
                    "ALTER TABLE $TABLE_PANTRY_ITEMS ADD COLUMN $COLUMN_IMAGE_PATH TEXT NOT NULL DEFAULT ''"
                )
            }
        }
        if (oldVersion < 5) {
            db.execSQL(
                "ALTER TABLE $TABLE_SHOPPING_ITEMS ADD COLUMN $COLUMN_AMOUNT INTEGER NOT NULL DEFAULT 1"
            )
            if (oldVersion >= 3) {
                db.execSQL(
                    "ALTER TABLE $TABLE_PANTRY_ITEMS ADD COLUMN $COLUMN_AMOUNT INTEGER NOT NULL DEFAULT 1"
                )
            }
        }
    }

    /**
     * Creates the pantry item table used by the Pantry screen.
     */
    private fun createPantryTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_PANTRY_ITEMS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_QUANTITY TEXT NOT NULL,
                $COLUMN_CATEGORY TEXT NOT NULL,
                $COLUMN_EXPIRY_DATE TEXT NOT NULL,
                $COLUMN_AMOUNT INTEGER NOT NULL DEFAULT 1,
                $COLUMN_IMAGE_PATH TEXT NOT NULL DEFAULT ''
            )
            """.trimIndent()
        )
    }

    /**
     * Adds starter rows so the app demonstrates stored data before the add screen exists.
     */
    fun seedSampleItemsIfEmpty() {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_SHOPPING_ITEMS", null)

        cursor.use {
            val itemCount = if (it.moveToFirst()) it.getInt(0) else 0

            if (itemCount > 0) {
                markSeedCompleted(PREF_SHOPPING_SEEDED)
                return
            }

            if (!isSeedCompleted(PREF_SHOPPING_SEEDED)) {
                insertShoppingItem("Milk", "2 bottles", "Dairy", "High", "Check expiry date")
                insertShoppingItem("Bread", "1 loaf", "Bakery", "Medium", "")
                insertShoppingItem("Apples", "6 pieces", "Fruit", "Low", "Any variety is fine")
                insertShoppingItem("Pasta", "2 packs", "Pantry", "Medium", "")
                insertShoppingItem("Tomatoes", "4 cans", "Pantry", "High", "For sauce")
                markSeedCompleted(PREF_SHOPPING_SEEDED)
            }
        }
    }

    /**
     * Adds starter pantry rows so the pantry and recipe screens are useful in a fresh demo install.
     */
    fun seedSamplePantryItemsIfEmpty() {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM $TABLE_PANTRY_ITEMS", null)

        cursor.use {
            val itemCount = if (it.moveToFirst()) it.getInt(0) else 0

            if (itemCount > 0) {
                markSeedCompleted(PREF_PANTRY_SEEDED)
                return
            }

            if (!isSeedCompleted(PREF_PANTRY_SEEDED)) {
                insertPantryItem("Tomatoes", "4 cans", "Pantry", "2026-05-28")
                insertPantryItem("Rice", "1 bag", "Pantry", "No expiry set")
                insertPantryItem("Eggs", "6", "Fridge", "2026-05-27")
                insertPantryItem("Chicken", "2 portions", "Freezer", "2026-06-02")
                markSeedCompleted(PREF_PANTRY_SEEDED)
            }
        }
    }

    /**
     * Checks whether a sample-data seed has already been handled for this install.
     */
    private fun isSeedCompleted(key: String): Boolean {
        return appContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(key, false)
    }

    /**
     * Records that a sample-data seed should not run again after user deletions.
     */
    private fun markSeedCompleted(key: String) {
        appContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(key, true)
            .apply()
    }

    /**
     * Inserts one shopping item into the local SQLite database.
     */
    fun insertShoppingItem(
        name: String,
        quantity: String,
        category: String,
        priority: String,
        notes: String,
        amount: Int = 1,
        imagePath: String = ""
    ): Long {
        val existingItem = findMatchingShoppingItem(
            name = name,
            quantity = quantity,
            category = category,
            priority = priority,
            notes = notes,
            imagePath = imagePath,
            excludedItemId = NO_ITEM_ID
        )

        if (existingItem != null) {
            updateShoppingAmount(existingItem.id, existingItem.amount + amount.coerceAtLeast(1))
            return existingItem.id
        }

        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_QUANTITY, quantity)
            put(COLUMN_CATEGORY, category)
            put(COLUMN_PRIORITY, priority)
            put(COLUMN_NOTES, notes)
            put(COLUMN_AMOUNT, amount.coerceAtLeast(1))
            put(COLUMN_IMAGE_PATH, imagePath)
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
                        amount = it.getInt(it.getColumnIndexOrThrow(COLUMN_AMOUNT)),
                        imagePath = it.getString(it.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)),
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
                    amount = it.getInt(it.getColumnIndexOrThrow(COLUMN_AMOUNT)),
                    imagePath = it.getString(it.getColumnIndexOrThrow(COLUMN_IMAGE_PATH)),
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
        notes: String,
        amount: Int = 1,
        imagePath: String = ""
    ) {
        val existingItem = findMatchingShoppingItem(
            name = name,
            quantity = quantity,
            category = category,
            priority = priority,
            notes = notes,
            imagePath = imagePath,
            excludedItemId = itemId
        )

        if (existingItem != null) {
            updateShoppingAmount(existingItem.id, existingItem.amount + amount.coerceAtLeast(1))
            deleteShoppingItem(itemId)
            return
        }

        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_QUANTITY, quantity)
            put(COLUMN_CATEGORY, category)
            put(COLUMN_PRIORITY, priority)
            put(COLUMN_NOTES, notes)
            put(COLUMN_AMOUNT, amount.coerceAtLeast(1))
            put(COLUMN_IMAGE_PATH, imagePath)
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
     * Inserts one pantry item into local SQLite storage.
     */
    fun insertPantryItem(
        name: String,
        quantity: String,
        category: String,
        expiryDate: String,
        amount: Int = 1,
        imagePath: String = ""
    ): Long {
        val existingItem = findMatchingPantryItem(
            name = name,
            quantity = quantity,
            category = category,
            expiryDate = expiryDate,
            imagePath = imagePath,
            excludedItemId = NO_ITEM_ID
        )

        if (existingItem != null) {
            updatePantryAmount(existingItem.id, existingItem.amount + amount.coerceAtLeast(1))
            return existingItem.id
        }

        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_QUANTITY, quantity)
            put(COLUMN_CATEGORY, category)
            put(COLUMN_EXPIRY_DATE, expiryDate)
            put(COLUMN_AMOUNT, amount.coerceAtLeast(1))
            put(COLUMN_IMAGE_PATH, imagePath)
        }

        return writableDatabase.insert(TABLE_PANTRY_ITEMS, null, values)
    }

    /**
     * Reads all saved pantry items in the order they were added.
     */
    fun getAllPantryItems(): List<PantryItem> {
        val items = mutableListOf<PantryItem>()
        val cursor = readableDatabase.query(
            TABLE_PANTRY_ITEMS,
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
                    PantryItem(
                        id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                        name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                        quantity = it.getString(it.getColumnIndexOrThrow(COLUMN_QUANTITY)),
                        category = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                        expiryDate = it.getString(it.getColumnIndexOrThrow(COLUMN_EXPIRY_DATE)),
                        amount = it.getInt(it.getColumnIndexOrThrow(COLUMN_AMOUNT)),
                        imagePath = it.getString(it.getColumnIndexOrThrow(COLUMN_IMAGE_PATH))
                    )
                )
            }
        }

        return items
    }

    /**
     * Reads one pantry item by id so the pantry form can be pre-filled.
     */
    fun getPantryItemById(itemId: Long): PantryItem? {
        val cursor = readableDatabase.query(
            TABLE_PANTRY_ITEMS,
            null,
            "$COLUMN_ID = ?",
            arrayOf(itemId.toString()),
            null,
            null,
            null
        )

        cursor.use {
            return if (it.moveToFirst()) {
                PantryItem(
                    id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID)),
                    name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                    quantity = it.getString(it.getColumnIndexOrThrow(COLUMN_QUANTITY)),
                    category = it.getString(it.getColumnIndexOrThrow(COLUMN_CATEGORY)),
                    expiryDate = it.getString(it.getColumnIndexOrThrow(COLUMN_EXPIRY_DATE)),
                    amount = it.getInt(it.getColumnIndexOrThrow(COLUMN_AMOUNT)),
                    imagePath = it.getString(it.getColumnIndexOrThrow(COLUMN_IMAGE_PATH))
                )
            } else {
                null
            }
        }
    }

    /**
     * Updates an existing pantry item in the local database.
     */
    fun updatePantryItem(
        itemId: Long,
        name: String,
        quantity: String,
        category: String,
        expiryDate: String,
        amount: Int = 1,
        imagePath: String = ""
    ) {
        val existingItem = findMatchingPantryItem(
            name = name,
            quantity = quantity,
            category = category,
            expiryDate = expiryDate,
            imagePath = imagePath,
            excludedItemId = itemId
        )

        if (existingItem != null) {
            updatePantryAmount(existingItem.id, existingItem.amount + amount.coerceAtLeast(1))
            deletePantryItem(itemId)
            return
        }

        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_QUANTITY, quantity)
            put(COLUMN_CATEGORY, category)
            put(COLUMN_EXPIRY_DATE, expiryDate)
            put(COLUMN_AMOUNT, amount.coerceAtLeast(1))
            put(COLUMN_IMAGE_PATH, imagePath)
        }

        writableDatabase.update(
            TABLE_PANTRY_ITEMS,
            values,
            "$COLUMN_ID = ?",
            arrayOf(itemId.toString())
        )
    }

    /**
     * Deletes a pantry item from the local database.
     */
    fun deletePantryItem(itemId: Long) {
        writableDatabase.delete(
            TABLE_PANTRY_ITEMS,
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

    /**
     * Moves a bought shopping item into the pantry and removes it from the shopping list.
     */
    fun moveShoppingItemToPantry(item: ShoppingItem, expiryDate: String) {
        insertPantryItem(
            name = item.name,
            quantity = item.quantity,
            category = item.category,
            expiryDate = expiryDate,
            amount = item.amount,
            imagePath = item.imagePath
        )
        deleteShoppingItem(item.id)
    }

    /**
     * Updates the stacked amount for a shopping item row.
     */
    private fun updateShoppingAmount(itemId: Long, amount: Int) {
        val values = ContentValues().apply {
            put(COLUMN_AMOUNT, amount.coerceAtLeast(1))
        }
        writableDatabase.update(
            TABLE_SHOPPING_ITEMS,
            values,
            "$COLUMN_ID = ?",
            arrayOf(itemId.toString())
        )
    }

    /**
     * Updates the stacked amount for a pantry item row.
     */
    private fun updatePantryAmount(itemId: Long, amount: Int) {
        val values = ContentValues().apply {
            put(COLUMN_AMOUNT, amount.coerceAtLeast(1))
        }
        writableDatabase.update(
            TABLE_PANTRY_ITEMS,
            values,
            "$COLUMN_ID = ?",
            arrayOf(itemId.toString())
        )
    }

    /**
     * Finds a shopping item whose identifying traits match the submitted item.
     */
    private fun findMatchingShoppingItem(
        name: String,
        quantity: String,
        category: String,
        priority: String,
        notes: String,
        imagePath: String,
        excludedItemId: Long
    ): ShoppingItem? {
        return getAllShoppingItems().firstOrNull { item ->
            item.id != excludedItemId &&
                item.name.equals(name, ignoreCase = true) &&
                item.quantity.equals(quantity, ignoreCase = true) &&
                item.category == category &&
                item.priority == priority &&
                item.notes == notes &&
                item.imagePath == imagePath
        }
    }

    /**
     * Finds a pantry item whose identifying traits match the submitted item.
     */
    private fun findMatchingPantryItem(
        name: String,
        quantity: String,
        category: String,
        expiryDate: String,
        imagePath: String,
        excludedItemId: Long
    ): PantryItem? {
        return getAllPantryItems().firstOrNull { item ->
            item.id != excludedItemId &&
                item.name.equals(name, ignoreCase = true) &&
                item.quantity.equals(quantity, ignoreCase = true) &&
                item.category == category &&
                item.expiryDate == expiryDate &&
                item.imagePath == imagePath
        }
    }

    companion object {
        private const val DATABASE_NAME = "quickcart.db"
        private const val DATABASE_VERSION = 5

        private const val TABLE_SHOPPING_ITEMS = "shopping_items"
        private const val TABLE_PANTRY_ITEMS = "pantry_items"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_QUANTITY = "quantity"
        private const val COLUMN_CATEGORY = "category"
        private const val COLUMN_PRIORITY = "priority"
        private const val COLUMN_NOTES = "notes"
        private const val COLUMN_AMOUNT = "amount"
        private const val COLUMN_IS_BOUGHT = "is_bought"
        private const val COLUMN_EXPIRY_DATE = "expiry_date"
        private const val COLUMN_IMAGE_PATH = "image_path"

        private const val PREFS_NAME = "quickcart_seed_preferences"
        private const val PREF_SHOPPING_SEEDED = "shopping_seeded"
        private const val PREF_PANTRY_SEEDED = "pantry_seeded"
        private const val NO_ITEM_ID = -1L
    }
}
