# Entrybook Android app

The merchant and personal ledger app for GWL Enterprises. Shop owners keep customers, vendors, sales, udhar, and expenses. Personal accounts keep friends and family, money given and received, and personal spending.

Administrators stay on the website. This app is for merchant accounts only.

## Build

From this folder:

```bash
export JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/openjdk-21.0.1/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew :app:assembleDebug
```

The installable file is written outside this folder, at the project root:

```text
output/entrybook.apk
```

A release build writes `output/entrybook-release.apk`. `assembleDebug` does not need a phone connected. Install the file with `adb install` once a device is attached.

Package name: `com.greenworldlighting.entrybook`. Version: 1.0.0. Minimum Android 8.0.

## Server

The app calls `https://entrybook.greenworldlighting.com/api/v1/` unless a server address is set on the login screen. A local API is `http://10.0.2.2:3000/api/v1` from the emulator, or the computer’s LAN address from a phone. The address must end at `/api/v1`.

The debug build allows plain HTTP. A release build expects HTTPS.

## Accounts

New accounts start as personal use, with one year already active. A new shop account gets six months already active. Switching between personal and shop does not change the subscription end date. An existing account with no usage set stays a shop.

Passwords must be at least 6 characters. An administrator sets a forgotten merchant password from the website.

## Personal

- Friends list shows who owes you, in green, or who you owe, in red.
- Money you gave is green. Money you received is red.
- Settle writes the opposite entry for the current net, so the balance returns to zero. Older rows stay in the history, including a Settled row for that amount.
- The + button above More adds a personal expense or a people transaction.

## Shop

- Customers show Collect in red when money is still due, and Advance in green when the customer has paid extra.
- Vendors show how much has been paid. Settle sets that paid total back to zero and keeps every past payment in History, plus one Settled row for the total up to that day. Later payments start again from zero.
- A customer Settle clears Collect with a receipt, or clears Advance with a sale, for the same amount.
- The + button above More adds a personal expense, a business expense, or a vendor expense.

Posted sales, receipts, and payments can have their amount edited. An edited row is marked Edited, and the detail page keeps the earlier amounts.
