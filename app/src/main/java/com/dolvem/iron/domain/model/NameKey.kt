// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.domain.model

import java.text.Normalizer
import java.util.Locale

// D4: única función de normalización de nameKey, con NFC y Locale.ROOT. La usan el insert, el
// renombrar y la búsqueda, así que la base de datos y el repositorio coinciden con acentos.
// SQLite no puede normalizar caracteres no ASCII, ni con COLLATE NOCASE ni con lower().
fun nameKey(name: String): String = Normalizer.normalize(name, Normalizer.Form.NFC).lowercase(Locale.ROOT)
