# iDock integration plan (device layer)

The companion spec is `docs/protocol-usbmux-lockdown.md`. Section numbers like "§6.1" below refer to it.
The tags [SRC], [DOC] and [INF] have the same meaning as in the spec. Nothing here has been tested on hardware yet.

## 1. Feasibility

Legend:
- Yes: works without pairing.
- Yes-T: works after the owner taps Trust and pairing succeeds.
- Dev: needs Developer Mode plus a Developer Disk Image (DDI).
- Partial: only part of the feature is possible.
- No: not possible in scope.

Effort: S ≤ 2 days, M ≤ 1 week, L ≤ 3 weeks, XL > 3 weeks. Estimates assume the shared layers (USB, mux, lockdown, TLS) are already done.

| Feature | Feasible? | Protocol / service | Effort | Main risk |
|---|---|---|---|---|
| Device info | Yes (basic), Yes-T (full) | lockdown GetValue without a session (§4.5); full set after StartSession | S | Which keys the device returns unpaired varies by iOS version. Decide at runtime. |
| Battery health & cycle count | Yes-T | diagnostics_relay `IORegistry` `EntryClass=IOPMPowerSource` (§6.3). Simple percentage from the lockdown `com.apple.mobile.battery` domain. | S | The health formula and its agreement with what Settings shows. Key names can change between iOS versions. |
| Storage usage | Yes-T | AFC `GET_DEVINFO` (`FSTotalBytes`/`FSFreeBytes`). Lockdown domain `com.apple.disk_usage`. | S | Key names in disk_usage are [INF]. The AFC numbers cover the data volume only. |
| Photo/video import | Yes-T (read only) | AFC `/DCIM/**` (§6.1) | M | iCloud "Optimize Storage" means many originals are not on the device. HEIC/HEVC decoding on older Android versions. Throughput. |
| File browser (media partition) | Yes-T | AFC (`com.apple.afc`) | M | AFC only sees `/var/mobile/Media`. Users may expect more than that. |
| Per-app documents | Partial (Yes-T) | house_arrest `VendDocuments` → AFC (§6.2) | M | Only works for apps with `UIFileSharingEnabled`. `VendContainer` is restricted. |
| App list | Yes-T | installation_proxy `Browse` (§6.4) | S | Large replies. Ask only for the `ReturnAttributes` you need. Icons need springboardservices, which is not covered here. |
| Install .ipa | Partial (Yes-T) | AFC upload to `PublicStaging/` + installation_proxy `Install` | M | The IPA must already be validly signed for this device. iDock does not re-sign. |
| Uninstall app | Yes-T | installation_proxy `Uninstall` | S | Some system apps refuse with `UninstallProhibited`. |
| Backup | Yes-T | mobilebackup2 + notification_proxy + AFC sync lock + EscrowBag (§6.8) | XL | Protocol size, encrypted backups, gigabytes of data over OTG, the phone running out of storage or battery. |
| Restore | Partial | mobilebackup2 `Restore` | XL | Can lose user data if a bug occurs mid-restore. Find My must be off [INF]. Ship it last, if at all. |
| Ringtone transfer | Partial | AFC to `/iTunes_Control/Ringtones/*.m4r` + `Ringtones.plist` [INF] | M | Not documented anywhere I could verify. iOS may ignore files the Music/Sync framework did not write. Test per iOS version. |
| Screenshot | Dev (≤16) / No for v1 (17+) | screenshotr + DDI (≤16). RSD tunnel + DTX on 17+ (§6.7, §7). | L (≤16), XL+ (17+) | Personalized DDI signing and a userspace IPv6/HTTP2/XPC stack on iOS 17+. |
| Reboot / shutdown | Yes-T | diagnostics_relay `Restart` / `Shutdown` / `Sleep` | S | None significant. Handle the USB detach that follows. |
| Real-time logs | Yes-T | syslog_relay (text), os_trace_relay (structured) (§6.6) | S / M | Volume. Use a ring buffer and filters. os_trace field layout is [SRC] but undocumented by Apple. |
| Component "verification report" | Partial / mostly No | Current values only: lockdown (serials, MACs, baseband), IORegistry (battery serial etc. [INF]) | M | The device exposes no "factory" part serials over these protocols. MobileGestalt is deprecated on iOS 17.4+. Apple's Parts & Service History is not reachable. Show "current identifiers"; never claim genuine vs. replaced. |

## 2. Recommendation: pure Kotlin vs. NDK + libimobiledevice

| | Pure Kotlin | NDK: libusb + libimobiledevice + libplist + libimobiledevice-glue + OpenSSL/mbedTLS (+ usbmuxd logic) |
|---|---|---|
| USB access | `UsbDeviceConnection` directly | libusb ≥1.0.23 with `libusb_wrap_sys_device(fd)` on the Android fd, and device discovery disabled (`LIBUSB_OPTION_NO_DEVICE_DISCOVERY`). No root needed. |
| The usbmux part | About 600 lines of Kotlin (§2–3) | usbmuxd is a *daemon* speaking a Unix socket. You would have to embed its `device.c`/`usb.c` in-process or run it as a child process bound to an app socket, then point libusbmuxd at it. That is non-trivial glue. |
| Effort to Stage 3 | ~6–8 engineer-weeks | ~4–6 weeks of build/porting/JNI work, then less protocol work. Debugging crosses JNI. |
| APK size | +~3–4 MB (BouncyCastle, dd-plist) | +~4–8 MB per ABI (OpenSSL dominates; mbedTLS is smaller) |
| Licensing | Original code. Reading GPL/LGPL sources to learn the *protocol* and writing your own implementation is the normal clean-room-style approach. Do not copy code. | **usbmuxd is GPL-2.0 or GPL-3.0** ("either version 2 or version 3" in its headers [SRC]). Linking or embedding it makes the whole APK a GPL work: the full app source must be offered under the GPL. **libimobiledevice, libusbmuxd and libplist are LGPL-2.1-or-later** [SRC]. Dynamic linking (.so) is allowed from a closed app, but you must ship the LGPL source and offers, allow relinking (shipping replaceable .so files is generally accepted), and not forbid reverse-engineering for debugging. Static linking or a "port" (translating their code to Kotlin) creates a derivative work under the LGPL/GPL. go-ios is MIT. pymobiledevice3 is GPL-3 (reference only). Get a lawyer's sign-off either way. |
| Fit | Coroutines, structured errors, no ABI matrix, testable on the JVM with recorded byte traces | Mature and battle-tested protocol code, iOS quirks already handled |
| iOS 17+ tunnel later | Needs your own userspace TCP/IPv6 + HTTP/2 + XPC (XL) | libimobiledevice does not do RemoteXPC either, so this is no advantage |

**Recommendation: pure Kotlin.** The hard part is not the protocol volume; it is getting USB right on Android, and libusb on Android gives no advantage there. The GPL status of usbmuxd is the deciding factor. The one piece we would need from it (the mux) is small and fully specified in §2–3. LGPL libimobiledevice would still require the usbmuxd glue. Use go-ios (MIT) as the preferred readable cross-reference. Keep libimobiledevice as the reference of record. Add an `AfcClient` conformance test against byte traces captured with `usbmuxd` + `idevicesyslog`/`afcclient` running on Linux with `USBMUXD` debug logs (`usbmuxd -v -v -f`).

## 3. Staged roadmap

Package root: `dev.idock.device`. All I/O runs in coroutines on `Dispatchers.IO` plus one dedicated USB reader thread.

### Stage 1: USB detection + unpaired lockdown info
Classes:
- `usb/AppleUsbMonitor`: attach/detach receivers, PID classification `enum DeviceUsbMode { NORMAL, RECOVERY, DFU, UNSUPPORTED }` (§1.1), permission flow.
- `usb/UsbTransport` (interface: `write(ByteArray)`, `readLoop(onBytes)`, `close()`), and `AndroidUsbTransport` implementing it: configuration/interface selection (§1.2), optional GET_MODE (§1.3), chunked writes + ZLP or the "avoid multiple of 512" fallback (§1.5).
- `mux/MuxFrame` (encode/decode v1/v2), `mux/MuxDevice` (version handshake, SETUP, seq counters, packet demux), `mux/MuxConnection` (TCP-like state machine; exposes `InputStream`/`OutputStream` or `ByteReadChannel`/`ByteWriteChannel`), `mux/PortAllocator`.
- `plist/PlistCodec` (dd-plist wrapper, XML out, auto-detect in), `lockdown/PlistServiceConnection` (4-byte BE framing over any stream), `lockdown/LockdownClient` (`queryType()`, `getValue(domain?, key?)`, `goodbye()`), `lockdown/LockdownError` (maps §5.6 strings).
- `DeviceRepository`: exposes `StateFlow<DeviceState>` to the Compose UI.

Tests:
- JVM unit tests for `MuxFrame`, using the exact bytes of the §3.4 worked example.
- A fake device loopback for `MuxConnection` that checks ACK and window logic.
- On hardware: plug in (both locked and unlocked), check that `ProductType`/`ProductVersion`/`UniqueDeviceID` show up, and that the UDID matches `getSerial()`. Test on at least one Android 8 (API 26/27, 16 KiB limit) and one Android 13+ phone, and on iOS 15, 17 and 18 devices. Log every mux packet in hex in debug builds.

### Stage 2: Pairing + TLS + battery/diagnostics
Classes:
- `pairing/PairRecord` (data class plus plist serialization), `pairing/PairRecordStore` (Keystore-encrypted file per UDID; stores HostID, SystemBUID, EscrowBag), `pairing/CertificateFactory` (BouncyCastle, §5.1), `pairing/PairingFlow` (state machine: `NeedsUnlock` → `AwaitingTrust` → `Paired`/`Denied`, resending Pair every 1 s).
- `tls/TlsStream` (SSLEngine or SSLSocket over a `MuxConnection`; host cert first, root cert as fallback), `lockdown/LockdownSession` (`startSession`, `startService(name, withEscrowBag)` → `ServiceDescriptor(port, ssl)`), `service/ServiceConnector` (opens the port and wraps it in TLS when the descriptor says so).
- `services/DiagnosticsRelayClient` (`battery()`, `restart()`, `shutdown()`, `sleep()`), `model/BatteryHealth`.

Tests:
- JVM test that generated certificates parse and have empty DNs, serial 0, and the expected extensions (compare with `openssl x509 -text` on a cert produced by libimobiledevice).
- On hardware: first pairing with the device locked (expect `PasswordProtected`) and unlocked (Trust dialog). Tap Don't Trust and check that `UserDeniedPairing` is handled. Use Settings → Reset Location & Privacy to invalidate the pairing and check that `InvalidHostID` leads to re-pairing. Check the battery values against Settings → Battery Health. Test reboot.

### Stage 3: AFC files and photos
Classes:
- `services/afc/AfcPacket`, `AfcClient` (`list`, `stat`, `deviceInfo`, `open`/`read`/`write`/`seek`/`close`, `mkdir`, `remove`, `rename`), `AfcFileSystem` (path helpers, recursive walk, streaming copy with progress `Flow`), `services/HouseArrestClient` (`vendDocuments(bundleId)` → `AfcClient`).
- `photos/PhotoImporter` (walks DCIM, de-duplicates by name+size+mtime, writes to MediaStore).

Tests:
- JVM tests for header encoding and NUL-list parsing.
- On hardware: list `/`, `/DCIM`. Copy a 4 GB video and record throughput and memory use. Write and then delete a file in `/Downloads` [INF dir]. Open an app's documents via house_arrest (e.g. VLC or another app that has file sharing enabled).

### Stage 4: Apps
Classes:
- `services/InstallationProxyClient` (`browse(type, attrs)`, `lookup`, `install(stagedPath)`, `uninstall(bundleId)`, progress `Flow`).
- `services/NotificationProxyClient`.
- `services/SyslogRelayClient`, `services/OsTraceClient` (logs fit here; both are small).
- `apps/IpaInstaller` (AFC staging + install).

Tests:
- On hardware: list user and system apps. Install a validly signed test IPA (e.g. built from a sample project with a free development profile for that device). Uninstall it. Check that the `application_installed`/`application_uninstalled` notifications arrive. Stream logs for 10 minutes and watch memory.

### Stage 5: Backup
Classes:
- `services/devicelink/DeviceLinkClient` (version exchange, process message, status response), `services/backup/MobileBackup2Client`.
- `backup/BackupEngine` (handlers for every DLMessage in §6.8), `backup/BackupStore` (SAF tree URI or app storage; needs free-space accounting), `backup/SyncLock` (notification_proxy + AFC lock).

Tests:
- Back up a small test device and compare the folder layout with an `idevicebackup2 backup` of the same device on Linux.
- Interrupt with a cable pull and check that the device recovers and the next backup works.
- Encrypted-backup on/off.
- Restore only to a disposable test device.

## 4. Open questions (need real hardware)

1. Which configuration does an Android kernel pick for an iPhone at enumeration? Does `setConfiguration(4)` fail with EBUSY? Is the claim-all-then-release workaround (§1.5) needed and sufficient? Is `ipheth` built into common Android kernels?
2. Are the configuration numbers and interface layout really as assumed (§1.2), with usbmux in config 4 in initial mode, on current iPhones and on USB-C iPhones (15+)?
3. Does a zero-length `bulkTransfer` actually emit a ZLP on Android usbfs? Does the "avoid length ≡ 0 mod 512" fallback work?
4. What does GET_MODE return on iOS 17/18? Is any mode switch ever necessary on Android?
5. What exactly do the v2 mux `rx_seq`/`tx_seq` fields mean? Does the device care if we copy usbmuxd verbatim (§2.3)? Is the SYN-ACK's sequence number always 0?
6. Is there a sustained AFC throughput limit with the fixed 128 KiB window and Android's bulk API? What are the best read size and the best number of parallel `UsbRequest`s?
7. What is the exact unpaired GetValue key set on iOS 17/18 (§4.5)? Is `DeviceName` still returned?
8. Does Conscrypt accept the empty-DN, serial-0 certificates in its KeyManager path? Which TLS version and cipher get negotiated with iOS 15/17/18? Does the device accept the host cert as the client cert (it should, per go-ios)?
9. Which services set `EnableServiceSSL=true` on current iOS?
10. Ringtones: does iOS 17/18 pick up `.m4r` files and `Ringtones.plist` edits written through AFC?
11. Battery: which IORegistry fields match the "Maximum Capacity" shown in Settings? Is `CycleCount` present on all models?
12. Charging behaviour: does the iPhone pull too much current from the Android phone during long transfers or backups? Does it disconnect when the Android screen turns off (USB suspend)?
13. Backup: performance and storage needs on a typical phone (tens of GB). Is EscrowBag-based StartService accepted while the device is locked?
