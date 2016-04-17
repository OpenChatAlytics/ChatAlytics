package com.chatalytics.web.utils;

import com.google.common.base.Preconditions;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Utility class for dealing with {@link DateTime} objects and string parameters
 *
 * @author giannis
 *
 */
public class DateTimeUtils {

    private static final String PARAMETER_WITH_DAY_DTF_STR = "YYYY-MM-dd";
    public static final DateTimeFormatter PARAMETER_WITH_DAY_DTF =
        DateTimeFormat.forPattern(PARAMETER_WITH_DAY_DTF_STR).withZoneUTC();
    private static final String PARAMETER_WITH_HOUR_DTF_STR = "YYYY-MM-dd_HH";
    public static final DateTimeFormatter PARAMETER_WITH_HOUR_DTF =
        DateTimeFormat.forPattern(PARAMETER_WITH_HOUR_DTF_STR).withZoneUTC();

    /**
     * Helper method that parses a date time string and returns an actual {@link DateTime} object.
     *
     * @param dateTimeStr
     *            The string parameter to parse. It has to be in one of the following supported
     *            formats:
     *            <ol>
     *            <li>{@value #PARAMETER_WITH_DAY_DTF_STR}</li>
     *            <li>{@value #PARAMETER_WITH_HOUR_DTF_STR}</li>
     *            </ol>
     * @return A {@link DateTime} object
     */
    public static DateTime getDateTimeFromParameter(String dateTimeStr, DateTimeZone dtZone) {
        Preconditions.checkNotNull(dateTimeStr,
                                   "Both start and end time date parameters cannot be null");
        Preconditions.checkArgument(dateTimeStr.length() == PARAMETER_WITH_DAY_DTF_STR.length() ||
            dateTimeStr.length() == PARAMETER_WITH_HOUR_DTF_STR.length(),
                                    String.format("Time parameters have to be of the form %s or %s",
                                                  PARAMETER_WITH_DAY_DTF_STR,
                                                  PARAMETER_WITH_HOUR_DTF_STR));

        // fix time zone based on config and parse date
        DateTimeFormatter zoneAdjustedDtf;
        if (dateTimeStr.length() == PARAMETER_WITH_DAY_DTF_STR.length()) {
            zoneAdjustedDtf = PARAMETER_WITH_DAY_DTF.withZone(dtZone);
        } else {
            zoneAdjustedDtf = PARAMETER_WITH_HOUR_DTF.withZone(dtZone);
        }

        // convert to UTC since all the dates in the DB are stored with UTC
        return zoneAdjustedDtf.parseDateTime(dateTimeStr).toDateTime(DateTimeZone.UTC);
    }

}
