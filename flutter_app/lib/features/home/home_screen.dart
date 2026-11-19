import 'package:material_ui/material_ui.dart';

import '../../core/catalog/tool_catalog.dart';
import '../../core/theme/app_theme.dart';
import '../community/community_screen.dart';
import '../editor/tool_workspace_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({
    super.key,
    required this.isDarkMode,
    required this.onThemeChanged,
  });

  final bool isDarkMode;
  final ValueChanged<bool> onThemeChanged;

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _tab = 0;

  void _openCommunity() {
    Navigator.of(context).push(
      PageRouteBuilder<void>(
        transitionDuration: const Duration(milliseconds: 260),
        pageBuilder: (_, animation, secondaryAnimation) => const CommunityScreen(),
        transitionsBuilder: (_, animation, secondaryAnimation, child) {
          final curved = CurvedAnimation(parent: animation, curve: Curves.easeOutCubic);
          return FadeTransition(
            opacity: curved,
            child: SlideTransition(
              position: Tween<Offset>(begin: const Offset(0.035, 0), end: Offset.zero).animate(curved),
              child: child,
            ),
          );
        },
      ),
    );
  }

  void _openTool(ToolDescriptor tool) {
    Navigator.of(context).push(
      PageRouteBuilder<void>(
        transitionDuration: const Duration(milliseconds: 280),
        pageBuilder: (_, animation, secondaryAnimation) => ToolWorkspaceScreen(tool: tool),
        transitionsBuilder: (_, animation, secondaryAnimation, child) {
          final curved = CurvedAnimation(parent: animation, curve: Curves.easeOutCubic);
          return FadeTransition(
            opacity: curved,
            child: SlideTransition(
              position: Tween<Offset>(begin: const Offset(0.04, 0), end: Offset.zero).animate(curved),
              child: child,
            ),
          );
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Scaffold(
      appBar: AppBar(
        leading: Padding(
          padding: const EdgeInsets.only(left: 14),
          child: DecoratedBox(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(13),
              gradient: const LinearGradient(colors: [AppTheme.electricBlue, AppTheme.violet]),
            ),
            child: const Icon(Icons.auto_awesome, color: Colors.white),
          ),
        ),
        title: const Text('Image Tools', style: TextStyle(fontWeight: FontWeight.w900, letterSpacing: -0.4)),
        actions: [
          IconButton(
            tooltip: widget.isDarkMode ? 'Tema claro' : 'Tema escuro',
            onPressed: () => widget.onThemeChanged(!widget.isDarkMode),
            icon: Icon(widget.isDarkMode ? Icons.light_mode_outlined : Icons.dark_mode_outlined),
          ),
          IconButton(
            tooltip: 'Partilhar e comunidade',
            onPressed: _openCommunity,
            icon: const Icon(Icons.share_outlined),
          ),
          IconButton(
            tooltip: 'Premium',
            onPressed: () => setState(() => _tab = 2),
            icon: const Icon(Icons.workspace_premium, color: Color(0xFFFFC928)),
          ),
          const SizedBox(width: 6),
        ],
      ),
      body: _tab == 0 ? _homeBody(context, colors) : _secondaryBody(context),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tab,
        onDestinationSelected: (value) => setState(() => _tab = value),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.home_outlined), selectedIcon: Icon(Icons.home), label: 'Início'),
          NavigationDestination(icon: Icon(Icons.folder_open_outlined), selectedIcon: Icon(Icons.folder), label: 'Projetos'),
          NavigationDestination(icon: Icon(Icons.workspace_premium_outlined), selectedIcon: Icon(Icons.workspace_premium), label: 'Premium'),
          NavigationDestination(icon: Icon(Icons.settings_outlined), selectedIcon: Icon(Icons.settings), label: 'Definições'),
        ],
      ),
    );
  }

  Widget _homeBody(BuildContext context, ColorScheme colors) {
    return CustomScrollView(
      slivers: [
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 14),
          sliver: SliverToBoxAdapter(child: _heroCard()),
        ),
        SliverPadding(
          padding: const EdgeInsets.symmetric(horizontal: 18),
          sliver: SliverToBoxAdapter(
            child: _sectionHeading(context, 'Ferramentas principais', 'Ver todas', () => setState(() => _tab = 1)),
          ),
        ),
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(14, 12, 14, 18),
          sliver: SliverGrid(
            delegate: SliverChildBuilderDelegate(
              (context, index) => _toolCard(ToolCatalog.primary[index], compact: true),
              childCount: ToolCatalog.primary.length,
            ),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 3,
              mainAxisSpacing: 10,
              crossAxisSpacing: 10,
              childAspectRatio: 0.88,
            ),
          ),
        ),
        SliverPadding(
          padding: const EdgeInsets.symmetric(horizontal: 18),
          sliver: SliverToBoxAdapter(
            child: _sectionHeading(context, 'Ferramentas avançadas', 'Ver todas', () => setState(() => _tab = 1)),
          ),
        ),
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(14, 12, 14, 26),
          sliver: SliverGrid(
            delegate: SliverChildBuilderDelegate(
              (context, index) => _toolCard(ToolCatalog.advanced[index], compact: false),
              childCount: 3,
            ),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 3,
              mainAxisSpacing: 10,
              crossAxisSpacing: 10,
              childAspectRatio: 1.0,
            ),
          ),
        ),
        SliverPadding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 28),
          sliver: SliverToBoxAdapter(child: _communityCard()),
        ),
      ],
    );
  }

  Widget _communityCard() {
    return Material(
      color: Theme.of(context).colorScheme.surfaceContainer,
      borderRadius: BorderRadius.circular(22),
      child: InkWell(
        borderRadius: BorderRadius.circular(22),
        onTap: _openCommunity,
        child: Padding(
          padding: const EdgeInsets.all(17),
          child: Row(
            children: [
              Container(
                width: 48,
                height: 48,
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(15),
                  gradient: const LinearGradient(colors: [AppTheme.electricBlue, AppTheme.violet]),
                ),
                child: const Icon(Icons.campaign_rounded, color: Colors.white, size: 25),
              ),
              const SizedBox(width: 13),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text('Comunidade Nexauren', style: TextStyle(fontWeight: FontWeight.w900)),
                    const SizedBox(height: 4),
                    Text('Partilha a app e acompanha novidades', style: Theme.of(context).textTheme.bodySmall),
                  ],
                ),
              ),
              const Icon(Icons.arrow_forward_ios_rounded, size: 17),
            ],
          ),
        ),
      ),
    );
  }

  Widget _heroCard() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(25),
        gradient: const LinearGradient(
          colors: [Color(0xFF1649C8), Color(0xFF6D28D9), Color(0xFF111C46)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        boxShadow: [
          BoxShadow(color: AppTheme.electricBlue.withValues(alpha: 0.2), blurRadius: 24, offset: const Offset(0, 12)),
        ],
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('EDIÇÃO PROFISSIONAL', style: TextStyle(color: AppTheme.cyan, fontSize: 10, fontWeight: FontWeight.w900, letterSpacing: 1.5)),
                const SizedBox(height: 8),
                const Text('Qualidade máxima.\nControlo total.', style: TextStyle(color: Colors.white, fontSize: 23, height: 1.12, fontWeight: FontWeight.w900)),
                const SizedBox(height: 8),
                const Text('Rápido • Local • Poderoso', style: TextStyle(color: Colors.white70, fontSize: 12)),
                const SizedBox(height: 14),
                FilledButton.tonal(
                  onPressed: () => _openTool(ToolCatalog.primary.first),
                  style: FilledButton.styleFrom(backgroundColor: Colors.white, foregroundColor: const Color(0xFF1D4ED8)),
                  child: const Text('Começar a editar'),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          Container(
            width: 84,
            height: 104,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(22),
              color: Colors.white.withValues(alpha: 0.12),
              border: Border.all(color: Colors.white.withValues(alpha: 0.24)),
            ),
            child: const Icon(Icons.photo_filter_rounded, size: 50, color: Colors.white),
          ),
        ],
      ),
    );
  }

  Widget _sectionHeading(BuildContext context, String title, String action, VoidCallback onTap) {
    return Row(
      children: [
        Expanded(child: Text(title, style: Theme.of(context).textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w900))),
        TextButton(onPressed: onTap, child: Text(action)),
      ],
    );
  }

  Widget _toolCard(ToolDescriptor tool, {required bool compact}) {
    return Material(
      color: Theme.of(context).colorScheme.surfaceContainer,
      borderRadius: BorderRadius.circular(21),
      child: InkWell(
        borderRadius: BorderRadius.circular(21),
        onTap: () => _openTool(tool),
        child: Padding(
          padding: const EdgeInsets.all(10),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                width: compact ? 48 : 44,
                height: compact ? 48 : 44,
                decoration: BoxDecoration(
                  color: tool.color,
                  borderRadius: BorderRadius.circular(15),
                  boxShadow: [BoxShadow(color: tool.color.withValues(alpha: 0.25), blurRadius: 12, offset: const Offset(0, 5))],
                ),
                child: Icon(tool.icon, color: Colors.white, size: 25),
              ),
              const SizedBox(height: 9),
              Text(
                tool.title,
                textAlign: TextAlign.center,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(fontWeight: FontWeight.w800, fontSize: 11.5, height: 1.1),
              ),
              if (tool.premium) ...[
                const SizedBox(height: 4),
                const Icon(Icons.workspace_premium, size: 13, color: Color(0xFFFFC928)),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _secondaryBody(BuildContext context) {
    if (_tab == 1) {
      return ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text('Biblioteca de ferramentas', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
          const SizedBox(height: 8),
          const Text('Cada ferramenta terá o seu espaço, controlos e processo de exportação.'),
          const SizedBox(height: 18),
          ...ToolCatalog.advanced.map((tool) => Padding(
            padding: const EdgeInsets.only(bottom: 9),
            child: ListTile(
              tileColor: Theme.of(context).colorScheme.surfaceContainer,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(17)),
              leading: CircleAvatar(backgroundColor: tool.color, child: Icon(tool.icon, color: Colors.white)),
              title: Text(tool.title, style: const TextStyle(fontWeight: FontWeight.w800)),
              subtitle: Text(tool.subtitle),
              trailing: tool.premium ? const Icon(Icons.workspace_premium, color: Color(0xFFFFC928)) : const Icon(Icons.chevron_right),
              onTap: () => _openTool(tool),
            ),
          )),
        ],
      );
    }

    final isPremium = _tab == 2;
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(isPremium ? Icons.workspace_premium : Icons.settings, size: 58, color: isPremium ? const Color(0xFFFFC928) : Theme.of(context).colorScheme.primary),
            const SizedBox(height: 16),
            Text(isPremium ? 'Image Tools Premium' : 'Definições', style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w900)),
            const SizedBox(height: 8),
            Text(
              isPremium
                  ? 'A ligação ao PayPal e a validação offline serão integradas com o serviço atual, sem alterar a conta nem o plano existente.'
                  : 'Escolha o tema da aplicação.',
              textAlign: TextAlign.center,
            ),
            if (!isPremium) ...[
              const SizedBox(height: 18),
              SwitchListTile(
                value: widget.isDarkMode,
                onChanged: widget.onThemeChanged,
                title: const Text('Tema escuro'),
              ),
              const SizedBox(height: 10),
              OutlinedButton.icon(
                onPressed: _openCommunity,
                icon: const Icon(Icons.share_outlined),
                label: const Text('Partilhar e seguir a Nexauren'),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
