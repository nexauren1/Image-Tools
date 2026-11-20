import 'package:flutter/material.dart';
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../core/theme/app_theme.dart';

class CommunityScreen extends StatefulWidget {
  const CommunityScreen({super.key});

  @override
  State<CommunityScreen> createState() => _CommunityScreenState();
}

class _CommunityScreenState extends State<CommunityScreen> {
  static final Uri _facebook = Uri.parse('https://www.facebook.com/nexaurentech');
  static final Uri _website = Uri.parse('https://www.nexaurenstory.com');

  Future<void> _shareApp() async {
    const message = 'Experimenta o Image Tools — ferramentas para recortar, redimensionar, comprimir, converter e melhorar imagens no Android.\n\n'
        'Segue a Nexauren no Facebook: https://www.facebook.com/nexaurentech\n'
        'Acompanha novidades e aplicações: https://www.nexaurenstory.com';

    try {
      await SharePlus.instance.share(
        ShareParams(
          text: message,
          subject: 'Image Tools — edição de imagens para Android',
        ),
      );
    } catch (error) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Não foi possível abrir a partilha: $error')),
      );
    }
  }

  Future<void> _openLink(Uri uri) async {
    try {
      final opened = await launchUrl(uri, mode: LaunchMode.externalApplication);
      if (!opened && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Não foi possível abrir este endereço.')),
        );
      }
    } catch (_) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Verifica a ligação à Internet e tenta novamente.')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(
        title: const Text('Comunidade Nexauren'),
      ),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(18, 12, 18, 28),
        children: [
          Container(
            padding: const EdgeInsets.all(22),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(26),
              gradient: const LinearGradient(
                colors: [Color(0xFF123B9B), Color(0xFF6927C9), Color(0xFF111B43)],
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              boxShadow: [
                BoxShadow(
                  color: AppTheme.electricBlue.withValues(alpha: 0.2),
                  blurRadius: 25,
                  offset: const Offset(0, 12),
                ),
              ],
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Row(
                  children: [
                    Icon(Icons.auto_awesome, color: AppTheme.cyan),
                    SizedBox(width: 8),
                    Text(
                      'NEXAUREN COMMUNITY',
                      style: TextStyle(color: AppTheme.cyan, fontSize: 11, letterSpacing: 1.4, fontWeight: FontWeight.w900),
                    ),
                  ],
                ),
                const SizedBox(height: 14),
                const Text(
                  'Partilha ferramentas.\nAcompanha novidades.',
                  style: TextStyle(color: Colors.white, fontSize: 25, height: 1.08, fontWeight: FontWeight.w900),
                ),
                const SizedBox(height: 9),
                const Text(
                  'Ajuda mais pessoas a descobrir o Image Tools e acompanha os próximos lançamentos da Nexauren.',
                  style: TextStyle(color: Colors.white70, height: 1.4),
                ),
                const SizedBox(height: 18),
                FilledButton.icon(
                  onPressed: _shareApp,
                  icon: const Icon(Icons.share_rounded),
                  label: const Text('Partilhar Image Tools'),
                  style: FilledButton.styleFrom(
                    backgroundColor: Colors.white,
                    foregroundColor: const Color(0xFF1745A7),
                    minimumSize: const Size.fromHeight(54),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 24),
          Text('Segue os nossos canais', style: Theme.of(context).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w900)),
          const SizedBox(height: 12),
          _channelCard(
            context,
            icon: Icons.facebook_rounded,
            accent: const Color(0xFF1877F2),
            title: 'Nexauren no Facebook',
            subtitle: 'Segue a página para novidades, lançamentos e atualizações',
            action: 'Abrir Facebook',
            onTap: () => _openLink(_facebook),
          ),
          const SizedBox(height: 12),
          _channelCard(
            context,
            icon: Icons.language_rounded,
            accent: AppTheme.electricBlue,
            title: 'Nexauren News',
            subtitle: 'Visita www.nexaurenstory.com e acompanha artigos e aplicações',
            action: 'Visitar site',
            onTap: () => _openLink(_website),
          ),
          const SizedBox(height: 22),
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: colors.surfaceContainer,
              borderRadius: BorderRadius.circular(18),
              border: Border.all(color: colors.outlineVariant.withValues(alpha: 0.35)),
            ),
            child: const Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Icon(Icons.privacy_tip_outlined, color: AppTheme.cyan),
                SizedBox(width: 12),
                Expanded(
                  child: Text(
                    'A partilha utiliza o menu de partilha do Android. Nenhuma fotografia editada é enviada automaticamente.',
                    style: TextStyle(height: 1.4),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _channelCard(
    BuildContext context, {
    required IconData icon,
    required Color accent,
    required String title,
    required String subtitle,
    required String action,
    required VoidCallback onTap,
  }) {
    return Material(
      color: Theme.of(context).colorScheme.surfaceContainer,
      borderRadius: BorderRadius.circular(21),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(21),
        child: Padding(
          padding: const EdgeInsets.all(17),
          child: Row(
            children: [
              Container(
                width: 50,
                height: 50,
                decoration: BoxDecoration(
                  color: accent.withValues(alpha: 0.14),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Icon(icon, color: accent, size: 27),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title, style: const TextStyle(fontWeight: FontWeight.w900)),
                    const SizedBox(height: 4),
                    Text(subtitle, style: Theme.of(context).textTheme.bodySmall),
                    const SizedBox(height: 9),
                    Text(action, style: TextStyle(color: accent, fontWeight: FontWeight.w900, fontSize: 12)),
                  ],
                ),
              ),
              const Icon(Icons.open_in_new_rounded, size: 18),
            ],
          ),
        ),
      ),
    );
  }
}
