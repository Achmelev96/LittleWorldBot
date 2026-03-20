# 🎧 LittleWorldBot

A multifunctional Discord bot featuring music playback, queue control, autocomplete suggestions, and a clean modular command architecture.  
Built with **Java 25+, JDA, and LavaPlayer**.

---

## 🌟 Features

### 🎵 Music Commands
- `/play` — plays a url, track or playlist
- `/skip` — skips the current track
- `/leave` — disconnects the bot
- Currently supports **YouTube** as the audio source

### 🤖 Command Architecture
- Commands split into separate **handler classes**
- Unified `SlashCommand` interface
- Easily extendable with custom commands

### ⚙️ Autocomplete
- Suggests query options in real time
- Flexible system based on `AutocompleteProvider`

### 🔊 Advanced Audio System
- Custom `GuildHandler` for per-guild audio management
- Automatic AFK disconnect logic

### 🧩 Modular Design
- Centralized `MusicCore` singleton
- Clean package structure
- Easy to maintain and expand

---

## 🚀 Installation & Setup

### 1. Requirements
- **Java 25+**
- **Maven**
- **Discord Bot Token**

### 2. Configuration

Create a file in the project root:

config.properties

DISCORD_TOKEN=your_token_here


---

## 📜 License

This project is licensed under the **MIT License**.  
You are free to use, modify, and distribute this project with attribution.

MIT License © 2025 Achmelev96

