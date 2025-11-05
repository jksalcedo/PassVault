<h1 align="center">
  <br>
    <img src="./metadata/en-US/images/icon.png" alt="PassVault icon" width="150" />
  <br>
  PassVault
  <br>
</h1>

<h4 align="center">A secure, offline password manager for Android built with Kotlin.</h4>

<h4 align="center">
<img alt="GitHub Downloads (all assets, all releases)" src="https://img.shields.io/github/downloads/jksalcedo/PassVault/total?logo=GitHub">
<img alt="SourceForge Downloads" src="https://img.shields.io/sourceforge/dt/passvault-app?logo=SourceForge">
<img alt="GitHub License" src="https://img.shields.io/github/license/jksalcedo/PassVault">
<img alt="GitHub Release" src="https://img.shields.io/github/v/release/jksalcedo/PassVault">
<img src="https://img.shields.io/badge/Kotlin-2.0.21-7f52ff?logo=kotlin&logoColor=white" alt="Kotlin Version">
</h4>

<h1 align="center">
    <img src="./metadata/en-US/images/phoneScreenshots/1.jpg" alt="PassVault Screenshot" width="250" />
  <img src="./metadata/en-US/images/phoneScreenshots/2.jpg" alt="PassVault Screenshot" width="250" />
  <img src="./metadata/en-US/images/phoneScreenshots/3.jpg" alt="PassVault Screenshot" width="250" />
  <br>
  
  <br>
</h1>

PassVault is a lightweight password manager that allows users to securely store credentials on their
device. All data is stored locally and sensitive information is encrypted using the Android Keystore
system. The application does not require internet permissions. User data never leaves the device.

## Core Features

- [x] Secure PIN Authentication
- [x] Biometric (Fingerprint) Login
- [x] Encrypted Database (AES-256)
- [x] Add New Passwords
- [x] View Passwords
- [x] In-app Password Generator

## Planned Features

- [X] **Edit** existing password entries
- [X] **Delete** existing password entries
- [X] "Copy to Clipboard" button for password generator
- [ ] Secure Notes (for storing non-password secrets)
- [ ] Encrypted Import/Export (for backups)
- [ ] Desktop Version

## Download

Download at SourceForge or
from ![GitHub Releases](https://github.com/jksalcedo/PassVault/releases) <br>
[![Download PassVault](https://a.fsdn.com/con/app/sf-download-button)](https://sourceforge.net/projects/passvault-app/files/latest/download)

## Tech Stack & Architecture

This project follows the MVVM (Model-View-ViewModel) architecture.

- **Language**: Kotlin
- **UI**: XML Layouts with ViewBinding and Material Design Components
- **Database**: Room Persistence Library
- **Architecture**: ViewModel, Repository, LiveData
- **Security**: Android Keystore, AES/CBC/PKCS7 Encryption
