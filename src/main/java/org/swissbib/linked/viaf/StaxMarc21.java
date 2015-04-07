package org.swissbib.linked.viaf;

import com.sun.xml.stream.events.XMLEventAllocatorImpl;
import java.io.*;
import java.util.*;
import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.util.XMLEventAllocator;

/**
 *
 * @author streifdaniel
 */
public class StaxMarc21 {

    private static XMLEventAllocator allocator;
//    private static BufferedWriter fileOut;
    // the state
    private static boolean inViafIdElement = false;
    // personname 700
    private static String preferredTag = "700";
    private static boolean inPreferredElement = false; // set if datafield-tag contains 700, unset after datafield-tag contained 700
    private static final Set<String> preferredCodes = new LinkedHashSet<String>(Arrays.asList(
            new String[]{"a", "b", "c", "d", "q"}));
    private static boolean usePreferredElement = false; // set if subfield in preferred element contains data of preferred code, unset after datafield got consumed
    // personname 400
    private static String alternateTag = "400";
    private static boolean inAlternateElement = false;
    private static final Set<String> alternateCodes = new LinkedHashSet<String>(Arrays.asList(
            new String[]{"a", "b", "c", "d", "n"}));
    private static boolean useAlternateElement = false;
    // the data
    private static String separator = " ";
    private static String viafId;
    private static boolean useCode = false; // characters will contain data of preferred or alternate element, set if in preferred/alternate element and valid code, unset after consumption
    private static String code;
    private static Set<String> setPreferred = new HashSet<String>();
    private static Set<String> setAlternate = new HashSet<String>();
    private static HashSet setAll = new HashSet();
    private static HashMap<String, String> codes = new HashMap<String, String>(); // code => characters
    // solr docs
    private static String outputfolder = "outputfiles";
    private static int containerSize = 100000; // 100k records ~> 30k synonyms
    private static boolean useAllRecords = true;
    private static int containerCount = 1;
    private static int recordCount = 0;
    private static String docType = "personname";
    private static BufferedWriter solrOut;
    private static XMLOutputFactory solrOutput;
    private static XMLStreamWriter solrWriter;
    private static StringBuilder sb = new StringBuilder();

    public void parse(String fileLocation, String outputfolder) {
        this.outputfolder = outputfolder;
        try {
            XMLInputFactory xmlif = XMLInputFactory.newInstance();
            xmlif.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE); // converts &#x303; to ñ etc.


            //GH: ich denke die Implementierung des Allocators braucht man nicht mehr - ausprobieren

            xmlif.setEventAllocator(new XMLEventAllocatorImpl());
            allocator = xmlif.getEventAllocator();

            // marc21 data
            FileInputStream fileInputStream = new FileInputStream(fileLocation);
            XMLStreamReader xmlStreamReader = xmlif.createXMLStreamReader(fileInputStream);

            // synonyms.txt
//            fileOut = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputfile), "UTF-8"));

            // solr docs
            openSolrOut();

            while (xmlStreamReader.hasNext()) {
                parseRecord(xmlStreamReader);
            }
            xmlStreamReader.close();
//            fileOut.flush();
//            fileOut.close();
            closeSolrOut();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void openSolrOut() throws UnsupportedEncodingException, FileNotFoundException, XMLStreamException {
        solrOut = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputfolder + "/solr_" + containerCount + ".xml"), "UTF-8"));
        solrOutput = XMLOutputFactory.newInstance();
        solrWriter = solrOutput.createXMLStreamWriter(solrOut);
        solrWriter.writeStartDocument();
        solrWriter.writeStartElement("add");
    }

    private static void closeSolrOut() throws XMLStreamException {
        containerCount++;
        solrWriter.writeEndElement(); // add
        solrWriter.flush();
        solrWriter.close();
    }

    /*
     * Find container/record/datafield with tag="700" or tag="400", i.e.
     * Mutzenbecher, Esdras Heinrich Save: - container/record/controlfield with
     * tag="001", i.e. viaf148504847 - container/record/datafield (with
     * tag="700" or tag="400")/subfield(s) with code="a" etc, i.e. Hunziker, Tom
     *
     * @param reader @throws XMLStreamException
     */
    private static void parseRecord(XMLStreamReader reader) throws XMLStreamException, IOException {
        int eventCode = reader.next();
        String localname;
        switch (eventCode) {
            case XMLEvent.START_ELEMENT:
                localname = reader.getLocalName();
                /*
                 * Check attributes for controlfield (viafid), datafield (700 or
                 * 400) and subfield (codes)
                 */
                if (localname.equals("controlfield") || localname.equals("datafield") || ((inPreferredElement || inAlternateElement) && localname.equals("subfield"))) {
                    int count = reader.getAttributeCount();
                    if (count > 0) {
                        for (int i = 0; i < count; i++) {
                            String name = reader.getAttributeLocalName(i);
                            String value = reader.getAttributeValue(i);
                            // tag: 001 -> viafid, 700 or 400
                            if (name.equals("tag")) {
                                if (value.equals("001")) {
                                    // keep viaf id
                                    inViafIdElement = true;
                                } else if (value.equals(preferredTag)) {
                                    inPreferredElement = true;
                                } else if (value.equals(alternateTag)) {
                                    inAlternateElement = true;
                                }
                            }
                            // code
                            if ((inPreferredElement || inAlternateElement) && name.equals("code")) {
                                if (preferredCodes.contains(value) || alternateCodes.contains(value)) {
                                    if (inPreferredElement) {
//                                    System.out.println("s_e: preferred code " + value);
                                        usePreferredElement = true;
                                    } else {
                                        // alternate
//                                    System.out.println("s_e: alternate code " + value);
                                        useAlternateElement = true;
                                    }
                                    code = value;
                                    useCode = true;
                                }
                            }
                            // TODO: skip remaining attributes
                        }
                    }
                }
                break;
            case XMLEvent.CHARACTERS:
                if (inViafIdElement == true) {
                    viafId = reader.getText();
                    inViafIdElement = false;
                } else {
                    if (useCode && (inPreferredElement || inAlternateElement)) {
                        String text = reader.getText();
//                        System.out.println("c: preferred: code = " + code + ", value = " + text);
                        codes.put(code, text);
                        useCode = false;
                    }
                }
                break;
            case XMLEvent.END_ELEMENT:
                if (reader.getLocalName().equals("datafield")) {
                    inPreferredElement = false;
                    inAlternateElement = false;
                }
                if ((usePreferredElement || useAlternateElement) && reader.getLocalName().equals("datafield")) {
                    // combine values
                    for (String s : preferredCodes) {
                        // only existing codes
                        if (codes.containsKey(s)) {
                            sb.append(codes.get(s));
                            sb.append(separator);
                        }
                        // all codes
//                        if (codes.containsKey(s)) {
//                            sb.append(codes.get(s));
//                        }
//                        sb.append("|");
                    }
                    if (sb.length() > 0) {
                        sb.deleteCharAt(sb.length() - 1);
                    }
                    if (usePreferredElement) {
//                        System.out.println("preferred: " + sb);
                        setPreferred.add(sb.toString().trim());
                    } else {
//                        System.out.println("alternate: " + sb);
                        setAlternate.add(sb.toString().trim());
                    }

                    codes = new HashMap<String, String>();
                    sb = new StringBuilder();
                    usePreferredElement = false;
                    useAlternateElement = false;
                } else if (reader.getLocalName().equals("record") && (setPreferred.size() > 0 || setAlternate.size() > 0)) {
                    // combine
                    setAll.addAll(setPreferred);
                    setAll.addAll(setAlternate);
                    
                    if (useAllRecords || setAll.size() > 1) {
                        // append to synonyms
//                        fileOut.write("#" + viafId + "\n");
//                        fileOut.write("# viafid: " + viafId + "\n");
//                        fileOut.write("# Ansetzungsformen: " + implodeArray((String[]) setPreferred.toArray(new String[setPreferred.size()]), ",") + "\n");
//                        fileOut.write("# Verworfene Formen: " + implodeArray((String[]) setAlternate.toArray(new String[setAlternate.size()]), ",") + "\n");
//                        fileOut.write(implodeArray((String[]) setAll.toArray(new String[setAll.size()]), ",") + "\n");

                        // add solr doc
                        solrWriter.writeStartElement("doc");
                        // id
                        solrWriter.writeStartElement("field");
                        solrWriter.writeAttribute("name", "id");
                        solrWriter.writeCharacters(viafId);
                        solrWriter.writeEndElement(); // field
                        // viaf doc type
                        solrWriter.writeStartElement("field");
                        solrWriter.writeAttribute("name", "viaftype");
                        solrWriter.writeCharacters(docType);
                        solrWriter.writeEndElement(); // field
                        // preferred
                        for (String s : setPreferred) {
                            solrWriter.writeStartElement("field");
                            solrWriter.writeAttribute("name", "preferred");
                            solrWriter.writeCharacters(s);
                            solrWriter.writeEndElement(); // field
                        }
                        // alternate
                        for (String s : setAlternate) {
                            solrWriter.writeStartElement("field");
                            solrWriter.writeAttribute("name", "alternate");
                            solrWriter.writeCharacters(s);
                            solrWriter.writeEndElement(); // field
                        }
                        // matchstring
                        setPreferred.addAll(setAlternate);
                        setAlternate = new HashSet();
                        for (String s : setPreferred) {
                            setAlternate.add(s.replaceAll("[^\\p{L}\\p{M}*\\p{N}]", ""));
                        }
                        for (String s : setAlternate) {
                            solrWriter.writeStartElement("field");
                            solrWriter.writeAttribute("name", "matchstring");
                            solrWriter.writeCharacters(s.replaceAll("[^\\p{L}\\p{M}*\\p{N}]", ""));
                            solrWriter.writeEndElement(); // field
                        }
                        solrWriter.writeEndElement(); // doc
                        solrWriter.writeCharacters("\n");

                        recordCount++;
//                        System.out.println(recordCount);
                        if (recordCount % containerSize == 0) {
                            closeSolrOut();
                            openSolrOut();
                        }
                    }
                    setPreferred = new HashSet();
                    setAlternate = new HashSet();
                    setAll = new HashSet();
                }
                break;
        }
    }

    public static String implodeArray(String[] inputArray, String glueString) {
        String output = "";
        if (inputArray.length > 0) {
            StringBuilder implode = new StringBuilder();
            implode.append(inputArray[0].replace(",", "\\,"));

            for (int i = 1; i < inputArray.length; i++) {
                implode.append(glueString);
                implode.append(inputArray[i].replace(",", "\\,"));
            }

            output = implode.toString();
        }
        return output;
    }
}