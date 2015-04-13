package org.swissbib.linked.viaf;

/**
 *
 * @author streifdaniel
 */
public class SwissbibViaf2 {

    private static String filename = null;
    private static String outputfolder = null;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        boolean dev = true;
    
        if (dev && args.length == 0) {
            //filename = "resources/test_1.xml";
            //filename = "resources/Feb12.m21.10k";
            //filename = "resources/viaf-20130115-clusters-marc21.congress10k.xml";
            //filename = "resources/viaf-20130115-clusters-marc21.o_10k_l_10k.xml";
            filename = "data/small.viaf.xml";
            outputfolder = "outputfiles";
        } else {
            try {
                if (args[0].equals("-i") && args[2].equals("-o")) {
                    filename = args[1];
//                filename = "resources/test_1.xml";
//                filename = "resources/Feb12.m21.10k";
                    outputfolder = args[3];
                } else {
                    printUsage();
                }
            } catch (ArrayIndexOutOfBoundsException aioobe) {
                printUsage();
                System.exit(0);
            } catch (Exception ex) {
                printUsage();
                ex.printStackTrace();
            }
        }
        StaxMarc212 m21parser = new StaxMarc212();
        m21parser.records.add(new Record(
                "personname",
                "700", new String[]{"a", "b", "c", "d", "q"},
                "400", new String[]{"a", "b", "c", "d", "n"}));
        m21parser.records.add(new Record(
                "corporatebody",
                "710", new String[]{"a", "b"},
                "410", new String[]{"a", "b"}));
        m21parser.records.add(new Record(
                "event",
                "711", new String[]{"a", "b"},
                "411", new String[]{"a", "b"}));
        // System.out.println("Parsing " + filename + " to " + outputfolder);
        // TODO: configuration with apache commons and .properties file
        m21parser.parse(filename, outputfolder);
    }

    private static void printUsage() {
        System.out.println("usage: java -i <xmlfile> -o <outputfolder>");
    }
}
