# PassVault

A secure, offline password manager for Android built with Kotlin and modern security practices.

PassVault is a lightweight password manager that allows users to securely store credentials on their device. All data is stored locally and sensitive information is encrypted using the Android Keystore system. The application does not require internet permissions. User data never leaves the device.

### Core Features
- [x] Secure PIN Authentication
- [x] Biometric (Fingerprint) Login
- [x] Encrypted Database (AES-256)
- [x] Add New Passwords
- [x] View Passwords
- [x] In-app Password Generator

### Planned Features
- [X] **Edit** existing password entries
- [ ] **Delete** existing password entries
- [X] "Copy to Clipboard" button for password generator
- [ ] Secure Notes (for storing non-password secrets)
- [ ] Encrypted Import/Export (for backups)
- [ ] Desktop Version 


### Tech Stack & Architecture

This project follows the MVVM (Model-View-ViewModel) architecture.

-    **Language**: Kotlin
 -   **UI**: XML Layouts with ViewBinding and Material Design Components
-    **Database**: Room Persistence Library
 -   **Architecture**: ViewModel, Repository, LiveData
 -   **Security**: Android Keystore, AES/CBC/PKCS7 Encryption
