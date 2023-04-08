// PM, mars 2023

// Hypothèse : plusieurs registres possibles, mais duplication sur le même ensemble de sites.

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import java.io.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.Naming;

public class MoniteurImpl extends UnicastRemoteObject implements Moniteur {
    /*******
     * RMI *
     *******/
    private static String hostname = "localhost";	// server site address
    private static int port = 4000;

    private static Server_itf server;		// référence du serveur distant
    private static Moniteur monMon;		    // référence distante du moniteur
    private static Set<Client_itf> sites = new HashSet<Client_itf>();// ensemble des sites

    private static HashSet<String> suivis = new HashSet<String>();// registres suivis
    private static HashMap<Integer,String> suivisInv= new HashMap<Integer,String>();
    // table inverse pour les registres suivis (id->nom)
    // (bof (on impose que les noms soient univoques et stables), mais bon...)

    private static HashMap<String,Integer> délais = new HashMap<String,Integer>();
    private static int largeurC0 = 4; // "site"
    // nom site -> délai de réponse du site

    public MoniteurImpl() throws RemoteException {
        super();
    }


    // démarrage du moniteur
    public static void init() {
        try {
            Moniteur monMon = new MoniteurImpl();
            server = (Server_itf) Naming.lookup("//"+hostname+":"+port+"/Server");
            System.out.println("Bound to Server");
            //attendre que tous les sites soient prêts et en récupérer la liste
            sites = server.setMonitor(monMon);
            System.out.println("Tout le monde est prêt.");
            for (Client_itf s : sites) {
                String nomSite = s.getSite();
                // évaluer la largeur de la colonne sites des tableaux
                largeurC0 = Math.max(largeurC0, nomSite.length());
                //initialiser délais à "indéterminé"
                délais.put(nomSite,-1);
            }
        } catch (Exception ex) {
            System.out.println("Server error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /*******************
     * Instrumentation *
     *******************/

    public void feuVert(String site, int facteur) throws RemoteException {
        // minimal (indistinct, pas de trace)

        int d = 0;
        try {
        for (int i=0; i<facteur; i++) {
        	d = délais.get(site);
            while (d == -1) {
                Thread.sleep(10000);
                d = délais.get(site);
            }
            if (d == -2) Thread.sleep(1_800_000_000); // une implémentation de l'infini
            else Thread.sleep(d*1000);
            }
        } catch (InterruptedException iex) {
            System.out.println("Server error: " + iex.getMessage());
            iex.printStackTrace();
        }
        return;
    }

    public void signaler(String événement,String site,int idReg) throws RemoteException {
        String registre = suivisInv.get(idReg);
        if (suivis.contains(registre)) {
            System.out.println(site + " : " + événement);
            listerRegistre(registre);
        }
    }

    public static void listerRegistre(String name) {
        // en-tête
        System.out.println(name+" :");
        System.out.println("-".repeat(largeurC0 + 28));
        System.out.printf("|%"+ (largeurC0 + 2) + "s |  valeur  |  version  |\n", "site");
        System.out.println("-".repeat(largeurC0 + 28));

        // lignes justifiées
        try {
            for (Client_itf s : sites) {
                System.out.printf("|%"+(largeurC0+2)+"s | %8s | %9s |\n",s.getSite(),s.getObj(name),s.getVersion(name));
            }
        } catch (RemoteException rex) {
            System.out.println("erreur accès client: " + rex.getMessage());
            rex.printStackTrace();
        }
        System.out.println("-".repeat(largeurC0 + 28));
    }

    public static String afficher(int d) {
        if (d == -1) return "-";
        else if (d == -2) return "X";
        else return String.valueOf(d);
    }
    public static void listerDélais() {
        // en-tête
        System.out.println("-".repeat(largeurC0+15));
        // lignes justifiées
        try {
            for (Client_itf s : sites) {
                String nomSite = s.getSite();
                System.out.printf("|%"+(largeurC0+2)+"s | %7s |\n", nomSite,afficher(délais.get(nomSite)));
            }
        } catch (RemoteException rex) {
            System.out.println("erreur accès client : " + rex.getMessage());
            rex.printStackTrace();
        }
        System.out.println("-".repeat(largeurC0+15));
    }

    /*******************
     * IHM (dont main) *
     *******************/

    private interface Action {
        public void run (String[] args);
    }

    private static HashMap<String,SharedObject> objectCache = new HashMap<String,SharedObject>();

    private static SharedObject getObject (String objname) {
        SharedObject obj = (SharedObject) objectCache.get (objname);
        if (obj == null) {
            obj = Client.lookup (objname);
            if (obj == null)
                throw new Error ("Object "+objname+" not found.");
            objectCache.put (objname, obj);
        }
        return obj;
    }

    private static String previousName = null;
    private static String extractName (String[] a) {
        String name = null;
        if (a.length == 1) {
            name = previousName;
        } else {
            name = a[1];
            try {
                if (server.lookup(name) < 0) name = null ;
                else previousName = name;
            } catch (RemoteException rex) {
                System.out.println("erreur lookup : " + rex.getMessage());
                rex.printStackTrace();
            }
        }
        return name;
    }

    public static void main(String[] args) throws Exception {
        MoniteurImpl.init(); // démarrage RMI

        Map actions = new HashMap();

        actions.put ("help", new Action() {
            public void run (String[] args) {
                System.out.println ("t[race] [nom]");
                System.out.println ("l[aisser] [nom]");
                System.out.println ("c[liche] [nom|*|--d]");
                System.out.println ("d[elai] <site|*> [valeur]");
                System.out.println ("q[uitter]");
                System.out.println ("h[elp]");
            }
        });
        actions.put ("h", actions.get("help"));

        actions.put ("trace", new Action() {
            public void run (String[] args) {
                String name = extractName (args);
                if (name != null) {
                    try {
                        suivis.add(name);
                        suivisInv.put(server.lookup(name),name);
                    } catch (RemoteException rex) {
                        System.out.println("erreur lookup : " + rex.getMessage());
                        rex.printStackTrace();
                    }
                }
                else System.out.println ("??? (nom inconnu)");
            }
        });
        actions.put ("t", actions.get("trace"));

        actions.put ("laisser", new Action() {
            public void run (String[] args) {
                String name = extractName (args);
                if (name != null) {
                    try {
                        suivis.remove(name);
                        suivisInv.remove(server.lookup(name));
                    } catch (RemoteException rex) {
                        System.out.println("erreur accès client : " + rex.getMessage());
                        rex.printStackTrace();
                    }
                }
                else System.out.println ("??? (nom inconnu)");
            }
        });
        actions.put ("l", actions.get("laisser"));

        actions.put ("cliche", new Action() {
            public void run (String[] args) {
                if (args[1].equals("--d")) listerDélais();
                else if (args[1].equals("*")) {
                    try {
                        System.out.println ("============");
                        for (String nom : server.list()) listerRegistre(nom);
                        System.out.println ("============");
                    } catch (RemoteException rex) {
                        System.out.println("erreur list : " + rex.getMessage());
                        rex.printStackTrace();
                    }

                }
                else {
                    String name = extractName (args);
                    if (name != null) listerRegistre(name);
                    else System.out.println ("??? (nom inconnu)");
                }
            }
        });
        actions.put ("c", actions.get("cliche"));
        actions.put ("cliché", actions.get("cliche"));

        actions.put ("delai", new Action() {
            public void run (String[] args) {
                int d = -1 ; // attente indéterminée
                //analyse des paramètres
                if (args.length == 1) throw new IllegalArgumentException("??? (nom inconnu)");
                if (args.length == 3) {
                    if (args[2].equals("X")) d=-2;
                    else if (args[2].equals("-")) d=-1;
                    else  try {
                            d = Integer.parseInt (args[2]);
                            if (d < -2)  {
                                d=-1;
                                System.out.println ("??? (délai hors bornes)");
                            }
                        } catch (NumberFormatException nfx) {
                            throw new IllegalArgumentException("??? (délai non entier)");
                        }
                }

                String name = args[1];
                if (name.equals("*")) {
                    try {
                        for (Client_itf s : sites) {
                            if (délais.get(s.getSite()) > -2) délais.put(s.getSite(),d);
                        }
                    } catch (RemoteException rex) {
                        System.out.println("erreur accès client : " + rex.getMessage());
                        rex.printStackTrace();
                    }
                } else {
                    if (délais.keySet().contains(name)) {
                        if (délais.get(name) > -2) délais.put(name,d);
                        else System.out.println ("??? (site en panne)");
                    }
                    else System.out.println ("??? (nom de site inconnu)");
                }
            }
        });
        actions.put ("d", actions.get("delai"));
        actions.put ("délai", actions.get("delai"));

        actions.put ("quitter", new Action() {
            public void run (String[] args) {
                System.exit(0);
            }
        });
        actions.put ("q", actions.get("quitter"));

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print ("> ");
            String line = in.readLine();
            if (line == null)
                break;
            String[] cmde = line.split("\\s");
            Action a = (Action) actions.get(cmde[0]);
            if (a != null) {
                try {
                    a.run (cmde);
                } catch (Throwable e) {
                    System.out.println ("Echec : "+e);
                }
            } else {
                System.out.println ("???");
            }
        }
        System.exit(0);
    }
}
