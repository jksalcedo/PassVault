# PassVault

A secure, offline password manager for Android built with Kotlin and modern security practices.

PassVault is a lightweight password manager that allows users to securely store credentials on their device. All data is stored locally and sensitive information is encrypted using the Android Keystore system. The application does not require internet permissions, ensuring user data never leaves the device.

### Core Features

-    **Secure Authentication**: Access your vault using a PIN or Biometric (fingerprint) authentication.

-    **Encrypted Storage**: All passwords are encrypted at rest using AES-256 and stored securely in a local Room database. Encryption keys are managed by the Android Keystore, providing hardware-backed security.

-    **Full CRUD Operations**: Add, view, edit, and delete password entries through a clean and simple user interface.

-    **Password Generation**: Create strong, random passwords directly within the app.

-    **Offline-First**: The app is fully functional without an internet connection.

### Tech Stack & Architecture

This project follows the MVVM (Model-View-ViewModel) architecture to ensure a clean separation of concerns and a scalable codebase.

-    **Language**: Kotlin

 -   **UI**: XML Layouts with ViewBinding and Material Design Components

-    **Database**: Room Persistence Library

 -   **Architecture**: ViewModel, Repository, LiveData

 -   **Security**: Android Keystore, AES/CBC/PKCS7 Encryption

-   **Core Components**: RecyclerView, BiometricPrompt

### Project Structure

The codebase is organized by layer into packages such as data for the Room database and repository, ui for activities and adapters, and viewmodel for business logic. The core encryption logic is encapsulated in a CryptoHelper utility.

### Future Enhancements

-    Encrypted database export and import

-    Password strength analysis

-    Search and filtering capabilities

-    Auto-lock timer on inactivity

-    Dark theme support
