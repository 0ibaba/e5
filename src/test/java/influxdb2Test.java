import com.influxdb.LogLevel;
import com.influxdb.client.*;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import io.qyi.e5.outlook_log.entity.OutlookLog;
import org.junit.jupiter.api.Test;
import org.springframework.cglib.beans.BeanMap;

import java.time.Instant;
import java.util.*;

public class influxdb2Test {
    InfluxDBClient influxDBClient = InfluxDBClientFactory.create("http://127.0.0.1:8086", "ko6GtE_P5R2AlMkCBkEgBwW7rVBl46GYx0IoCrG-Dd5VFxTDSnFJ--BB2f8FRFcGd6Tb_yu6-MlMAD-lMSbH6A==".toCharArray()
    );

    private String org = "luoye";

    @Test
    public void save(){

        influxDBClient.setLogLevel(LogLevel.BASIC);
        WriteOptions writeOptions = WriteOptions.builder()
                .batchSize(5000)
                .flushInterval(1000)
                .bufferLimit(10000)
                .jitterInterval(1000)
                .retryInterval(5000)
                .build();

        List<OutlookLog> list = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            OutlookLog outlookLog = new OutlookLog();
            outlookLog.setMsg(i + "- ok").setOriginalMsg("加入成功").setCallTime(Instant.now());
            list.add(outlookLog);
        }

        try (WriteApi writeApi = influxDBClient.getWriteApi()) {
            // writeApi.writeMeasurement("e5", org ,WritePrecision.NS,outlookLog);
            writeApi.writeMeasurements("e5", org ,WritePrecision.NS,list);
            List<Point> list1 = new ArrayList<>();

            list.forEach(outlookLog -> {
                BeanMap beanMap = BeanMap.create(outlookLog);
                Point point = Point
                        .measurement("githubId_100")
                        .addTag("githubId", "123465")
                        // .addFields(beanMap)
                        .addFields(beanMap);
                list1.add(point);
            });
            System.out.println("list 大小:" + list1.size());
            Map<String, Object> aa = new HashMap<>();
            aa.put("a1", 1);
            writeApi.writePoint("e5",org,list1.get(0));
        }
        influxDBClient.close();
    }

    @Test
    public void saveLog(){
        influxDBClient.setLogLevel(LogLevel.BASIC);
        for (int i = 0; i < 100; i++) {
            addLog(1002, 37,"error", 0, "检测到3次连续错误，下次将不再自动调用，请修正错误后再授权开启续订。");
        }

    }

    public void addLog(int githubId, int outlookId, String msg, int result, String original_msg) {
        try (WriteApi writeApi = influxDBClient.getWriteApi()) {
            OutlookLog log = new OutlookLog();
            log.setCallTime(Instant.now())
                    .setGithubId(String.valueOf(githubId) )
                    .setOutlookId(String.valueOf(outlookId))
                    .setMsg(msg)
                    .setOriginalMsg(original_msg).setResultc(result);
            writeApi.writeMeasurement("e5",org, WritePrecision.NS, log);


        }

    }
    public void addLog2(int githubId, int outlookId, String msg, int result, String original_msg) {
        try (WriteApi writeApi = influxDBClient.getWriteApi()) {


            Point point = Point.measurement("OutlookLog")
                    .addTag("githubId",String.valueOf(githubId))
                    .addTag("outlookId",String.valueOf(outlookId))
                    .addField("msg", msg)
                    .addField("resultc", result)
                    .addField("originalMsg", original_msg);
            writeApi.writePoint("e5",org,point);
        }

    }


    @Test
    public void find(){
        String flux = "from(bucket:\"e5\") |> range(start: 0)" +
                "|> filter(fn: (r) => r[\"_measurement\"] == \"OutlookLog\")" +
                "|> filter(fn: (r) => r[\"githubId\"] == \"1002\")" +
                "|> filter(fn: (r) => r[\"outlookId\"] == \"37\")" +
                "|> limit(n: 100)";
        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables = queryApi.query(flux,org);
        for (FluxTable fluxTable : tables) {
            List<FluxRecord> records = fluxTable .getRecords();

            for (FluxRecord fluxRecord : records) {
                // System.out.println(fluxRecord.getField());
                System.out.println(fluxRecord.getField() + " ->" + fluxRecord.getValueByKey("_value"));
            }
            System.out.println("------------------------------------------");
        }
        influxDBClient.close();
    }

    @Test
    public void findPojo(){
        String flux = "from(bucket:\"e5\") |> range(start: 0)" +
                "|> filter(fn: (r) => r[\"_measurement\"] == \"OutlookLog\")" +
                "|> filter(fn: (r) => r[\"githubId\"] == \"1002\")" +
                "|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")" +
                "|> limit(n: 100)";
        QueryApi queryApi = influxDBClient.getQueryApi();
        List<OutlookLog> tables = queryApi.query(flux,org,OutlookLog.class);
        for (OutlookLog table : tables) {
            if (table.getMsg() == null) {
                continue;
            }
            System.out.println("Msg: " + table.getMsg());
            System.out.println("OriginalMsg: " + table.getOriginalMsg());
            System.out.println("---------------");
        }
        System.out.println("tables 大小:" + tables.size());
        influxDBClient.close();
    }

    @Test
    public void findPojoAsync() throws InterruptedException {
        String flux = "from(bucket:\"e5\") |> range(start: 0)" +
                "|> filter(fn: (r) => r[\"_measurement\"] == \"OutlookLog\")" +
                "|> filter(fn: (r) => r[\"githubId\"] == \"1002\")" +
                "|> limit(n: 100)";
        QueryApi queryApi = influxDBClient.getQueryApi();
         queryApi.query(flux,org,OutlookLog.class,(cancellable, outlookLog) -> {
             if (outlookLog.getMsg() != null) {
                 System.out.println(outlookLog);
             }

        });
        System.out.println("查询完成");
        Thread.sleep(5_000);
        influxDBClient.close();
    }

}
