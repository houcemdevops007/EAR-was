# -*- coding: utf-8 -*-
# =============================================================================
#  EFI Academy - WebSphere Application Lab
#  update-ear.py : met a jour une application deja installee
#
#  Usage (depuis le DMGR) :
#    wsadmin.sh -lang jython -f update-ear.py --ear /chemin/efi-quiz.ear --app efi-quiz \
#        [--context /efi-quiz] [--sync-timeout 180] [--dry-run]
#    wsadmin.sh -lang jython -f update-ear.py --batch apps.batch [--dry-run]
#
#  Remplace le contenu de l'application (AdminApp.update -operation update -contents)
#  en conservant ses cibles, liaisons et racine de contexte ; enregistre, synchronise
#  les noeuds, attend la distribution puis verifie que l'application tourne.
#
#  Le script ne redemarre JAMAIS le DMGR, les Node Agents ou les serveurs.
#  Aucun mot de passe n'est lu ni stocke ici : l'authentification est geree par wsadmin.
#  Compatible Jython 2.1 (defaut de wsadmin 8.5.5) et Jython 2.7.
# =============================================================================
import sys
import time

USAGE = '''Usage : wsadmin.sh -lang jython -f update-ear.py --ear FICHIER --app NOM [--context /RACINE] [--batch FICHIER] [--sync-timeout S] [--dry-run]'''

# ----------------------------------------------------------------------------
# Bloc commun EFI (identique dans deploy-ear.py, update-ear.py, uninstall-ear.py, app-control.py)
# ----------------------------------------------------------------------------
EFI_FAILURES = []

def efi_log(level, msg):
    print '[%s] %s' % (level, msg)

def info(msg):
    efi_log('INFO', msg)

def ok(msg):
    efi_log(' OK ', msg)

def warn(msg):
    efi_log('WARN', msg)

def error(msg):
    efi_log('ERREUR', msg)
    EFI_FAILURES.append(msg)

def die(msg, code=1):
    efi_log('ERREUR', msg)
    sys.exit(code)

def parse_args(argv, valued, flags):
    opts = {}
    i = 0
    while i < len(argv):
        a = argv[i]
        if a[:2] != '--':
            die('Argument inattendu : %s' % a, 2)
        key = a[2:]
        if key in flags:
            opts[key] = 1
            i = i + 1
        elif key in valued:
            if i + 1 >= len(argv):
                die('Valeur manquante pour --%s' % key, 2)
            opts[key] = argv[i + 1]
            i = i + 2
        else:
            die('Option inconnue : --%s' % key, 2)
    return opts

def lines(text):
    result = []
    if not text:
        return result
    for l in text.splitlines():
        l = l.strip()
        if l:
            result.append(l)
    return result

def contains(text, sub):
    return text is not None and text.find(sub) >= 0

def read_batch(path):
    items = []
    f = open(path, 'r')
    try:
        for l in f.readlines():
            l = l.strip()
            if not l or l[0] == '#':
                continue
            parts = l.split('|')
            if len(parts) < 3:
                die('Ligne invalide dans %s : %s (format ear|app|context)' % (path, l), 2)
            items.append((parts[0].strip(), parts[1].strip(), parts[2].strip()))
    finally:
        f.close()
    return items

def parse_targets(opts):
    targets = []
    if opts.get('targets'):
        for t in opts['targets'].split(','):
            t = t.strip()
            if not t:
                continue
            p = t.split(':')
            if len(p) != 2:
                die('Cible invalide : %s (format noeud:serveur)' % t, 2)
            targets.append((p[0], p[1]))
    elif opts.get('node') and opts.get('server'):
        targets.append((opts['node'], opts['server']))
    return targets

def current_cell(opts):
    if opts.get('cell'):
        return opts['cell']
    return AdminControl.getCell()

def check_target(cell, node, server):
    if not AdminConfig.getid('/Cell:%s/' % cell):
        die('Cellule inconnue : %s (cellules : %s)' % (cell, ', '.join(names('Cell'))))
    node_id = AdminConfig.getid('/Cell:%s/Node:%s/' % (cell, node))
    if not node_id:
        die('Noeud inconnu : %s (noeuds de %s : %s)' % (node, cell, ', '.join(names('Node'))))
    if not AdminConfig.getid('/Cell:%s/Node:%s/Server:%s/' % (cell, node, server)):
        known = []
        for s in lines(AdminConfig.list('Server', node_id)):
            known.append(AdminConfig.showAttribute(s, 'name'))
        die('Serveur inconnu : %s sur %s (serveurs : %s)' % (server, node, ', '.join(known)))
    ok('Cible valide : cell=%s, node=%s, server=%s' % (cell, node, server))

def names(config_type):
    result = []
    for o in lines(AdminConfig.list(config_type)):
        result.append(AdminConfig.showAttribute(o, 'name'))
    return result

def app_installed(app):
    return app in lines(AdminApp.list())

def sync_node(node):
    sync = AdminControl.completeObjectName('type=NodeSync,process=nodeagent,node=%s,*' % node)
    if not sync:
        warn('Node Agent du noeud %s injoignable : synchronisation impossible. '
             'Verifier son etat (serverStatus.sh nodeagent) - ne pas le redemarrer sans autorisation.' % node)
        return 0
    result = AdminControl.invoke(sync, 'sync')
    if result == 'true':
        ok('Noeud %s synchronise' % node)
        return 1
    warn('Synchronisation du noeud %s : resultat %s' % (node, result))
    return 0

def sync_nodes(nodes):
    done = []
    for n in nodes:
        if n not in done:
            sync_node(n)
            done.append(n)

def wait_ready(app, timeout):
    deadline = time.time() + timeout
    while time.time() < deadline:
        if AdminApp.isAppReady(app) == 'true':
            ok('%s distribuee sur les noeuds (isAppReady=true)' % app)
            return 1
        info('Distribution de %s en cours...' % app)
        time.sleep(5)
    warn('%s pas encore prete apres %s s' % (app, timeout))
    try:
        info('Etat de deploiement : %s' % AdminApp.getDeployStatus(app))
    except:
        pass
    return 0

def app_manager(node, server):
    return AdminControl.queryNames('type=ApplicationManager,process=%s,node=%s,*' % (server, node))

def app_running(app, node, server):
    return AdminControl.completeObjectName('type=Application,name=%s,process=%s,node=%s,*' % (app, server, node)) != ''

def start_app(app, node, server):
    mgr = app_manager(node, server)
    if not mgr:
        warn('Serveur %s/%s arrete ou injoignable : %s demarrera avec le serveur '
             '(aucun redemarrage n\'est effectue par ce script).' % (node, server, app))
        return 0
    if app_running(app, node, server):
        ok('%s deja demarree sur %s/%s' % (app, node, server))
        return 1
    AdminControl.invoke(mgr, 'startApplication', app)
    if app_running(app, node, server):
        ok('%s demarree sur %s/%s' % (app, node, server))
        return 1
    error('%s ne demarre pas sur %s/%s : consulter SystemOut.log du serveur' % (app, node, server))
    return 0

def stop_app(app, node, server):
    mgr = app_manager(node, server)
    if not mgr:
        info('Serveur %s/%s arrete : rien a arreter pour %s' % (node, server, app))
        return 1
    if not app_running(app, node, server):
        ok('%s deja arretee sur %s/%s' % (app, node, server))
        return 1
    AdminControl.invoke(mgr, 'stopApplication', app)
    if not app_running(app, node, server):
        ok('%s arretee sur %s/%s' % (app, node, server))
        return 1
    error('%s ne s\'arrete pas sur %s/%s' % (app, node, server))
    return 0

def app_targets(app):
    """Couples (noeud, serveur) sur lesquels l'application est mappee."""
    result = []
    deployment = AdminConfig.getid('/Deployment:%s/' % app)
    if not deployment:
        return result
    deployed = AdminConfig.showAttribute(deployment, 'deployedObject')
    targets = AdminConfig.showAttribute(deployment, 'deploymentTargets')
    for t in targets[1:-1].split():
        if contains(t, 'ServerTarget'):
            result.append((AdminConfig.showAttribute(t, 'nodeName'), AdminConfig.showAttribute(t, 'name')))
    return result

def check_context(app, context):
    view = AdminApp.view(app, '-CtxRootForWebMod')
    if contains(view, context):
        ok('Racine de contexte %s confirmee pour %s' % (context, app))
        return 1
    error('Racine de contexte %s introuvable pour %s :\n%s' % (context, app, view))
    return 0

def http_url(cell, node, server, context):
    node_id = AdminConfig.getid('/Cell:%s/Node:%s/' % (cell, node))
    host = AdminConfig.showAttribute(node_id, 'hostName')
    port = ''
    for se in lines(AdminConfig.list('ServerEntry', node_id)):
        if AdminConfig.showAttribute(se, 'serverName') == server:
            for nep in lines(AdminConfig.list('NamedEndPoint', se)):
                if AdminConfig.showAttribute(nep, 'endPointName') == 'WC_defaulthost':
                    port = AdminConfig.showAttribute(AdminConfig.showAttribute(nep, 'endPoint'), 'port')
    if port:
        return 'http://%s:%s%s/' % (host, port, context)
    return '(port WC_defaulthost introuvable)'

def finish(label):
    print ''
    if EFI_FAILURES:
        efi_log('BILAN', '%s : %d erreur(s)' % (label, len(EFI_FAILURES)))
        for f in EFI_FAILURES:
            print '   - %s' % f
        sys.exit(1)
    efi_log('BILAN', '%s : termine sans erreur' % label)
# ----------------------------------------------------------------------------
# Fin du bloc commun EFI
# ----------------------------------------------------------------------------

def main(argv):
    opts = parse_args(argv, ['ear', 'app', 'context', 'cell', 'batch', 'sync-timeout'], ['dry-run', 'help'])
    if opts.get('help'):
        print USAGE
        return
    dry = opts.get('dry-run')
    timeout = int(opts.get('sync-timeout') or '180')
    if opts.get('batch'):
        items = read_batch(opts['batch'])
    else:
        for k in ['ear', 'app']:
            if not opts.get(k):
                die('Option --%s obligatoire (ou --batch)' % k, 2)
        items = [(opts['ear'], opts['app'], opts.get('context') or '')]
    cell = current_cell(opts)
    info('Connecte a la cellule %s - mise a jour de %d application(s)%s' % (cell, len(items), (dry and ' [DRY-RUN]') or ''))

    updated = []
    for (ear, app, context) in items:
        print ''
        info('=== %s ===' % app)
        from java.io import File
        if not File(ear).isFile():
            error('EAR introuvable : %s' % ear)
            continue
        if not app_installed(app):
            error('%s n\'est pas installee : utiliser deploy-ear.py' % app)
            continue
        targets = app_targets(app)
        info('Cibles actuelles : %s' % (', '.join(map(lambda t: '%s/%s' % t, targets)) or 'aucune'))
        options = '[-operation update -contents %s -nouseMetaDataFromBinary]' % ear
        if dry:
            info('[DRY-RUN] AdminApp.update("%s", "app", "%s")' % (app, options))
            continue
        try:
            AdminApp.update(app, 'app', options)
        except:
            error('Echec de AdminApp.update pour %s : %s' % (app, sys.exc_info()[1]))
            AdminConfig.reset()
            continue
        AdminConfig.save()
        ok('%s mise a jour et configuration enregistree' % app)
        updated.append((app, context, targets))

    if dry:
        info('[DRY-RUN] aucune modification effectuee')
        finish('Mise a jour (simulation)')
        return
    nodes = []
    for (app, context, targets) in updated:
        for (node, server) in targets:
            nodes.append(node)
    sync_nodes(nodes)
    for (app, context, targets) in updated:
        wait_ready(app, timeout)
        if context:
            check_context(app, context)
        for (node, server) in targets:
            if app_manager(node, server) and not app_running(app, node, server):
                start_app(app, node, server)
            state = (app_running(app, node, server) and 'DEMARREE') or 'ARRETEE'
            info('%s sur %s/%s : %s' % (app, node, server, state))
    finish('Mise a jour')

main(sys.argv)
