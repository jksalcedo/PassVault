## [unreleased]

### 🚀 Features

- Add option to print vault entries via native PrintManager and WebView
- *(settings)* Enhance print export with timestamped filename, metadata, and modernized HTML
  encoding
- Add biometric authentication for password visibility in edit mode

### 🐛 Bug Fixes

- Non-persistent category tabs upon device rotation

### ⚙️ Miscellaneous Tasks

- *(l10n)* Add Estonian translation
- *(l10n)* Update Estonian translation
- *(l10n)* Update Estonian translation
- *(l10n)* Update Estonian translation

## [2.2.2] - 2026-09-21

### 🚀 Features

- Add delete all trashed entries functionality

### 🐛 Bug Fixes

- Initialize default categories only on first launch
- Apply status bar insets to selectionBar when bottom app bar is enabled (fixes #244)
- Preserve selection mode and sub-fragment state on screen rotation (fixes #245)

### 📚 Documentation

- Update README with Codeberg Translate badge
- Add changelog for release 44 (2.2.2)

### ⚙️ Miscellaneous Tasks

- Fix SDK manager tools error by accepting SDK licenses directly
- Restore setup-android action and specify sdkmanager path explicitly
- Fix setup-android trying to install deprecated 'tools' package by providing empty 'packages'
  argument

## [2.2.1] - 2026-08-14

### 🚀 Features

- *(autofill)* Derive entry title from app package label

### 💼 Other

- Bump minSdk to 28 and update version to 2.2.1

## [2.2.0] - 2026-07-26

### 🚀 Features

- Integrate Crowdin for translations and update README
- Add multi-select to Password Health screen and main list
- Replace entry icons with title-based colored monograms
- Add KDBX export support

### 🐛 Bug Fixes

- *(ci)* Enable APK Signature Scheme v3 and add v42 changelog
- Support subfolders, correct password decryption, group categories, custom fields
- Wrong param name

### ⚙️ Miscellaneous Tasks

- *(workflow)* Refactor Android release to use apksigner and signature lineage
- *(workflow)* Refactor Android release to use apksigner and signature lineage
- *(workflow)* Refactor Android release to use apksigner and signature lineage
- Add min SDK version 26 to release signing
- *(ci)* Add multi-signer support for APKs
- Upgrade targetSdk to 37 and remove developer information
- Add rotation min sdk version 28 to signer

## [2.0.2] - 2026-07-11

### 🐛 Bug Fixes

- Bitwarden import issue

### ⚙️ Miscellaneous Tasks

- *(release)* Add workflow to mirror GitHub releases to Codeberg
- *(release)* Add workflow to mirror GitHub releases to Codeberg
## [2.0.0] - 2026-06-15

### 🚀 Features

- *(ui)* Add OLED pure black theme support
- *(ui)* Add restart dialog for OLED theme changes
- Implement Secure Notes feature
- *(ui)* Update entry item layout and add database schema version 6
- Implement Recycle Bin feature (rebased)
- Implement Recycle Bin feature
- *(db)* Add database schema version 7
- Implement Password Health Audit feature (rebased onto master)
- Implement Password Health Audit feature
- Add progress indicator and background loading to Health Audit
- Bump version to 2.0.0 and update build tools

### 🐛 Bug Fixes

- Add default value for type column to support auto-migration
- Update health audit logic to only process active password entries
- Remove isDeleted check to fix build (field not in master yet)
- Update health audit logic to exclude deleted entries and secure notes
- *(health)* Refine audit thresholds and header descriptions
- *(health)* Force headers to show in audit screen

### ⚡ Performance

- Optimize health audit UI with ConcatAdapter to prevent ANR

### ⚙️ Miscellaneous Tasks

- Prepare release 2.0.0

## [1.9.2] - 2026-04-29

### 🚀 Features

- *(ui)* Update Material 3 themes and fix bottom bar layout obstruction
- *(ui)* Add copy to clipboard functionality for notes

### 🐛 Bug Fixes

- *(ui)* Prevent BottomAppBar from obstructing the last list item

### 🎨 Styling

- *(ui)* Comment out custom BottomAppBar styles

## [1.9.1] - 2026-03-09

### 🚀 Features

- *(settings)* Create missing categories during data import
- *(utils)* Map custom fields when converting password entry to record

### 🐛 Bug Fixes

- Preserve custom categories and fields on backup import and bump version to 1.9.1

## [1.9.0] - 2026-02-23

### 🚀 Features

- *(auth)* Handle invalidated keystore and add app reset flow
- Add progress tracking to import and export operations
- *(ui)* Add first-launch dialog with backup recommendations
- Add progress indicators for export and import processes
- *(autofill)* Implement save request and pre-fill AddEditActivity

### 📚 Documentation

- Update changelog for version 35

### 🚜 Refactor

- Inherit from BaseActivity and centralize session lock logic

### 💼 Other

- Update AGP to 9.0.1 and Gradle to 9.2.1
## [1.9.0-beta1] - 2026-02-11

### 🚀 Features

- Implement Android Autofill service for credential suggestions
- Improve autofill field detection and entry matching logic
- Improve autofill field detection and entry matching logic

### ⚙️ Miscellaneous Tasks

- Remove redundant release notes initialization from android-release workflow

## [1.8.0] - 2026-01-31

### 🚀 Features

- *(ui)* Configure BottomAppBar cradle and fix FAB anchoring appearance
- Add change master password functionality and allow alphanumeric pins
- Add change master password functionality and allow alphanumeric pins

## [1.7.1] - 2026-01-18

### 🚀 Features

- *(auth)* Implement configurable PIN lockout after failed attempts
- *(ui)* Improve BottomAppBar edge-to-edge layout and search behavior

### 🚜 Refactor

- *(ui)* Migrate SearchView to androidx and update menu properties
- *(ui)* Import PreferenceRepository in UnlockActivity

### 🎨 Styling

- *(ui)* Set windowSoftInputMode and fix BottomAppBar height

### ⚙️ Miscellaneous Tasks

- Bump version to 1.7.1 and remove emojis from release notes
- Bump versionCode to 32 and add changelog for v1.7.1

## [1.7.0] - 2026-01-13

### 🚀 Features

- Implement app shortcuts for adding and generating passwords
- Implement app shortcuts for adding and generating passwords

### 🎨 Styling

- *(ui)* Set windowSoftInputMode and fix BottomAppBar height

### ⚙️ Miscellaneous Tasks

- Categorize release notes by features and fixes in android workflow
- Update README.md

## [1.6.1] - 2026-01-09

### ⚙️ Miscellaneous Tasks

- Implement categorized release notes and remove network permissions
- Implement categorized release notes and remove network permissions

## [1.6.0] - 2026-01-06

### 🚀 Features

- *(ui)* Add illustrations to empty and no-results screens
- *(ui)* Add illustrations to empty and no-results screens
- *(ui)* Add illustrations to empty and no-results screens

### ⚙️ Miscellaneous Tasks

- Upgrade Gradle wrapper to 9.2.0
- Automate release notes generation and switch to gh cli
- Improve release workflow with caching and retries
- *(ci)* Upgrade android-release workflow to ubuntu-24.04
- Upgrade gradle to 9.2 and enable build caching
## [1.6.0-beta1] - 2026-01-03

### 🚀 Features

- *(entries)* Implement custom fields for password entries
- *(entries)* Implement custom fields for password entries
## [1.5.0] - 2026-01-02

### ⚙️ Miscellaneous Tasks

- *(release)* Prepare for version 1.5.0
## [1.5.0-beta3] - 2026-01-02

### 🚀 Features

- *(crash)* Implement crash reporting and log sharing
- *(crash)* Implement crash reporting and log sharing

## [1.5.0-beta2] - 2026-01-01

### 🚀 Features

- *(backup)* Implement configurable backup filename
## [1.5.0-beta1] - 2025-12-29

### 🚀 Features

- *(security)* Implement configurable auto-lock timeout

### 💼 Other

- Improve search functionality and fix UI issues
- Improve search functionality and fix UI issues
## [1.3.1] - 2025-12-18

### 🐛 Bug Fixes

- *(various)* Correct FAB shape, entry editing, and unit tests
- *(various)* Correct FAB shape, entry editing, and unit tests

### 🚜 Refactor

- *(deps)* Replace OSS Licenses plugin with AboutLibraries
## [1.3.0] - 2025-12-15

### 🚀 Features

- *(category)* Remove `isDefault` flag and use hex colors
- *(category)* Remove `isDefault` flag and use hex colors

### 🚜 Refactor

- *(UI)* Improve contrast and update icons
- *(UI/UX)* Improve UI components and user experience
## [1.2.1] - 2025-12-12

### 🐛 Bug Fixes

- *(UI)* Correct FAB position and fix settings crash
## [1.2.0] - 2025-12-12

### 🚀 Features

- *(UI)* Refactor Chip creation and update styling
- *(settings)* Add setting for number of backup copies
- *(import)* Introduce detailed import status report
- *(UI)* Introduce optional Bottom App Bar
- *(UI)* Implement Theme and Dynamic Color options
- *(UI)* Implement Theme and Dynamic Color options

### 🚜 Refactor

- Relocate enums to a dedicated package
- *(UI)* Convert Manage Categories Dialog to BottomSheet
- *(Settings)* Reorganize settings into categories
## [1.1.0] - 2025-12-09

### 🚀 Features

- Implement custom category management
- Add option to block screenshots
- Add custom backup location and retention policy
- Refactor backup file saving to use ACTION_CREATE_DOCUMENT
- Add backup copies setting and refactor code
- Add backup copies setting and refactor code

### 🚜 Refactor

- Make only title field mandatory

### ⚙️ Miscellaneous Tasks

- Update database schema to version 3
## [1.0.0] - 2025-12-08

### 🚀 Features

- Add URL field to password entries
- Pre-fill email and URL fields on edit
- Add email and URL validation
- Add button to open URL in browser
- Add category filter functionality
- Add category filter functionality
- Add URL field, category filter, and other enhancements
- Add URL field, category filter, and other enhancements
- Add password strength analyzer
- Improve export/import robustness and add Keystore validation
- Add sorting functionality for password entries
- Add sorting functionality for password entries

### 🚜 Refactor

- Centralize category color logic

### 🎨 Styling

- Update Store screenshot
- Update Store screenshot

### 💼 Other

- Remove leading space in EXTRA_ENTRY constant
## [1.0.0-beta01] - 2025-12-07

### 🚀 Features

- Add categories and enhance entry fields
- Add categories and enhance entry fields
## [0.9.1] - 2025-12-02

### 🚀 Features

- Prevent auto-lock on first launch and when resuming

### 🐛 Bug Fixes

- Prevent UnlockActivity from showing on a finishing activity
- Improve null safety and handling of empty data
- Improve null safety and handling of empty data

### 🚜 Refactor

- Remove automatic last backup time update
- Improve password generator dialog behavior

### ⚙️ Miscellaneous Tasks

- Fix typo in UnlockActivity filename
## [0.9.0] - 2025-12-02

### 🚀 Features

- Add search functionality
- Add password generator to main screen
## [0.8.0] - 2025-11-30

### 🚀 Features

- Add warning dialog for unencrypted export
- Add warning dialog for unencrypted export

### 🐛 Bug Fixes

- Allow importing entries with empty passwords
## [0.8.0-beta02] - 2025-11-27

### 🚀 Features

- Refactor import/export to use plain text passwords

### 📚 Documentation

- Add KDoc comments to classes and public methods

### ⚙️ Miscellaneous Tasks

- Simplify APK artifact path in release workflow
- Simplify APK artifact path in release workflow
## [0.8.0-beta01] - 2025-11-25

### 🚀 Features

- Update README with additional sections
- Enhance backup encryption with Argon2 and refactor restart logic
- Enhance backup encryption with Argon2 and refactor restart logic
- Enhance backup encryption and refactor restart logic
## [0.7.1] - 2025-11-21

### 🚜 Refactor

- Remove unnecessary `@RequiresApi` annotations and add compatibility checks
- Replace launcher icons with adaptive vector drawables
- Replace launcher icons with adaptive vector drawables
- Allow format override during import
- Enhance serialization, timestamp format, and importer logic
- Enhance import logic and replace launcher icons

### 💼 Other

- Configure ABI splits and update APK naming convention
- Configure ABI splits and update APK naming convention
## [0.7.0] - 2025-11-20

### 🚀 Features

- Enable manual release workflow and mark all releases as pre-releases

### 🚜 Refactor

- Enhance import functionality and error handling
- Enhance import functionality and error handling
- Simplify and improve import process

### ⚙️ Miscellaneous Tasks

- Update CHANGELOG for version 0.7.0
## [0.7.0-beta01] - 2025-11-20

### 🚀 Features

- Add support for importing Bitwarden JSON exports
- Add KeePass DX/XC CSV and KDBX import functionality
- Add Bitwarden and KeePass import functionality

### 📚 Documentation

- Update app store metadata with new features

### 🚜 Refactor

- Allow device credential authentication for encryption key
- Enhance import/export functionality and UI state management

### ⚙️ Miscellaneous Tasks

- Update IDE configurations and build script
- Update dependencies and clean up IDE configurations
## [0.6.0] - 2025-11-18

### 🚀 Features

- Add new screenshot for app listing

### 📚 Documentation

- Update changelog for version 0.6.0-beta2

### 🚜 Refactor

- Simplify date format and respect user locale
- Broaden biometric support and improve database stability
- Allow device credential as an authentication method

### ⚙️ Miscellaneous Tasks

- Bump version to 0.6.0
## [0.6.0-beta2] - 2025-11-18

### 🚜 Refactor

- Replace SetPinDialog with SetPinFragment for a full-screen UI
- Improve Password Generator dialog and SharedPreferences usage
- Improve password dialogs and replace AlertDialog

### 💼 Other

- V0.6.0-beta2
## [0.6.0-beta] - 2025-11-17

### 🚀 Features

- Add changelog and git-cliff config
- Update dependencies
- Enable R8 full mode and add dependency analysis plugin
- Add BackupWorker for automatic data backup
- Add Toolbar to Settings screen
- Encrypt automatic backups
- Add backup management screen
- Implement backup file sharing and deletion
- Add backup deletion and refactor BackupWorker
- Set entry title in Toolbar and update layout
- Add automatic app lock on inactivity
- Require password for import/export operations
- Enable ProGuard and add new dependencies
- Add changelog and git-cliff config
- Introduce automatic backups, security enhancements, and UI improvements

### 🚜 Refactor

- Remove unused TestWorkerFactory

### 💼 Other

- Convert launcher icons to WebP and remove adaptive icon XML
- Remove unused resources
- Use withText instead of withId for button selection in tests
- Introduce repositories for data abstraction
- Use PreferenceRepository for settings
- Set correct title for AboutFragment
- Correct ActionBar title in AboutFragment
- Move PVAdapter to a new package
- Adjust padding and add dividers to entry view
- Remove toast messages from backup file copy operation
- Use `onSupportNavigateUp` for back navigation
- Include time in formatted date string
- Lazily initialize repositories in Application class
- Use `getPasswordForAutoBackups` in `BackupWorker`
- Prevent NullPointerException in BackupAdapter and disable androidx-startup
## [0.5.0] - 2025-11-15

### 🚀 Features

- *(security)* Disable automatic backup and data extraction
- Introduce multiple enhancements and refactors

### 💼 Other

- Add Toolbar and Up Navigation to Add/Edit screen
- Simplify ViewEntryActivity and layout
## [0.4-beta] - 2025-11-08

### 🚀 Features

- Add new features, improvements, and bug fixes for v5

### 💼 Other

- Add database size utility functions
- Rename APK artifact and update version
## [0.4-alpha] - 2025-11-07

### 🚀 Features

- Implement encrypted data import/export
- Add backup/restore and settings screen

### 💼 Other

- Rename "Passkey" to "Password" for clarity

## [0.3-alpha] - 2025-11-06

### 💼 Other

- Implement FAB speed dial in ViewEntryActivity
- Add delete functionality and confirmation dialog
- Correct FAB layout and add content descriptions
- Correct FAB layout and add content descriptions
## [0.2-alpha] - 2025-11-04

### 💼 Other

- Rename AddEditActivity to AddActivity and move package
- Rename `AddActivity` to `AddEditActivity` and move files
- Implement comprehensive testing and improve Add/Edit functionality
- Update variable and preference key names for clarity
## [0.1-alpha] - 2025-11-02
