---
title: "Manual de Identidad Visual"
subtitle: "TuHospedaje"
author: ["Equipo de Desarrollo e Identidad"]
date: "Mayo 2026"
geometry: margin=2.5cm
fontsize: 11pt
linestretch: 1.25
header-includes:
  - \usepackage{fancyhdr}
  - \pagestyle{fancy}
  - \fancyfoot[CO,CE]{Documento generado en Mayo 2026 — TuHospedaje}
  - \fancyfoot[LE,RO]{\thepage}
  - \fancyhead[LO,LE]{Manual de Identidad Visual}
  - \fancyhead[RO,RE]{}
---

# 1. Marca

**Nombre:** TuHospedaje

**Descripción:** Plataforma web centralizada de reservas de alojamientos turísticos. El nombre comunica cercanía y pertenencia, invitando al usuario a sentirse como en casa.

\newpage

# 2. Logotipos

La marca cuenta con cuatro variantes para distintos contextos de uso.

### 2.1. Isologotipo (icono + texto horizontal)
Uso principal en header, footer y firmas institucionales.

![Isologotipo](imagenes/TuHospedaje_Isologotipo.png){ width=70% }

\vspace{1cm}

### 2.2. Logotipo (texto únicamente)
Uso en contextos donde el icono no sea necesario (documentos, formularios).

![Logotipo](imagenes/TuHospedaje_Logotipo.png){ width=50% }

\newpage

### 2.3. Isotipo (icono únicamente)
Uso en espacios reducidos: favicon, avatar, app mobile.

![Isotipo](imagenes/TuHospedaje_Isotipo.png){ width=30% }

\vspace{1cm}

### 2.4. Imagotipo (icono + texto apilado)
Uso en vertical: banners, presentaciones, mockups.

![Imagotipo](imagenes/TuHospedaje_Imagotipo.png){ width=40% }

\newpage

# 3. Paleta de Colores

Para garantizar la consistencia visual, se definen los siguientes valores cromáticos:

| Rol | Color | Código Hex | Muestra |
|:---|:---|:---|:---:|
| **Primary** (acciones, botones) | Naranja | `#ff6b35` | ![■](https://placehold.co/20x20/ff6b35/ff6b35) |
| **Secondary** (nav, header, footer) | Verde oscuro | `#264653` | ![■](https://placehold.co/20x20/264653/264653) |
| **Accent** (destacados, badges) | Verde agua | `#2a9d8f` | ![■](https://placehold.co/20x20/2a9d8f/2a9d8f) |
| **Background** (fondo principal) | Gris claro | `#f4f4f9` | ![■](https://placehold.co/20x20/f4f4f9/f4f4f9) |
| **Text** (texto principal) | Gris oscuro | `#333333` | ![■](https://placehold.co/20x20/333333/333333) |

> **Versión oscura:** La paleta incluye valores para tema oscuro (disponible en `data-theme="dark"`). Su implementación en la UI está planificada para Sprints futuros.

*Referencia visual completa adjunta en el archivo adjunto: `Paleta_de_colores_TuHospedaje.pdf`*

\vspace{1cm}

# 4. Tipografía

**Familia principal:** `Segoe UI`  
**Fallback:** Tahoma, Geneva, Verdana, sans-serif

La tipografía se hereda directamente del sistema operativo del cliente para garantizar un renderizado óptimo y rápido sin depender de la carga externa de fuentes tipográficas.

\newpage

# 5. Usos Correctos e Incorrectos

### 5.1. Espacio de seguridad
Mantener un área de respeto alrededor del isologotipo equivalente al alto de la letra **"T"** de *TuHospedaje*. No se deben ubicar elementos gráficos, bordes ni bloques de texto dentro de esta zona delimitada.

### 5.2. Usos Correctos
* Usar el isologotipo sobre fondo blanco (`#f4f4f9`) o secundario (`#264653`).
* Usar la versión en una sola pieza (no separar el icono del bloque de texto bajo ningún contexto en el Sprint 1).

### 5.3. Usos Incorrectos
* Deformar, estirar o alterar las proporciones del logotipo.
* Cambiar o sustituir los colores corporativos originales.
* Rotar o inclinar el logotipo.
* Aplicar sombras paralelas, biseles o efectos visuales no contemplados en este manual.
* Colocar el logotipo sobre fondos complejos o con bajo nivel de contraste.

\newpage

# 6. Especificaciones Técnicas de Recursos

A continuación se detallan las rutas relativas de los activos digitales dentro del monorrepositorio:

| Recurso Estructural | Ubicación del Archivo fuente |
|:---|:---|
| **Isologotipo** | `imagenes/TuHospedaje_Isologotipo.png` |
| **Logotipo** | `imagenes/TuHospedaje_Logotipo.png` |
| **Isotipo** | `imagenes/TuHospedaje_Isotipo.png` |
| **Imagotipo** | `imagenes/TuHospedaje_Imagotipo.png` |
| **Paleta de colores** | `Paleta_de_colores_TuHospedaje.pdf` |
