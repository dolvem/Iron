// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.db

import androidx.room3.migration.Migration

// D48: el framework de migraciones, la lista que AppContainer pasa a addMigrations. En v1 no hay
// migraciones reales todavía, así que la lista está vacía. La primera migración real se añade aquí, en
// orden ascendente, y el JSON exportado en schemas/com.dolvem.iron.data.db.IronDatabase/2.json es la
// fuente para comprobarla: el SQL de la migración tiene que producir exactamente ese esquema.
// D51: nunca fallbackToDestructiveMigration en builds de release, así que desde v2 la lista tiene que
// cubrir todos los tramos entre BASE_VERSION y la versión del @Database. Si falta un tramo, Room lanza
// IllegalStateException al abrir la base de datos en lugar de borrar las tablas.
object IronMigrations {

    const val BASE_VERSION = 1

    val migrations: List<Migration> = listOf()

    // Room resuelve el camino internamente con la misma regla: cada tramo empieza donde terminó el
    // anterior. Se expone como función pura para poder comprobar el orden en los tests (D18, fila
    // Migraciones). Devuelve vacío cuando hay un hueco, que en release es el fallo de D51.
    fun path(from: Int, to: Int, source: List<Migration> = migrations): List<Migration> {
        val ordered = source.sortedBy { it.startVersion }
        val chain = mutableListOf<Migration>()
        var version = from
        while (version < to) {
            val next = ordered.firstOrNull { it.startVersion == version && it.endVersion <= to }
            if (next == null) return emptyList()
            chain += next
            version = next.endVersion
        }
        return chain
    }
}
