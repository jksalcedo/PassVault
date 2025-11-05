<h1 align="center">
  <br>
  <a href="http://www.amitmerchant.com/electron-markdownify"><img src="https://raw.githubusercontent.com/amitmerchant1990/electron-markdownify/master/app/img/markdownify.png" alt="Markdownify" width="200"></a>
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
<img src="https://img.shields.io/badge/Kotlin-2.0.21-7f52ff?logo=kotlin&logoColor=white">
</h4>

PassVault is a lightweight password manager that allows users to securely store credentials on their device. All data is stored locally and sensitive information is encrypted using the Android Keystore system. The application does not require internet permissions. User data never leaves the device.

## Core Features
- [x] Secure PIN Authentication
- [x] Biometric (Fingerprint) Login
- [x] Encrypted Database (AES-256)
- [x] Add New Passwords
- [x] View Passwords
- [x] In-app Password Generator

## Planned Features
- [X] **Edit** existing password entries
- [ ] **Delete** existing password entries
- [X] "Copy to Clipboard" button for password generator
- [ ] Secure Notes (for storing non-password secrets)
- [ ] Encrypted Import/Export (for backups)
- [ ] Desktop Version

## Download
Download at SourceForge or from ![GitHub Releases](https://github.com/jksalcedo/PassVault/releases) <br>
[![Download PassVault](https://a.fsdn.com/con/app/sf-download-button)](https://sourceforge.net/projects/passvault-app/files/latest/download)


## Tech Stack & Architecture

This project follows the MVVM (Model-View-ViewModel) architecture.

-    **Language**: Kotlin
 -   **UI**: XML Layouts with ViewBinding and Material Design Components
-    **Database**: Room Persistence Library
 -   **Architecture**: ViewModel, Repository, LiveData
 -   **Security**: Android Keystore, AES/CBC/PKCS7 Encryption
