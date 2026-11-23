import 'package:firebase_auth/firebase_auth.dart' show User;
import 'package:material_ui/material_ui.dart';

import '../../core/theme/app_theme.dart';
import 'app_session.dart';

class AccountScreen extends StatefulWidget {
  const AccountScreen({super.key, required this.session});

  final AppSession session;

  @override
  State<AccountScreen> createState() => _AccountScreenState();
}

class _AccountScreenState extends State<AccountScreen> {
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();
  bool _createAccount = false;
  bool _busy = false;
  String? _error;
  String? _message;

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _submitEmail() async {
    final email = _emailController.text.trim();
    final password = _passwordController.text;
    if (!email.contains('@') || password.length < 6) {
      setState(() => _error = 'Introdu um email válido e uma palavra-passe com pelo menos 6 caracteres.');
      return;
    }

    setState(() {
      _busy = true;
      _error = null;
      _message = null;
    });
    try {
      if (_createAccount) {
        await widget.session.registerEmail(email, password);
        _message = 'Conta criada. Enviámos uma mensagem para confirmar o endereço de email.';
      } else {
        await widget.session.signInEmail(email, password);
        _message = 'Sessão iniciada.';
      }
    } catch (error) {
      _error = _authError(error);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _signInGoogle() async {
    setState(() {
      _busy = true;
      _error = null;
      _message = null;
    });
    try {
      await widget.session.signInGoogle();
      _message = 'Sessão Google iniciada.';
    } catch (error) {
      _error = _authError(error);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _resetPassword() async {
    try {
      await widget.session.sendPasswordReset(_emailController.text.trim());
      if (!mounted) return;
      setState(() {
        _error = null;
        _message = 'Enviámos uma ligação para repor a palavra-passe.';
      });
    } catch (error) {
      if (!mounted) return;
      setState(() => _error = _authError(error));
    }
  }

  Future<void> _sendVerification() async {
    setState(() {
      _busy = true;
      _error = null;
      _message = null;
    });
    try {
      await widget.session.sendEmailVerification();
      _message = 'Mensagem de verificação enviada.';
    } catch (error) {
      _error = _authError(error);
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _startPremium() async {
    setState(() {
      _busy = true;
      _error = null;
      _message = null;
    });
    try {
      await widget.session.startPremiumCheckout();
      _message = 'O PayPal foi aberto para aprovares a assinatura. Depois regressa à aplicação.';
    } catch (error) {
      _error = error.toString();
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _cancelPremium() async {
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (dialogContext) => AlertDialog(
        title: const Text('Cancelar assinatura?'),
        content: const Text(
          'A solicitação será enviada ao PayPal e ao backend. A aplicação só atualizará o estado Premium após a confirmação do servidor.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogContext, false),
            child: const Text('Manter assinatura'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(dialogContext, true),
            child: const Text('Confirmar cancelamento'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    setState(() {
      _busy = true;
      _error = null;
      _message = null;
    });
    try {
      final synced = await widget.session.cancelPremiumSubscription();
      _message = synced
          ? 'Cancelamento confirmado pelo PayPal e pelo servidor.'
          : 'O PayPal recebeu o pedido, mas a sincronização da assinatura precisa de nova verificação.';
    } catch (error) {
      _error = error.toString();
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  String _authError(Object error) {
    final text = error.toString().toLowerCase();
    if (text.contains('already') && text.contains('use')) {
      return 'Este email já está associado a uma conta.';
    }
    if (text.contains('invalid-email')) return 'O endereço de email não é válido.';
    if (text.contains('weak-password')) return 'Escolhe uma palavra-passe mais forte.';
    if (text.contains('wrong-password') || text.contains('invalid-credential')) {
      return 'Email ou palavra-passe incorretos.';
    }
    if (text.contains('network')) return 'Falha de ligação. Verifica a Internet.';
    if (text.contains('cancelled') || text.contains('canceled')) {
      return 'Autenticação cancelada.';
    }
    return error.toString();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: widget.session,
      builder: (context, _) {
        final user = widget.session.user;
        return Scaffold(
          appBar: AppBar(title: const Text('Conta Nexauren')),
          body: ListView(
            padding: const EdgeInsets.fromLTRB(18, 12, 18, 28),
            children: [
              _accountHero(context, user?.email),
              const SizedBox(height: 16),
              if (user == null) _signInPanel(context) else _signedInPanel(context, user),
              if (_message != null) ...[
                const SizedBox(height: 12),
                _statusPanel(context, _message!, success: true),
              ],
              if (_error != null) ...[
                const SizedBox(height: 12),
                _statusPanel(context, _error!, success: false),
              ],
            ],
          ),
        );
      },
    );
  }

  Widget _accountHero(BuildContext context, String? email) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(23),
        gradient: const LinearGradient(
          colors: [Color(0xFF123B9B), Color(0xFF6927C9), Color(0xFF111B43)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
      ),
      child: Row(
        children: [
          const CircleAvatar(
            radius: 27,
            backgroundColor: Colors.white24,
            child: Icon(Icons.person_rounded, color: Colors.white, size: 29),
          ),
          const SizedBox(width: 13),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'IMAGE TOOLS ACCOUNT',
                  style: TextStyle(
                    color: AppTheme.cyan,
                    fontSize: 10,
                    fontWeight: FontWeight.w900,
                    letterSpacing: 1.2,
                  ),
                ),
                const SizedBox(height: 5),
                Text(
                  email ?? 'Modo visitante',
                  maxLines: 2,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: Colors.white,
                    fontWeight: FontWeight.w900,
                    fontSize: 16,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  email == null
                      ? 'As ferramentas locais continuam disponíveis.'
                      : 'A conta e o estado Premium são sincronizados.',
                  style: const TextStyle(color: Colors.white70, fontSize: 11),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _signInPanel(BuildContext context) {
    return _panel(
      context,
      'Iniciar sessão ou criar conta',
      Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          FilledButton.icon(
            onPressed: _busy ? null : _signInGoogle,
            icon: const Icon(Icons.account_circle_rounded),
            label: const Text('Continuar com Google'),
          ),
          const SizedBox(height: 14),
          const Divider(),
          TextField(
            controller: _emailController,
            keyboardType: TextInputType.emailAddress,
            autofillHints: const [AutofillHints.email],
            decoration: const InputDecoration(
              labelText: 'Email',
              prefixIcon: Icon(Icons.email_outlined),
            ),
          ),
          const SizedBox(height: 10),
          TextField(
            controller: _passwordController,
            obscureText: true,
            autofillHints: const [AutofillHints.password],
            decoration: const InputDecoration(
              labelText: 'Palavra-passe',
              prefixIcon: Icon(Icons.lock_outline_rounded),
            ),
          ),
          const SizedBox(height: 14),
          FilledButton(
            onPressed: _busy ? null : _submitEmail,
            child: _busy
                ? const SizedBox(
                    width: 19,
                    height: 19,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : Text(_createAccount ? 'Criar conta' : 'Iniciar sessão'),
          ),
          TextButton(
            onPressed: _busy
                ? null
                : () => setState(() {
                      _createAccount = !_createAccount;
                      _error = null;
                      _message = null;
                    }),
            child: Text(
              _createAccount
                  ? 'Já tenho uma conta'
                  : 'Ainda não tenho conta',
            ),
          ),
          if (!_createAccount)
            TextButton(
              onPressed: _busy ? null : _resetPassword,
              child: const Text('Esqueci-me da palavra-passe'),
            ),
          const SizedBox(height: 4),
          Text(
            'Podes continuar a usar as ferramentas locais sem iniciar sessão. A conta é necessária para gerir o Premium.',
            style: Theme.of(context).textTheme.bodySmall,
          ),
        ],
      ),
    );
  }

  Widget _signedInPanel(BuildContext context, User user) {
    final isEmailProvider = user.providerData.any(
      (provider) => provider.providerId == 'password',
    );
    return Column(
      children: [
        _panel(
          context,
          'Estado da conta',
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _infoRow('Email', user.email ?? '—'),
              _infoRow(
                'Verificação',
                user.emailVerified ? 'Confirmado' : 'Pendente',
              ),
              _infoRow(
                'Premium',
                widget.session.isPremium ? 'Ativo' : 'Gratuito',
              ),
              if (isEmailProvider && !user.emailVerified) ...[
                const SizedBox(height: 10),
                OutlinedButton.icon(
                  onPressed: _busy ? null : _sendVerification,
                  icon: const Icon(Icons.mark_email_unread_outlined),
                  label: const Text('Enviar email de verificação'),
                ),
              ],
              OutlinedButton.icon(
                onPressed: _busy ? null : widget.session.reloadUser,
                icon: const Icon(Icons.sync_rounded),
                label: const Text('Atualizar estado da conta'),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        _panel(
          context,
          'Image Tools Premium',
          Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text(
                widget.session.isPremium
                    ? 'A assinatura está ativa. As ferramentas Premium são desbloqueadas pelo estado confirmado no servidor.'
                    : 'Associa a tua conta a uma assinatura PayPal para desbloquear as ferramentas Premium.',
                style: Theme.of(context).textTheme.bodyMedium,
              ),
              const SizedBox(height: 12),
              if (widget.session.isPremium)
                OutlinedButton.icon(
                  onPressed: _busy ? null : _cancelPremium,
                  icon: const Icon(Icons.cancel_outlined),
                  label: const Text('Gerir / cancelar assinatura'),
                )
              else
                FilledButton.icon(
                  onPressed: _busy ? null : _startPremium,
                  icon: const Icon(Icons.workspace_premium_rounded),
                  label: const Text('Subscrever com PayPal'),
                ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        OutlinedButton.icon(
          onPressed: _busy
              ? null
              : () async {
                  await widget.session.signOut();
                  if (mounted) {
                    setState(() {
                      _message = 'Sessão terminada.';
                      _error = null;
                    });
                  }
                },
          icon: const Icon(Icons.logout_rounded),
          label: const Text('Terminar sessão'),
        ),
      ],
    );
  }

  Widget _panel(BuildContext context, String title, Widget child) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainer,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(
          color: Theme.of(context).colorScheme.outlineVariant.withValues(alpha: 0.4),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(fontWeight: FontWeight.w900, fontSize: 16)),
          const SizedBox(height: 13),
          child,
        ],
      ),
    );
  }

  Widget _infoRow(String title, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 5),
      child: Row(
        children: [
          Expanded(child: Text(title)),
          Flexible(
            child: Text(
              value,
              textAlign: TextAlign.end,
              style: const TextStyle(fontWeight: FontWeight.w800),
            ),
          ),
        ],
      ),
    );
  }

  Widget _statusPanel(BuildContext context, String message, {required bool success}) {
    final color = success ? AppTheme.cyan : Theme.of(context).colorScheme.error;
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.10),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: color.withValues(alpha: 0.30)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(
            success ? Icons.check_circle_outline_rounded : Icons.error_outline_rounded,
            color: color,
          ),
          const SizedBox(width: 10),
          Expanded(child: Text(message)),
        ],
      ),
    );
  }
}
