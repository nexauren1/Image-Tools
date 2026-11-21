import 'dart:convert';

import 'package:http/http.dart' as http;

class SubscriptionStart {
  const SubscriptionStart({
    required this.subscriptionId,
    required this.approveUrl,
  });

  final String subscriptionId;
  final String approveUrl;
}

class PaymentException implements Exception {
  const PaymentException({
    required this.code,
    required this.stage,
    required this.httpStatus,
    required this.message,
  });

  final String code;
  final String stage;
  final int httpStatus;
  final String message;

  @override
  String toString() => message;
}

abstract final class PaymentRepository {
  static const String workerUrl =
      'https://steep-pine-34fe.nexaurenstore.workers.dev';

  static final http.Client _client = http.Client();

  static Future<SubscriptionStart> createSubscription(String token) async {
    final data = await _request(
      uri: Uri.parse('$workerUrl/paypal/create-subscription'),
      token: token,
      method: 'POST',
      body: const <String, Object?>{},
      stage: 'subscription-create',
    );
    final subscriptionId = data['subscriptionId']?.toString() ?? '';
    final approveUrl = data['approveUrl']?.toString() ?? '';
    if (subscriptionId.isEmpty || approveUrl.isEmpty) {
      throw const PaymentException(
        code: 'INVALID_SUBSCRIPTION_RESPONSE',
        stage: 'subscription-create',
        httpStatus: 200,
        message: 'O PayPal não devolveu os dados necessários para iniciar a assinatura.',
      );
    }
    final uri = Uri.tryParse(approveUrl);
    if (uri == null || uri.scheme != 'https') {
      throw const PaymentException(
        code: 'INVALID_APPROVAL_URL',
        stage: 'subscription-create',
        httpStatus: 200,
        message: 'O endereço de aprovação do PayPal não é válido.',
      );
    }
    return SubscriptionStart(
      subscriptionId: subscriptionId,
      approveUrl: approveUrl,
    );
  }

  static Future<bool> refreshSubscription(
    String token, {
    String? subscriptionId,
  }) async {
    final baseUri = Uri.parse('$workerUrl/paypal/subscription-status');
    final uri = subscriptionId == null || subscriptionId.trim().isEmpty
        ? baseUri
        : baseUri.replace(queryParameters: <String, String>{
            'subscriptionId': subscriptionId.trim(),
          });
    final data = await _request(
      uri: uri,
      token: token,
      method: 'GET',
      stage: 'subscription-status',
    );
    return data['premium'] == true;
  }

  static Future<bool> cancelSubscription(
    String token, {
    String? subscriptionId,
  }) async {
    final data = await _request(
      uri: Uri.parse('$workerUrl/paypal/cancel-subscription'),
      token: token,
      method: 'POST',
      body: <String, Object?>{
        'subscriptionId': subscriptionId ?? '',
      },
      stage: 'subscription-cancel',
    );
    return data['entitlementSynced'] != false;
  }

  static Future<Map<String, dynamic>> _request({
    required Uri uri,
    required String token,
    required String method,
    required String stage,
    Map<String, Object?>? body,
  }) async {
    if (token.trim().isEmpty) {
      throw const PaymentException(
        code: 'MISSING_AUTH_TOKEN',
        stage: 'authentication',
        httpStatus: 401,
        message: 'A sessão expirou. Inicia sessão novamente.',
      );
    }

    if (uri.scheme != 'https') {
      throw const PaymentException(
        code: 'INSECURE_WORKER_URL',
        stage: 'configuration',
        httpStatus: 0,
        message: 'O serviço de pagamentos tem de utilizar HTTPS.',
      );
    }

    try {
      final headers = <String, String>{
        'Authorization': 'Bearer $token',
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      };
      final response = method == 'POST'
          ? await _client
              .post(uri, headers: headers, body: jsonEncode(body ?? const {}))
              .timeout(const Duration(seconds: 20))
          : await _client
              .get(uri, headers: headers)
              .timeout(const Duration(seconds: 20));

      final raw = utf8.decode(response.bodyBytes).trim();
      if (raw.isEmpty) {
        throw PaymentException(
          code: 'EMPTY_PAYMENT_RESPONSE',
          stage: stage,
          httpStatus: response.statusCode,
          message: 'O serviço de pagamentos devolveu uma resposta vazia (HTTP ${response.statusCode}).',
        );
      }

      Object? parsed;
      try {
        parsed = jsonDecode(raw);
        if (parsed is String) parsed = jsonDecode(parsed);
      } on FormatException {
        final preview = raw.replaceAll(RegExp(r'\s+'), ' ').substring(
              0,
              raw.length < 240 ? raw.length : 240,
            );
        throw PaymentException(
          code: 'INVALID_PAYMENT_RESPONSE',
          stage: stage,
          httpStatus: response.statusCode,
          message: 'Resposta inválida do serviço de pagamentos (HTTP ${response.statusCode}): $preview',
        );
      }

      if (parsed is! Map<String, dynamic>) {
        throw PaymentException(
          code: 'INVALID_PAYMENT_RESPONSE',
          stage: stage,
          httpStatus: response.statusCode,
          message: 'O serviço de pagamentos devolveu um formato inesperado.',
        );
      }

      if (response.statusCode < 200 ||
          response.statusCode >= 300 ||
          parsed['ok'] != true) {
        throw PaymentException(
          code: parsed['code']?.toString() ??
              parsed['error']?.toString() ??
              'PAYMENT_ERROR',
          stage: parsed['stage']?.toString() ?? stage,
          httpStatus: response.statusCode,
          message: parsed['error']?.toString() ??
              'Não foi possível concluir esta operação PayPal.',
        );
      }
      return parsed;
    } on PaymentException {
      rethrow;
    } catch (error) {
      throw PaymentException(
        code: 'PAYMENT_CONNECTION_ERROR',
        stage: stage,
        httpStatus: 0,
        message: 'Falha de ligação ao PayPal. Verifica a Internet e tenta novamente. ($error)',
      );
    }
  }
}
