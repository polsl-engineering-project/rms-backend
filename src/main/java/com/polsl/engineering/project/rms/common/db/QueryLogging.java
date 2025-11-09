package com.polsl.engineering.project.rms.common.db;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class QueryLogging {
    public static final String KIND_QUERY = "query";
    public static final String KIND_UPDATE = "update";

    private static final String LOG_SQL_FMT = "Executing {}: {} params: {}";
    private static final String LOG_BATCH_FMT = "Executing batch update: {} batchSize: {} sampleArgs: {}";

    private QueryLogging() {
    }

    private static String singleLine(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    public static void logSql(Logger log, String kind, String sql, Object... params) {
        String single = singleLine(sql);
        final String paramsStr;
        if (params == null || params.length == 0) {
            paramsStr = "[]";
        } else if (params.length == 1) {
            paramsStr = Objects.toString(params[0]);
        } else {
            paramsStr = Arrays.toString(params);
        }
        log.info(LOG_SQL_FMT, kind, single, paramsStr);
    }

    public static void logBatch(Logger log, String sql, int batchSize, Object sampleArgs) {
        Object printableSample = sampleArgs;
        if (sampleArgs instanceof List<?> list) {
            var printable = new ArrayList<String>(list.size());
            for (var item : list) {
                if (item instanceof Object[] objectArray) {
                    printable.add(Arrays.toString(objectArray));
                } else {
                    printable.add(Objects.toString(item));
                }
            }
            printableSample = printable;
        } else if (sampleArgs instanceof Object[] objectArray) {
            printableSample = Arrays.toString(objectArray);
        }
        log.info(LOG_BATCH_FMT, singleLine(sql), batchSize, printableSample);
    }
}
