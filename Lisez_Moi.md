Contenu du répertoire
=====================
Ce répertoire contient
- le code source d'un programme destiné à contrôler et suivre l'exécution du service de gestion robuste d'objets dupliqués (podr) : `Moniteur/MoniteurImpl`
- le code source d'un client interactif permettant de créer, lire, écrire des objets dupliqués : `Shell`
- une version étendue des interfaces `Client_itf` et `Server_itf` qu'il faut implanter afin de pouvoir utiliser le Moniteur
- un(e suggestion de) squelette pour la classe SharedObject, où sont placées les sondes permettant l'instrumentation
- une version de l'application `Irc` adaptée au podr instrumenté.
- ce fichier, qui documente brièvement les fichiers sources et donne un mode d'emploi pour l'instrumentation du podr en vue de l'utilisation du Moniteur  

Les interfaces étendues
=======================
Le moniteur doit permettre de suivre l'évolution des registres site par site. Pour faciliter la lecture, un nom de site doit être fourni lors de l'initialisation du client (voir Shell et Irc)

Client_itf
----------
Ajout des méthodes 
- getSite(), fournissant le nom du site
- getObj() et getVersion(), fournissant la valeur et la version d'un objet à partir de son nom

*Note* : Le squelette fourni pour la classe SharedObject appelle une méthode getIdSite() de la classe Client, qui est une version identique, mais locale (non Remote) de la méthode getSite().

Server_Itf
----------
- nommage : ajout d'une méthode list(), fournissant la liste des noms d'objets enregistrés auprès du service
- instrumentation : ajout d'accesseurs pour le Moniteur associé au serveur

Shell
-----
Implante un client interactif, qui permet de
- créer un nouvel objet dupliqué, en lui donnant une valeur initiale
- lire/écrire un objet donné
- afficher la valeur de la copie locale (get)
- lister les valeurs de l'ensemble des copies locales
Comme pour Irc, les Shells sont supposés être lancés au démarrage.

Moniteur
--------
- exporte une interface distante permettant de contrôler(feuVert) les réponses aux requêtes du protocole (voir plus bas) et de suivre (signaler) l'exécution des opérations de lecture/écriture du protocole
- l'implémentation permet, de manière interactive de
   * suivre (tracer) les débuts/fins d'opérations de lecture/écriture par les différents sites sur les différents objets dupliqués, ou abandonner ce suivi (laisser)
   * fixer le temps de réponse de chacun (ou de l'ensemble) des sites, ou de geler provisoirement (délai "-") ou définitivement (délai "X") les réponses (Délai "X" permet de simuler simplement une panne.) Initialement, les réponses sont provisoirement gelées sur l'ensemble des sites
   * lister la valeur des différentes copies d'un objet dupliqué, ou de l'ensemble des objets dupliqués (cliché nomObjet ou cliché *)
   * lister les délais sur chacun des sites (délai --d)

Instrumentation
===============
Pour pouvoir suivre et contrôler le protocole, le moniteur demande (outre l'implémentation des méthodes ajoutées aux interfaces) quelques adaptations
- au niveau du serveur : lors du démarrage de l'application, ajouter le moniteur à la barrière de synchronisation.
- au niveau du SharedObject
  * pour la méthode read, encadrer l'appel à Client.read() par un appel à signaler("DL"...) et signaler("TL"...) sur le moniteur
  * idem pour write ("DE"/"TE")
  * pour les méthodes update et reportValue, commencer la méthode par un appel à feuVert() sur le moniteur. Cet appel permettra de contrôler le temps de réponse d'un site par rapport aux requêtes de lecture/écriture qu'il reçoit. Le facteur de répétition permet d'avoir des temps de réponse différents pour les écritures et pour les lectures, ce qui peut s'avère utile lors des tests, par exemple pour mettre en évidence le phénomène d'inversion de valeur avec les registres réguliers.

Mode d'emploi
=============
Le serveur doit être lancé en premier. Il prend un paramètre correspondant au nombre de clients initial.

Le moniteur et les clients doivent ensuite être lancés, dans n'importe que ordre

Telle quelle, l'implémentation est destinée à fonctionner sur une même machine.

Le code requiert Java 11 (ou ultérieur). Vous pouvez bien sûr l'adapter à votre gré, surtout si vous y trouvez des bugs, ce qui arrivera sûrement, car il s'agit d'un code tout neuf.
