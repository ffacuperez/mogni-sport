# Paleta de colores — Mogni Sport

Paleta validada por el cliente (grises + rojo), tomada de
[MogniSystem-Design-Guide.md](MogniSystem-Design-Guide.md). Este documento describe **cómo está
implementada** en el frontend.

Todos los colores están definidos **una sola vez**, como tokens de Tailwind v4, en
[`frontend/src/index.css`](../frontend/src/index.css) (bloque `@theme`). Cada token genera sus clases
automáticamente: `--color-primary` → `bg-primary`, `text-primary`, `border-primary`, etc.

**Regla:** en los componentes no se usan hex sueltos (`bg-[#D32F2F]`) ni la paleta genérica de Tailwind
(`slate-*`, `gray-*`, `red-*`, `emerald-*`). Si hace falta un color nuevo, se agrega como token en
`index.css` y se documenta acá.

---

## 1. Primario (rojo)

| Token | Hex | Token de la guía | Dónde se usa |
|---|---|---|---|
| `primary` | `#D32F2F` | `--color-primary` | Botones primarios y de peligro, ítem activo del sidebar, foco de inputs, logo |
| `primary-hover` | `#B71C1C` | `--color-primary-hover` | Hover de botones primarios |
| `primary-light` | `#EF5350` | `--color-primary-light` | Acento de la fila seleccionada en tablas, "SPORT" del logo sobre fondo oscuro |

## 2. Neutros

| Token | Hex | Token de la guía | Dónde se usa |
|---|---|---|---|
| `page` | `#F0F0F0` | `--color-bg-page` | Fondo de todas las pantallas |
| `card` | `#FFFFFF` | `--color-bg-card` | Cards, modales, toasts, barra superior del panel |
| `sidebar` | `#2C2C2C` | `--color-bg-sidebar` | Sidebar del panel, barra de navegación de la Home y de la vista del alumno |
| `sidebar-hover` | `#3A3A3A` | `--color-bg-sidebar-hover` | Hover y fondo del ítem activo del sidebar, divisores dentro del sidebar |
| `thead` | `#E0E0E0` | `--color-bg-table-header` | Encabezado de tablas, botones deshabilitados |
| `row-hover` | `#F5F5F5` | `--color-bg-table-row-hover` | Hover de filas y de botones ghost, cajas de solo lectura |
| `input` | `#FAFAFA` | `--color-bg-input` | Fondo de inputs y selects |
| `line` | `#E0E0E0` | `--color-border` | Bordes de cards, inputs y divisores |
| `line-soft` | `#F0F0F0` | (guía 4.6, borde de fila) | Separador entre filas de tablas |

## 3. Texto

| Token | Hex | Token de la guía | Dónde se usa |
|---|---|---|---|
| `ink` | `#212121` | `--color-text-primary` | Títulos y texto principal (color por defecto del `body`) |
| `ink-soft` | `#616161` | `--color-text-secondary` | Labels, info secundaria, encabezados de tabla, botones ghost |
| `ink-muted` | `#9E9E9E` | `--color-text-muted` | Placeholders, ayudas, estados vacíos, íconos inactivos del sidebar |
| `on-dark` | `#FFFFFF` | `--color-text-on-dark` / `--color-text-on-primary` | Texto sobre el sidebar y sobre botones rojos |

## 4. Estados

| Token | Hex | Fondo | Significado | Dónde se usa |
|---|---|---|---|---|
| `success` / `success-bg` | `#388E3C` | `#E8F5E9` | Correcto / al día | Alumno activo, cuota al día, inscripción activa, toast de éxito, "Pagada" bajo la fecha |
| `warning` | `#F9A825` | `#FFF8E1` (`warning-bg`) | Atención | Color de advertencia de la guía (reservado para íconos y realces) |
| `warning-text` | `#F57F17` | `#FFF8E1` (`warning-bg`) | Atención | Texto del badge "Próxima a vencer", aviso "Vence en N días" (guía 4.2) |
| `error` / `error-bg` | `#D32F2F` | `#FFEBEE` | Error / deuda | Cuota vencida, alumno inactivo, inscripción cancelada, mensajes de error, hover del botón secundario |
| `info` / `info-bg` | `#1976D2` | `#E3F2FD` | Informativo | Toasts informativos |

## 5. Otros

| Token | Hex | Dónde se usa |
|---|---|---|
| `whatsapp` | `#25D366` | Botón flotante de WhatsApp de la Home (color oficial de la marca WhatsApp; no está en la guía) |

Sombras:

| Token | Valor | Dónde se usa |
|---|---|---|
| `shadow-card` | `0 1px 3px rgba(0,0,0,0.08)` | Cards |
| `shadow-modal` | `0 4px 16px rgba(0,0,0,0.15)` | Modales, toasts, ilustración de la Home |

El overlay de los modales es negro al 50 % (`bg-black/50`), como pide la guía.

---

## Cómo se combinan

### Botones (`Button` en `shared/components/ui.jsx`, guía 4.1)

| Variante | Colores | Cuándo usarla |
|---|---|---|
| `primary` | fondo `primary`, texto blanco, hover `primary-hover` | Confirmar, crear, registrar pago, iniciar sesión |
| `secondary` | fondo blanco, texto y borde `primary`, hover `error-bg` | Modificar alumno / membresía, ver historial, registrarse |
| `ghost` | sin fondo, texto `ink-soft`, hover `row-hover` | Cancelar, cerrar, volver |
| `danger` | igual que primary | Dar de baja, eliminar, confirmar una acción destructiva |
| deshabilitado | fondo `thead`, texto `ink-muted` | Cualquier botón con `disabled` |

Radio 8px, texto 14px semibold.

### Badges (`Badge`, guía 4.2)

Sin borde, forma de píldora, texto 12px medium.

| Tono | Colores | Uso |
|---|---|---|
| `success` | `success` sobre `success-bg` | Activo, Al día, Inscripción activa |
| `warning` | `warning-text` sobre `warning-bg` | Próxima a vencer |
| `error` | `error` sobre `error-bg` | Vencida, Inactivo, Inscripción cancelada |
| `info` | `info` sobre `info-bg` | Informativos |
| `neutral` | `ink-soft` sobre `row-hover` | Sin uso actual (reservado) |

### Sidebar (guía 4.5)

- Fondo `sidebar`, ancho 240px, ítems de 44px de alto.
- Ítem activo: borde izquierdo de 3px `primary` y fondo `sidebar-hover`.
- Íconos de 20px en `ink-muted` (blancos si el ítem está activo), texto `on-dark`.

### Tablas (guía 4.6)

- Encabezado: fondo `thead`, texto `ink-soft` 13px semibold.
- Filas de 48px, hover `row-hover`, separador `line-soft`.
- Fila seleccionada: fondo `error-bg` y barra izquierda de 3px `primary-light`.

### Inputs (guía 4.3)

Fondo `input`, borde `line`, foco con borde `primary`, radio 8px, placeholder `ink-muted`.

## Tipografía

**Inter** (400–900) para todo el texto, cargada desde Google Fonts en `frontend/index.html`.
Respaldo: `-apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif`.
Los números (DNI, montos, fechas) usan `tabular-nums` para que las columnas queden alineadas.

## Favicon y logo

- Favicon (`frontend/public/favicon.svg`): cuadrado `primary` con una "M" blanca.
- Logo (`Logo` en `shared/components/ui.jsx`): "M" estilizada en rojo + "MOGNI / SPORT". Con `onDark`
  el texto pasa a blanco para el sidebar y las barras oscuras. Cuando exista el logo definitivo, se
  reemplaza solo en ese componente.
