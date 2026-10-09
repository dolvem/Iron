// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.db

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// D18, fila "Migraciones": el framework se ejercita con los JSON de esquema versionados (D48).
// Migration.migrate recibe una SQLiteConnection, y Transactor no es una SQLiteConnection, así que el
// test abre su propia conexión con AndroidSQLiteDriver, el driver que Robolectric shadowea y el mismo
// que usan los tests de Room en memoria.
@RunWith(RobolectricTestRunner::class)
class MigrationTest {

    // Migración de prueba, solo añade una columna. No hay un esquema v2 real en schemas/, porque en v1
    // todavía no hay cambios: el esquema v2 de prueba vive aquí, en el test.
    private val testMigration = object : Migration(1, 2) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE `Exercise` ADD COLUMN `source` TEXT NOT NULL DEFAULT 'manual'")
        }
    }

    private val nextTestMigration = object : Migration(2, 3) {
        override suspend fun migrate(connection: SQLiteConnection) {}
    }

    private val v1ExerciseColumns = listOf("id", "name", "nameKey", "notes", "archived", "categoryId")

    // D48: el JSON versionado es la fuente. Gradle ejecuta los tests con el directorio del módulo como
    // working directory, así que la ruta relativa es la del módulo; se acepta también la ruta desde la
    // raíz por si el working directory cambia.
    private fun schemaText(): String {
        val module = File("schemas/com.dolvem.iron.data.db.IronDatabase/1.json")
        val root = File("app/schemas/com.dolvem.iron.data.db.IronDatabase/1.json")
        val file = if (module.exists()) module else root
        assertTrue("el esquema versionado de v1 debe estar en schemas/ (D48)", file.exists())
        return file.readText()
    }

    // Se busca la cadena de la tabla sin parsear el JSON completo.
    private fun createSqlFor(schema: String, tableName: String): String {
        val marker = "\"tableName\": \"$tableName\""
        val start = schema.indexOf(marker)
        assertTrue("la tabla $tableName no está en el esquema v1", start >= 0)
        val key = "\"createSql\": \""
        val from = schema.indexOf(key, start) + key.length
        val to = schema.indexOf('"', from)
        return schema.substring(from, to).replace("\${TABLE_NAME}", tableName)
    }

    private suspend fun columnsOf(connection: SQLiteConnection, tableName: String): List<String> {
        val columns = mutableListOf<String>()
        connection.prepare("PRAGMA table_info(`$tableName`)").use { statement ->
            while (statement.step()) {
                columns += statement.getText(1)
            }
        }
        return columns
    }

    // D18: la lista arranca en v1 y se aplica en orden. En v1 está vacía, así que el orden se comprueba
    // con dos migraciones de prueba encadenadas.
    @Test
    fun migrationsStartAtVersionOneAndApplyInOrder() {
        assertEquals(1, IronMigrations.BASE_VERSION)
        assertTrue(IronMigrations.migrations.isEmpty())

        val ordered = listOf(testMigration, nextTestMigration)
        assertEquals(ordered, IronMigrations.path(1, 3, ordered))

        // D51: un hueco no se resuelve, porque en release no hay fallback destructivo.
        assertEquals(emptyList<Migration>(), IronMigrations.path(1, 3, listOf(nextTestMigration)))
    }

    // D18 y D48: la migración de prueba se ejecuta sobre una base de datos creada con el createSql del
    // esquema v1, y el resultado es el esquema de v1 más el cambio declarado.
    @Test
    fun testMigrationFromV1ToV2MatchesTheV1SchemaPlusTheDeclaredChange() = runBlocking {
        val schema = schemaText()
        val connection = AndroidSQLiteDriver().open(":memory:")
        try {
            connection.execSQL(createSqlFor(schema, "Exercise"))
            testMigration.migrate(connection)

            assertEquals(v1ExerciseColumns + "source", columnsOf(connection, "Exercise"))
        } finally {
            connection.close()
        }
    }

    // D48: el JSON versionado coincide con las entidades de v1, por eso el createSql anterior es el mismo
    // que declara la entidad Exercise.
    @Test
    fun versionedSchemaMatchesTheV1Entities() {
        val schema = schemaText()
        val sql = createSqlFor(schema, "Exercise")

        assertTrue("el esquema v1 debe declarar la tabla Exercise", schema.contains("\"tableName\": \"Exercise\""))
        assertTrue("el esquema versionado debe ser la versión 1", schema.contains("\"version\": 1"))
        // D4: el índice único está en nameKey, la columna normalizada derivada.
        assertTrue("nameKey debe ser NOT NULL en el esquema v1", sql.contains("`nameKey` TEXT NOT NULL"))
    }
}
