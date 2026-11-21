import 'package:firebase_auth/firebase_auth.dart';
import 'package:google_sign_in/google_sign_in.dart';

class AuthRepository {
  AuthRepository({FirebaseAuth? auth}) : _auth = auth ?? FirebaseAuth.instance;

  static const String googleServerClientId =
      '1062438516387-i2plli4d5mq9gkaauik6q2tf4nb1bqq7.apps.googleusercontent.com';

  static Future<void>? _googleInitialization;
  final FirebaseAuth _auth;

  User? get currentUser => _auth.currentUser;
  Stream<User?> get authStateChanges => _auth.authStateChanges();

  Future<void> signInEmail(String email, String password) async {
    await _auth.signInWithEmailAndPassword(
      email: email.trim(),
      password: password,
    );
  }

  Future<void> registerEmail(String email, String password) async {
    final credential = await _auth.createUserWithEmailAndPassword(
      email: email.trim(),
      password: password,
    );
    final user = credential.user;
    if (user == null) {
      throw StateError('Não foi possível criar a conta.');
    }
    await user.sendEmailVerification();
  }

  Future<void> sendPasswordReset(String email) async {
    if (!email.trim().contains('@')) {
      throw const FormatException('Introduz um endereço de email válido.');
    }
    await _auth.sendPasswordResetEmail(email: email.trim());
  }

  Future<void> sendEmailVerification() async {
    final user = _auth.currentUser;
    if (user == null) {
      throw StateError('Inicia sessão primeiro.');
    }
    await user.sendEmailVerification();
  }

  Future<void> reloadCurrentUser() async {
    await _auth.currentUser?.reload();
  }

  Future<void> signInGoogle() async {
    _googleInitialization ??= GoogleSignIn.instance.initialize(
      serverClientId: googleServerClientId,
    );
    await _googleInitialization!;

    final account = await GoogleSignIn.instance.authenticate();
    final idToken = account.authentication.idToken;
    if (idToken == null || idToken.isEmpty) {
      throw StateError('O Google não devolveu um token de autenticação.');
    }

    final credential = GoogleAuthProvider.credential(idToken: idToken);
    await _auth.signInWithCredential(credential);
  }

  Future<String?> idToken({bool forceRefresh = false}) async {
    return _auth.currentUser?.getIdToken(forceRefresh);
  }

  Future<void> signOut() async {
    try {
      await GoogleSignIn.instance.signOut();
    } catch (_) {
      // Email/password users may not have an active Google session.
    }
    await _auth.signOut();
  }
}
