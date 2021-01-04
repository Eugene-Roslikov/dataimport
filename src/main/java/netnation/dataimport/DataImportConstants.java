package netnation.dataimport;

import java.util.Map;
import java.util.Set;

public final class DataImportConstants {
    private DataImportConstants () { // private constructor
    }
    public static final String chargeableSqlFileName = "insertChargeable.sql";
    public static final String domainsSqlFileName = "insertDomains.sql";
    public static final String reportFileName = "successLog.txt";

    public static Map<String, Integer> getItemCountMap() {
        return Map.of(
                "EA000001GB0O",1000,
                "PMQ00005GB0R",5000,
                "SSX006NR",1000,
                "SPQ00001MB0R",2000
        );
    }
    public static Set<Integer> getPartnerIDMap() {
        return Set.of( 26392 );
    }
}
