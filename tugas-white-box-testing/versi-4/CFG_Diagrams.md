# Versi 4 — CFG Diagrams

Diagram berikut menggunakan Mermaid sehingga dapat dirender sebagai gambar/diagram pada GitHub.

## 1. Persamaan Kuadrat

```mermaid
flowchart TD
 A[Start] --> B{a == 0?}
 B -- Yes --> C{b == 0?}
 B -- No --> D[Hitung D = b² - 4ac]
 C -- Yes --> E{c == 0?}
 C -- No --> F[Solusi linear]
 E -- Yes --> G[Infinite solutions]
 E -- No --> H[No solution]
 D --> I{D < 0?}
 I -- Yes --> H
 I -- No --> J{D == 0?}
 J -- Yes --> K[Double root]
 J -- No --> L[Two real roots]
```

Decision-level V(G) = 6.

## 2. Spring Owner.getPet()

```mermaid
flowchart TD
 A[Normalize name] --> B{Pet tersedia?}
 B -- No --> G[return null]
 B -- Yes --> C{!ignoreNew || !pet.isNew()?}
 C -- No --> B
 C -- Yes --> D[Get and normalize pet name]
 D --> E{compName equals name?}
 E -- Yes --> F[return pet]
 E -- No --> B
```

Decision-level V(G) = 4.

Catatan: operator 