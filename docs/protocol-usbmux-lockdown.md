# iDock device layer: USB / usbmux / lockdown protocol spec

Status: research document for a pure-Kotlin implementation on the Android USB Host API.
Nothing here has been tested against real hardware. Every statement is tagged:

- **[SRC]**: verified by reading reference source. The file and function are named. Sources are shallow clones under `/home/claude/ref/` taken 2026-11-13: `usbmuxd`, `libimobiledevice`, `libusbmuxd`, `libirecovery` (all libimobiledevice.org), `go-ios` (MIT) and `pymobiledevice3` (GPL-3).
- **[DOC]**: taken from external documentation or a web page (cited inline).
- **[INF]**: my inference. It must be confirmed on a device.

Byte order: the mux layer and the TCP-like layer are **big-endian** (network order). AFC is **little-endian**. The plist length prefixes are big-endian.

Scope: device management only. The flow requires the owner to unlock the device and tap "Trust". Recovery and DFU product IDs appear here only so the app can *recognise* those modes and tell the user. It does nothing in those modes.

---

## 1. USB level

### 1.1 Identification

| Item | Value | Source |
|---|---|---|
| Vendor ID | `0x05AC` | [SRC] `usbmuxd/src/usb.h` `VID_APPLE` |
| Normal-mode (usbmux-capable) PIDs | `0x1290`–`0x12AF` inclusive | [SRC] `usb.h` `PID_RANGE_LOW/MAX`, filter in `usb.c:usb_device_add` |
| Also accepted by usbmuxd | `0x8600` (T2 coprocessor), `0x1901`–`0x1905` (Apple-silicon Mac restore) | [SRC] `usb.h`. iDock should **ignore** these. |
| Recovery mode | `0x1280`, `0x1281`, `0x1282`, `0x1283` | [SRC] `libirecovery/include/libirecovery.h` `IRECV_K_RECOVERY_MODE_1..4` |
| DFU / "WTF" | `0x1227` (DFU), `0x1222` (WTF), `0xF014` (port DFU) | [SRC] same header |

App rule: for a PID in the recovery or DFU set, show "device is in recovery/DFU mode; restart it normally", and stop there. Run the pipeline below only for a PID in `0x1290..0x12AF`. The udev rule in `usbmuxd/udev/39-usbmuxd.rules.in` matches `5ac/12[9a][0-9a-f]`, which is the same range.

**UDID**: usbmuxd reads the iSerialNumber string descriptor and converts it to ASCII. If the result is exactly 24 characters (the newer UDID format), it inserts a `-` after character 8, which gives `XXXXXXXX-XXXXXXXXXXXXXXXX`. [SRC] `usb.c:get_serial_callback`. On Android, use `UsbDeviceConnection.getSerial()` after opening. `UsbDevice.getSerialNumber()` throws without permission on API 29+ [INF from Android behaviour; verify]. Apply the same hyphen rule. Lockdown's `UniqueDeviceID` should match the result. Treat lockdown's value as authoritative.

### 1.2 Configurations and the usbmux interface

The usbmux interface descriptor is **class 0xFF, subclass 0xFE, protocol 0x02**, and it has exactly 2 bulk endpoints (one IN, one OUT). [SRC] `usb.h` `INTERFACE_CLASS/SUBCLASS/PROTOCOL`, `usb.c:set_valid_configuration`.

How usbmuxd picks the configuration ([SRC] `usb.c:set_valid_configuration`):
1. It iterates configuration values **from `bNumConfigurations` down to 1** (highest first).
2. In each configuration it checks `altsetting[0]` of every interface. It takes the first interface that matches and has 2 endpoints, one OUT and one IN. The order of the two endpoints does not matter; swapped order is handled.
   - Note: usbmuxd's match uses `class==255 || subclass==254 || protocol==2`, which is a logical OR. That looks like a latent looseness, because any vendor-class interface matches. **In Kotlin, match all three values (AND).**
3. If the device's current configuration differs, usbmuxd first detaches the kernel drivers from every interface of the *target* configuration. Then it calls `set_configuration(j)`.
4. It records `ep_out`, `ep_in` and `wMaxPacketSize` of the OUT endpoint (default 64 if unknown). It claims the interface, reads the serial, and then starts **3 parallel bulk-IN reads of 16384 bytes each** (`NUM_RX_LOOPS=3`, `USB_MRU=16384`).

On Linux, udev first puts the device in configuration 0 (unconfigured) so that no kernel driver binds. [SRC] `udev/39-usbmuxd.rules.in` (`ATTR{bConfigurationValue}="0"`). Android has no equivalent; see the pitfalls below.

Typical layout [INF from commonly published `lsusb -v` dumps; verify on hardware]. In "initial" mode there are 4 configurations: 1 = PTP (still image, class 6), 2 = iPod USB Interface (audio/HID), 3 = PTP + Apple Mobile Device (usbmux), 4 = PTP + Apple Mobile Device + Apple USB Ethernet (class 255/253/1, tethering). usbmuxd therefore normally selects **configuration 4**. With 5 configurations (Valeria or CDC-NCM mode), configuration 5 also contains usbmux.

### 1.3 The iOS 16+ "device mode" vendor requests

[SRC] `usb.c:usb_device_add`, `get_mode_cb`, `switch_mode_cb`, `guess_mode`, `submit_vendor_specific`.

Both requests are **vendor, device-to-host, recipient device**: `bmRequestType = 0xC0`.

| Request | bRequest | wValue | wIndex | wLength | Response |
|---|---|---|---|---|---|
| GET_MODE | `0x45` | 0 | 0 | 4 | 4 bytes. The source comment says `3:3:3:0` in initial mode and `5:3:3:0` otherwise. |
| SET_MODE | `0x52` | 0 | desired mode (1..5) | 1 | 1 byte. `0x00` means accepted. |

Modes, as `guess_mode` infers them from `bNumConfigurations`: 1 = initial (≤4 configurations), 2 = Valeria (5 configurations, configuration 5 has an interface 255/42/255 plus usbmux), 3 = CDC-NCM (5 configurations, configuration 5 has class 2/subclass 0x0D plus usbmux), 4 = USB-Ethernet + CDC-NCM (6 configurations), 5 = CDC-NCM direct (1 configuration).

What usbmuxd does:
- It always sends GET_MODE first (timeout 1000 ms). If the transfer fails (older devices), it continues initialisation in the current mode.
- The desired mode is the environment variable `USBMUXD_DEFAULT_DEVICE_MODE`, default **1**. usbmuxd sends SET_MODE only if the guess succeeded (>0) and differs from the desired mode.
- If SET_MODE returns `data[0]==0`, usbmuxd does **not** finish initialisation. It expects the device to **re-enumerate**: it disconnects and comes back with the new configuration set, and the new device is handled from scratch. If the reply is non-zero or the transfer fails, it continues in the current mode.

**iDock recommendation**: send GET_MODE only for logging, and do **not** send SET_MODE. A device freshly plugged into a non-Mac host is in mode 1 [INF]. Every listed mode still contains a usbmux interface, so choosing the highest configuration that has usbmux works in all of them. If SET_MODE is ever used, expect `ACTION_USB_DEVICE_DETACHED` followed by `ATTACHED`, which means a **new USB permission prompt** on Android.

### 1.4 Bulk transfer rules

| Rule | Value | Source |
|---|---|---|
| Read buffer per bulk-IN transfer | 16384 (`USB_MRU`) | [SRC] `usb.h`, `usb.c:start_rx_loop` |
| Max assembled device→host mux packet | 65536 (`DEV_MRU`) | [SRC] `device.c` |
| Max host→device mux packet (total incl. headers) | 49152 = 3×16384 (`USB_MTU`) | [SRC] `usb.h`, `device.c:send_packet` |
| ZLP | After a write whose length is an exact multiple of the OUT endpoint's `wMaxPacketSize` (512 at high speed), send a separate **zero-length** bulk OUT transfer. | [SRC] `usb.c:usb_send` |

How usbmuxd reassembles incoming packets ([SRC] `device.c:device_data_input`): if a read returns exactly 16384 bytes and the header's length is larger, it buffers the bytes and appends later reads. The packet is complete when a read is shorter than 16384 or when the accumulated length equals the header length. Then `header.length` must equal the received byte count. Otherwise the packet is dropped.

**Kotlin recommendation**: do not depend on transfer boundaries. Keep a byte accumulator. When it holds at least 8 bytes, read `length` (bytes 4..7, big-endian), wait until `length` bytes are present, emit the packet, and repeat. This works with either 16 KiB or larger reads. Reject `length < 8` or `length > 65536` as a framing error and reset the device session.

### 1.5 Android mapping

| Step | Android API | Notes |
|---|---|---|
| Discover | `UsbManager.getDeviceList()`, plus `BroadcastReceiver` for `UsbManager.ACTION_USB_DEVICE_ATTACHED/DETACHED`. A manifest `<intent-filter>` with `<meta-data android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" android:resource="@xml/device_filter">` and `<usb-device vendor-id="1452"/>` (1452 = 0x05AC) lets the app be offered on plug-in. | Filter by PID in code. |
| Permission | `UsbManager.hasPermission(dev)`. If false, call `requestPermission(dev, PendingIntent)`. On API 31+ the PendingIntent must be `FLAG_MUTABLE` so the system can add the extras [INF, standard practice]. | The dialog appears again after every re-enumeration unless the user ticks "always open" from the attach intent. |
| Open | `UsbManager.openDevice(dev)` → `UsbDeviceConnection` | |
| Enumerate configurations | `dev.configurationCount`, `dev.getConfiguration(i)` → `UsbConfiguration.id`, then `getInterface(j)` → `interfaceClass/Subclass/Protocol`, `endpointCount`, `getEndpoint(k)` → `direction` (`USB_DIR_IN=0x80`), `type == USB_ENDPOINT_XFER_BULK`, `maxPacketSize` | Do **not** use `UsbDevice.getInterface(i)`. It flattens all configurations together. Iterate configurations from the highest `id` down, like usbmuxd. |
| Set configuration | `UsbDeviceConnection.setConfiguration(UsbConfiguration)` (API 21+) | Pitfall: unlike libusb, Android has no "detach all kernel drivers" call. If a kernel driver (for example `ipheth`, if the Android kernel has it) is bound to an interface of the current configuration, the call can fail with EBUSY. Mitigation [INF]: call `claimInterface(intf, true)` on every interface of the *current* configuration (this detaches kernel drivers), then `releaseInterface`, then `setConfiguration`. If the device is already in the target configuration, skip this step. The Linux kernel usually picks a configuration at enumeration, so expect to need this. Verify on hardware. |
| GET_MODE | `conn.controlTransfer(0xC0, 0x45, 0, 0, buf4, 4, 1000)` | Returns the byte count, or <0 on failure. A failure is not fatal. |
| Claim | `conn.claimInterface(usbmuxIntf, true)` | `force=true` means "disconnect kernel driver if necessary" [DOC: Android `UsbDeviceConnection` reference, https://developer.android.com/reference/android/hardware/usb/UsbDeviceConnection?authuser=1]. |
| Write | `conn.bulkTransfer(epOut, buf, off, len, timeoutMs)` | Before API 28 (P), any `length` > 16384 is **truncated to 16384** [DOC, same page]. Below API 28, split writes into ≤16384-byte chunks yourself. The device sees one USB transfer as long as no chunk boundary falls on a short packet. 16384 is a multiple of 512, so the chunks are back-to-back full packets [INF]. After the final chunk, if `totalLen % maxPacketSize == 0`, send a ZLP with `bulkTransfer(epOut, ByteArray(0), 0, timeout)` [INF: usbfs accepts zero-length bulk URBs; verify]. Fallback if ZLPs turn out to be unreliable on some phones: never emit a mux packet whose total length is a multiple of 512. When a TCP data segment would produce one, shorten the payload by 1 byte and send that byte in the next segment. This is legal because the stream has no message boundaries. |
| Read | Dedicated reader **thread** looping `conn.bulkTransfer(epIn, buf, buf.size, 0 /*or ~1000*/)`. Alternatively queue 2–3 `UsbRequest`s and `requestWait()` to mirror usbmuxd's 3 parallel reads. | `bulkTransfer` is blocking and not cancellable except by `close()`. Use a timeout (e.g. 1000 ms) so the thread can exit. A `-1` return on timeout is normal. Use a 16384-byte buffer on API <28 and 65536 on API ≥28. |
| Detach | `ACTION_USB_DEVICE_DETACHED`, or a bulk return of -1 with no timeout elapsed | Tear down all mux connections. Close the connection. |

Other Android pitfalls:
- Keep all USB I/O off the main thread. One writer must serialise mux packets (use a single-thread executor or a `Mutex`), because each packet carries the shared `tx_seq` counter.
- The iPhone charges from the Android phone over OTG. Battery drain is significant, and some phones cap OTG current. A powered OTG hub helps [INF].
- `getFileDescriptor()` is "intended for passing to native code" [DOC]. This is what the NDK/libusb alternative would use (see integration-plan.md).

---

## 2. Mux layer

[SRC] `usbmuxd/src/device.c` (`struct mux_header`, `struct version_header`, `send_packet`, `device_data_input`, `device_version_input`, `device_add`).

### 2.1 Headers

**v1 header (8 bytes)**

| Off | Size | Field |
|---|---|---|
| 0 | u32 BE | protocol |
| 4 | u32 BE | length = total packet length *including* this header |

**v2 header (16 bytes)**

| Off | Size | Field |
|---|---|---|
| 0 | u32 BE | protocol |
| 4 | u32 BE | length (total, including header) |
| 8 | u32 BE | magic `0xFEEDFACE` |
| 12 | u16 BE | tx_seq |
| 14 | u16 BE | rx_seq |

**Protocol numbers**: `0` = VERSION, `1` = CONTROL, `2` = SETUP, `6` = TCP (`IPPROTO_TCP`).

**Version payload (12 bytes)**: `major u32 BE`, `minor u32 BE`, `padding u32 = 0`.

### 2.2 Handshake

1. As soon as the interface is claimed, the host sends a **VERSION** packet with major=2, minor=0. It is sent with the **8-byte v1 header**, because the device's version is still unknown (`dev->version==0` → header size 8). Total length 20. [SRC] `device_add`, `send_packet`.
2. The device replies with a VERSION packet. It is parsed with the 8-byte header, because `version` is still 0 when it is parsed. The host accepts major 1 or 2 and drops the device otherwise. [SRC] `device_version_input`.
3. If major ≥ 2, the host sets version=2 and immediately sends a **SETUP** packet (protocol 2) with a v2 header and a **1-byte payload `0x07`**. Total length 17. [SRC] `device_version_input`.
4. The device is then active. Any TCP packet that arrives before this point is discarded.

If major == 1, every later packet uses the 8-byte header, and there is no magic and no sequence fields.

### 2.3 Sequence counters (v2) exactly as usbmuxd keeps them

[SRC] `send_packet`, `device_data_input`.
- State per device: `tx_seq: u16`, `rx_seq: u16`.
- When **sending a SETUP packet**, first set `tx_seq = 0` and `rx_seq = 0xFFFF`.
- For **every** packet sent with a v2 header: write the current `tx_seq` and `rx_seq` into the header, then `tx_seq++` (wrapping at 16 bits).
- For **every** packet received while version ≥ 2: `rx_seq = header.rx_seq`. The device's *rx_seq* field is copied into ours. The device's `tx_seq` field is ignored.
  - This is literally what the source does. The semantics are undocumented. Replicate it verbatim and log the device's `tx_seq` for debugging. [SRC; meaning INF]
- The VERSION reply is received before `version` becomes 2, so it does not update `rx_seq`.

### 2.4 CONTROL packets (device → host, protocol 1)

The payload begins with a type byte. `3` = error text, `5` = warning text, `7` = info text. The remaining bytes are an ASCII message. usbmuxd only logs them. [SRC] `device_control_input`. Note: usbmuxd locates the payload at offset 16 regardless of version.

---

## 3. TCP-like stream layer (protocol 6)

[SRC] `device.c` (`send_tcp`, `send_anon_rst`, `device_start_connect`, `device_tcp_input`, `update_connection`, `device_client_process`, `connection_device_input`, `device_check_timeouts`, `connection_teardown`).

### 3.1 Header as sent (standard 20-byte TCP header, no options)

| Off | Size | Field | Value sent by usbmuxd |
|---|---|---|---|
| 0 | u16 BE | source port | host's local port (see 3.2) |
| 2 | u16 BE | dest port | device service port (62078 = `0xF27E` for lockdown) |
| 4 | u32 BE | seq | `tx_seq` (connection-level, 32-bit) |
| 8 | u32 BE | ack | `tx_ack` |
| 12 | u8 | data offset / reserved | `0x50` (offset = 5 words, no options) |
| 13 | u8 | flags | FIN=0x01 SYN=0x02 RST=0x04 PSH=0x08 ACK=0x10 |
| 14 | u16 BE | window | `tx_win >> 8` = `131072 >> 8` = **`0x0200`** |
| 16 | u16 BE | checksum | **0** (never computed) |
| 18 | u16 BE | urgent | 0 |

Window scaling is implicit and fixed at **<<8 in both directions**. Our advertised window is 131072 bytes. The device's window is `th_win << 8`. No options are negotiated.

### 3.2 Ports

- Source ports are allocated per device starting at **1** and counting upward. usbmuxd skips values that are in use (`find_sport`, `next_sport = 1` in `device_add`). Ports are never reused while a connection with that port is live.
- The destination port is the service port. When usbmuxd clients send it through the usbmuxd socket, it is byte-swapped (`htons`) in the plist/struct, and usbmuxd swaps it back. With direct USB, simply write the port big-endian: 62078 → bytes `F2 7E`. [SRC] `libusbmuxd.c:usbmuxd_connect`, `usbmuxd/src/client.c`.

### 3.3 Connection state machine

Per-connection state (usbmuxd names): `tx_seq`, `tx_ack`, `tx_acked`, `tx_win=131072`, `rx_seq`, `rx_ack`, `rx_win`, `max_payload = 49152 − 16 − 20 = 49116`.

**Open** (`device_start_connect`)
1. `tx_seq = 0`, `tx_ack = 0`. Send flags=`SYN`, seq 0, ack 0, win 0x0200, no payload.

**On every incoming TCP packet for this (sport, dport)** (`device_tcp_input`)
- Store `rx_seq = th.seq`, `rx_ack = th.ack`, `rx_win = th.win << 8`.
- If there is no matching connection and the packet is not RST: reply with an "anonymous RST". Its header is all zeros except the ports, `ack = th.seq`, flags=RST and offset 5.
- If RST carries a payload, the payload is an ASCII reason string. Log it.

**CONNECTING**
- If flags are exactly `SYN|ACK` (0x12): `tx_seq++` (→1), `tx_ack++` (→1). Send a pure `ACK`. The state becomes CONNECTED.
  - Note: usbmuxd **never derives `tx_ack` from the device's seq**. It assumes that the device's initial sequence number is 0, so the first expected byte is 1 [SRC]. Kotlin: copy usbmuxd, and log a warning if the SYN-ACK seq ≠ 0.
- Any other flags: the connection is refused. RST means "refused" (port not listening). Tear down.

**CONNECTED**
- If flags are exactly `ACK`: deliver the payload to the application buffer, then send an ACK immediately (`send_tcp_ack`). "Device likes it best when we are promptly ACKing data" (source comment).
- Any other flags (RST, FIN, …): the connection is reset by the device. Tear down, and do **not** send an RST back if the device sent RST.

**Sending data** (`device_client_process`)
- `sendable = rx_win − (tx_seq − rx_ack)`, clamped to ≥0, to the out-buffer capacity (65536) and to `max_payload` (49116).
- Send flags=`ACK` (no PSH), `seq = tx_seq`, `ack = tx_ack`, with the payload. Then `tx_seq += len`.
- If `sendable == 0`, wait for an incoming ACK that raises `rx_ack` or `rx_win`.

**ACK number = bytes consumed by the application, not bytes received.**
- `tx_ack` grows only when bytes are actually handed to the local client (`device_client_process`: `conn->tx_ack += size` after `client_write`). The immediate ACK after a data packet therefore usually carries the *old* ack value.
- When `tx_ack != tx_acked` (an ACK is pending), an ACK is sent once **30 ms** (`ACK_TIMEOUT`) have passed since the last segment (`device_check_timeouts`). Any data segment we send also carries the current ack and clears the pending flag.
- Together with the fixed 128 KiB advertised window, this is the flow control. The device never has more than 128 KiB un-ACKed, and the host buffers at most 256 KiB per connection (`CONN_INBUF_SIZE`). usbmuxd tears down on overflow.
- Kotlin recommendation: hand payloads to a bounded per-connection pipe (≥256 KiB). Advance `ack` as the consumer *reads*. Send a window-update ACK when the un-ACKed consumed delta is ≥ 32 KiB or after 30 ms, whichever comes first. There is no separate "window update" message. The window value never changes; only the ack advances.

**Close** (`connection_teardown`)
- There is **no FIN handshake**. To close, send flags=`RST` with current seq and ack, then free the state. Skip this if the device is gone or the connection was already reset or refused.
- No retransmission and no reordering: USB bulk transfers are reliable and in order. usbmuxd never retransmits and never checks `rx_seq` for gaps.

### 3.4 Worked example: open lockdown (port 62078) from source port 1, then send QueryType

Assumptions: mux v2 device; the device replied to VERSION and sent nothing else before the SYN. Hex is grouped per field.

**① Host → device: VERSION (v1 header, 20 bytes)**
```
00 00 00 00   protocol = 0 (VERSION)
00 00 00 14   length = 20
00 00 00 02   major = 2
00 00 00 00   minor = 0
00 00 00 00   padding
```
**② Device → host: VERSION reply** [INF: expected shape. Parsed with the 8-byte header; the host accepts major 1 or 2]
```
00 00 00 00 | 00 00 00 14 | 00 00 00 02 | 00 00 00 xx | 00 00 00 00
```
**③ Host → device: SETUP (v2 header, 17 bytes)**. Before sending: tx_seq←0, rx_seq←0xFFFF. After sending: tx_seq=1.
```
00 00 00 02   protocol = 2 (SETUP)
00 00 00 11   length = 17
FE ED FA CE   magic
00 00         tx_seq = 0
FF FF         rx_seq = 0xFFFF
07            payload
```
**④ Host → device: SYN (36 bytes)**. After sending: mux tx_seq=2.
```
00 00 00 06   protocol = 6 (TCP)
00 00 00 24   length = 36
FE ED FA CE   magic
00 01         tx_seq = 1
FF FF         rx_seq (= last value copied from the device; still 0xFFFF here)
00 01         sport = 1
F2 7E         dport = 62078
00 00 00 00   seq = 0
00 00 00 00   ack = 0
50            data offset 5
02            flags SYN
02 00         window 0x0200 (=131072>>8)
00 00         checksum 0
00 00         urgent 0
```
**⑤ Device → host: SYN-ACK** (expected): sport `F2 7E`, dport `00 01`, seq `00000000` [INF], ack `00000001`, flags `0x12`, window = device's value. The host copies this packet's mux `rx_seq` field into its `rx_seq` (shown as `RR RR` below).

**⑥ Host → device: ACK (36 bytes)**. tx_seq=1, tx_ack=1. Mux tx_seq becomes 3 after sending.
```
00 00 00 06 | 00 00 00 24 | FE ED FA CE | 00 02 | RR RR
00 01 | F2 7E | 00 00 00 01 | 00 00 00 01 | 50 | 10 | 02 00 | 00 00 | 00 00
```
**⑦ Host → device: first data segment, lockdown QueryType.** The XML plist below (libplist formatting, tab indentation, trailing newline) is **278 bytes = 0x116**. The lockdown framing adds a 4-byte BE length prefix, so the TCP payload is 282 bytes and the mux total is 16+20+282 = **318 = 0x13E**.
```
00 00 00 06 | 00 00 01 3E | FE ED FA CE | 00 03 | RR RR      mux header
00 01 | F2 7E | 00 00 00 01 | 00 00 00 01 | 50 | 10 | 02 00 | 00 00 | 00 00   TCP: seq=1 ack=1 ACK
00 00 01 16                                                  plist length = 278
3C 3F 78 6D 6C ...  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE plist ...>\n<plist version=\"1.0\">\n<dict>\n\t<key>Label</key>\n\t<string>idock</string>\n\t<key>Request</key>\n\t<string>QueryType</string>\n</dict>\n</plist>\n"
```
After sending: `tx_seq = 1 + 282 = 283`. 318 is not a multiple of 512, so no ZLP is needed.
libimobiledevice sends the 4-byte length and the body as **two separate writes** (`property_list_service.c:internal_plist_send`). That produces two TCP segments: seq=1 len=4, then seq=5 len=278. Both forms are valid. Coalescing them into one segment is recommended.

**⑧ Device → host**: one or more ACK-flagged segments carrying `00 00 0x xx` + the response plist. Ack each one as described in 3.3.

---

## 4. Lockdown (port 62078) without pairing

### 4.1 Framing

[SRC] `property_list_service.c:internal_plist_send` / `internal_plist_receive_timeout`.
- Each message is a **u32 big-endian byte length**, followed by the plist bytes.
- Host → device: libimobiledevice sends **XML** for lockdown (`lockdownd_send` → `send_xml_plist`). Binary plists are also accepted by most services [INF]. Use XML for lockdown to match libimobiledevice.
- Device → host: detect the encoding by content. `bplist00` (8 bytes) means binary. `<?xml` means XML. Workaround in libimobiledevice: before parsing XML, replace control characters < 0x20 (other than TAB, LF and CR) with spaces. This is noted as an iOS 4.3+ hack.
- Default receive timeout in libimobiledevice: 30 s.
- Kotlin plist library suggestion: **dd-plist** (`com.googlecode.plist:dd-plist`, MIT). It reads and writes both XML and binary.

Every request may carry `"Label"` (a client name string). libimobiledevice adds it to every request.

### 4.2 Response checking

[SRC] `lockdown.c:lockdown_check_result`
- `Request` in the reply must equal the request name.
- If `Error` (a string) is present, the request failed. Map the string (see 5.6).
- If `Result` is present: `"Success"` or `"Failure"`. "With iOS 5+ 'Result' is not present anymore". If there is no `Error`, the request succeeded.

### 4.3 QueryType

Request:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<key>Label</key>
	<string>idock</string>
	<key>Request</key>
	<string>QueryType</string>
</dict>
</plist>
```
Response: `Request`="QueryType", `Type`="com.apple.mobile.lockdown". Any other `Type` means the device is not talking to lockdownd; treat it as an error. [SRC] `lockdownd_client_new`; pymobiledevice3 `lockdown.py:_initialize` raises `IncorrectModeError` on any other value.

### 4.4 GetValue

Request keys: `Request`="GetValue", optional `Domain` (string), optional `Key` (string), `Label`. [SRC] `lockdownd_get_value`.

With a key:
```xml
<plist version="1.0">
<dict>
	<key>Key</key>
	<string>ProductVersion</string>
	<key>Label</key>
	<string>idock</string>
	<key>Request</key>
	<string>GetValue</string>
</dict>
</plist>
```
Response (shape [SRC: reads `Value`]; example values [INF]):
```xml
<plist version="1.0">
<dict>
	<key>Key</key>
	<string>ProductVersion</string>
	<key>Request</key>
	<string>GetValue</string>
	<key>Value</key>
	<string>18.1</string>
</dict>
</plist>
```
With no Domain and no Key, `Value` is a **dict** of every value the current trust level allows. With a Domain and no Key, `Value` is that domain's dict. Errors come back as `Error` (for example `MissingValue`, `GetProhibited`, `InvalidHostID`). Treat any error as "not available at this trust level".

### 4.5 What is readable without a pair record or session

Evidence:
- [SRC] Before pairing, libimobiledevice reads `ProductVersion` and `DeviceClass` (`lockdownd_client_new`), plus `DevicePublicKey` and `WiFiAddress` (`pair_record_generate`, `lockdownd_do_pair`). `ideviceinfo -s/--simple` deliberately skips pairing (`tools/ideviceinfo.c`).
- [SRC] pymobiledevice3 `lockdown.py:_initialize` calls `GetValue()` with no key **before** any pairing. From the result it reads `UniqueDeviceID`, `UniqueChipID`, `DevicePublicKey`, `ProductType`, `DeviceClass`, `DeviceName`, `BuildVersion`, `ProductVersion` and `WiFiAddress` (its `short_info` and `wifi_mac_address`). After a successful session it re-reads everything ("reload data after pairing"), which implies the pre-session set is smaller.
- [DOC] Elcomsoft (Oct 2017, iOS 11 era) lists what their Info command obtains from a locked device with no pairing record: BuildVersion, DeviceClass, DeviceColor, DeviceName, HardwareModel, HasSiDP, ProductType, ProductVersion, UniqueChipID, UniqueDeviceID, WiFiAddress (https://blog.elcomsoft.com/2017/10/can-you-unlock-that-iphone).

| Key | Unpaired? | Confidence |
|---|---|---|
| DeviceName, ProductType, ProductVersion, BuildVersion, DeviceClass, UniqueDeviceID, UniqueChipID, WiFiAddress, DevicePublicKey | Yes | [SRC] (both reference clients depend on it) |
| HardwareModel, DeviceColor, HasSiDP | Yes on iOS 11 | [DOC] 2017; unverified on iOS 17/18 |
| SerialNumber, PhoneNumber, IMEI/MEID, ActivationState, battery/disk domains | Assume **no**, until paired + StartSession | [INF] |

iOS-version differences: none that I could substantiate from source. `DeviceName` may be generic or empty on recent iOS for privacy reasons [INF]. Display it, but do not key anything on it. **Probe at runtime**: call `GetValue` with no key and show whatever comes back.

Recommended Stage-1 sequence: connect to 62078 → QueryType → GetValue (no key) → render → send `{"Request":"Goodbye"}` (optional, [SRC] `lockdownd_goodbye`) → RST the connection.



---

## 5. Pairing, sessions, TLS, services

### 5.1 Pair record: what the host generates

[SRC] `libimobiledevice/common/userpref.c:pair_record_generate_keys_and_certs` (OpenSSL branch), `lockdown.c:pair_record_generate` and `lockdownd_do_pair`. Cross-checked against go-ios `ios/crypto_utils.go` and pymobiledevice3 `ca.py`.

1. Read `DevicePublicKey` with GetValue (no session needed). It is plist **data** holding a PEM **`-----BEGIN RSA PUBLIC KEY-----`** block (PKCS#1 RSAPublicKey). go-ios parses it with `asn1.Unmarshal` into `rsa.PublicKey`, and pymobiledevice3 requires the "RSA PUBLIC KEY" PEM format.
2. Generate two **RSA-2048** key pairs (e = 65537): *root* and *host*.
3. Build three X.509 **v3** certificates. Each has **serial 0** and an **empty subject and issuer DN**, notBefore = now and notAfter = now + 10 years. Signatures are **SHA-256 with RSA**; libimobiledevice uses SHA-1 only for iOS < 4. go-ios always uses SHA1WithRSA and still works.
   - **RootCertificate**: root public key, self-signed, `basicConstraints = critical, CA:TRUE`.
   - **HostCertificate**: host public key, signed by the root key, `basicConstraints = critical, CA:FALSE`, `keyUsage = critical, digitalSignature, keyEncipherment`.
   - **DeviceCertificate**: the *device's* public key, signed by the root key, `basicConstraints = critical, CA:FALSE`, `subjectKeyIdentifier = hash`, `keyUsage = critical, digitalSignature, keyEncipherment`.
   - go-ios also adds an SKI to all three. pymobiledevice3's docstring notes that a subject/issuer helper "is not suitable for pairing", which suggests the empty DN matters [INF].
4. Encode certificates as PEM and private keys as PEM. libimobiledevice writes them via `PEM_write_bio_PrivateKey`; pymobiledevice3 writes PKCS#8. Either works.
5. **HostID** is a new random UUID, uppercase, in 8-4-4-4-12 form (`generate_uuid`). **SystemBUID** is a host-wide UUID in the same format that usbmuxd generates once and persists ([SRC] `usbmuxd/src/conf.c:config_generate_uuid`). iDock should generate one SystemBUID per installation and keep it.

Stored pair record keys ([SRC] `common/userpref.h`): `DeviceCertificate`, `HostCertificate`, `RootCertificate` (data, PEM), `HostPrivateKey`, `RootPrivateKey` (data, PEM), `HostID`, `SystemBUID` (strings), `WiFiMACAddress` (string, from GetValue `WiFiAddress`), and `EscrowBag` (data, returned by the device on Pair).

Android: the platform's internal BouncyCastle is not usable, so bundle `org.bouncycastle:bcpkix-jdk18on` + `bcprov`. Use `KeyPairGenerator.getInstance("RSA")` with 2048 bits, `X509v3CertificateBuilder(X500Name(arrayOf<RDN>()), BigInteger.ZERO, ...)` and `JcaContentSignerBuilder("SHA256withRSA")`. Parse the device key with `PEMParser`, which yields `SubjectPublicKeyInfo` or `RSAPublicKey`. Store the pair record encrypted, with an Android Keystore-wrapped AES key, in app-private storage. Check that BC accepts serial 0 [INF: it does in recent versions; verify].

### 5.2 Pair request and the trust dialog

Request ([SRC] `lockdownd_do_pair`, `lockdownd_pair`):
```xml
<plist version="1.0">
<dict>
	<key>Label</key><string>idock</string>
	<key>PairRecord</key>
	<dict>
		<key>DeviceCertificate</key><data>…PEM base64…</data>
		<key>HostCertificate</key><data>…</data>
		<key>HostID</key><string>XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX</string>
		<key>RootCertificate</key><data>…</data>
		<key>SystemBUID</key><string>XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX</string>
	</dict>
	<key>PairingOptions</key>
	<dict><key>ExtendedPairingErrors</key><true/></dict>
	<key>ProtocolVersion</key><string>2</string>
	<key>Request</key><string>Pair</string>
</dict>
</plist>
```
- **Private keys are never sent.** libimobiledevice removes `RootPrivateKey` and `HostPrivateKey` before sending. pymobiledevice3 includes `RootPrivateKey` in the record it sends, which iDock must *not* copy. pymobiledevice3 also adds an optional `HostName` string at the top level and a `WiFiMACAddress` inside the record.
- Read `WiFiAddress` *before* sending Pair. The libimobiledevice comment says reading it afterwards fails on iOS 7 and forces a reconnect.
- Responses and errors ([SRC] `lockdown.c` error table; pymobiledevice3 `_request_pair`):
  - `PasswordProtected`: the device is locked. Show "Unlock your iPhone", then retry.
  - `PairingDialogResponsePending`: the Trust dialog is on screen. **Resend the same Pair request every ~1 s** until another answer arrives (pymobiledevice3 loops with a 1 s sleep).
  - `UserDeniedPairing`: the user tapped "Don't Trust". Stop. Tell the user to replug the cable to get the prompt again [INF].
  - Others: `InvalidHostID`, `PairingProhibitedOverThisConnection`, `MCChallengeRequired` (supervised devices; out of scope).
  - Success: no `Error`. The reply may contain `EscrowBag` (data). **Save it**: mobilebackup2 needs it.
- **ValidatePair** (same dict, `Request`="ValidatePair", no PairingOptions) is required only for iOS < 7 ([SRC] `lockdownd_client_new_with_handshake`). For iOS ≥ 7, go straight to StartSession.
- **Unpair**: `Request`="Unpair" with the record. The reply's `Request` may say "ValidatePair" (a firmware quirk handled in the source).

### 5.3 StartSession and TLS on the existing stream

Request ([SRC] `lockdownd_start_session`): `Label`, `Request`="StartSession", `HostID`, `SystemBUID`.
Reply: `SessionID` (string) and `EnableSessionSSL` (bool, true on all modern iOS [INF]). `InvalidHostID` means the device no longer knows us; delete the record and pair again.

If `EnableSessionSSL` is true, start TLS **immediately, on the same mux connection (port 62078)**, before sending another plist:
- **The host is the TLS client** and the device is the server ([SRC] `idevice.c:idevice_connection_enable_ssl`: `SSL_set_connect_state`, `GNUTLS_CLIENT`, `MBEDTLS_SSL_IS_CLIENT`).
- **Client certificate**: the device requests one. Sources disagree on which one to present, and both are evidently accepted:
  - libimobiledevice with OpenSSL and mbedTLS: **RootCertificate + RootPrivateKey**.
  - libimobiledevice with GnuTLS, go-ios (`deviceconnection.go:createClientTLSConn`) and pymobiledevice3 (`ssl_file`): **HostCertificate + HostPrivateKey**.
  - **Use the host cert/key**, and fall back to root if the handshake fails.
- **Server certificate verification**: none in the references (`InsecureSkipVerify`; the verify callbacks return OK). For iDock, accept any certificate, or optionally pin to `DeviceCertificate` or the chain from our root [INF].
- **TLS versions**: pymobiledevice3 uses min TLS 1.2, max TLS 1.3, for iOS ≥ 5 (SSLv3–TLS1.0 only before iOS 5). libimobiledevice caps at TLS 1.0 for iOS < 10, but only for very old OpenSSL builds. Ciphers: pymobiledevice3 sets `ALL:!aNULL:!eNULL:@SECLEVEL=0`, and libimobiledevice sets OpenSSL security level 0. Both exist to accommodate old devices. On Android, the Conscrypt defaults (TLS 1.2/1.3, ECDHE-RSA-AES-GCM) should work with iOS ≥ 11 [INF]. Do not restrict further.
- Android: `SSLContext.getInstance("TLS")`. Use a `KeyManager` built from an in-memory `KeyStore` (PKCS12) holding the host key with the chain [host, root], plus an accept-all `X509TrustManager`. Then `createSSLEngine()` with `useClientMode = true`. Drive the `SSLEngine` with `wrap`/`unwrap` over the mux connection's byte stream. Wrapping the mux connection as a fake `Socket` for `SSLSocketFactory.createSocket(socket, host, port, autoClose)` also works, if `MuxConnection` exposes blocking `InputStream`/`OutputStream`. That is simpler and recommended.
- After TLS is up, every lockdown plist goes through TLS with the same 4-byte length framing.
- **StopSession** (`Request`="StopSession", `SessionID`) ends the session. You do not need to stop the session in order to start services.

### 5.4 StartService and service connections

Request ([SRC] `lockdownd_build_start_service_request`): `Label`, `Request`="StartService", `Service`="<name>", and optionally `EscrowBag` (data from the pair record). The escrow bag is required for `com.apple.mobilebackup2` (`idevicebackup2` uses `lockdownd_start_service_with_escrow_bag`) and possibly for other services while the device is locked [INF].

Reply: `Port` (uint) and `EnableServiceSSL` (bool, optional). Errors: `InvalidService` (unknown service, or a developer service with no DDI mounted), `ServiceProhibited`, `ServiceLimit`, `PasswordProtected`, `SessionInactive`.

Then ([SRC] `service.c:service_client_new`):
1. Open a **new** mux TCP connection (new source port) to `Port`.
2. If `EnableServiceSSL` is true, do a TLS client handshake on it using the same certs as 5.3. Some DTX developer services do "TLS handshake only, then plaintext" (go-ios `EnableSessionSslHandshakeOnly`; libimobiledevice `service_disable_bypass_ssl(…,1)` in `debugserver.c`). That does not apply to the services in this document.
3. Speak the service protocol.

The lockdown connection may be closed (RST) after StartService. libimobiledevice frees the lockdown client right after StartService (`service_client_factory_start_service`). Each service port accepts one connection [INF].

### 5.5 Session flow summary
```
connect 62078 → QueryType → GetValue(DevicePublicKey, WiFiAddress)      (first time only)
  → Pair (loop on PairingDialogResponsePending; PasswordProtected → ask to unlock)
  → save record (+EscrowBag)
connect 62078 → QueryType → StartSession(HostID, SystemBUID) → TLS(client, host cert)
  → GetValue (full) / GetValue(Domain=…) / StartService(name[, EscrowBag]) → close
connect <Port> → [TLS if EnableServiceSSL] → service protocol
```

### 5.6 Lockdown error strings ([SRC] `lockdown.c` table)
InvalidResponse, MissingKey, MissingValue, GetProhibited, SetProhibited, RemoveProhibited, ImmutableValue, PasswordProtected, UserDeniedPairing, PairingDialogResponsePending, MissingHostID, InvalidHostID, SessionActive, SessionInactive, MissingSessionID, InvalidSessionID, MissingService, InvalidService, ServiceLimit, MissingPairRecord, SavePairRecordFailed, InvalidPairRecord, InvalidActivationRecord, MissingActivationRecord, ServiceProhibited, EscrowLocked, PairingProhibitedOverThisConnection, FMiPProtected, MCProtected, MCChallengeRequired.

### 5.7 Useful post-session GetValue domains

- `com.apple.mobile.battery`: `BatteryCurrentCapacity` (0–100), `BatteryIsCharging`, `ExternalConnected`, `ExternalChargeCapable`, `FullyCharged`, `GasGaugeCapability`, `HasBattery`. [SRC] go-ios `batterydiagnostics.go`
- `com.apple.disk_usage`: listed in `ideviceinfo.c`. Key names such as `TotalDiskCapacity`, `TotalDataCapacity`, `TotalDataAvailable` and `AmountDataAvailable` are [INF]. Read the whole domain and show what comes back.
- `com.apple.security.mac.amfi` / `DeveloperModeStatus` (bool, iOS 16+). [DOC] libimobiledevice commit f7819059 (2022-12-10), ideviceimagemounter.
- `com.apple.mobile.backup` / `WillEncrypt`. [SRC] `idevicebackup2.c`

---

## 6. Service protocols

General rule: honour `EnableServiceSSL` from the StartService reply instead of hard-coding SSL. Below, "SSL" means the expected value; it is [INF] unless stated.

### 6.1 AFC: `com.apple.afc` (media partition, `/private/var/mobile/Media` as `/`)

[SRC] `libimobiledevice/src/afc.h`, `afc.c`, `include/libimobiledevice/afc.h`, `tools/afcclient.c`.

**Packet header (40 bytes, all little-endian)**

| Off | Size | Field |
|---|---|---|
| 0 | 8 | magic ASCII `CFA6LPAA` (`43 46 41 36 4C 50 41 41`) |
| 8 | u64 | entire_length = 40 + header-data length + payload length |
| 16 | u64 | this_length = 40 + header-data length |
| 24 | u64 | packet_num: starts at 0 per client and is incremented **before** each send (so the first packet is 1) |
| 32 | u64 | operation |

Then come the "header data" (`this_length − 40` bytes) and the payload (`entire_length − this_length` bytes). The reply must carry the **same packet_num**. libimobiledevice treats a mismatch as an error.

**Reply handling** (`afc_receive_data`): read 40 bytes and check the magic. Read `this_length−40` bytes, then the rest up to `entire_length−40`. Reply operations:
- `0x01 STATUS`: the first u64 is an AFC error code (0 = success).
- `0x02 DATA`: the bytes are the result.
- `0x0E FILE_OPEN_RES`: the first u64 is the file handle.
- `0x13 FILE_TELL_RES`: the first u64 is the position.
- An empty packet (lengths == 40) is OK only for DATA.

**Operations needed**

| Op | Code | Header data | Payload | Reply |
|---|---|---|---|---|
| READ_DIR | `0x03` | path + `\0` | — | DATA: NUL-separated names (includes `.` and `..`) |
| GET_FILE_INFO | `0x0A` | path + `\0` | — | DATA: NUL-separated key, value, key, value… Keys: `st_size`, `st_blocks`, `st_nlink`, `st_ifmt` (`S_IFREG`/`S_IFDIR`/`S_IFLNK`/…), `st_mtime` and `st_birthtime` (**nanoseconds** since the epoch; birthtime on iOS 7+), `LinkTarget` for symlinks [INF] |
| GET_DEVINFO | `0x0B` | — | — | DATA: key/value list: `Model`, `FSTotalBytes`, `FSFreeBytes`, `FSBlockSize` |
| FILE_OPEN | `0x0D` | u64 mode + path + `\0` | — | FILE_OPEN_RES handle. Modes: 1 = r, 2 = r+ (create), 3 = w (create+truncate), 4 = w+, 5 = a, 6 = a+ |
| FILE_READ | `0x0F` | u64 handle, u64 length | — | DATA (≤ length bytes; 0 bytes = EOF) |
| FILE_WRITE | `0x10` | u64 handle | file bytes | STATUS |
| FILE_SEEK | `0x11` | u64 handle, u64 whence (0 = SET, 1 = CUR, 2 = END), i64 offset | — | STATUS |
| FILE_CLOSE | `0x14` | u64 handle | — | STATUS |
| REMOVE_PATH | `0x08` | path `\0` | — | STATUS |
| MAKE_DIR | `0x09` | path `\0` | — | STATUS |
| RENAME_PATH | `0x18` | from `\0` to `\0` | — | STATUS |
| REMOVE_PATH_AND_CONTENTS | `0x22` | path `\0` (iOS 6+) | — | STATUS |

The handle is opaque: echo its 8 bytes back exactly as received. Chunk sizes: `afcclient` uses 1 MiB (`0x100000`) per read and write, and pymobiledevice3 uses up to 4 MiB. Start with 1 MiB, or 256 KiB to stay under the 128 KiB mux window per round trip; tune on hardware.
AFC error codes: 0 success, 1 unknown, 2 op header invalid, 3 no resources, 4 read error, 5 write error, 6 unknown packet type, 7 invalid arg, 8 object not found, 9 object is dir, 10 permission denied, 11 service not connected, 12 timeout, 13 too much data, 14 end of data, 15 op not supported, 16 object exists, 17 object busy, 18 no space left, 19 would block, 20 IO error, 21 interrupted, 22 in progress, 23 internal error.
SSL: no [INF]. iOS caveats: none known. AFC is still served by lockdown on iOS 17/18. pymobiledevice3 additionally knows an RSD name, `com.apple.afc.shim.remote`.

**Photos**: `/DCIM/1xxAPPLE/IMG_xxxx.HEIC|JPG|MOV|PNG` (plus `.AAE` sidecars). This gives read access to camera-roll originals **present on the device**. With iCloud "Optimize Storage", many originals are not local [INF]. Writing into the Photos library is not possible through AFC, because the library DB is under `/PhotoData` [INF].

### 6.2 house_arrest: `com.apple.mobile.house_arrest` (per-app containers)

[SRC] `house_arrest.c`, `include/libimobiledevice/house_arrest.h`. Send an XML plist `{"Command":"VendDocuments","Identifier":"<bundle id>"}`. `VendContainer` is the other known command. The reply is `{"Status":"Complete"}` or `{"Error":"…"}` (e.g. `ApplicationLookupFailed`, `InstallationLookupFailed` [INF]). After success, **the same connection speaks AFC** (`afc_client_new_from_house_arrest_client` reuses the socket), rooted at the app's `Documents`. `VendDocuments` works only for apps with `UIFileSharingEnabled` ("Files"/iTunes file sharing). `VendContainer` gives the whole container, but only for development-signed apps on recent iOS [INF]. Find eligible apps via installation_proxy Browse with the attribute `UIFileSharingEnabled`. SSL: no [INF].

### 6.3 diagnostics_relay: `com.apple.mobile.diagnostics_relay`

[SRC] `diagnostics_relay.c`, go-ios `ios/diagnostics/*`, pymobiledevice3 `services/diagnostics.py`. Messages are plist dicts with `Request`. Replies carry `Status` (`"Success"`, `"Failure"`, `"UnknownRequest"`) and usually `Diagnostics` (a dict).

| Request dict | Use |
|---|---|
| `{"Request":"IORegistry","EntryClass":"IOPMPowerSource"}` | **Battery health**. The reply is `Diagnostics.IORegistry`, a dict with `CycleCount`, `DesignCapacity`, `NominalChargeCapacity`, `AppleRawMaxCapacity`, `AppleRawCurrentCapacity`, `CurrentCapacity`, `Temperature` (centi-°C [INF]), `Voltage` (mV), `InstantAmperage`, `IsCharging`, `AtCriticalLevel`. Key names are from go-ios `request.go`. Health ≈ `NominalChargeCapacity` (or `AppleRawMaxCapacity`) / `DesignCapacity` [INF; compare with Settings on a real device]. |
| `{"Request":"IORegistry","EntryName":"…"}` / `"CurrentPlane":"IODeviceTree"` | Other registry queries. `AppleSmartBattery` and `AppleARMPMUCharger` are suggested by `idevicediagnostics` [INF which keys are useful]. |
| `{"Request":"All"}` / `"GasGauge"` / `"WiFi"` / `"NAND"` / `"HDMI"` | Generic diagnostics. GasGauge has `CycleCount`, `DesignCapacity`, `FullChargeCapacity` (go-ios). |
| `{"Request":"MobileGestalt","MobileGestaltKeys":[…]}` | Returns `Status`=`MobileGestaltDeprecated` on **iOS ≥ 17.4** ([SRC] pymobiledevice3 `diagnostics.py`). Do not rely on it. |
| `{"Request":"Restart","WaitForDisconnect":true,"DisplayPass":true,"DisplayFail":true}` | Reboot. Flags as in go-ios `Reboot`; all are optional. |
| `{"Request":"Shutdown", …same flags}` | Power off |
| `{"Request":"Sleep"}` | Sleep |
| `{"Request":"Goodbye"}` | Close politely |

Sent as XML ([SRC] `diagnostics_relay_send`). SSL: no [INF]. Works on iOS 17/18 over lockdown [INF, consistent with pymobiledevice3 using it unchanged].

### 6.4 installation_proxy: `com.apple.mobile.installation_proxy`

[SRC] `installation_proxy.c`, go-ios `installationproxy.go`.
- **Browse**: `{"Command":"Browse","ClientOptions":{"ApplicationType":"User"|"System"|"Any","ReturnAttributes":["CFBundleIdentifier","CFBundleDisplayName","CFBundleName","CFBundleShortVersionString","CFBundleVersion","ApplicationType","UIFileSharingEnabled","StaticDiskUsage","DynamicDiskUsage","Path","Container","SignerIdentity"]}}`. Omitting `ReturnAttributes` returns everything, which is slow and large. The disk-usage keys are [INF]. The device streams several replies: `{"Status":"BrowsingApplications","CurrentIndex":n,"CurrentAmount":k,"Total":t,"CurrentList":[{…app…}]}`, and finishes with `{"Status":"Complete"}`. go-ios caps the number of chunks to avoid an endless loop.
- **Uninstall**: `{"Command":"Uninstall","ApplicationIdentifier":"<id>","ClientOptions":{}}`. Progress messages carry `Status` and `PercentComplete`. The end is `Status`=`Complete`, or an `Error` (+ `ErrorDescription`).
- **Install**: upload the .ipa via AFC to `PublicStaging/<name>.ipa` (go-ios zipconduit uses `PublicStaging/`). Then send `{"Command":"Install","PackagePath":"PublicStaging/<name>.ipa","ClientOptions":{}}` and follow progress until `Complete`. The IPA must already be signed so that this device accepts it (App Store, enterprise or development profile). iDock does not re-sign.
- **Lookup**: `{"Command":"Lookup","ClientOptions":{"BundleIDs":[…],"ReturnAttributes":[…]}}`. The reply has `LookupResult`.
- XML plists. SSL: no [INF].

### 6.5 notification_proxy: `com.apple.mobile.notification_proxy`

[SRC] `notification_proxy.c`. Messages: `{"Command":"PostNotification","Name":n}`, `{"Command":"ObserveNotification","Name":n}`, `{"Command":"Shutdown"}`. Device events: `{"Command":"RelayNotification","Name":n}`, and `{"Command":"ProxyDeath"}` on close. Useful names (header constants): `com.apple.mobile.application_installed`, `…application_uninstalled`, `com.apple.mobile.lockdown.device_name_changed`, `…disk_usage_changed`, and the backup sync names `com.apple.itunes-mobdev.syncWillStart`, `syncDidStart`, `syncDidFinish`, `syncLockRequest`, plus `com.apple.itunes-client.syncCancelRequest`. A variant also exists: `com.apple.mobile.insecure_notification_proxy` (pymobiledevice3).

### 6.6 Logs: `com.apple.syslog_relay` and `com.apple.os_trace_relay`

- **syslog_relay** [SRC] `syslog_relay.c`: after connecting, the device streams raw text. Each log line ends with `\0` or `\n`. The non-raw mode drops the NUL bytes. No request is needed. Text lines look like `Mon DD HH:MM:SS name process[pid] <Level>: message` (format parsed in `tools/idevicesyslog.c`).
- **os_trace_relay** [SRC] `ostrace.c`, `include/libimobiledevice/ostrace.h`. Used by `idevicesyslog` for iOS ≥ 9.
  - Send: a u32 **big-endian** length + **binary** plist `{"Request":"StartActivity","Pid":0xFFFFFFFF (all processes),"MessageFilter":0xFFFF,"StreamFlags":0x3C}`.
  - Receive: `u8 type` + u32 length + payload. The length is **big-endian if type==1** and **little-endian if type==2**. The first reply is a plist with `Status`=`RequestSuccessful`. After that, type-2 frames carry binary log entries.
  - Entry layout (packed, little-endian [INF on endianness; matches ARM]): marker u8 (=2), type u32 (2 or 8), header_size u32 (=0x81=129), pid u32, procid u64, procuuid[16], procpath_len u16, aid u64, paid u64, time_sec u64, time_usec u32, unk u8, **level u8** (0 Notice, 1 Info, 2 Debug, 0x10 Error, 0x11 Fault), 6× unk u8, timestamp u64, thread_id u32, unk u32, imageuuid[16], imagepath_len u16, message_len u32, offset u32, subsystem_len u16, unk u16, category_len u16, unk u16, unk u32. Strings follow at `header_size`: procpath, imagepath, message, subsystem, category (each NUL-terminated, with the stated lengths).
  - Other requests: `{"Request":"PidList"}`, `{"Request":"CreateArchive"}` (logarchive).
  - Recommendation: implement syslog_relay first (trivial), then os_trace_relay.

### 6.7 screenshotr: `com.apple.mobile.screenshotr`

[SRC] `screenshotr.c`, `device_link_service.c`. This service exists **only when a Developer Disk Image is mounted** (`idevicescreenshot` prints "Remember that you have to mount the Developer disk image"). The protocol is DeviceLink: binary plist arrays with 4-byte BE length framing.
1. Receive `["DLMessageVersionExchange", major, minor]`. Send `["DLMessageVersionExchange","DLVersionsOk", 400]` (`SCREENSHOTR_VERSION_INT1=400`). Receive `["DLMessageDeviceReady"]`.
2. Send `["DLMessageProcessMessage", {"MessageType":"ScreenShotRequest"}]`. Receive `["DLMessageProcessMessage", {"MessageType":"ScreenShotReply","ScreenShotData":<data: PNG or TIFF>}]`.
3. Send `["DLMessageDisconnect","___EmptyParameterString___"]`.

Requirements:
- **iOS ≤ 15**: mount the version-matched `DeveloperDiskImage.dmg` + `.signature` via `com.apple.mobile.mobile_image_mounter`.
- **iOS 16**: Developer Mode must also be enabled ([DOC] libimobiledevice commit f7819059).
- **iOS 17+**: only the **personalized** DDI exists. Its manifest has to be signed per device by Apple's signing server, using a nonce and identifiers fetched from the device ([DOC] go-ios `imagemounter` docs, https://pkg.go.dev/github.com/danielpaulus/go-ios@v1.0.125/ios/imagemounter). Even then, `com.apple.mobile.screenshotr` over plain lockdown failed with "SecureStartService of com.apple.mobile.screenshotr Failed with 0xe8000022 The service is invalid" on an iOS 17.0 iPad in an open idb report ([DOC] https://www.github.com/facebook/idb/issues/850). pymobiledevice3 marks the legacy screenshotr command as "deprecated; prefer `dvt screenshot`", which runs over the iOS 17 tunnel.
- Conclusion: screenshots on iOS 17+ realistically need the RSD tunnel and DTX (§7). Treat this as **not feasible for v1**.

### 6.8 mobilebackup2: `com.apple.mobilebackup2` (high level)

[SRC] `mobilebackup2.c`, `device_link_service.c`, `tools/idevicebackup2.c`.

Setup:
- Start with `StartService` **including `EscrowBag`**.
- DeviceLink version exchange (major 400, `MBACKUP2_VERSION_INT1`).
- Then `["DLMessageProcessMessage", {"MessageName":"Hello","SupportedProtocolVersions":[2.0, 2.1]}]`. The reply has `MessageName`="Response", `ProtocolVersion` and `ErrorCode`.

Before the backup (as idevicebackup2 does):
- Start notification_proxy. Post `syncWillStart`.
- Open `/com.apple.itunes.lock_sync` via AFC and take an exclusive `FILE_LOCK` (op `0x1B`, mode `AFC_LOCK_EX`=6). Post `syncLockRequest`, wait for the lock, then post `syncDidStart`.
- Read `com.apple.mobile.backup/WillEncrypt` via lockdown.

Request (wrapped like Hello in `["DLMessageProcessMessage", dict]`): `{"MessageName":"Backup","TargetIdentifier":udid,"SourceIdentifier":udid?,"Options":{…e.g. "ForceFullBackup":true}}`. Other requests: `Restore`, `Info`, `List`, `Unback`, `EnableCloudBackup`, and `ChangePassword` [INF].

Then the **device drives** the exchange. It sends `DLMessage*` commands that the host must execute against a local backup directory, replying each time with `["DLMessageStatusResponse", code, "___EmptyParameterString___" | msg, dict]`:

| Command | Host action | Minimal backup client |
|---|---|---|
| `DLMessageUploadFiles` | Device → host file stream | **Required** |
| `DLMessageDownloadFiles` | Host → device (Status.plist / Manifest on incremental and restore) | **Required** (answer "not found" for absent files) |
| `DLMessageGetFreeDiskSpace` | Report host free bytes | **Required** |
| `DLMessageCreateDirectory` | Create a directory | **Required** |
| `DLMessageMoveFiles` / `DLMessageMoveItems` | Move | **Required** |
| `DLMessageRemoveFiles` / `DLMessageRemoveItems` | Delete | **Required** |
| `DLContentsOfDirectory` | List | **Required** |
| `DLMessageCopyItem` | Copy | **Required** |
| `DLMessagePurgeDiskSpace` | Free space | Reply with an error |
| `DLMessageProcessMessage` | Final result (`ErrorCode`) | **Required** |
| `DLMessageDisconnect` | End | **Required** |

Raw file streams use [SRC] `mb2_handle_receive_files` framing: u32 BE length + device-side directory name, then u32 BE length + file name, then blocks of `u32 BE length (incl. code byte)` + `u8 code` + data. Codes: `0x0C` file data, `0x00` success / end of file, `0x06` local error, `0x0B` remote error. The host also writes `Info.plist` itself. The device produces `Manifest.plist`, `Manifest.db` and `Status.plist`.

Effort: **XL**. Port the logic of `idevicebackup2.c` (about 2.8 k lines) in your own words. Encrypted backups need the user's password for restore. Backup is achievable as a "dump to folder"; restore is higher risk.

---

## 7. iOS 17+ changes

[DOC] pymobiledevice3 `misc/RemoteXPC.md` and `docs/guides/ios17-tunnels.md` (cloned 2026-11-13). [SRC] go-ios `ios/tunnel/tunnel_lockdown.go`.

- **iOS 17.0 (Sept 2023)** introduced RemoteXPC: XPC dictionaries over HTTP/2 to a RemoteServiceDiscovery (RSD) port (`58783` untrusted). There is a USB-Ethernet (CDC-NCM) IPv6 link and a QUIC or TLS-PSK "trusted tunnel" negotiated through `com.apple.internal.dt.coredevice.untrusted.tunnelservice`, using SRP pairing with the fixed password `000000`. This is a **separate pairing** from the lockdown pair record.
- **Developer services moved behind the tunnel**: DVT/instruments (DTX), the screenshot replacement (`dvt screenshot`), debugserver, process lists, and so on. They require a mounted personalized DDI, Developer Mode, and the tunnel. Lockdown services are also reachable *through* the tunnel as `*.shim.remote` names after an `RSDCheckin` request (`{"Label":…,"ProtocolVersion":"2","Request":"RSDCheckin","EscrowBag":…}`).
- **The classic lockdown services still work over plain usbmux on iOS 17/18**: AFC, house_arrest, installation_proxy, notification_proxy, diagnostics_relay, syslog_relay, os_trace_relay, mobilebackup2 and mobile_image_mounter. Evidence: libimobiledevice 1.4 and pymobiledevice3 still open these via lockdown, and pymobiledevice3 tunnels only for `developer` commands ("So `developer` commands (and a few others) need such a tunnel"). [DOC/INF]
- **iOS 17.4+** added the lockdown service **`com.apple.internal.devicecompute.CoreDeviceProxy`** ([SRC] go-ios `tunnel_lockdown.go`; pymobiledevice3 "iOS 17.0-17.3 has no CoreDeviceProxy"). It is started via ordinary StartService over usbmux. After a parameter exchange, the connection carries **raw IPv6 packets** (go-ios reads a 40-byte IPv6 header plus the payload). The client then needs a userspace TCP/IPv6 stack, plus HTTP/2 + XPC to reach RSD. pymobiledevice3 does this without root ("userspace tunnel"). An Android app could too, but with very large effort (XL+).
- **iOS 17.4+**: the diagnostics_relay MobileGestalt request returns `MobileGestaltDeprecated` ([SRC] pymobiledevice3).
- **Developer Mode** (iOS 16+): needed for DDI mounting and developer services. Read it with `GetValue(Domain="com.apple.security.mac.amfi", Key="DeveloperModeStatus")`. The user can enable it in Settings → Privacy & Security only after a developer tool has prompted for it once [INF].
- None of the iDock v1 features depend on the tunnel, except screenshot on iOS 17+ (and live screen capture).
