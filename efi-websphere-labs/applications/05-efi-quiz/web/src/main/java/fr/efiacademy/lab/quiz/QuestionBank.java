package fr.efiacademy.lab.quiz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Banque de questions sur IBM WebSphere ND 8.5.5, adaptée au laboratoire EFI
 * (DMGR vm-was-efi, SOAP 8880, console 9044, nœuds nodesa1/nodesa2).
 */
public final class QuestionBank {

    public static final String DMGR = "dmgr";
    public static final String NODEAGENT = "nodeagent";
    public static final String FEDERATION = "federation";
    public static final String PACKAGING = "packaging";
    public static final String SSL = "ssl";
    public static final String SYNC = "sync";
    public static final String DEPLOY = "deploy";

    private static final Map<String, String[]> THEMES = new LinkedHashMap<String, String[]>();
    private static final List<Question> QUESTIONS = new ArrayList<Question>();

    static {
        THEMES.put(DMGR, new String[] {"Deployment Manager", "🧭"});
        THEMES.put(NODEAGENT, new String[] {"Node Agent", "🔄"});
        THEMES.put(FEDERATION, new String[] {"Fédération", "🔗"});
        THEMES.put(PACKAGING, new String[] {"EAR / WAR", "📦"});
        THEMES.put(SSL, new String[] {"SSL / TLS", "🔐"});
        THEMES.put(SYNC, new String[] {"Synchronisation", "♻️"});
        THEMES.put(DEPLOY, new String[] {"Déploiement", "🚀"});

        // ---------------- DMGR ----------------
        q(DMGR, "Quel est le rôle principal du Deployment Manager (DMGR) ?",
                new String[] {"Exécuter les applications de production de la cellule",
                    "Administrer la cellule de façon centralisée et détenir le référentiel maître de configuration",
                    "Répartir la charge HTTP entre les serveurs d'applications",
                    "Stocker les sessions HTTP des utilisateurs"}, 1,
                "Le DMGR est le point central d'administration : il héberge la console, le référentiel maître "
                + "(config/cells/...) et pilote les nœuds via leurs Node Agents.");
        q(DMGR, "Dans le laboratoire EFI, sur quel port HTTPS la console d'administration du DMGR est-elle accessible ?",
                new String[] {"9043", "9060", "9044", "9443"}, 2,
                "Le port par défaut d'un DMGR est 9043, mais dans ce laboratoire les ports ont été incrémentés : "
                + "la console est sur https://vm-was-efi:9044/ibm/console.");
        q(DMGR, "Quel script démarre le Deployment Manager ?",
                new String[] {"startNode.sh", "startManager.sh", "startServer.sh server1", "wsadmin.sh -start"}, 1,
                "startManager.sh, situé dans profiles/Dmgr01/bin, démarre le processus dmgr. "
                + "startNode.sh démarre un Node Agent.");
        q(DMGR, "Quel connecteur et quel port wsadmin utilise-t-il pour joindre le DMGR du laboratoire ?",
                new String[] {"RMI 2809", "SOAP 8880", "HTTP 9080", "IPC 9632"}, 1,
                "wsadmin.sh -conntype SOAP -host vm-was-efi -port 8880. Le port SOAP par défaut d'un DMGR est 8879, "
                + "il vaut 8880 dans ce laboratoire.");
        q(DMGR, "Doit-on déployer des applications métier sur le processus dmgr ?",
                new String[] {"Oui, c'est le serveur le plus fiable de la cellule",
                    "Oui, mais uniquement les EAR de moins de 10 Mo",
                    "Non : le DMGR est réservé à l'administration, les applications vont sur les serveurs des nœuds",
                    "Non, car le DMGR ne possède pas de JVM"}, 2,
                "Le DMGR n'héberge que les applications d'administration (console, etc.). Les applications métier "
                + "sont mappées sur des serveurs d'applications ou des clusters des nœuds fédérés.");
        q(DMGR, "Que se passe-t-il pour les applications en cours d'exécution si le DMGR s'arrête ?",
                new String[] {"Elles s'arrêtent immédiatement",
                    "Elles continuent de fonctionner ; seule l'administration centralisée est indisponible",
                    "Elles passent en lecture seule",
                    "Elles basculent automatiquement sur le DMGR de secours"}, 1,
                "Le DMGR n'est pas dans le chemin des requêtes : les serveurs d'applications continuent de servir "
                + "les utilisateurs, mais on ne peut plus administrer la cellule depuis la console.");

        // ---------------- NODE AGENT ----------------
        q(NODEAGENT, "Quel est le rôle du Node Agent ?",
                new String[] {"Servir les pages JSP aux utilisateurs",
                    "Relayer l'administration du DMGR sur le nœud, synchroniser la configuration et gérer les serveurs du nœud",
                    "Chiffrer les communications entre navigateur et serveur",
                    "Compiler les applications avant déploiement"}, 1,
                "Le Node Agent est un processus d'administration présent sur chaque nœud fédéré : il synchronise "
                + "la configuration locale, démarre, arrête et surveille les serveurs du nœud.");
        q(NODEAGENT, "Quelle commande démarre le Node Agent du nœud nodesa1 ?",
                new String[] {"profiles/Custom01/bin/startNode.sh", "profiles/Dmgr01/bin/startManager.sh",
                    "profiles/Custom01/bin/startServer.sh test", "bin/nodeagent.sh --start"}, 0,
                "startNode.sh, lancé depuis le répertoire bin du profil personnalisé Custom01, démarre le nodeagent.");
        q(NODEAGENT, "Le Node Agent est arrêté. Qu'advient-il du serveur « test » déjà démarré ?",
                new String[] {"Il s'arrête aussitôt",
                    "Il continue de fonctionner mais n'est plus administrable ni synchronisable depuis le DMGR",
                    "Il redémarre en boucle",
                    "Il devient le nouveau Node Agent"}, 1,
                "Les serveurs d'applications sont des JVM indépendantes. Sans Node Agent, ils continuent de servir "
                + "les requêtes, mais la console ne peut plus les piloter et la configuration n'est plus synchronisée.");
        q(NODEAGENT, "Combien de Node Agents possède un nœud fédéré ?",
                new String[] {"Un par serveur d'applications", "Un seul", "Deux (actif / passif)", "Aucun"}, 1,
                "Chaque nœud fédéré possède exactement un processus nodeagent.");
        q(NODEAGENT, "Quelle option de stopNode.sh arrête aussi tous les serveurs d'applications du nœud ?",
                new String[] {"-all", "-force", "-stopservers", "-cascade"}, 2,
                "stopNode.sh -stopservers arrête les serveurs du nœud avant d'arrêter le Node Agent "
                + "(ajouter -username/-password si la sécurité administrative est active).");

        // ---------------- FÉDÉRATION ----------------
        q(FEDERATION, "Quelle commande fédère le profil Custom01 de nodesa1 dans la cellule du laboratoire ?",
                new String[] {"addNode.sh vm-was-efi 8880", "joinCell.sh vm-was-efi 9044",
                    "syncNode.sh vm-was-efi 8880 -add", "manageprofiles.sh -federate"}, 0,
                "addNode.sh <hôte_dmgr> <port_soap_dmgr>, exécuté depuis le profil à fédérer, rattache le nœud "
                + "à la cellule et crée son Node Agent.");
        q(FEDERATION, "Que contient un profil personnalisé (Custom) juste après sa création ?",
                new String[] {"Un serveur server1 prêt à l'emploi", "Un nœud vide, sans serveur d'applications, destiné à être fédéré",
                    "Un Deployment Manager secondaire", "Un serveur HTTP IBM"}, 1,
                "Un profil personnalisé crée un nœud vide. Les serveurs d'applications sont créés ensuite depuis "
                + "le DMGR, une fois le nœud fédéré.");
        q(FEDERATION, "Quelle option d'addNode.sh conserve les applications déjà installées sur un nœud autonome ?",
                new String[] {"-keepapps", "-includeapps", "-withapps", "-importapps"}, 1,
                "-includeapps transfère les applications du profil autonome vers la cellule lors de la fédération.");
        q(FEDERATION, "Quelle commande retire un nœud de la cellule ?",
                new String[] {"deleteNode.sh", "removeNode.sh", "addNode.sh -remove", "cleanupNode.sh"}, 1,
                "removeNode.sh défédère le nœud et restaure sa configuration d'origine. cleanupNode.sh, lui, "
                + "nettoie la configuration côté DMGR d'un nœud déjà perdu.");
        q(FEDERATION, "Quel prérequis horaire doit être respecté lors d'un addNode ?",
                new String[] {"Le nœud doit être sur le fuseau UTC",
                    "Les horloges du nœud et du DMGR doivent être synchronisées (écart inférieur à 5 minutes)",
                    "Le DMGR doit être redémarré juste avant",
                    "Aucun prérequis horaire"}, 1,
                "Un écart d'horloge trop important (plus de 5 minutes) fait échouer la fédération et "
                + "l'authentification : synchroniser les machines avec NTP.");

        // ---------------- EAR / WAR ----------------
        q(PACKAGING, "Quel fichier déclare les modules et les racines de contexte d'un EAR ?",
                new String[] {"WEB-INF/web.xml", "META-INF/application.xml", "META-INF/MANIFEST.MF", "ibm-web-ext.xml"}, 1,
                "META-INF/application.xml liste les modules (web, ejb, java) et, pour chaque module web, "
                + "sa racine de contexte (context-root).");
        q(PACKAGING, "Où se trouvent les classes Java compilées d'un module WAR ?",
                new String[] {"À la racine du WAR", "Dans META-INF/classes", "Dans WEB-INF/classes", "Dans lib/"}, 2,
                "WEB-INF/classes contient les .class de l'application ; WEB-INF/lib contient les JAR tiers.");
        q(PACKAGING, "Quel est le descripteur de déploiement standard d'un module web ?",
                new String[] {"WEB-INF/web.xml", "META-INF/application.xml", "WEB-INF/ibm-web-bnd.xml", "server.xml"}, 0,
                "web.xml déclare servlets, filtres, listeners, pages d'erreur et contraintes de sécurité.");
        q(PACKAGING, "Quelle version de Java EE implémente WebSphere Application Server 8.5.5 (profil complet) ?",
                new String[] {"J2EE 1.4", "Java EE 5", "Java EE 6", "Jakarta EE 9"}, 2,
                "WAS 8.5.5 traditionnel implémente Java EE 6 : Servlet 3.0, JSP 2.2, EJB 3.1, JPA 2.0, CDI 1.0.");
        q(PACKAGING, "Pourquoi les applications du laboratoire utilisent-elles javax.servlet et non jakarta.servlet ?",
                new String[] {"Par habitude, les deux fonctionnent",
                    "Parce que l'espace de noms jakarta.* (Jakarta EE 9+) n'est pas reconnu par WAS 8.5.5",
                    "Parce que jakarta.* est payant",
                    "Parce que javax.* est plus rapide"}, 1,
                "Jakarta EE 9 a renommé les paquets javax.* en jakarta.*. WAS 8.5.5 ne connaît que javax.* : "
                + "une application jakarta.* ne démarrerait pas (ClassNotFoundException).");
        q(PACKAGING, "Quelle version majeure de fichier .class correspond au bytecode Java 6 ?",
                new String[] {"49", "50", "51", "52"}, 1,
                "49 = Java 5, 50 = Java 6, 51 = Java 7, 52 = Java 8. Vérification : javap -v MaClasse.class | grep major.");

        // ---------------- SSL ----------------
        q(SSL, "Une application doit appeler https://api.open-meteo.com et échoue avec une erreur de certificat. Que faire ?",
                new String[] {"Désactiver SSL dans toute la cellule",
                    "Importer le certificat signataire du site dans le truststore WebSphere (ex. CellDefaultTrustStore)",
                    "Redémarrer le DMGR",
                    "Passer l'URL en http://"}, 1,
                "Console : Sécurité > Gestion des certificats SSL et des clés > Magasins de clés et certificats > "
                + "CellDefaultTrustStore > Certificats de signataire > Récupérer du port (hôte, 443).");
        q(SSL, "Quel magasin contient les certificats signataires de confiance au niveau de la cellule ?",
                new String[] {"CellDefaultKeyStore", "CellDefaultTrustStore", "NodeLTPAKeys", "cacerts du navigateur"}, 1,
                "CellDefaultTrustStore (trust.p12) contient les signataires ; CellDefaultKeyStore (key.p12) contient "
                + "les certificats personnels.");
        q(SSL, "Quel est le format par défaut des magasins de clés créés par WAS 8.5 ?",
                new String[] {"JKS", "PKCS12", "CMS (.kdb)", "PEM"}, 1,
                "Depuis WAS 7, les magasins par défaut sont au format PKCS12 (key.p12, trust.p12).");
        q(SSL, "Pourquoi le navigateur affiche-t-il un avertissement sur https://vm-was-efi:9044 ?",
                new String[] {"La console est compromise",
                    "Le certificat est émis par l'autorité racine interne de la cellule, inconnue du navigateur",
                    "Le port 9044 n'accepte que HTTP",
                    "Java 6 ne supporte pas HTTPS"}, 1,
                "WebSphere génère sa propre autorité (root signer). Le navigateur ne la connaît pas : il faut "
                + "l'accepter ou importer le certificat racine dans le poste.");
        q(SSL, "Quel objet de configuration associe keystore, truststore et protocole pour des connexions SSL ?",
                new String[] {"Un fournisseur JDBC", "Une configuration SSL (ex. NodeDefaultSSLSettings)",
                    "Un hôte virtuel", "Un domaine de sécurité LTPA"}, 1,
                "Une configuration SSL (CellDefaultSSLSettings, NodeDefaultSSLSettings...) référence un keystore, "
                + "un truststore, le protocole (TLSv1.2...) et les suites de chiffrement.");

        // ---------------- SYNCHRONISATION ----------------
        q(SYNC, "Après « Enregistrer » dans la console, comment les nœuds reçoivent-ils la modification si la synchronisation automatique est active ?",
                new String[] {"Ils doivent être redémarrés",
                    "Le Node Agent récupère les changements à la prochaine synchronisation automatique (1 minute par défaut)",
                    "Il faut copier les fichiers à la main",
                    "Les changements ne sont jamais propagés"}, 1,
                "Le service de synchronisation de fichiers du Node Agent compare les documents avec le référentiel "
                + "maître toutes les 60 secondes par défaut.");
        q(SYNC, "Le Node Agent de nodesa1 ne démarre plus à cause d'une configuration locale corrompue. Quelle commande lancer ?",
                new String[] {"syncNode.sh vm-was-efi 8880 (Node Agent arrêté)", "addNode.sh vm-was-efi 8880",
                    "startManager.sh -sync", "wsadmin.sh -sync"}, 0,
                "syncNode.sh, exécuté depuis le profil Custom01 alors que le Node Agent est arrêté, recopie la "
                + "configuration depuis le DMGR.");
        q(SYNC, "Où lance-t-on une synchronisation manuelle d'un nœud dans la console ?",
                new String[] {"Applications > Synchroniser", "Administration système > Nœuds > sélectionner le nœud > Synchroniser",
                    "Sécurité > Synchroniser", "Serveurs > Clusters > Synchroniser"}, 1,
                "Administration système > Nœuds : boutons « Synchroniser » et « Synchronisation complète ». "
                + "L'option « Synchroniser les modifications avec les nœuds » existe aussi lors de l'enregistrement.");
        q(SYNC, "Quelle est la différence entre « Synchroniser » et « Synchronisation complète » ?",
                new String[] {"Aucune",
                    "La synchronisation complète retransfère tous les documents, même ceux considérés comme à jour",
                    "La synchronisation complète redémarre le nœud",
                    "La synchronisation simple ne fonctionne qu'en Jython"}, 1,
                "La synchronisation complète ignore l'état mémorisé (epochs) et compare/transfère l'intégralité "
                + "du référentiel : utile après une modification manuelle des fichiers.");
        q(SYNC, "Quel MBean wsadmin permet de synchroniser un nœud ?",
                new String[] {"ApplicationManager", "NodeSync", "Server", "ConfigService"}, 1,
                "sync = AdminControl.completeObjectName('type=NodeSync,node=nodesa1Node01,*') puis "
                + "AdminControl.invoke(sync, 'sync').");

        // ---------------- DÉPLOIEMENT ----------------
        q(DEPLOY, "Quelle commande Jython installe une application EAR ?",
                new String[] {"AdminConfig.install()", "AdminApp.install()", "AdminControl.deploy()", "AdminTask.addEAR()"}, 1,
                "AdminApp.install('/chemin/app.ear', '[-appname efi-welcome -MapModulesToServers [...]]').");
        q(DEPLOY, "Quelle étape est indispensable après AdminApp.install() pour que l'installation soit persistée ?",
                new String[] {"AdminConfig.save()", "AdminApp.commit()", "Redémarrer wsadmin", "Aucune"}, 0,
                "Sans AdminConfig.save(), les modifications restent dans l'espace de travail temporaire et sont "
                + "perdues à la fin de la session wsadmin.");
        q(DEPLOY, "Quelle option d'AdminApp.install associe les modules au serveur test de nodesa1 ?",
                new String[] {"-target test", "-MapModulesToServers [[.* .* WebSphere:cell=vm-was-efiCell02,node=nodesa1Node01,server=test]]",
                    "-server nodesa1/test", "-MapWebModToVH"}, 1,
                "-MapModulesToServers mappe chaque module sur une cible WebSphere:cell=...,node=...,server=... "
                + "(ou cluster=...). -MapWebModToVH concerne les hôtes virtuels.");
        q(DEPLOY, "Comment démarrer une application installée avec wsadmin ?",
                new String[] {"AdminApp.start('efi-welcome')",
                    "Invoquer startApplication sur le MBean ApplicationManager du serveur cible",
                    "AdminConfig.start('efi-welcome')", "startServer.sh efi-welcome"}, 1,
                "am = AdminControl.queryNames('type=ApplicationManager,node=nodesa1Node01,process=test,*') puis "
                + "AdminControl.invoke(am, 'startApplication', 'efi-welcome').");
        q(DEPLOY, "Où consulter les journaux du serveur test du nœud nodesa1 ?",
                new String[] {"profiles/Dmgr01/logs/dmgr/SystemOut.log", "profiles/Custom01/logs/test/SystemOut.log",
                    "/var/log/messages", "logs/test.log à la racine de l'installation"}, 1,
                "Chaque serveur écrit dans <profil>/logs/<serveur>/ : SystemOut.log, SystemErr.log, "
                + "native_stdout.log, startServer.log...");
        q(DEPLOY, "Quel message de SystemOut.log confirme le démarrage d'une application ?",
                new String[] {"ADMU3000I", "WSVR0221I", "SRVE0190E", "CWWKZ0001I"}, 1,
                "WSVR0221I : Application démarrée. SRVE0250I indique qu'un module web est lié à un hôte virtuel. "
                + "CWWKZ0001I est l'équivalent Liberty.");
    }

    private QuestionBank() {
    }

    private static void q(String theme, String text, String[] choices, int answer, String explanation) {
        QUESTIONS.add(new Question(QUESTIONS.size() + 1, theme, text, choices, answer, explanation));
    }

    public static List<Question> all() {
        return Collections.unmodifiableList(QUESTIONS);
    }

    public static Question get(int id) {
        return id >= 1 && id <= QUESTIONS.size() ? QUESTIONS.get(id - 1) : null;
    }

    public static List<Question> byTheme(String theme) {
        List<Question> out = new ArrayList<Question>();
        for (Question q : QUESTIONS) {
            if (theme == null || "all".equals(theme) || q.getTheme().equals(theme)) {
                out.add(q);
            }
        }
        return out;
    }

    public static boolean isTheme(String theme) {
        return "all".equals(theme) || THEMES.containsKey(theme);
    }

    public static Map<String, String[]> themes() {
        return Collections.unmodifiableMap(THEMES);
    }

    public static String label(String theme) {
        String[] t = THEMES.get(theme);
        return t == null ? "Tous les thèmes" : t[0];
    }

    public static String icon(String theme) {
        String[] t = THEMES.get(theme);
        return t == null ? "🎯" : t[1];
    }
}
