package netnation.dataimport;

import java.util.HashMap;

public interface OutputService {
    void saveSql(String sql, String filename);
    void saveReport(HashMap<String,Integer> productItemCountTotalMap, String filename);
}
