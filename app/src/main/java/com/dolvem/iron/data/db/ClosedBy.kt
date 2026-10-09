// SPDX-FileCopyrightText: 2026 dolvem
// SPDX-License-Identifier: GPL-3.0-or-later

package com.dolvem.iron.data.db

// D62: enum de cómo se cerró la sesión, guardado como TEXT. AUTO es el cierre al cambiar de día
// (D1), EXPLICIT el cierre dentro del mismo día. El historial los distingue (D44).
enum class ClosedBy {
    AUTO,
    EXPLICIT
}
