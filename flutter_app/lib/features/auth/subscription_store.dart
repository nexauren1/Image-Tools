import 'package:shared_preferences/shared_preferences.dart';

abstract final class SubscriptionStore {
  static const String _legacyKey = 'paypal_subscription_id';
  static const String _keyPrefix = 'paypal_subscription_id_';

  static String _keyForUser(String uid) => '$_keyPrefix$uid';

  static Future<String?> get(String? uid) async {
    final preferences = await SharedPreferences.getInstance();
    if (uid != null && uid.isNotEmpty) {
      final userValue = preferences.getString(_keyForUser(uid));
      if (userValue != null && userValue.isNotEmpty) return userValue;
    }
    final legacyValue = preferences.getString(_legacyKey);
    return legacyValue == null || legacyValue.isEmpty ? null : legacyValue;
  }

  static Future<void> save(String? uid, String subscriptionId) async {
    if (subscriptionId.trim().isEmpty) return;
    final preferences = await SharedPreferences.getInstance();
    if (uid != null && uid.isNotEmpty) {
      await preferences.setString(_keyForUser(uid), subscriptionId);
      await preferences.remove(_legacyKey);
    } else {
      await preferences.setString(_legacyKey, subscriptionId);
    }
  }

  static Future<void> clear(String? uid) async {
    final preferences = await SharedPreferences.getInstance();
    if (uid != null && uid.isNotEmpty) {
      await preferences.remove(_keyForUser(uid));
    }
    await preferences.remove(_legacyKey);
  }
}
