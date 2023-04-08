// PM, mars 2023

// Hypothèse : plusieurs registres possibles, mais duplication sur le même ensemble de sites.

import java.util.Map;
import java.util.HashMap;
import java.io.*;

public class Shell {

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
        String name;
        if (a.length == 1) {
            name = previousName;
        } else {
            name = a[1];
            previousName = name;
        }
        return name;
    }

    public static void main(String[] args) throws Exception {
   
		if (args.length != 1) {
			System.out.println("java Shell <name>");
			return;
		}
        Client.init(args[0]);

        Map actions = new HashMap();

        actions.put ("help", new Action() {
            public void run (String[] args) {
                System.out.println ("c[reate] name val");
                System.out.println ("r[ead] [name]");
                System.out.println ("w[rite] [name] val");
                System.out.println ("g[et] [name]");
                System.out.println ("l[ist]");
                System.out.println ("h[elp]");
                System.out.println ("q[uit]");
            }
        });
        actions.put ("h", actions.get("help"));

        actions.put ("create", new Action() {
            public void run (String[] args) {
                SharedObject obj = Client.publish (args[1], args[2], false);
                objectCache.put (args[1], obj);
                previousName = args[1];
            }
        });
        actions.put ("c", actions.get("create"));

        actions.put ("write", new Action() {
            public void run (String[] args) {
                String name = extractName (args);
                SharedObject obj = getObject (name);
                obj.write(args[args.length-1]);
            }
        });
        actions.put ("w", actions.get("write"));

        actions.put ("read", new Action() {
            public void run (String[] args) {
                String name = extractName (args);
                SharedObject obj = getObject (name);
                obj.read();
            }
        });
        actions.put ("r", actions.get("read"));

        actions.put ("get", new Action() {
            public void run (String[] args) {
                String name = extractName (args);
                SharedObject obj = getObject (name);
                System.out.println (name + " = " + obj.obj + " ("+obj.getVersion()+")");
            }
        });
        actions.put ("g", actions.get("get"));

        actions.put ("list", new Action() {
            public void run (String[] args) {
                System.out.println ("Copies locales utilisées :");
                System.out.println ("==========================");
                for(Map.Entry<String, SharedObject> entry : objectCache.entrySet()) {
                    System.out.println (entry.getKey() + " : " + entry.getValue().obj
                       + " (" + entry.getValue().getVersion() + ")");
                }
            }
        });
        actions.put ("l", actions.get("list"));

        actions.put ("quit", new Action() {
            public void run (String[] args) {
                System.exit(0);
            }
        });
        actions.put ("q", actions.get("quit"));


        BufferedReader in = new BufferedReader (new InputStreamReader (System.in));
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
