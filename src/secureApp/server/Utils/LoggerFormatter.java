package secureApp.server.Utils;

import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LoggerFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        return new SimpleDateFormat("[dd/MM/yyyy - HH:mm:ss]").format(record.getMillis()) + ": " + record.getMessage() + "\n";
    }
}
