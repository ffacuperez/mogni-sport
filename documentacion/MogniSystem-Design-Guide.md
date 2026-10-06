# MogniSystem — Design System Reference Guide

> **Alcance:** Este documento define la identidad visual de toda la aplicación MogniSystem. Todos los colores, tipografía, componentes y estilos aplican de forma global a todas las pantallas y módulos del sistema (Alumnos, Membresías, Cuotas, Empleados, Horarios, Ejercicios, Planes de Entrenamiento, Sesiones, Dashboard, Login, etc.). Paleta validada por el cliente — Opción 4: Grises + Rojo.

---

## 1. Color Palette (Global — aplica a toda la app)

### Primary Colors
| Token | Hex | Usage |
|---|---|---|
| `--color-primary` | `#D32F2F` | Primary buttons, active states, CTAs, links hover |
| `--color-primary-hover` | `#B71C1C` | Primary button hover state |
| `--color-primary-light` | `#EF5350` | Subtle highlights, selected row accent |

### Neutrals (Gray Scale)
| Token | Hex | Usage |
|---|---|---|
| `--color-bg-page` | `#F0F0F0` | Page background |
| `--color-bg-card` | `#FFFFFF` | Cards, modals, dropdowns |
| `--color-bg-sidebar` | `#2C2C2C` | Sidebar, nav bar |
| `--color-bg-sidebar-hover` | `#3A3A3A` | Sidebar item hover |
| `--color-bg-table-header` | `#E0E0E0` | Table header row |
| `--color-bg-table-row-hover` | `#F5F5F5` | Table row hover |
| `--color-bg-input` | `#FAFAFA` | Input fields background |
| `--color-border` | `#E0E0E0` | Card borders, dividers, input borders |
| `--color-border-focus` | `#D32F2F` | Input focus border |

### Text Colors
| Token | Hex | Usage |
|---|---|---|
| `--color-text-primary` | `#212121` | Headings, primary text |
| `--color-text-secondary` | `#616161` | Labels, secondary info, placeholders |
| `--color-text-muted` | `#9E9E9E` | Disabled text, hints |
| `--color-text-on-dark` | `#FFFFFF` | Text on sidebar, dark backgrounds |
| `--color-text-on-primary` | `#FFFFFF` | Text on red buttons |

### Status / Semantic Colors
| Token | Hex | Usage |
|---|---|---|
| `--color-success` | `#388E3C` | "Activo", "Pagada" badges |
| `--color-success-bg` | `#E8F5E9` | Success badge background |
| `--color-warning` | `#F9A825` | "Próxima a vencer" badges |
| `--color-warning-bg` | `#FFF8E1` | Warning badge background |
| `--color-error` | `#D32F2F` | "Vencida", "Inactivo", error messages |
| `--color-error-bg` | `#FFEBEE` | Error badge background |
| `--color-info` | `#1976D2` | Informational links, tooltips |
| `--color-info-bg` | `#E3F2FD` | Info badge background |

---

## 2. Typography

| Element | Font | Weight | Size | Color |
|---|---|---|---|---|
| H1 (Page title) | Inter | 700 (Bold) | 24px | `--color-text-primary` |
| H2 (Section title) | Inter | 600 (Semibold) | 20px | `--color-text-primary` |
| H3 (Card title) | Inter | 600 (Semibold) | 16px | `--color-text-primary` |
| Body | Inter | 400 (Regular) | 14px | `--color-text-primary` |
| Body small | Inter | 400 (Regular) | 12px | `--color-text-secondary` |
| Label | Inter | 500 (Medium) | 12px | `--color-text-secondary` |
| Button text | Inter | 600 (Semibold) | 14px | `--color-text-on-primary` |
| Table header | Inter | 600 (Semibold) | 13px | `--color-text-secondary` |
| Table cell | Inter | 400 (Regular) | 14px | `--color-text-primary` |
| Sidebar nav item | Inter | 500 (Medium) | 14px | `--color-text-on-dark` |

**Fallback stack:** `Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif`

---

## 3. Spacing & Layout

| Token | Value | Usage |
|---|---|---|
| `--spacing-xs` | 4px | Inline spacing, badge padding |
| `--spacing-sm` | 8px | Between small elements |
| `--spacing-md` | 16px | Card padding, form gap |
| `--spacing-lg` | 24px | Section spacing |
| `--spacing-xl` | 32px | Page margin, large gaps |
| `--radius-sm` | 4px | Badges, small elements |
| `--radius-md` | 8px | Cards, inputs, buttons |
| `--radius-lg` | 12px | Modals, large cards |
| `--shadow-card` | `0 1px 3px rgba(0,0,0,0.08)` | Card elevation |
| `--shadow-modal` | `0 4px 16px rgba(0,0,0,0.15)` | Modal/dropdown elevation |

### Grid
- **Sidebar width:** 240px (fixed)
- **Content area:** fluid, max-width 1200px, centered
- **Card grid:** 16px gap between cards
- **Table:** full width within card, 48px row height

---

## 4. Component Specifications

### 4.1 Buttons

| Variant | Background | Text | Border | Hover |
|---|---|---|---|---|
| Primary | `#D32F2F` | `#FFFFFF` | none | `#B71C1C` |
| Secondary | `#FFFFFF` | `#D32F2F` | 1px solid `#D32F2F` | bg `#FFEBEE` |
| Ghost | transparent | `#616161` | none | bg `#F5F5F5` |
| Danger | `#D32F2F` | `#FFFFFF` | none | `#B71C1C` |
| Disabled | `#E0E0E0` | `#9E9E9E` | none | no change |

- Padding: 10px 20px
- Border radius: `--radius-md` (8px)
- Font: 14px, Semibold

### 4.2 Badges (Status)

| Status | Text | Background | Border |
|---|---|---|---|
| Activo / Pagada | `#388E3C` | `#E8F5E9` | none |
| Próxima a vencer | `#F57F17` | `#FFF8E1` | none |
| Vencida / Inactivo | `#D32F2F` | `#FFEBEE` | none |

- Padding: 4px 12px
- Border radius: 12px (pill)
- Font: 12px, Medium

### 4.3 Input Fields

- Background: `#FAFAFA`
- Border: 1px solid `#E0E0E0`
- Border focus: 1px solid `#D32F2F`
- Border radius: 8px
- Padding: 10px 12px
- Font: 14px Regular
- Placeholder: `#9E9E9E`

### 4.4 Cards

- Background: `#FFFFFF`
- Border: 1px solid `#E0E0E0`
- Border radius: 8px
- Padding: 16px
- Shadow: `0 1px 3px rgba(0,0,0,0.08)`

### 4.5 Sidebar Navigation

- Background: `#2C2C2C`
- Width: 240px
- Item height: 44px
- Item padding: 0 16px
- Active item: left border 3px `#D32F2F`, background `#3A3A3A`
- Hover: background `#3A3A3A`
- Icon size: 20px, color `#9E9E9E` (active: `#FFFFFF`)
- Text: 14px Medium, `#FFFFFF`

### 4.6 Table

- Header: background `#E0E0E0`, text `#616161` 13px Semibold
- Row height: 48px
- Row hover: `#F5F5F5`
- Cell padding: 0 12px
- Row border: 1px solid `#F0F0F0`
- Pagination: bottom-center, 32px height buttons

### 4.7 Modals

- Overlay: `rgba(0,0,0,0.5)`
- Background: `#FFFFFF`
- Border radius: 12px
- Shadow: `0 4px 16px rgba(0,0,0,0.15)`
- Padding: 24px
- Max width: 560px

---

## 5. Screen Specifications

### 5.1 Listado de Alumnos
- **Layout:** Card containing search bar + table + pagination
- **Search bar:** top-left, icon prefix (🔍), placeholder "Nombre o DNI"
- **"Nuevo alumno" button:** top-right, Primary variant
- **Table columns:** Nombre | DNI | Membresía | Estado (badge) | Estado de cuota (badge)
- **Pagination:** bottom-center, shows page numbers

### 5.2 Detalle del Alumno
- **Layout:** Two-column within a card
- **Left column (60%):** Datos Personales
  - Avatar (64x64, circle, border-radius 50%)
  - Fields: Nombre, Apellido, DNI, Email, Teléfono, Fecha de nacimiento
  - "Editar datos" button (Secondary variant, full-width of column)
- **Right column (40%):** 3 stacked info cards
  - **Membresía Actual:** Tipo, Fecha inscripción, Fecha vencimiento, "Cambiar plan" (Primary button)
  - **Estado de Cuota:** Badge, Último pago, Monto, "Registrar pago" (Primary button), "Ver historial de pagos" (link)
  - **Estado del Alumno:** Badge, "Desactivar" (Danger button)
- **Back link:** top-left "← Volver al listado"

### 5.3 Gestión de Membresías
- **Layout:** Card with table
- **Table columns:** Nombre del plan | Días semanales | Precio | Acciones
- **Actions:** Editar (Ghost button), Eliminar (Ghost/Danger)
- **"Nueva membresía" button:** top-right, Primary

### 5.4 Registrar Pago (Modal)
- **Trigger:** desde Detalle del Alumno
- **Fields:** Monto (number input), Medio de pago (select: Efectivo / Transferencia), Fecha (date picker)
- **Actions:** "Registrar" (Primary), "Cancelar" (Ghost)

### 5.5 Historial de Pagos
- **Layout:** Card with table, accessed from Detalle del Alumno
- **Table columns:** Fecha | Monto | Medio de pago | Estado
- **Sorted:** most recent first

### 5.6 Login
- **Layout:** Centered card on gray background
- **Fields:** Email, Contraseña
- **Button:** "Iniciar sesión" (Primary, full-width)
- **Link:** "¿Olvidaste tu contraseña?"
- **Logo:** top-center, MogniSystem branding

### 5.7 Dashboard (Propietario)
- **Cards resumen:** Total alumnos activos, Cuotas pagadas del mes, Cuotas por vencer, Ingresos del mes
- **Each card:** Icon + number + label, white card on gray bg

---

## 6. Responsive Behavior

| Breakpoint | Behavior |
|---|---|
| ≥1200px | Full layout: sidebar + content |
| 992–1199px | Sidebar collapses to icons only (56px) |
| 768–991px | Sidebar hidden, hamburger menu. Detalle del alumno stacks vertically |
| <768px | Mobile: single column, cards stack, table becomes card list |

---

## 7. Iconography

- **Library:** Lucide React (consistent, open source)
- **Size:** 20px for nav, 16px inline
- **Color:** inherits from text color
- **Common icons:** Users, CreditCard, Calendar, Search, Plus, Edit, Trash2, ChevronLeft, LogOut, Menu, BarChart3

---

## 8. AI Prompt Template

Use this base prompt and append the specific screen name:

> Generate a high-fidelity UI mockup for MogniSystem gym management app. Use this design system: light gray page background (#F0F0F0), white cards, dark charcoal sidebar (#2C2C2C), bold red primary buttons (#D32F2F), green badges for active/paid states (#388E3C on #E8F5E9), amber badges for warnings (#F9A825 on #FFF8E1), red badges for errors. Inter font family. Clean, industrial, modern, high-contrast. Desktop resolution 1440x900.
>
> Screen: [SCREEN NAME AND DETAILS]

### Screen prompts to append:

**Listado de Alumnos:**
> Screen: "Listado de Alumnos". Dark sidebar on left with nav items (Alumnos highlighted with red left border, Membresías, Cuotas, Empleados, Horarios, Ejercicios). Main content: white card with search bar (placeholder "Nombre o DNI"), red "Nuevo alumno" button top-right. Table with columns: Nombre, DNI, Membresía, Estado (green "Activo" badge), Estado de cuota (green "Pagada" or amber "Próxima a vencer" badge). 10 rows of sample data. Pagination bottom-center.

**Detalle del Alumno:**
> Screen: "Detalle del Alumno". Back link "← Volver al listado" top-left. White card, two columns. Left: profile avatar (circle), personal data labels and values (Nombre, Apellido, DNI, Email, Teléfono, Fecha de nacimiento), "Editar datos" outlined red button. Right: three stacked mini-cards — "Membresía Actual" (tipo, fechas, red "Cambiar plan" button), "Estado de Cuota" (green "Pagada" badge, último pago date, monto, red "Registrar pago" button, blue "Ver historial" link), "Estado del Alumno" (green "Activo" badge, red "Desactivar" button).

**Login:**
> Screen: Login page. Centered white card on #F0F0F0 background. MogniSystem logo top-center (stylized "M" in red). Email input field, password input field with show/hide toggle, red full-width "Iniciar sesión" button. "¿Olvidaste tu contraseña?" link below. Clean and minimal.

**Dashboard Propietario:**
> Screen: Dashboard for gym owner. Dark sidebar on left. Main content: 4 summary cards in a row (Total alumnos activos, Cuotas pagadas, Cuotas por vencer, Ingresos del mes) each with icon, large number, and label. Below: a bar chart showing monthly income trend and a table of recent activity (últimos pagos registrados).

**Gestión de Membresías:**
> Screen: "Gestión de Membresías". Dark sidebar, Membresías nav item highlighted. White card with red "Nueva membresía" button top-right. Table: Nombre del plan, Días semanales, Precio, Acciones (edit icon, delete icon). 3-4 rows: "Plan Familiar 3 días", "Plan Familiar 4 días", "Pase Libre 5 días".

**Registrar Pago (Modal):**
> Screen: Modal overlay on Detalle del Alumno. Centered white modal card. Title "Registrar Pago". Fields: Monto (number input), Medio de pago (dropdown: Efectivo / Transferencia), Fecha (date picker). Two buttons bottom-right: gray "Cancelar", red "Registrar". Semi-transparent dark overlay behind.

**Historial de Pagos:**
> Screen: "Historial de Pagos de [Alumno]". Back link to detalle. White card with table: Fecha, Monto, Medio de pago, Estado (green "Pagada" badge). 6-8 rows sorted by most recent. No action buttons, read-only view.
