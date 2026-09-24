<div align="center">

# 📒 Pattubook

### Your personal lending ledger.

A private, offline-first Android app for keeping track of money you've given, received back, and still have outstanding.

<br>

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Offline First](https://img.shields.io/badge/Offline--First-Yes-8B5CF6?style=for-the-badge)

</div>

---

## ✦ About

**Pattubook** is a personal money-lending ledger built to make tracking informal loans simple.

Instead of trying to remember who owes what, Pattubook keeps a complete local record of:

- 💸 Money given
- ↩️ Money returned
- 🧮 Outstanding balances
- 📝 Transaction notes
- 🗓️ Custom transaction dates
- 👤 Person-specific history

Everything is stored locally on the device.

**No accounts. No cloud. No unnecessary complexity.**

---

## ✨ Features

### 💰 Lending Ledger
Track every amount you've given or received back.

### 👤 People
Keep separate financial records for each person, including:

- Total given
- Total returned
- Outstanding balance
- Complete transaction history

### 📊 Outstanding Overview
See your total outstanding balance at a glance and break it down person-by-person.

### ✏️ Transaction Editing
Correct an existing transaction without deleting and recreating it.

Edit:

- Amount
- Date & time
- Note

The original transaction identity is preserved.

### 👆 Swipe to Delete
Swipe a transaction left to reveal the delete action.

- Less than or equal to 70% → snaps back
- More than 70% → deletes
- No confirmation popup
- Snackbar provides Undo

### ↩️ Undo & Redo
Accidentally changed something?

Pattubook supports undo and a single-level redo for recent transaction actions.

### 🗑️ Recycle Bin
Deleted people and transactions aren't immediately destroyed.

Restore them later or permanently delete them from the Recycle Bin.

### 🙈 Hide People
Hide people from the main overview without deleting their financial history.

### 🔐 App Lock
Optional device authentication using Android's biometric/device credential system.

Your financial information can stay behind the device's existing security.

### 💾 Backup & Restore
Create a complete local backup and restore it later.

Backups preserve:

- People
- Transactions
- Dates
- Notes
- Hidden state
- Deleted state
- Original IDs

Restore operations are validated before modifying existing data.

---

## 🎨 Design

Pattubook uses a dark, minimal visual language focused on readability and hierarchy.

- Deep charcoal surfaces
- Burgundy/maroon accents
- Soft rounded cards
- Large financial figures
- Subtle gradients
- Minimal iconography
- Floating navigation
- Instant screen transitions

The interface is intentionally designed to feel more like a focused personal finance tool than a generic Material demo.

---

## 🛠️ Built With

| Technology | Purpose |
|---|---|
| **Kotlin** | Application language |
| **Jetpack Compose** | UI |
| **Material 3** | UI foundation |
| **Room** | Local database |
| **Navigation Compose** | Screen navigation |
| **AndroidX Biometric** | App Lock |
| **SharedPreferences / DataStore** | Local preferences |
| **Android Storage Access Framework** | Backup & restore |

---

## 🏗️ Architecture

Pattubook follows a simple offline-first architecture:

```text
┌─────────────────────┐
│     Compose UI      │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│      ViewModel      │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│     Repository      │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│    Room Database    │
└─────────────────────┘
