# Anex Chat for Android

## What you can do

- Open **Anex Chat** from the launcher. The white speech bubble with a blue A also appears on welcome and in **You → Settings → Help & support → About**. Android controls the launcher mask and the themed icon color.
- On **Chats**, tap **+** beside Search to share your invite, scan a QR code, or paste a link. Open **Contacts** to start a conversation. Tap **Calls → +** to choose a contact for an audio call. Tap the call button inside a direct chat for audio or video.
- Open **You → Settings → Chat data → Set database passphrase** and choose a passphrase you can keep safely. Return to **Back up chats → Choose where to save**, pick a folder and tap **Save**. Chat data shows the date after a successful save. A fresh installation has no history of earlier backup dates.
- To restore, tap **Restore from backup → Choose backup ZIP** on welcome or in **Chat data**. Select the ZIP, confirm replacement, and enter the database passphrase used for that backup. Restoring replaces all local profiles and chats. Back up the current phone before testing a restore.
- For an additional profile, open **You → Your chat profiles → Add profile**. After creation, tap **Protect profile** to set its hidden-profile password. To find it again, enter that password in Your chat profiles. At least one profile must stay visible.
- To move, open **You → Settings → Migrate to another device** and follow the instructions on both phones. Use the transferred profile on the new phone only.

## Limits

- A profile and its chat history live on the device. The core has no account service for logging into the same profile on two phones at once. Desktop linking uses a different connection. Running two restored copies of a profile can disrupt delivery.
- Backups are manual. Android’s file picker can save to local storage, a USB drive or a cloud provider installed on the phone. Cloud availability and upload completion depend on that provider. Anex has no Google SDK or background cloud backup.
- The archive contains the encrypted databases. The outer ZIP has no password of its own. Attachments and wallpapers can be readable. Choose a private destination. Keep the database passphrase somewhere safe. Anex cannot reset it. A hidden-profile password only reveals that profile inside the app.
- The release is for ARM64 Android phones. A debug build and this release use different signing keys. Back up your debug data before replacing that installation.
- Glass coverage is listed in GLASS_COVERAGE.md. In **You → Settings → Appearance → Glass effects**, On enables glass, Off selects solid controls, and Auto follows the device settings.
- Call history uses the cross-chat history available on the phone. A live two-way audio or video call and incoming notifications require another connected endpoint.

## Release checks

The signed ARM64 FOSS APK installed on device 3a6cab29 without the Android test-install option and opened from its launcher. Android package tools confirmed the Anex Chat label, launcher and notification artwork, release signature, and absence of the test-only flag. Both core libraries matched the official v7.1.4 ARM64 release. The final folder contains one APK, install instructions and license credits.

On the clean installation, welcome displayed the Restore from backup action, which opened Android’s file picker. A new test profile reached Network commitments. Acceptance requires the owner. Post-acceptance server switches, message and photo delivery, scanner preview, call screens, glass appearance, and a full backup/restore round trip have not been verified on this release. No exception to Android’s battery restrictions was granted.

## What to do next

- Set up your own TURN relay for calls and configure it in **Settings → Network & servers → WebRTC ICE servers**. The app currently ships upstream call relay configuration.
- Back up the SMP and XFTP server identity keys and server state separately from phone backups. Keep private copies offline and practice recovering the servers.
- Test FOSS notifications on both phones with screens locked and after reboot. Grant Android notification permission. Instant delivery uses the background service. Battery restrictions and periodic mode can delay messages and calls.
- Back up both files from the Anex-Keys folder to secure offline storage. The release keystore and its password file are required to sign compatible updates. Only share the three files in the final download folder.

Implementation references include `views/database/AnexArchiveView.kt`, `views/database/DatabaseView.kt`, `views/usersettings/HiddenProfileView.kt`, and the Android file picker in `platform/Files.android.kt`, under `apps/multiplatform/common/src`. The existing archive format is implemented in `src/Simplex/Chat/Archive.hs`.
