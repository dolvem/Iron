// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.domain.model

// D32: en la fusión el usuario elige el destino y sobrevive su nombre, así que el nombre no se calcula:
// es el del destino. Estas dos funciones son puras porque el remapeo de las FKs ocurre en el
// repositorio, dentro de la transacción, y son las reglas de D32 las que se testean con JUnit4.

// D32: en archived gana el estado menos restrictivo, que es el activo. Fusionar un ejercicio activo en
// uno archivado no puede archivar el resultado, porque eso lo saca de la lista por defecto.
fun mergedArchived(origin: Boolean, destination: Boolean): Boolean = origin && destination

// D32: en notes gana el menos restrictivo, con las notas concatenadas. El destino va primero porque es
// el que sobrevive. Las notas en blanco se descartan, porque concatenar un vacío repite el separador.
fun mergedNotes(origin: String?, destination: String?): String? {
    val destinationNotes = destination?.trim().orEmpty()
    val originNotes = origin?.trim().orEmpty()
    return when {
        destinationNotes.isEmpty() -> originNotes.ifEmpty { null }
        originNotes.isEmpty() -> destinationNotes
        else -> "$destinationNotes\n$originNotes"
    }
}
