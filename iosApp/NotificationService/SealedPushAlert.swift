import Foundation
import Security
import os

// Opens what the server sealed with this phone's public key, the same way the app's Kotlin code does.
struct SealedPushAlert {
    let title: String
    let body: String
    let kind: String
    let data: [String: String]

    init?(userInfo: [AnyHashable: Any]) {
        guard let appGroup = Bundle.main.object(forInfoDictionaryKey: appGroupKey) as? String else {
            logger.error("Info.plist has no \(appGroupKey, privacy: .public)")
            return nil
        }
        guard let sealed = userInfo[sealedField] as? String,
              let opened = open(sealed: sealed, appGroup: appGroup) else {
            return nil
        }
        guard let sealedContent = try? JSONDecoder().decode(SealedContent.self, from: opened) else {
            logger.warning("A sealed push alert holds no title and body")
            return nil
        }
        title = sealedContent.title
        body = sealedContent.body
        kind = sealedContent.data[kindField] ?? ""
        data = sealedContent.data
    }
}

private struct SealedContent: Decodable {
    let title: String
    let body: String
    let data: [String: String]
}

private func open(sealed: String, appGroup: String) -> Data? {
    guard let sealedData = Data(base64Encoded: sealed) else {
        logger.warning("A sealed push alert is not base64")
        return nil
    }
    guard let privateKey = pushAlertKey(appGroup: appGroup) else {
        return nil
    }
    var error: Unmanaged<CFError>?
    guard let opened = SecKeyCreateDecryptedData(privateKey, .eciesEncryptionCofactorVariableIVX963SHA256AESGCM, sealedData as CFData, &error) else {
        logger.warning("This phone's key cannot open a sealed push alert: \(error?.takeRetainedValue().localizedDescription ?? "", privacy: .public)")
        return nil
    }
    return opened as Data
}

private func pushAlertKey(appGroup: String) -> SecKey? {
    let query: [String: Any] = [
        kSecClass as String: kSecClassKey,
        kSecAttrKeyClass as String: kSecAttrKeyClassPrivate,
        kSecAttrApplicationTag as String: Data(pushAlertKeyAlias.utf8),
        kSecAttrAccessGroup as String: appGroup,
        kSecReturnRef as String: true
    ]
    var item: CFTypeRef?
    let status = SecItemCopyMatching(query as CFDictionary, &item)
    guard status == errSecSuccess, let item, CFGetTypeID(item) == SecKeyGetTypeID() else {
        logger.warning("The Keychain answered \(status, privacy: .public) when asked for the push alert key")
        return nil
    }
    return (item as! SecKey)
}

private let logger = Logger(subsystem: "se.vadretapp.vadret", category: "pushalert")

private let appGroupKey = "AppGroup"
// The app makes the key with this tag in IosPushAlertModule.
private let pushAlertKeyAlias = "se.vadretapp.vadret.pushalert"
private let sealedField = "sealed"
private let kindField = "kind"
