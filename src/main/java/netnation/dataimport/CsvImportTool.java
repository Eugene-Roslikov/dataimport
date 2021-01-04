package netnation.dataimport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.commons.lang3.math.NumberUtils;
import org.jooq.InsertValuesStep2;
import org.jooq.InsertValuesStep5;
import org.jooq.conf.ParamType;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.insertInto;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SpringBootApplication
public class CsvImportTool implements CommandLineRunner {
     private static final Logger log = LogManager.getLogger(CsvImportTool.class);

     @Autowired
     private OutputService outputService;

     @Override
     public void run(String... args)  {

          ObjectMapper mapper = new ObjectMapper();
          Map<String, String> map;
          try (Reader jsonReader = new FileReader(args[0])) {
               map = mapper.readValue(jsonReader, Map.class);
          } catch (Exception exception){
               log.error("Failed to read json file.", exception);
               return;
          }

          HashMap<String,String> domainMap = new HashMap<String,String>();
          HashMap<String,Integer> productItemCountTotalMap = new HashMap<String,Integer>();

          CsvMapper csvMapper = new CsvMapper();
          CsvSchema schema = CsvSchema.emptySchema().withHeader();

          try (Reader csvReader = new FileReader(args[1])) {
               MappingIterator<Map<String,String>> it = csvMapper.readerFor(Map.class)
                       .with(schema)
                       .readValues(csvReader);

               InsertValuesStep5<?, Integer, String, String, String, Integer> chargeableStep = insertInto( DSL.table("chargeable"),
                       field("partnerID", SQLDataType.INTEGER),
                       field("product", SQLDataType.VARCHAR),
                       field("partnerPurchasedPlanID", SQLDataType.VARCHAR),
                       field("plan", SQLDataType.VARCHAR),
                       field("usage",  SQLDataType.INTEGER)
               );

               while (it.hasNext()) {
                    Map<String, String> rowAsMap = it.next();

                    Integer partnerID = NumberUtils.toInt(rowAsMap.get("PartnerID"), 0);
                    // Skip entries where the value of PartnerID is in the skip list
                    if(DataImportConstants.getPartnerIDMap().contains(partnerID)){
                         continue;
                    }

                    String partnerPurchasedPlanID = rowAsMap.get("accountGuid").replaceAll("[^a-zA-Z0-9]", "");
                    domainMap.putIfAbsent(partnerPurchasedPlanID, rowAsMap.get("domains"));

                    String partNumber = rowAsMap.get("PartNumber");
                    if(partNumber.isBlank()){
                         log.error("PartNumber is empty. partnerID: {}", partnerID);
                         continue;
                    }

                    Integer itemCount = NumberUtils.toInt(rowAsMap.get("itemCount"), 0);
                    if(itemCount < 1){
                         log.error("itemCount is not positive: {}  partnerID: {}", itemCount, partnerID);
                         continue;
                    }

                    Integer usageValue = itemCount / DataImportConstants.getItemCountMap().getOrDefault(partNumber, 1);

                    productItemCountTotalMap.put(map.getOrDefault(partNumber, partNumber), productItemCountTotalMap.getOrDefault(partNumber, 0) + usageValue);

                    chargeableStep.values(
                            partnerID,
                            map.getOrDefault(partNumber, partNumber),
                            partnerPurchasedPlanID,
                            rowAsMap.get("plan"),
                            usageValue
                    );
               }

               String chargeableSql = chargeableStep.getSQL(ParamType.INLINED);
               outputService.saveSql(chargeableSql, DataImportConstants.chargeableSqlFileName);

          }catch (Exception exception){
               log.error("Error reading CSV file.", exception);
               return;
          }

          String domainsSql = renderDomainsSql(domainMap);

          outputService.saveSql(domainsSql, DataImportConstants.domainsSqlFileName);
          outputService.saveReport(productItemCountTotalMap, DataImportConstants.reportFileName);
     }

     private static String renderDomainsSql(HashMap<String, String> domainMap) {
          try (InsertValuesStep2<?, String, String> domainsStep = insertInto(DSL.table("domains"),
                  field("partnerPurchasedPlanID", SQLDataType.VARCHAR),
                  field("domain", SQLDataType.VARCHAR)
          )) {

               for (String key : domainMap.keySet()) {
                    domainsStep.values(key, domainMap.get(key));
               }

               return domainsStep.getSQL(ParamType.INLINED);
          }
     }

}
