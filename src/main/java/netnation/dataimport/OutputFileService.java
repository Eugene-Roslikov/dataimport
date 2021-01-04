package netnation.dataimport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

@Service
public class OutputFileService implements OutputService {
    private static final Logger log = LogManager.getLogger("dataImport");

    @Override
    public void saveSql(String sql, String filename) {
        try ( FileWriter writer = new FileWriter(filename);){
            writer.write(sql);

            log.info("Successfully wrote to the file {}.",filename);
        } catch (IOException exception) {
            log.error("Failed to write to the file.", exception);
        }
    }

    @Override
    public void saveReport(HashMap<String,Integer> productItemCountTotalMap, String filename) {
        try ( FileWriter writer = new FileWriter(filename);){
            for (String key : productItemCountTotalMap.keySet()) {
                writer.write(key + "\t" + productItemCountTotalMap.get(key).toString()+"\n");
            }
            log.info("Successfully wrote to the file {}.",filename);
        } catch (IOException exception) {
            log.error("Failed to write to the file.", exception);
        }
    }
}
