# Implementation Plan: Full Dutch Translation

This plan outlines the steps to fully translate the PassVault app into Dutch. This involves externalizing hardcoded strings from layouts, preference XMLs, menus, and Kotlin code, and then providing the corresponding Dutch translations.

## Proposed Changes

### [Component Name]

#### [MODIFY] [strings.xml](file:///C:/Users/Rudi/Development/PassVault/app/src/main/res/values/strings.xml)
- Add all missing strings that were hardcoded in XML and Kotlin files.

#### [MODIFY] [strings.xml](file:///C:/Users/Rudi/Development/PassVault/app/src/main/res/values-nl/strings.xml)
- Add Dutch translations for all existing and new strings.
- Fix the copyright symbol warning.

#### [MODIFY] [settings_root.xml](file:///C:/Users/Rudi/Development/PassVault/app/src/main/res/xml/settings_root.xml)
- Use string resources instead of hardcoded titles.

#### [MODIFY] [settings_display.xml](file:///C:/Users/Rudi/Development/PassVault/app/src/main/res/xml/settings_display.xml)
- Use string resources instead of hardcoded titles and summaries.

#### [MODIFY] [settings_security.xml](file:///C:/Users/Rudi/Development/PassVault/app/src/main/res/xml/settings_security.xml)
- Use string resources instead of hardcoded titles and summaries.

#### [MODIFY] [settings_data_sync.xml](file:///C:/Users/Rudi/Development/PassVault/app/src/main/res/xml/settings_data_sync.xml)
- Use string resources instead of hardcoded titles and summaries.

#### [MODIFY] [settings_about.xml](file:///C:/Users/Rudi/Development/PassVault/app/src/main/res/xml/settings_about.xml)
- Use string resources instead of hardcoded titles and summaries.

#### [MODIFY] [fragment_about.xml](file:///C:/Users/Rudi/Development/PassVault/app/src/main/res/layout/fragment_about.xml)
- Use string resources instead of hardcoded text.

#### [MODIFY] [dialog_import.xml](file:///C:/Users/Rudi/Development/PassVault/app/src/main/res/layout/dialog_import.xml)
- Use string resources for "Proceed" and "Importing...".

#### [MODIFY] [dialog_backup_filename.xml](file:///C:/Users/Rudi/Development/PassVault/app/src/main/res/layout/dialog_backup_filename.xml)
- Use string resources for all hardcoded text.

#### [MODIFY] [dialog_color_picker.xml](file:///C:/Users/Rudi/Development/PassVault/app/src/main/res/layout/dialog_color_picker.xml)
- Use string resources for "Pick a Color".

#### [MODIFY] [menu_main.xml](file:///C:/Users/Rudi/Development/PassVault/app/src/main/res/menu/menu_main.xml)
- Use `@string/recycle_bin` and `@string/password_health` instead of hardcoded titles.

#### [MODIFY] [AddEditActivity.kt](file:///C:/Users/Rudi/Development/PassVault/app/src/main/java/com/jksalcedo/passvault/ui/addedit/AddEditActivity.kt)
- Externalize Toast messages.

#### [MODIFY] [PasswordGenDialog.kt](file:///C:/Users/Rudi/Development/PassVault/app/src/main/java/com/jksalcedo/passvault/ui/addedit/PasswordGenDialog.kt)
- Externalize Toast message.

#### [MODIFY] [SetPinFragment.kt](file:///C:/Users/Rudi/Development/PassVault/app/src/main/java/com/jksalcedo/passvault/ui/auth/SetPinFragment.kt)
- Externalize Toast message.

#### [MODIFY] [ViewEntryActivity.kt](file:///C:/Users/Rudi/Development/PassVault/app/src/main/java/com/jksalcedo/passvault/ui/view/ViewEntryActivity.kt)
- Externalize Toast messages.

#### [MODIFY] [AboutFragment.kt](file:///C:/Users/Rudi/Development/PassVault/app/src/main/java/com/jksalcedo/passvault/ui/settings/AboutFragment.kt)
- Use `getString()` with parameters for version and developer strings.

## Verification Plan

### Automated Tests
- Run `app:assembleDebug` to ensure everything still compiles.

### Manual Verification
- Deploy to a device/emulator.
- Switch language to Dutch in settings.
- Verify that the settings screens, about screen, and various dialogs are translated.
- Check Toast messages when copying passwords or setting PINs.
