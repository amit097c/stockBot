package com.stock.util;

public class TimeUtils {
    
}
package com.stock.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TimeUtils {

    public static Map<String, String> getTimeWindow() {
        ZoneId eastern = ZoneId.of("America/New_York");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        List<ZonedDateTime> marketDays = new ArrayList<>();
        ZonedDateTime current = ZonedDateTime.now(eastern).withHour(16).withMinute(0).withSecond(0).withNano(0);

        while (current.getDayOfWeek() == DayOfWeek.SATURDAY || current.getDayOfWeek() == DayOfWeek.SUNDAY) {
            current = current.minusDays(1);
        }

        while (marketDays.size() < 7) {
            if (current.getDayOfWeek().getValue() <= 5) {
                marketDays.add(current);
            }
            current = current.minusDays(1);
        }

        ZonedDateTime startET = marketDays.get(6).withHour(9).withMinute(30);
        ZonedDateTime endET = marketDays.get(0).withHour(16);

        ZonedDateTime startUTC = startET.withZoneSameInstant(ZoneOffset.UTC);
        ZonedDateTime endUTC = endET.withZoneSameInstant(ZoneOffset.UTC);

        Map<String, String> result = new HashMap<>();
        result.put("start", formatter.format(startUTC));
        result.put("end", formatter.format(endUTC));
        return result;
    }

    public static String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }
}
