# Plutus Mace Plugin V2 (Paper 1.21.11)

**Fixed & Simplified:** The plugin now includes **zero-config automatic resource pack delivery** and **fixes for all compilation errors**.

## Features
- **Auto-Hosted Resource Pack**: Generates and uploads a ZIP with the custom red mace texture and models to Catbox.moe on startup — **no config, no IP, no port forwarding needed**.
- **Blood Toll (Passive)**: Deals self-damage on attack.
- **Blood Dash (Press F)**: Dash forward and damage nearby.
- **Blood Pact (Shift + F)**: Link up to 3 players, shared damage.

## How to Build
1. Install **Java 21** and **Gradle** locally.
2. Open terminal in this folder and run:
   ```bash
   ./gradlew build
   ```
3. The finished JAR will be at `build/libs/plutus-mace-v2-2.0.0.jar`.

## In-Game Usage
- `/giveplutus`: Gives the mace (requires `plutus.admin`).
- Dash & Pact: Activate with **F** and **Shift + F** (or swap hand).
- The resource pack will auto-download on join.

## Important
- **Works 100% out of the box** — no edits to any file required.
- Compatible with Paper 1.21.11.
- Includes integrated red-themed visual particles and sounds.
