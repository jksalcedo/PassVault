<h1 align="center">
  <br>
    <img src="./metadata/en-US/images/icon.png" alt="PassVault icon" width="150" />
  <br>
  PassVault
  <br>
</h1>

<h4 align="center">A secure, lightweight, and offline password manager for Android built with
Kotlin.</h4>

<h4 align="center">
<img alt="GitHub Downloads (all assets, all releases)" src="https://img.shields.io/github/downloads/jksalcedo/PassVault/total?logo=GitHub">
<img alt="SourceForge Downloads" src="https://img.shields.io/sourceforge/dt/passvault-app?logo=SourceForge">
<img alt="GitHub License" src="https://img.shields.io/github/license/jksalcedo/PassVault">
<img alt="GitHub Release" src="https://img.shields.io/github/v/release/jksalcedo/PassVault?include_prereleases">
<img src="https://img.shields.io/badge/Kotlin-2.2.21-7f52ff?logo=kotlin&logoColor=white" alt="Kotlin Version">
  <img alt="GitHub repo size" src="https://img.shields.io/github/repo-size/jksalcedo/PassVault?logo=Android">

</h4>
<div align="center">
  <a href="https://github.com/jksalcedo/PassVault/releases">
    <img src="https://img.shields.io/badge/Get_it_on-GitHub-24292e?style=for-the-badge&logo=github&logoColor=white" alt="Get it on GitHub" />
  </a>

  <a href="https://sourceforge.net/projects/passvault-app/files/latest/download">
    <img src="https://img.shields.io/badge/Get_it_on-SourceForge-orange?style=for-the-badge&logo=sourceforge&logoColor=white" alt="Get it on SourceForge" />
  </a>

  <a href="https://f-droid.org/packages/com.jksalcedo.passvault/">
    <img src="https://img.shields.io/badge/Get_it_on-F--Droid-green?style=for-the-badge&logo=f-droid&logoColor=white" alt="Get it on F-Droid" />
  </a>

  <a href="https://t.me/passvault_app">
    <img src="https://img.shields.io/badge/Telegram-2CA5E0?style=for-the-badge&logo=telegram&logoColor=white" alt="Join us on Telegram" />
  </a>

[![IzzyOnDroid Shield](https://img.shields.io/endpoint?url=https://apt.izzysoft.de/fdroid/api/v1/shield/com.jksalcedo.passvault&label=IzzyOnDroid&style=for-the-badge)](https://apt.izzysoft.de/packages/com.jksalcedo.passvault)
</div>



<h1 align="center">
    <img src="./metadata/en-US/images/phoneScreenshots/1.jpg" alt="PassVault Screenshot" width="250" />
  <img src="./metadata/en-US/images/phoneScreenshots/2.jpg" alt="PassVault Screenshot" width="250" />
  <img src="./metadata/en-US/images/phoneScreenshots/3.jpg" alt="PassVault Screenshot" width="250" />
    <img src="./metadata/en-US/images/phoneScreenshots/6.jpg" alt="PassVault Screenshot" width="250" />
    <img src="./metadata/en-US/images/phoneScreenshots/4.jpg" alt="PassVault Screenshot" width="250" />
  <img src="./metadata/en-US/images/phoneScreenshots/5.jpg" alt="PassVault Screenshot" width="250" />
  <br>

  <br>
</h1>

PassVault is a secure, lightweight, and completely offline password manager for Android. It empowers
you to store and manage your credentials locally with military-grade encryption (AES-256), ensuring
your data never leaves your device.

Built with a focus on privacy and usability, PassVault features a modern Material Design interface,
robust import capabilities (seamlessly migrating from KeePass and Bitwarden), and advanced security
options like biometric login and encrypted backups. Whether you're organizing passwords with custom
categories or generating strong credentials, PassVault keeps your digital life secure and
accessible.

## Core Features

- [x] Secure PIN Authentication
- [x] Biometric (Fingerprint) Login
- [x] Encrypted Database (AES-256)
- [X] [Argon2Kt](https://github.com/lambdapioneer/argon2kt) for file encryption
- [x] Add, View, Edit, & Delete Passwords
- [x] In-app Password Generator
- [X] Encrypted Import/Export (for backups and transfer)
- [X] Encrypted Automatic Backups
- [X] Robust Import System (KeePass CSV/KDBX & Bitwarden JSON)
- [X] Modern Material Design UI
- [X] Categories/Labels
- [X] Password strength scoring
- [X] Custom field templates

## Planned Features

- [X] Secure Notes (for storing non-password secrets)
- [ ] Desktop Version
- [ ] QR Code Password Sharing
- [ ] TOTP generator
- [ ] Migrate to Compose

## Tech Stack & Architecture

This project follows the MVVM (Model-View-ViewModel) architecture.

- **Language**: Kotlin
- **UI**: XML Layouts with ViewBinding and Material Design Components
- **Database**: Room Persistence Library
- **Architecture**: ViewModel, Repository, LiveData
- **Security**: Android Keystore, AES/CBC/PKCS7 Encryption, Argon2

## Requirements

- Android 8.0 (Oreo) or higher

## Permissions

PassVault is designed to be a completely offline password manager. It does not require any
permissions that would allow it to connect to the internet.

- `android.permission.USE_BIOMETRIC` - Used to allow fingerprint authentication.
- `android.permission.READ_EXTERNAL_STORAGE` - Used to import encrypted backups.
- `android.permission.WRITE_EXTERNAL_STORAGE` - Used to export encrypted backups.
