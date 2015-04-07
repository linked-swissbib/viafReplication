package org.swissbib.linked.viaf;

/**
 *
 * @author streifdaniel
 */
public class SwissbibViaf {

    private static String filename = null;
    private static String outputfolder = null;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
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

        StaxMarc21 m21parser = new StaxMarc21();
//        System.out.println("Parsing " + filename + " to " + outputfolder);
        // TODO: configuration with apache commons and .properties file
        m21parser.parse(filename, outputfolder);
    }

    private static void printUsage() {
        System.out.println("usage: java -i <xmlfile> -o <outputfolder>");
    }
}
