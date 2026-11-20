import 'package:flutter/material.dart';

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
  String _query = '';
  final TextEditingController _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _goTo(BuildContext drawerContext, int index) {
    Navigator.of(drawerContext).pop();
    setState(() => _tab = index);
  }

  void _openCommunity() {
    Navigator.of(context).push(
      PageRouteBuilder<void>(
        transitionDuration: const Duration(milliseconds: 280),
        pageBuilder: (_, animation, secondaryAnimation) =>
            const CommunityScreen(),
        transitionsBuilder: (_, animation, secondaryAnimation, child) {
          final curved = CurvedAnimation(
            parent: animation,
            curve: Curves.easeOutCubic,
          );
          return FadeTransition(
            opacity: curved,
            child: SlideTransition(
              position: Tween<Offset>(
                begin: const Offset(0.035, 0),
                end: Offset.zero,
              ).animate(curved),
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
        transitionDuration: const Duration(milliseconds: 320),
        pageBuilder: (_, animation, secondaryAnimation) =>
            ToolWorkspaceScreen(tool: tool),
        transitionsBuilder: (_, animation, secondaryAnimation, child) {
          final curved = CurvedAnimation(
            parent: animation,
            curve: Curves.easeOutCubic,
          );
          return FadeTransition(
            opacity: curved,
            child: SlideTransition(
              position: Tween<Offset>(
                begin: const Offset(0.045, 0),
                end: Offset.zero,
              ).animate(curved),
              child: child,
            ),
          );
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      drawer: _buildDrawer(context),
      appBar: AppBar(
        leading: Builder(
          builder: (context) => IconButton(
            tooltip: 'Menu',
            onPressed: () => Scaffold.of(context).openDrawer(),
            icon: const Icon(Icons.menu_rounded),
          ),
        ),
        titleSpacing: 0,
        title: Row(
          children: [
            Container(
              width: 34,
              height: 34,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(11),
                gradient: const LinearGradient(
                  colors: [AppTheme.electricBlue, AppTheme.violet],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
              ),
              child: const Icon(
                Icons.auto_awesome_rounded,
                color: Colors.white,
                size: 20,
              ),
            ),
            const SizedBox(width: 10),
            const Text('Image Tools'),
          ],
        ),
        actions: [
          IconButton(
            tooltip: 'Pesquisar ferramentas',
            onPressed: () => setState(() => _tab = 1),
            icon: const Icon(Icons.search_rounded),
          ),
          IconButton(
            tooltip: 'Premium',
            onPressed: () => setState(() => _tab = 2),
            icon: const Icon(
              Icons.workspace_premium_rounded,
              color: Color(0xFFFFC928),
            ),
          ),
          const SizedBox(width: 4),
        ],
      ),
      body: AnimatedSwitcher(
        duration: const Duration(milliseconds: 240),
        switchInCurve: Curves.easeOutCubic,
        switchOutCurve: Curves.easeInCubic,
        child: KeyedSubtree(
          key: ValueKey<int>(_tab),
          child: _pageForTab(),
        ),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tab,
        onDestinationSelected: (value) {
          setState(() {
            _tab = value;
            if (value != 1) _query = '';
          });
        },
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.home_outlined),
            selectedIcon: Icon(Icons.home_rounded),
            label: 'Início',
          ),
          NavigationDestination(
            icon: Icon(Icons.grid_view_outlined),
            selectedIcon: Icon(Icons.grid_view_rounded),
            label: 'Ferramentas',
          ),
          NavigationDestination(
            icon: Icon(Icons.workspace_premium_outlined),
            selectedIcon: Icon(Icons.workspace_premium_rounded),
            label: 'Premium',
          ),
          NavigationDestination(
            icon: Icon(Icons.settings_outlined),
            selectedIcon: Icon(Icons.settings_rounded),
            label: 'Definições',
          ),
        ],
      ),
    );
  }

  Widget _pageForTab() {
    switch (_tab) {
      case 0:
        return _homeBody();
      case 1:
        return _libraryBody();
      case 2:
        return _premiumBody();
      default:
        return _settingsBody();
    }
  }

  Widget _homeBody() {
    final advancedPreview = ToolCatalog.advanced.take(3).toList();
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 26),
      children: [
        _heroCard(),
        const SizedBox(height: 22),
        _sectionHeading(
          'Ferramentas principais',
          'Ver todas',
          () => setState(() => _tab = 1),
        ),
        const SizedBox(height: 12),
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemCount: ToolCatalog.primary.length,
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 3,
            mainAxisSpacing: 10,
            crossAxisSpacing: 10,
            childAspectRatio: 0.86,
          ),
          itemBuilder: (context, index) =>
              _toolCard(ToolCatalog.primary[index], primary: true),
        ),
        const SizedBox(height: 22),
        _sectionHeading(
          'Ferramentas avançadas',
          'Ver todas',
          () => setState(() => _tab = 1),
        ),
        const SizedBox(height: 12),
        GridView.builder(
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          itemCount: advancedPreview.length,
          gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
            crossAxisCount: 3,
            mainAxisSpacing: 10,
            crossAxisSpacing: 10,
            childAspectRatio: 0.90,
          ),
          itemBuilder: (context, index) =>
              _toolCard(advancedPreview[index], primary: false),
        ),
        const SizedBox(height: 16),
        _localProcessingCard(),
      ],
    );
  }

  Widget _heroCard() {
    return Container(
      constraints: const BoxConstraints(minHeight: 178),
      padding: const EdgeInsets.fromLTRB(19, 19, 14, 19),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(25),
        gradient: const LinearGradient(
          colors: [
            Color(0xFF104FCB),
            Color(0xFF5830D5),
            Color(0xFF101C43),
          ],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        boxShadow: [
          BoxShadow(
            color: AppTheme.electricBlue.withValues(alpha: 0.20),
            blurRadius: 25,
            offset: const Offset(0, 12),
          ),
        ],
      ),
      child: Stack(
        children: [
          Positioned(
            right: -18,
            top: -35,
            child: Container(
              width: 150,
              height: 150,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: Colors.white.withValues(alpha: 0.055),
              ),
            ),
          ),
          Positioned(
            right: 8,
            bottom: -52,
            child: Container(
              width: 118,
              height: 118,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                  color: AppTheme.cyan.withValues(alpha: 0.22),
                  width: 1.2,
                ),
              ),
            ),
          ),
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'EDIÇÃO PROFISSIONAL',
                      style: TextStyle(
                        color: AppTheme.cyan,
                        fontSize: 10,
                        fontWeight: FontWeight.w900,
                        letterSpacing: 1.5,
                      ),
                    ),
                    const SizedBox(height: 9),
                    const Text(
                      'Qualidade máxima.\nControlo total.',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 23,
                        height: 1.10,
                        fontWeight: FontWeight.w900,
                        letterSpacing: -0.5,
                      ),
                    ),
                    const SizedBox(height: 9),
                    const Text(
                      'Rápido  •  Fácil  •  Local',
                      style: TextStyle(color: Colors.white70, fontSize: 12),
                    ),
                    const SizedBox(height: 14),
                    FilledButton(
                      onPressed: () => _openTool(ToolCatalog.primary.first),
                      style: FilledButton.styleFrom(
                        backgroundColor: Colors.white,
                        foregroundColor: const Color(0xFF1745A7),
                        minimumSize: const Size(150, 42),
                        padding: const EdgeInsets.symmetric(horizontal: 14),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(13),
                        ),
                      ),
                      child: const Text(
                        'Começar a editar',
                        style: TextStyle(fontWeight: FontWeight.w900),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 5),
              SizedBox(
                width: 104,
                height: 122,
                child: Stack(
                  alignment: Alignment.center,
                  children: [
                    Container(
                      width: 82,
                      height: 104,
                      decoration: BoxDecoration(
                        borderRadius: BorderRadius.circular(22),
                        color: Colors.white.withValues(alpha: 0.12),
                        border: Border.all(
                          color: Colors.white.withValues(alpha: 0.23),
                        ),
                      ),
                    ),
                    const Positioned(
                      top: 20,
                      child: Icon(
                        Icons.image_rounded,
                        color: Colors.white,
                        size: 56,
                      ),
                    ),
                    Positioned(
                      right: 3,
                      bottom: 8,
                      child: Container(
                        width: 38,
                        height: 38,
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(13),
                          color: AppTheme.cyan,
                          boxShadow: [
                            BoxShadow(
                              color: AppTheme.cyan.withValues(alpha: 0.35),
                              blurRadius: 15,
                            ),
                          ],
                        ),
                        child: const Icon(
                          Icons.auto_fix_high_rounded,
                          color: Color(0xFF07162E),
                          size: 21,
                        ),
                      ),
                    ),
                    const Positioned(
                      top: 5,
                      right: 2,
                      child: Icon(
                        Icons.auto_awesome,
                        color: Color(0xFFFFD166),
                        size: 17,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _sectionHeading(String title, String action, VoidCallback onTap) {
    return Row(
      children: [
        Expanded(
          child: Text(
            title,
            style: Theme.of(context).textTheme.titleMedium?.copyWith(
                  fontWeight: FontWeight.w900,
                  letterSpacing: -0.2,
                ),
          ),
        ),
        TextButton(
          onPressed: onTap,
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(action),
              const SizedBox(width: 3),
              const Icon(Icons.chevron_right_rounded, size: 18),
            ],
          ),
        ),
      ],
    );
  }

  Widget _toolCard(ToolDescriptor tool, {required bool primary}) {
    return Material(
      color: Theme.of(context).colorScheme.surfaceContainer,
      borderRadius: BorderRadius.circular(21),
      child: InkWell(
        borderRadius: BorderRadius.circular(21),
        onTap: () => _openTool(tool),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(7, 11, 7, 9),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                width: primary ? 50 : 45,
                height: primary ? 50 : 45,
                decoration: BoxDecoration(
                  color: tool.color,
                  borderRadius: BorderRadius.circular(15),
                  boxShadow: [
                    BoxShadow(
                      color: tool.color.withValues(alpha: 0.25),
                      blurRadius: 13,
                      offset: const Offset(0, 5),
                    ),
                  ],
                ),
                child: Icon(
                  tool.icon,
                  color: Colors.white,
                  size: primary ? 25 : 23,
                ),
              ),
              const SizedBox(height: 9),
              Text(
                tool.title,
                textAlign: TextAlign.center,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  fontWeight: FontWeight.w800,
                  fontSize: primary ? 11.5 : 11,
                  height: 1.12,
                ),
              ),
              if (tool.premium) ...[
                const SizedBox(height: 3),
                const Icon(
                  Icons.workspace_premium_rounded,
                  size: 13,
                  color: Color(0xFFFFC928),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _localProcessingCard() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.surfaceContainer,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(
          color: AppTheme.electricBlue.withValues(alpha: 0.12),
        ),
      ),
      child: Row(
        children: [
          const Icon(Icons.shield_outlined, color: AppTheme.cyan, size: 22),
          const SizedBox(width: 10),
          Expanded(
            child: Text(
              'As imagens são processadas no dispositivo. Os ficheiros originais permanecem no teu controlo.',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    height: 1.35,
                    fontWeight: FontWeight.w600,
                  ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _libraryBody() {
    final normalized = _query.trim().toLowerCase();
    final tools = ToolCatalog.all.where((tool) {
      if (normalized.isEmpty) return true;
      return tool.title.toLowerCase().contains(normalized) ||
          tool.subtitle.toLowerCase().contains(normalized);
    }).toList();

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 10, 16, 8),
          child: TextField(
            controller: _searchController,
            onChanged: (value) => setState(() => _query = value),
            decoration: InputDecoration(
              hintText: 'Pesquisar uma ferramenta',
              prefixIcon: const Icon(Icons.search_rounded),
              suffixIcon: _query.isEmpty
                  ? null
                  : IconButton(
                      tooltip: 'Limpar pesquisa',
                      onPressed: () {
                        _searchController.clear();
                        setState(() => _query = '');
                      },
                      icon: const Icon(Icons.close_rounded),
                    ),
            ),
          ),
        ),
        Expanded(
          child: tools.isEmpty
              ? const Center(child: Text('Nenhuma ferramenta encontrada.'))
              : ListView.separated(
                  padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
                  itemCount: tools.length,
                  separatorBuilder: (_, __) => const SizedBox(height: 9),
                  itemBuilder: (context, index) {
                    final tool = tools[index];
                    return Material(
                      color: Theme.of(context).colorScheme.surfaceContainer,
                      borderRadius: BorderRadius.circular(18),
                      child: ListTile(
                        contentPadding: const EdgeInsets.symmetric(
                          horizontal: 13,
                          vertical: 5,
                        ),
                        leading: Container(
                          width: 44,
                          height: 44,
                          decoration: BoxDecoration(
                            color: tool.color,
                            borderRadius: BorderRadius.circular(14),
                          ),
                          child: Icon(tool.icon, color: Colors.white),
                        ),
                        title: Text(
                          tool.title,
                          style: const TextStyle(fontWeight: FontWeight.w800),
                        ),
                        subtitle: Text(tool.subtitle),
                        trailing: tool.premium
                            ? const Icon(
                                Icons.workspace_premium_rounded,
                                color: Color(0xFFFFC928),
                              )
                            : const Icon(Icons.chevron_right_rounded),
                        onTap: () => _openTool(tool),
                      ),
                    );
                  },
                ),
        ),
      ],
    );
  }

  Widget _projectsBody() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 86,
              height: 86,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(27),
                color: AppTheme.electricBlue.withValues(alpha: 0.13),
              ),
              child: const Icon(
                Icons.folder_open_rounded,
                color: AppTheme.electricBlue,
                size: 42,
              ),
            ),
            const SizedBox(height: 18),
            Text(
              'Os teus projetos',
              style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    fontWeight: FontWeight.w900,
                  ),
            ),
            const SizedBox(height: 8),
            Text(
              'As imagens exportadas ficam guardadas na galeria do dispositivo. O histórico de projetos será apresentado aqui.',
              textAlign: TextAlign.center,
              style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                    height: 1.4,
                  ),
            ),
            const SizedBox(height: 20),
            FilledButton.icon(
              onPressed: () => _openTool(ToolCatalog.primary.first),
              icon: const Icon(Icons.add_photo_alternate_outlined),
              label: const Text('Começar um projeto'),
            ),
          ],
        ),
      ),
    );
  }

  Widget _premiumBody() {
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 28),
      children: [
        Container(
          padding: const EdgeInsets.all(22),
          decoration: BoxDecoration(
            borderRadius: BorderRadius.circular(25),
            gradient: const LinearGradient(
              colors: [Color(0xFF6B42D9), Color(0xFF173A9F), Color(0xFF111A3B)],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            ),
          ),
          child: const Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(
                Icons.workspace_premium_rounded,
                color: Color(0xFFFFD166),
                size: 36,
              ),
              SizedBox(height: 14),
              Text(
                'Image Tools Premium',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 23,
                  fontWeight: FontWeight.w900,
                ),
              ),
              SizedBox(height: 7),
              Text(
                'Desbloqueia um fluxo de trabalho mais completo com ferramentas avançadas e processamento em lote.',
                style: TextStyle(color: Colors.white70, height: 1.4),
              ),
            ],
          ),
        ),
        const SizedBox(height: 18),
        _premiumFeature(
          Icons.layers_rounded,
          'Mais controlo',
          'Ferramentas avançadas para trabalhos de imagem.',
        ),
        _premiumFeature(
          Icons.bolt_rounded,
          'Fluxo de trabalho',
          'Ações organizadas num espaço de edição dedicado.',
        ),
        _premiumFeature(
          Icons.offline_bolt_outlined,
          'Processamento local',
          'As operações compatíveis continuam no próprio dispositivo.',
        ),
      ],
    );
  }

  Widget _premiumFeature(IconData icon, String title, String subtitle) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: ListTile(
        tileColor: Theme.of(context).colorScheme.surfaceContainer,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(17)),
        leading: Icon(icon, color: AppTheme.cyan, size: 26),
        title: Text(title, style: const TextStyle(fontWeight: FontWeight.w800)),
        subtitle: Text(subtitle),
      ),
    );
  }

  Widget _settingsBody() {
    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
      children: [
        Text(
          'Preferências',
          style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                fontWeight: FontWeight.w900,
              ),
        ),
        const SizedBox(height: 14),
        SwitchListTile(
          value: widget.isDarkMode,
          onChanged: widget.onThemeChanged,
          secondary: Icon(
            widget.isDarkMode
                ? Icons.dark_mode_outlined
                : Icons.light_mode_outlined,
          ),
          title: const Text(
            'Tema escuro',
            style: TextStyle(fontWeight: FontWeight.w800),
          ),
          subtitle: const Text('Alternar entre os temas escuro e claro.'),
          tileColor: Theme.of(context).colorScheme.surfaceContainer,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(18),
          ),
        ),
        const SizedBox(height: 10),
        ListTile(
          tileColor: Theme.of(context).colorScheme.surfaceContainer,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(18),
          ),
          leading: const Icon(Icons.people_alt_outlined),
          title: const Text(
            'Comunidade Nexauren',
            style: TextStyle(fontWeight: FontWeight.w800),
          ),
          subtitle: const Text('Partilhar a aplicação e acompanhar novidades.'),
          trailing: const Icon(Icons.chevron_right_rounded),
          onTap: _openCommunity,
        ),
        const SizedBox(height: 10),
        ListTile(
          tileColor: Theme.of(context).colorScheme.surfaceContainer,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(18),
          ),
          leading: const Icon(Icons.info_outline_rounded),
          title: const Text(
            'Sobre o Image Tools',
            style: TextStyle(fontWeight: FontWeight.w800),
          ),
          subtitle: const Text('Edição de imagens com processamento no dispositivo.'),
          trailing: const Icon(Icons.chevron_right_rounded),
          onTap: () => showAboutDialog(
            context: context,
            applicationName: 'Image Tools',
            applicationVersion: '2.0.0',
            applicationLegalese: 'Nexauren',
          ),
        ),
      ],
    );
  }

  Widget _buildDrawer(BuildContext context) {
    return Drawer(
      child: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(18, 22, 18, 20),
              child: Row(
                children: [
                  Container(
                    width: 48,
                    height: 48,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(15),
                      gradient: const LinearGradient(
                        colors: [AppTheme.electricBlue, AppTheme.violet],
                      ),
                    ),
                    child: const Icon(
                      Icons.auto_awesome_rounded,
                      color: Colors.white,
                      size: 27,
                    ),
                  ),
                  const SizedBox(width: 12),
                  const Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Image Tools',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w900,
                          ),
                        ),
                        SizedBox(height: 3),
                        Text(
                          'Edit • Resize • Enhance',
                          style: TextStyle(fontSize: 11),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const Divider(height: 1),
            _drawerItem(
              context,
              icon: Icons.home_outlined,
              title: 'Início',
              index: 0,
            ),
            _drawerItem(
              context,
              icon: Icons.grid_view_outlined,
              title: 'Biblioteca de ferramentas',
              index: 1,
            ),
            _drawerItem(
              context,
              icon: Icons.workspace_premium_outlined,
              title: 'Premium',
              index: 2,
            ),
            const Spacer(),
            SwitchListTile(
              value: widget.isDarkMode,
              onChanged: widget.onThemeChanged,
              secondary: const Icon(Icons.contrast_rounded),
              title: const Text('Tema escuro'),
            ),
            ListTile(
              leading: const Icon(Icons.people_alt_outlined),
              title: const Text('Comunidade Nexauren'),
              onTap: () {
                Navigator.of(context).pop();
                _openCommunity();
              },
            ),
            const SizedBox(height: 12),
          ],
        ),
      ),
    );
  }

  Widget _drawerItem(
    BuildContext context, {
    required IconData icon,
    required String title,
    required int index,
  }) {
    return Builder(
      builder: (tileContext) => ListTile(
        selected: _tab == index,
        leading: Icon(icon),
        title: Text(title),
        onTap: () => _goTo(tileContext, index),
      ),
    );
  }
}
