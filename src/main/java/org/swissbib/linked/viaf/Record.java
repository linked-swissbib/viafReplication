package org.swissbib.linked.viaf;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class Record {

    String docType;
    String preferredTag;
    Set<String> preferredCodes;
    boolean isPreferred = false;
    boolean usePreferred = false;
    String alternateTag;
    Set<String> alternateCodes;
    boolean isAlternate = false;
    boolean useAlternate = false;
    
    boolean useCode = false;
    String code;
    Map<String, String> codes = new HashMap<String, String>();

    public Record(String docType, String preferredTag, String[] preferredCodes, String alternateTag, String[] alternateCodes) {
        this.docType = docType;
        this.preferredTag = preferredTag;
        // ordered
        this.preferredCodes = new LinkedHashSet<String>(Arrays.asList(preferredCodes));
        this.alternateTag = alternateTag;
        // ordered
        this.alternateCodes = new LinkedHashSet<String>(Arrays.asList(alternateCodes));
    }
}
