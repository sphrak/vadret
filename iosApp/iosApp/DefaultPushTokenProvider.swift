import FirebaseMessaging
import VadretShared

final class DefaultPushTokenProvider: PushTokenProvider {
    func fetchToken(completion: @escaping (String?, String?) -> Void) {
        Messaging.messaging().token { token, error in
            completion(token, error?.localizedDescription)
        }
    }
}
