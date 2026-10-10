<!-- SPDX-FileCopyrightText: 2026 dolvem -->
<!-- SPDX-License-Identifier: GPL-3.0-or-later -->

# Iteración 9 — Import/export propio

Bloque de `docs/state/todo.md`. Decisiones del bloque: D11 a D16.

- [ ] Export CSV de gráficas: columnas `timestamp,exercise,metric,value`, UTF-8 con BOM, ventana actual de la pantalla, ventana vacía exporta cabeceras sin filas, respeta la exclusión del calentamiento (D15), por SAF.
- [ ] Backup completo: JSON/GZIP de la BD con formato v1, DTOs separados de las entidades, camelCase, nulls escritos como `null`, `exportedAt` en ISO 8601 con zona, timestamps en epoch millis UTC, DTOs excluyen `nameKey` (D16).
- [ ] Importación por fusión: dos pasadas con mapa `incomingId -> existingId`, raíz primero y dependientes después, gana el nombre existente, duplicados del fichero resueltos con la primera ocurrencia, colisión de ID con otro nombre recibe ID nuevo, `unique(planId, exerciseId)` gana la fila existente con conflicto en el resumen (D11).
- [ ] Import en una sola transacción, con backup automático previo en `filesDir/backups/`, retención de los 3 más recientes, GZIP, sin SAF, sin UI de restauración (D12).
- [ ] Versiones del formato: acepta `version <= current`, rechaza `app` distinto de `iron` y `version` mayor con error (D14).
- [ ] GZIP detectado por magic bytes, no por extensión.
- [ ] Los ajustes no entran en el backup (D13).

## Notas del bloque

Sin notas.
