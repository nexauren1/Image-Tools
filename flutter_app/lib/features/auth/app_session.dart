import 'dart:async';

import 'package:app_links/app_links.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:flutter/foundation.dart';
import 'package:url_launcher/url_launcher.dart';

import 'auth_repository.dart';
import 'payment_repository.dart';
import 'subscription_store.dart';

class AppSession extends ChangeNotifier {
  AppSession({
    AuthRepository? authRepository,
    FirebaseFirestore? firestore,
  })  : auth = authRepository ?? AuthRepository(),
        _firestore = firestore ?? FirebaseFirestore.instance;

  final AuthRepository auth;
  final FirebaseFirestore _firestore;
  final AppLinks _appLinks = AppLinks();

  StreamSubscription<User?>? _authSubscription;
  StreamSubscription<DocumentSnapshot<Map<String, dynamic>>>? _premiumSubscription;
  StreamSubscription<Uri>? _linkSubscription;
  bool _isPremium = false;
  bool _isReconciling = false;
  bool _disposed = false;

  User? get user => auth.currentUser;
  bool get isPremium => _isPremium;

  Future<void> initialize() async {
    _linkSubscription = _appLinks.uriLinkStream.listen(
      (uri) => unawaited(_handlePayPalReturn(uri)),
      onError: (Object _) {},
    );

    _authSubscription = auth.authStateChanges.listen(
      (user) => unawaited(_handleAuthChanged(user)),
      onError: (Object _) {},
    );

    final initialUri = await _appLinks.getInitialLink();
    await _handlePayPalReturn(initialUri);
  }

  Future<void> _handleAuthChanged(User? user) async {
    await _premiumSubscription?.cancel();
    _premiumSubscription = null;
    _isPremium = false;
    _notify();

    if (user == null) return;

    unawaited(refreshPremiumFromServer());
    _premiumSubscription = _firestore
        .collection('users')
        .doc(user.uid)
        .snapshots(includeMetadataChanges: true)
        .listen(
      (snapshot) {
        if (snapshot.metadata.isFromCache) return;
        _isPremium = snapshot.data()?['premium'] == true;
        _notify();
      },
      onError: (Object _) {},
    );

    unawaited(_reconcileSavedSubscription());
  }

  Future<void> refreshPremiumFromServer() async {
    final current = user;
    if (current == null) {
      _isPremium = false;
      _notify();
      return;
    }

    try {
      final snapshot = await _firestore
          .collection('users')
          .doc(current.uid)
          .get(const GetOptions(source: Source.server));
      _isPremium = snapshot.data()?['premium'] == true;
    } catch (_) {
      // Preserve the last server-confirmed value when the network is unavailable.
    }
    _notify();
  }

  Future<void> signInEmail(String email, String password) =>
      auth.signInEmail(email, password);

  Future<void> registerEmail(String email, String password) =>
      auth.registerEmail(email, password);

  Future<void> signInGoogle() => auth.signInGoogle();

  Future<void> sendPasswordReset(String email) =>
      auth.sendPasswordReset(email);

  Future<void> sendEmailVerification() => auth.sendEmailVerification();

  Future<void> reloadUser() async {
    await auth.reloadCurrentUser();
    await refreshPremiumFromServer();
    _notify();
  }

  Future<void> signOut() async {
    await auth.signOut();
    _isPremium = false;
    _notify();
  }

  Future<void> startPremiumCheckout() async {
    final current = user;
    if (current == null) {
      throw StateError('Inicia sessão antes de subscrever o Premium.');
    }
    final token = await auth.idToken();
    if (token == null || token.isEmpty) {
      throw StateError('A sessão expirou. Inicia sessão novamente.');
    }

    final subscription = await PaymentRepository.createSubscription(token);
    await SubscriptionStore.save(current.uid, subscription.subscriptionId);

    final opened = await launchUrl(
      Uri.parse(subscription.approveUrl),
      mode: LaunchMode.externalApplication,
    );
    if (!opened) {
      throw StateError('Não foi possível abrir a página de aprovação do PayPal.');
    }
  }

  Future<bool> cancelPremiumSubscription() async {
    final current = user;
    if (current == null) {
      throw StateError('Inicia sessão para gerir a assinatura.');
    }
    final token = await auth.idToken();
    if (token == null || token.isEmpty) {
      throw StateError('A sessão expirou. Inicia sessão novamente.');
    }
    final subscriptionId = await SubscriptionStore.get(current.uid);
    final synced = await PaymentRepository.cancelSubscription(
      token,
      subscriptionId: subscriptionId,
    );
    if (synced) {
      await SubscriptionStore.clear(current.uid);
      await refreshPremiumFromServer();
    }
    return synced;
  }

  Future<void> _handlePayPalReturn(Uri? uri) async {
    if (uri == null ||
        uri.scheme != 'imagetools' ||
        uri.host != 'paypal' ||
        uri.path != '/return') {
      return;
    }

    final subscriptionId = uri.queryParameters['subscriptionId'] ??
        uri.queryParameters['ba_token'] ??
        uri.queryParameters['orderId'];
    if (subscriptionId == null || subscriptionId.trim().isEmpty) return;

    final uid = user?.uid;
    await SubscriptionStore.save(uid, subscriptionId);
    if (uid != null) {
      await _reconcileSubscription(subscriptionId);
    }
  }

  Future<void> _reconcileSavedSubscription() async {
    final current = user;
    if (current == null) return;
    final subscriptionId = await SubscriptionStore.get(current.uid);
    if (subscriptionId != null) {
      await _reconcileSubscription(subscriptionId);
    }
  }

  Future<void> _reconcileSubscription(String subscriptionId) async {
    if (_isReconciling || subscriptionId.isEmpty || user == null) return;
    _isReconciling = true;
    try {
      for (var attempt = 0; attempt < 12; attempt++) {
        final token = await auth.idToken(forceRefresh: attempt > 0);
        if (token != null && token.isNotEmpty) {
          try {
            final active = await PaymentRepository.refreshSubscription(
              token,
              subscriptionId: subscriptionId,
            );
            if (active) {
              await refreshPremiumFromServer();
              return;
            }
          } catch (_) {
            // PayPal approval and the backend webhook can complete at different times.
          }
        }
        if (attempt < 11) {
          await Future<void>.delayed(const Duration(seconds: 3));
        }
      }
    } finally {
      _isReconciling = false;
    }
  }

  void _notify() {
    if (!_disposed) notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    unawaited(_authSubscription?.cancel());
    unawaited(_premiumSubscription?.cancel());
    unawaited(_linkSubscription?.cancel());
    super.dispose();
  }
}
