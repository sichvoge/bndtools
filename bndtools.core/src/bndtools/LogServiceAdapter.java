package bndtools;

import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

import bndtools.api.ILogger;

public class LogServiceAdapter implements LogService {

    private final ILogger delegate;

    public LogServiceAdapter(ILogger delegate) {
        this.delegate = delegate;
    }

    public void log(int level, String message) {
        log(null, level, message, null);
    }

    public void log(int level, String message, Throwable exception) {
        log(null, level, message, exception);
    }

    public void log(ServiceReference sr, int level, String message) {
        log(sr, level, message, null);
    }

    public void log(ServiceReference sr, int level, String message, Throwable exception) {
        switch (level) {
        case LogService.LOG_ERROR :
            delegate.logError(message, exception);
            break;
        case LogService.LOG_WARNING :
            delegate.logWarning(message, exception);
            break;
        case LogService.LOG_INFO :
            delegate.logInfo(message, exception);
            break;
        }
    }

}